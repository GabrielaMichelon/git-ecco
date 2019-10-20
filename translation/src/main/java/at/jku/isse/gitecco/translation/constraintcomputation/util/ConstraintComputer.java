package at.jku.isse.gitecco.translation.constraintcomputation.util;


import at.jku.isse.gitecco.core.solver.ExpressionSolver;
import at.jku.isse.gitecco.core.tree.nodes.BaseNode;
import at.jku.isse.gitecco.core.tree.nodes.ConditionalNode;
import at.jku.isse.gitecco.core.tree.nodes.RootNode;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.core.type.FeatureImplication;
import at.jku.isse.gitecco.translation.visitor.BuildImplicationsVisitor;

import java.util.*;
import java.util.stream.Collectors;

public class ConstraintComputer {

    private final List<String> featureList;

    public ConstraintComputer(List<String> featureList) {
        this.featureList = featureList;
    }

    /**
     * Computes and returns the configuration for a given changed Node.
     * Also filters the solution for only global features.
     * @param changedNode
     * @param tree
     * @return
     */
    public Map<Feature, Integer> computeConfig (ConditionalNode changedNode, RootNode tree) {
        //if the changed node is a base node just return BASE as solution
        if(changedNode instanceof BaseNode) {
            Map<Feature, Integer> ret = new HashMap<>();
            ret.put(new Feature("BASE"), 1);
            return ret;
        }

        ExpressionSolver solver = new ExpressionSolver();
        Map<Feature, Queue<FeatureImplication>> implMap = new HashMap<>();

        //setup and build constraint queues
        BuildImplicationsVisitor visitor = new BuildImplicationsVisitor(implMap, tree, changedNode.getLineFrom());
        changedNode.getContainingFile().accept(visitor);

        //hand the expression of the condition for the changed node to the solver.
        solver.setExpr(changedNode.getCondition());

        if(changedNode.getCondition().equals("(Y_MAX_PIN > -1) && (BASE)")){
            System.out.println("SIZE: "+implMap.size()+" NODE: "+changedNode.getCondition());
        }else {
            //add the built constraint queues to the solver which further constructs all the internal constraints
            for (Map.Entry<Feature, Queue<FeatureImplication>> featureQueueEntry : implMap.entrySet()) {
                solver.addClause(featureQueueEntry.getKey(), featureQueueEntry.getValue());
            }
        }
        //solve, filter for global features only and return the solution/configuration.
        //filtering should be optional because if a correct constraint is built we already get only global features.
        Map<Feature, Integer> ret = solver.solve();
        if(ret != null) {
            ret = ret
                    .entrySet()
                    .stream()
                    .filter(entry -> featureList.contains(entry.getKey().getName()))
                    .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
        } else {
            System.err.println("DEAD CODE: No solution found for "
                    + changedNode.getLocalCondition()
                    + " @ " + changedNode.getLineFrom()
                    + " in " + changedNode.getContainingFile().getFilePath());
        }

        return ret;
    }

    /**
     * Computes which features should be marked as changed.
     *
     * This can and will be replaced with a set of different
     * heuristics to achieve the most convenient and fitting result.
     * @param changedNode
     * @return
     */
    public Set<Feature> computeChangedFeatures (ConditionalNode changedNode, Map<Feature, Integer> config) {
        //if the changed node is a base node just return BASE as solution
        if(changedNode instanceof BaseNode) {
            Set<Feature> ret = new HashSet<>();
            ret.add(new Feature("BASE"));
            return ret;
        }

        ExpressionSolver solver = new ExpressionSolver();
        boolean repeat = true;
        Set<Feature> ret = null;

        StringBuilder configString = new StringBuilder();
        config.entrySet().forEach(x -> configString.append(" || ( " + x.getKey().getName() + "==" + x.getValue() + " )"));
        String configClause = configString.toString().replaceFirst("\\|\\|", "");
        configClause = " && ( " + configClause + ")";

        while((changedNode != null || !(changedNode instanceof BaseNode)) && repeat) {
            solver.setExpr(changedNode.getLocalCondition() + configClause);
            Map<Feature, Integer> result = solver.solve();
            if(result != null) {
                ret = result
                        .entrySet()
                        .stream()
                        .filter(entry -> featureList.contains(entry.getKey().getName()) && entry.getValue()!=0)
                        .map(entry -> entry.getKey())
                        .collect(Collectors.toSet());

                repeat = ret.size() < 1 ? true : false;
            } else {
                repeat = true;
            }
            if(repeat) changedNode = changedNode.getParent().getParent();
        }

        if(ret == null || repeat) {
            ret = new HashSet<>();
            ret.add(new Feature("BASE"));
        }

        return ret;
    }
}


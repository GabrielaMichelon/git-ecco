package at.jku.isse.gitecco.translation.constraintcomputation.util;

import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.solver.ExpressionSolver;
import at.jku.isse.gitecco.core.tree.nodes.BaseNode;
import at.jku.isse.gitecco.core.tree.nodes.ConditionalNode;
import at.jku.isse.gitecco.core.tree.nodes.RootNode;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.core.type.FeatureImplication;
import at.jku.isse.gitecco.translation.visitor.BuildImplicationsVisitor;

import java.util.*;
import java.util.stream.Collectors;

//changed:
// - used visitor pattern correctly
// - removed unused stuff
// - includes were not collected recursively
// - getLocalCondition in IFNDEF node --> return node.getparnet etc. ? why? --> changed to just return cond.
// - removed weird stuff at the bottom
// - now: traversing the tree,

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
        ExpressionSolver solver = new ExpressionSolver();
        Map<Feature, Queue<FeatureImplication>> implMap = new HashMap<>();

        //setup and build constraint queues
        BuildImplicationsVisitor visitor = new BuildImplicationsVisitor(implMap, tree, changedNode.getLineFrom());
        changedNode.getContainingFile().accept(visitor);

        //hand the expression of the condition for the changed node to the solver.
        solver.setExpr(changedNode.getCondition());

        //add the built constraint queues to the solver which further constructs all the internal constraints
        for (Map.Entry<Feature, Queue<FeatureImplication>> featureQueueEntry : implMap.entrySet()) {
            solver.addClause(featureQueueEntry.getKey(), featureQueueEntry.getValue());
        }

        //solve, filter for global features only and return the solution/configuration.
        //filtering should be optional because if a correct constraint is built we already get only global features.
        return solver.solve()
                .entrySet()
                .stream()
                .filter(entry -> featureList.contains(entry.getKey()))
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
    }

    /**
     * Computes which features should be marked as changed.
     *
     * This can and will be replaced with a set of different
     * heuristics to achieve the most convenient and fitting result.
     * @param changedNode
     * @return
     */
    public Set<Feature> computeChangedFeatures (ConditionalNode changedNode) {
        ExpressionSolver solver = new ExpressionSolver();
        boolean repeat = false;
        Set<Feature> ret = null;

        while(!(changedNode instanceof BaseNode) && repeat) {
            solver.setExpr(changedNode.getLocalCondition());
            ret = solver.solve()
                    .entrySet()
                    .stream()
                    .filter(entry -> featureList.contains(entry.getKey()) && entry.getValue()!=0)
                    .map(entry -> entry.getKey())
                    .collect(Collectors.toSet());

            repeat = ret.size() < 1 ? true : false;
            if(repeat) changedNode = changedNode.getParent().getParent();
        }

        if(ret == null || repeat) {
            ret = new HashSet<>();
            ret.add(new Feature("BASE"));
        }

        return ret;
    }
}


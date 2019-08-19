package at.jku.isse.gitecco.translation.constraintcomputation.util;

import at.jku.isse.gitecco.core.git.Change;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.solver.ExpressionSolver;
import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.core.type.FeatureImplication;
import org.apache.commons.math3.analysis.function.Exp;
import org.chocosolver.solver.constraints.nary.cnf.LogOp;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.io.File;
import java.security.Key;
import java.util.*;

public class ChangeConstraint {

    private ArrayList<String> featureList;

    public ArrayList<String> getFeatureList() {
        return featureList;
    }

    public void setFeatureList(ArrayList<String> featureList) {
        this.featureList = featureList;
    }

    public void constructConstraintPerFeature(ArrayList<ConditionalNode> classUntilChange, Set<ConditionalNode> changedNodes, GitHelper gitHelper, Change change, GetNodesForChangeVisitor visitor, FileNode child) {
        Feature feature = null;
        ExpressionSolver solver = new ExpressionSolver();
        PreprocessorHelper pph = new PreprocessorHelper();
        final File gitFolder = new File(gitHelper.getPath());
        final File eccoFolder = new File(gitFolder.getParent(), "ecco");
        Queue<FeatureImplication> featureImplications = new LinkedList<>();
        Queue<FeatureImplication> featureImplicationsAux = new LinkedList<>();
        FeatureImplication featureImplication;
        String elsePartFeature = null;

        for (ConditionalNode changedNode : changedNodes) {
            ArrayList<String> featureChangedNode = new ArrayList<>();
            if (!changedNode.getCondition().contains("BASE")) {
                ConditionalNode changedNodeParent = changedNode.getParent().getIfBlock().getParent().getParent();
                ConditionalNode conditionalNode = changedNode.getParent().getParent();
                String expression = "";
                expression = changedNode.getCondition();
                //adding the expression of the changedNode that solver will use to give a solution
                //concatenate the expression with each parent of its changed node until the parent be BASE
                while (!conditionalNode.getCondition().contains("BASE")) {
                    expression += " && " + conditionalNode.getCondition();
                    conditionalNode = conditionalNode.getParent().getParent();
                }
                expression += " && " + conditionalNode.getCondition();
                solver.setExpr(expression);
                //obtains the literals that we need to search its parents and defines to add to the queue implication
                featureChangedNode = searchingFeatureCorrespondingtoChangedNode(changedNode, featureList, expression);


                //adding clauses to the defines children of BASE that are above the changed node and that are not macro functions
                //and that has the featureChangedNode literals
                for (DefineNode defineNode : changedNodeParent.getDefineNodes()) {
                    if (defineNode instanceof Define && defineNode.getLineInfo() < changedNode.getLineFrom()) {
                        if (!(((Define) defineNode).getMacroExpansion().contains("(") && ((Define) defineNode).getMacroExpansion().contains(")"))) {//not add macro function
                            //just add if it is a featureChangedNode literal, i.e., any literal that is on the expression
                            if (featureChangedNode.contains(defineNode.getMacroName())) {
                                featureImplication = new FeatureImplication(changedNodeParent.getCondition(), defineNode.getMacroName() + ((Define) defineNode).getMacroExpansion());
                                featureImplications.add(featureImplication);
                                feature = new Feature(defineNode.getMacroName()); //feature is the variable/literal of the define or undef
                                //if the feature is just a literal that is not a feature we need to add the else part that means this literal is false or 0 when the if is not satisfied
                                if (!featureList.contains(feature)) {
                                    elsePartFeature = feature + "==0";
                                }
                                solver.addClause(feature, featureImplications, elsePartFeature);
                                for (int i = 0; i < solver.getVars().size(); i++) {
                                    if (!(featureChangedNode.contains(solver.getVars().get(i).getName()))) {
                                        featureChangedNode.add(solver.getVars().get(i).getName());
                                    }
                                }
                                elsePartFeature = null;
                            }
                        }
                        //adding undef
                    } else if (defineNode instanceof Undef && featureChangedNode.contains(defineNode.getMacroName()) && defineNode.getLineInfo() < changedNode.getLineFrom()) {
                        featureImplication = new FeatureImplication(changedNodeParent.getCondition(), defineNode.getMacroName() + "==0");
                        featureImplications.add(featureImplication);
                        feature = new Feature(defineNode.getMacroName());  //feature is the variable/literal of the define or undef
                        elsePartFeature = null;
                        solver.addClause(feature, featureImplications, elsePartFeature);
                        for (int i = 0; i < solver.getVars().size(); i++) {
                            if (!(featureChangedNode.contains(solver.getVars().get(i).getName()))) {
                                featureChangedNode.add(solver.getVars().get(i).getName());
                            }
                        }
                    }
                }

                //adding clauses for each BASE children Nodes that has defines above the changedNode that has literal which implies on the changedNode
                //for (ConditionBlockNode children : changedNodeParent.getChildren()) {
                for (int i = changedNodeParent.getChildren().size() - 1; i >= 0; i--) {
                    if (changedNodeParent.getChildren().get(i).getIfBlock().getLineFrom() < changedNode.getLineFrom()) {
                        searchingDefineNodes(changedNodeParent.getChildren().get(i), solver, featureImplications, changedNodeParent.getCondition(), changedNode.getLineFrom(), featureChangedNode);
                    }
                }


                //after added all clauses the solver returns a solution
                Map<Feature, Integer> result = solver.solve();
                solver.reset();
                pph.generateVariants(result, gitFolder, eccoFolder);
                System.out.println("\nBlock: " + changedNode.getLocalCondition() + " has change!");
                System.out.println("Solution ChocoSolver: ");
                result.entrySet().forEach(x -> System.out.print(x.getKey() + " = " + x.getValue() + "; "));
                System.out.println("\nConfiguration that has impact on this change:");
                Feature key;
                Integer value;
                Set<Map.Entry<Feature, Integer>> setRetornado = result.entrySet();
                for (Map.Entry<Feature, Integer> literalsValues : setRetornado) {
                    key = literalsValues.getKey();
                    value = literalsValues.getValue();
                    if (value >= 1 && (featureList.contains(key.toString()) || key.toString().contains("BASE")))
                        System.out.println("Feature " + key);

                }
                System.out.println("________________________");

            }
        }
    }

    //we need to look if the changedNode is a feature and look if exists any parent besides BASE
    public ArrayList<String> searchingFeatureCorrespondingtoChangedNode(ConditionalNode changedNode, ArrayList<String> featureList, String expression) {
        ArrayList<String> features = new ArrayList<>();
        String[] literals;
        ExpressionSolver ex = new ExpressionSolver();
        BoolVar boolVar = ex.getBoolVarFromExpr(expression);
        for (int i = 0; i < ex.getVars().size(); i++) {
            if (expression.contains(ex.getVars().get(i).getName())) {
                features.add(ex.getVars().get(i).getName());
            }
        }

        if (changedNode.getLocalCondition().contains("&&")) {
            literals = changedNode.getLocalCondition().split("&&");
            for (int i = 0; i < literals.length; i++) {
                if (literals[i].contains("!") && !(features.contains(literals[i].replace("!", ""))))
                    literals[i] = literals[i].replace("!", "");
                if (featureList.contains(literals[i]))
                    features.add(literals[i]);
            }
        } else if (!(features.contains(changedNode.getLocalCondition()))) {
            features.add(changedNode.getLocalCondition());
        }

        return features;
    }


    public void searchingDefineNodes(ConditionBlockNode children, ExpressionSolver solver, Queue<FeatureImplication> featureImplications, String changedNodeParent, Integer changedNodeLineFrom, ArrayList<String> featureChangedNode) {
        FeatureImplication featureImplication;
        Feature feature = null;
        String elsePartFeature = null;

        if (children.getIfBlock().getChildren().size() > 0) {
            //first we search for its definedNodes, and if exists we add if the definedNode.lineInfo() < changedNode.getLineFrom(), if not we look around its children
            if (children.getIfBlock().getDefineNodes().size() > 0) {
                for (DefineNode definedNode : children.getIfBlock().getDefineNodes()) {
                    if (definedNode instanceof Define && definedNode.getLineInfo() < changedNodeLineFrom && featureChangedNode.contains(definedNode.getMacroName())) {
                        //no add macro functions
                        if (!(((Define) definedNode).getMacroExpansion().contains("(") && ((Define) definedNode).getMacroExpansion().contains(")"))) {//not add macro function
                            if (!children.getIfBlock().getParent().getParent().getCondition().contains("BASE")) {
                                featureImplication = new FeatureImplication(changedNodeParent + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName() + ((Define) definedNode).getMacroExpansion());
                                featureImplications.add(featureImplication);
                                feature = new Feature(definedNode.getMacroName()); //feature is the variable of the define or undef
                                if (!featureList.contains(feature)) {
                                    elsePartFeature = feature + "==0";
                                } else {
                                    elsePartFeature = null;
                                }
                                solver.addClause(feature, featureImplications, elsePartFeature);
                                for (int i = 0; i < solver.getVars().size(); i++) {
                                    if (!(featureChangedNode.contains(solver.getVars().get(i).getName()))) {
                                        featureChangedNode.add(solver.getVars().get(i).getName());
                                    }
                                }
                            }
                        }
                    } else if (definedNode.getLineInfo() < changedNodeLineFrom && featureChangedNode.contains(definedNode.getMacroName())) { //undef
                        if (!children.getIfBlock().getParent().getParent().getCondition().contains("BASE")) {
                            featureImplication = new FeatureImplication(changedNodeParent + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName() + "==0");
                            featureImplications.add(featureImplication);
                            feature = new Feature(definedNode.getMacroName());
                            elsePartFeature = null;
                            solver.addClause(feature, featureImplications, elsePartFeature);
                            for (int i = 0; i < solver.getVars().size(); i++) {
                                if (!(featureChangedNode.contains(solver.getVars().get(i).getName()))) {
                                    featureChangedNode.add(solver.getVars().get(i).getName());
                                }
                            }
                        }
                    }

                }
            }
            for (ConditionBlockNode childrenChildren : children.getIfBlock().getChildren()) {
                searchingDefineNodes(childrenChildren, solver, featureImplications, changedNodeParent, changedNodeLineFrom, featureChangedNode);
            }
        } else {
            if (children.getIfBlock().getDefineNodes().size() > 0) {
                for (DefineNode definedNode : children.getIfBlock().getDefineNodes()) {
                    if (definedNode instanceof Define && definedNode.getLineInfo() < changedNodeLineFrom && featureChangedNode.contains(definedNode.getMacroName())) {
                        //no add macro functions
                        if (!(((Define) definedNode).getMacroExpansion().contains("(") && ((Define) definedNode).getMacroExpansion().contains(")"))) {//not add macro function
                            if (!children.getIfBlock().getParent().getParent().getCondition().contains("BASE")) {
                                featureImplication = new FeatureImplication(changedNodeParent + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName() + ((Define) definedNode).getMacroExpansion());
                                feature = new Feature(definedNode.getMacroName());
                                featureImplications.add(featureImplication);
                                if (!featureList.contains(feature)) {
                                    elsePartFeature = feature + "==0";
                                } else {
                                    elsePartFeature = null;
                                }
                                solver.addClause(feature, featureImplications, elsePartFeature);
                                for (int i = 0; i < solver.getVars().size(); i++) {
                                    if (!(featureChangedNode.contains(solver.getVars().get(i).getName()))) {
                                        featureChangedNode.add(solver.getVars().get(i).getName());
                                    }
                                }
                            } else {
                                featureImplication = new FeatureImplication(changedNodeParent + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName() + ((Define) definedNode).getMacroExpansion());
                                featureImplications.add(featureImplication);
                                feature = new Feature(definedNode.getMacroName());
                                if (!featureList.contains(feature)) {
                                    elsePartFeature = feature + "==0";
                                } else {
                                    elsePartFeature = null;
                                }
                                solver.addClause(feature, featureImplications, elsePartFeature);
                                for (int i = 0; i < solver.getVars().size(); i++) {
                                    if (!(featureChangedNode.contains(solver.getVars().get(i).getName()))) {
                                        featureChangedNode.add(solver.getVars().get(i).getName());
                                    }
                                }
                            }
                        }
                    } else if (definedNode.getLineInfo() < changedNodeLineFrom && featureChangedNode.contains(definedNode.getMacroName())) { //undef
                        if (!children.getIfBlock().getParent().getParent().getCondition().contains("BASE")) {
                            featureImplication = new FeatureImplication(changedNodeParent + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName() + "==0");
                            featureImplications.add(featureImplication);
                            feature = new Feature(definedNode.getMacroName());
                            elsePartFeature = null;
                            solver.addClause(feature, featureImplications, elsePartFeature);
                            for (int i = 0; i < solver.getVars().size(); i++) {
                                if (!(featureChangedNode.contains(solver.getVars().get(i).getName()))) {
                                    featureChangedNode.add(solver.getVars().get(i).getName());
                                }
                            }
                        }
                    }

                }
            }
        }
    }


}

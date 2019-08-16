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
                IFCondition changedNodeParent = (IFCondition) changedNode.getParent().getIfBlock().getParent().getParent();
                ConditionalNode conditionalNode = changedNode.getParent().getParent();
                String expression = "";
                if (changedNode.getCondition().contains("!")) {
                    expression = changedNode.getCondition().replace("!", "") + "==0";
                } else {
                    expression = changedNode.getCondition() + "==1";
                }
                //adding the expression of the changedNode that solver will use to give a solution
                //concatenate the expression with each parent of its changed node until the parent be BASE
                while (!conditionalNode.getCondition().contains("BASE")) {
                    if (conditionalNode instanceof IFCondition)
                        if (changedNode.getCondition().contains("!")) {
                            expression += " && " + conditionalNode.getCondition().replace("!", "") + "==0";
                        } else {
                            expression += " && " + conditionalNode.getCondition() + "==1";
                        }
                    else if (conditionalNode instanceof ELSECondition)
                        if (changedNode.getCondition().contains("!")) {
                            expression += " && " + conditionalNode.getCondition().replace("!", "") + "==0";
                        } else {
                            expression += " && " + conditionalNode.getCondition() + "==1";
                        }
                    else if (conditionalNode instanceof ELIFCondition) {
                        if (changedNode.getCondition().contains("!")) {
                            expression += " && " + conditionalNode.getCondition().replace("!", "") + "==0";
                        } else {
                            expression += " && " + conditionalNode.getCondition() + "==1";
                        }
                    }
                    conditionalNode = conditionalNode.getParent().getParent();
                }
                expression += " && " + conditionalNode.getCondition() + "==1";
                solver.setExpr(expression);
                featureChangedNode = searchingFeatureCorrespondingtoChangedNode(changedNode, featureList, expression);


                //adding clauses to the defines children of BASE that are above the changed node and that are not macro functions
                //and that has the featureChangedNode literals
                for (DefineNode defineNode : changedNodeParent.getDefineNodes()) {
                    if (defineNode instanceof Define && defineNode.getLineInfo() < changedNode.getLineFrom()) {
                        if (!(((Define) defineNode).getMacroExpansion().contains("(") && ((Define) defineNode).getMacroExpansion().contains(")"))) {//not add macro function
                            //just add if it is a featureChangedNode literal, i.e., any literal of the expression
                            if (featureChangedNode.contains(defineNode.getMacroName())) {
                                if (!(((Define) defineNode).getMacroExpansion().equals(""))) {
                                    //if it has no macro expansion we know that it will be anything other than zero (because jcpp just treat 0 as false)
                                    featureImplication = new FeatureImplication(changedNodeParent.getCondition() + "==1", defineNode.getMacroName() + "==" + ((Define) defineNode).getMacroExpansion());
                                } else {
                                    //we need to add 1 because we need to add the literal of define as integer to the solver variants (can be any integer excluding 0)
                                    featureImplication = new FeatureImplication(changedNodeParent.getCondition() + "==1", defineNode.getMacroName() + "==1");
                                }

                                featureImplications.add(featureImplication);
                                feature = new Feature(defineNode.getMacroName()); //feature is the variable of the define or undef
                                if (!featureList.contains(feature)) {
                                /*ExpressionSolver expressionSolver = new ExpressionSolver();
                                BoolVar typeOfVar = expressionSolver.getBoolVarFromExpr(feature.getName());
                                if(typeOfVar instanceof BoolVar) {
                                    featureImplication = new FeatureImplication(changedNodeParent.getCondition(), "!" + feature);
                                    featureImplications.add(featureImplication);
                                    elsePartFeature = "!" + feature;

                                }else{*/
                                    elsePartFeature = feature + "==0";
                                    //}
                                }
                                solver.addClause(feature, featureImplications, elsePartFeature);
                                elsePartFeature = null;
                            }
                        }
                    } else if (defineNode instanceof Undef && featureChangedNode.contains(defineNode.getMacroName()) && defineNode.getLineInfo() < changedNode.getLineFrom()) { //undef
                        featureImplication = new FeatureImplication(changedNodeParent.getCondition() + "==1", defineNode.getMacroName() + "==0");
                        featureImplications.add(featureImplication);
                        feature = new Feature(defineNode.getMacroName()); //feature is the variable of the define or undef
                        elsePartFeature = null;
                        solver.addClause(feature, featureImplications, elsePartFeature);
                    }
                }

                //adding clauses for each BASE children Nodes that has defines above the changedNode
                for (ConditionBlockNode children : changedNodeParent.getChildren()) {
                    /*if (children.getIfBlock().getDefineNodes().size() > 0) {
                        for (DefineNode definedNode : children.getIfBlock().getDefineNodes()) {
                            if (definedNode instanceof Define && definedNode.getLineInfo() < changedNode.getLineFrom()) {
                                if (!(((Define) definedNode).getMacroExpansion().contains("(") && ((Define) definedNode).getMacroExpansion().contains(")"))) {//not add macro function
                                    if (!(((Define) definedNode).getMacroExpansion().equals(""))) {
                                        featureImplication = new FeatureImplication(changedNodeParent.getCondition() + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName() + "==" + ((Define) definedNode).getMacroExpansion());
                                    } else {
                                        featureImplication = new FeatureImplication(changedNodeParent.getCondition() + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName());
                                    }
                                    featureImplications.add(featureImplication);
                                    feature = new Feature(definedNode.getMacroName()); //feature is the variable of the define or undef
                                    if(!featureList.contains(feature)){
                                        ExpressionSolver expressionSolver = new ExpressionSolver();
                                        BoolVar typeOfVar = expressionSolver.getBoolVarFromExpr(feature.getName());
                                        if(typeOfVar instanceof BoolVar) {
                                            //featureImplication = new FeatureImplication(changedNodeParent.getCondition(), "!" + feature);
                                            //featureImplications.add(featureImplication);
                                            elsePartFeature = "!" + feature;
                                        }else{
                                            elsePartFeature = null;
                                        }
                                    }
                                    solver.addClause(feature, featureImplications,elsePartFeature);
                                    elsePartFeature = null;
                                }
                            } else if (definedNode instanceof Undef && definedNode.getLineInfo() < changedNode.getLineFrom()) { //undef
                                featureImplication = new FeatureImplication(changedNodeParent.getCondition() + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName());
                                featureImplications.add(featureImplication);
                                feature = new Feature(definedNode.getMacroName()); //feature is the variable of the define or undef
                                if(!featureList.contains(feature)){
                                    ExpressionSolver expressionSolver = new ExpressionSolver();
                                    BoolVar typeOfVar = expressionSolver.getBoolVarFromExpr(feature.getName());
                                    if(typeOfVar instanceof BoolVar) {
                                        //featureImplication = new FeatureImplication(changedNodeParent.getCondition(), "!" + feature);
                                        //featureImplications.add(featureImplication);
                                        elsePartFeature = "!" + feature;
                                    }else{
                                        elsePartFeature = null;
                                    }
                                }
                                solver.addClause(feature, featureImplications, elsePartFeature);
                                elsePartFeature = null;
                            }

                        }
                    }*/
                    //search and add defined nodes above the changed node
                    if (children.getIfBlock().getLineFrom() < changedNode.getLineFrom()) {
                        searchingDefineNodes(children, solver, featureImplications, changedNodeParent.getCondition(), changedNode.getLineFrom(), featureChangedNode);
                    }
                }


                //after added all clauses the solver returns a solution
                Map<Feature, Integer> result = solver.solve();
                solver.reset();
                pph.generateVariants(result, gitFolder, eccoFolder);
                System.out.println("\nBlock: " + changedNode.getCondition() + " has change!");
                System.out.println("Solution ChocoSolver: ");
                result.entrySet().forEach(x -> System.out.print(x.getKey() + " = " + x.getValue() + "; "));
                System.out.println("\nConfiguration that has impact on this change: ");
                for (int j = 0; j < featureList.size(); j++) {
                    Feature key = new Feature(featureList.get(j));
                    if (result.containsKey(key)) {
                        System.out.println("Feature: " + featureList.get(j));
                    }
                }

                //System.out.println("Features with new version: ");
                //for (int i = 0; i < featureChangedNode.size(); i++) {
                //    System.out.println(featureChangedNode.get(i));
                //}
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

        if (changedNode.getCondition().contains("&&")) {
            literals = changedNode.getCondition().split("&&");
            for (int i = 0; i < literals.length; i++) {
                if (literals[i].contains("!") && !(features.contains(literals[i].replace("!", ""))))
                    literals[i] = literals[i].replace("!", "");
                //if (featureList.contains(literals[i])) {
                features.add(literals[i]);
                //}
            }
        } else if (changedNode.getCondition().contains("!") && !(features.contains(changedNode.getCondition()))) {
            //if (featureList.contains(changedNode.getCondition().replace("!", ""))) {
            features.add(changedNode.getCondition());
            //}
        }

        return features;
    }


    public void searchingDefineNodes(ConditionBlockNode children, ExpressionSolver solver, Queue<FeatureImplication> featureImplications, String changedNodeParent, Integer changedNodeLineFrom, ArrayList<String> featureChangedNode) {
        FeatureImplication featureImplication;
        Feature feature = null;
        String elsePartFeature = null;

        if (children.getIfBlock().getChildren().size() > 0) {
            //first we search for its definedNodes, and if exists we add if the definedNode line is < than changedNode.getLineFrom(), if not we look around its children
            if (children.getIfBlock().getDefineNodes().size() > 0) {
                for (DefineNode definedNode : children.getIfBlock().getDefineNodes()) {
                    if (definedNode instanceof Define && definedNode.getLineInfo() < changedNodeLineFrom && featureChangedNode.contains(definedNode.getMacroName())) {
                        //no add macro functions
                        if (!(((Define) definedNode).getMacroExpansion().contains("(") && ((Define) definedNode).getMacroExpansion().contains(")"))) {//not add macro function
                            if (!children.getIfBlock().getParent().getParent().getCondition().contains("BASE")) {
                                if (!(((Define) definedNode).getMacroExpansion().equals("")))
                                    featureImplication = new FeatureImplication(changedNodeParent + "==1" + " && " + children.getIfBlock().getParent().getParent().getCondition() + "==1" + " && " + children.getIfBlock().getCondition() + "==1", definedNode.getMacroName() + "==" + ((Define) definedNode).getMacroExpansion());
                                else
                                    featureImplication = new FeatureImplication(changedNodeParent + "==1" + " && " + children.getIfBlock().getParent().getParent().getCondition() + "==1" + " && " + children.getIfBlock().getCondition() + "==1", definedNode.getMacroName() + "==1");
                                featureImplications.add(featureImplication);
                                feature = new Feature(definedNode.getMacroName()); //feature is the variable of the define or undef
                                if (!featureList.contains(feature)) {
                                    //ExpressionSolver expressionSolver = new ExpressionSolver();
                                    //BoolVar typeOfVar = expressionSolver.getBoolVarFromExpr(feature.getName());
                                    //if(typeOfVar instanceof BoolVar) {
                                    //featureImplication = new FeatureImplication(changedNodeParent, "!" + feature);
                                    //featureImplications.add(featureImplication);
                                    elsePartFeature = feature + "==0";
                                    //
                                } else {
                                    elsePartFeature = null;
                                }
                                solver.addClause(feature, featureImplications, elsePartFeature);
                                elsePartFeature = null;
                            } /*else {
                                if (!(((Define) definedNode).getMacroExpansion().equals("")))
                                    featureImplication = new FeatureImplication(changedNodeParent + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName() + "==" + ((Define) definedNode).getMacroExpansion());
                                else
                                    featureImplication = new FeatureImplication(changedNodeParent + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName());
                                featureImplications.add(featureImplication);
                                feature = new Feature(definedNode.getMacroName()); //feature is the variable of the define or undef
                                if (!featureList.contains(feature)) {
                                    ExpressionSolver expressionSolver = new ExpressionSolver();
                                    BoolVar typeOfVar = expressionSolver.getBoolVarFromExpr(feature.getName());
                                    if (typeOfVar instanceof BoolVar) {
                                        //featureImplication = new FeatureImplication(changedNodeParent, "!" + feature);
                                        //featureImplications.add(featureImplication);
                                        elsePartFeature = "!" + feature;
                                    } else {
                                        elsePartFeature = null;
                                    }
                                }
                                solver.addClause(feature, featureImplications, elsePartFeature);
                                elsePartFeature = null;
                            }*/
                        }
                    } else if (definedNode.getLineInfo() < changedNodeLineFrom && featureChangedNode.contains(definedNode.getMacroName())) { //undef
                        if (!children.getIfBlock().getParent().getParent().getCondition().contains("BASE")) {
                            featureImplication = new FeatureImplication(changedNodeParent + "==1" + " && " + children.getIfBlock().getParent().getParent().getCondition() + "==1" + " && " + children.getIfBlock().getCondition() + "==1", definedNode.getMacroName() + "==0");
                            //else
                            //  featureImplication = new FeatureImplication(changedNodeParent + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName());
                            featureImplications.add(featureImplication);
                            feature = new Feature(definedNode.getMacroName()); //feature is the variable of the define or undef
                            elsePartFeature = null;
                            solver.addClause(feature, featureImplications, elsePartFeature);
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
                                if (!(((Define) definedNode).getMacroExpansion().equals("")))
                                    featureImplication = new FeatureImplication(changedNodeParent + "==1" + " && " + children.getIfBlock().getParent().getParent().getCondition() + "==1" + " && " + children.getIfBlock().getCondition() + "==1", definedNode.getMacroName() + "==" + ((Define) definedNode).getMacroExpansion());
                                else
                                    featureImplication = new FeatureImplication(changedNodeParent + "==1" + " && " + children.getIfBlock().getParent().getParent().getCondition() + "==1" + " && " + children.getIfBlock().getCondition() + "==1", definedNode.getMacroName() + "==1");
                                feature = new Feature(definedNode.getMacroName());
                                featureImplications.add(featureImplication);
                                if (!featureList.contains(feature)) {
                                    //ExpressionSolver expressionSolver = new ExpressionSolver();
                                    //BoolVar typeOfVar = expressionSolver.getBoolVarFromExpr(feature.getName());
                                    //if(typeOfVar instanceof BoolVar) {
                                    //featureImplication = new FeatureImplication(changedNodeParent, "!" + feature);
                                    //featureImplications.add(featureImplication);
                                    elsePartFeature = feature + "==0";
                                    //
                                } else {
                                    elsePartFeature = null;
                                }
                                solver.addClause(feature, featureImplications, elsePartFeature);
                                elsePartFeature = null;
                            } else {
                                if (!(((Define) definedNode).getMacroExpansion().equals("")))
                                    featureImplication = new FeatureImplication(changedNodeParent + "==1" + " && " + children.getIfBlock().getCondition() + "==1", definedNode.getMacroName() + "==" + ((Define) definedNode).getMacroExpansion());
                                else
                                    featureImplication = new FeatureImplication(changedNodeParent + "==1" + " && " + children.getIfBlock().getCondition() + "==1", definedNode.getMacroName() + "==1");
                                featureImplications.add(featureImplication);
                                feature = new Feature(definedNode.getMacroName()); //feature is the variable of the define or undef
                                if (!featureList.contains(feature)) {
                                    // ExpressionSolver expressionSolver = new ExpressionSolver();
                                    //BoolVar typeOfVar = expressionSolver.getBoolVarFromExpr(feature.getName());
                                    //if (typeOfVar instanceof BoolVar) {
                                    //featureImplication = new FeatureImplication(changedNodeParent, "!" + feature);
                                    //featureImplications.add(featureImplication);
                                    //    elsePartFeature = "!" + feature;
                                    elsePartFeature = feature + "==0";
                                } else {
                                    elsePartFeature = null;
                                    //}
                                }
                                solver.addClause(feature, featureImplications, elsePartFeature);
                                elsePartFeature = null;
                            }
                        }
                    } else if (definedNode.getLineInfo() < changedNodeLineFrom && featureChangedNode.contains(definedNode.getMacroName())) { //undef
                        if (!children.getIfBlock().getParent().getParent().getCondition().contains("BASE")) {
                            featureImplication = new FeatureImplication(changedNodeParent + "==1" + " && " + children.getIfBlock().getParent().getParent().getCondition() + "==1" + " && " + children.getIfBlock().getCondition() + "==1", definedNode.getMacroName() + "==0");
                            //else
                            //  featureImplication = new FeatureImplication(changedNodeParent + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName());
                            featureImplications.add(featureImplication);
                            feature = new Feature(definedNode.getMacroName()); //feature is the variable of the define or undef
                            elsePartFeature = null;
                            solver.addClause(feature, featureImplications, elsePartFeature);
                        }
                    }

                }
            }
        }
    }


}

package at.jku.isse.gitecco.translation.constraintcomputation.util;

import at.jku.isse.gitecco.core.git.Change;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.solver.ExpressionSolver;
import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.core.type.FeatureImplication;

import java.io.File;
import java.util.*;

public class ChangeConstraint {


    public void constructConstraintPerFeature(ArrayList<ConditionalNode> classUntilChange, Set<ConditionalNode> changedNodes, GitHelper gitHelper, Change change, GetNodesForChangeVisitor visitor, FileNode child) {
        Feature feature = null;
        ExpressionSolver solver = new ExpressionSolver();
        PreprocessorHelper pph = new PreprocessorHelper();
        final File gitFolder = new File(gitHelper.getPath());
        final File eccoFolder = new File(gitFolder.getParent(), "ecco");
        Queue<FeatureImplication> featureImplications = new LinkedList<>();
        Queue<FeatureImplication> featureImplicationsAux = new LinkedList<>();
        FeatureImplication featureImplication;
        for (ConditionalNode changedNode : changedNodes) {
            if (!changedNode.getCondition().contains("BASE")) {
                IFCondition changedNodeParent = (IFCondition) changedNode.getParent().getIfBlock().getParent().getParent();
                ConditionalNode conditionalNode = changedNode.getParent().getParent();
                String expression = changedNode.getCondition();
                //adding the expression of the changedNode that solver will use to give a solution
                while (!conditionalNode.getCondition().contains("BASE")) {
                    if (conditionalNode instanceof IFCondition)
                        expression += " && " + conditionalNode.getCondition();
                    else if (conditionalNode instanceof ELSECondition)
                        expression += " && " + conditionalNode.getCondition();
                    else if (conditionalNode instanceof ELIFCondition) {
                        expression += conditionalNode.getCondition();
                    }
                    conditionalNode = conditionalNode.getParent().getParent();
                }
                expression += " && " + conditionalNode.getCondition();
                solver.setExpr(expression);


                //adding clauses to the defines children of BASE
                for (DefineNode defineNode : changedNodeParent.getDefineNodes()) {
                    if (defineNode instanceof Define && defineNode.getLineInfo() < changedNode.getLineFrom()) {
                        if (((Define) defineNode).getMacroExpansion() != null) {
                            featureImplication = new FeatureImplication(changedNodeParent.getCondition(), defineNode.getMacroName() + "==" + ((Define) defineNode).getMacroExpansion());
                        } else {
                            featureImplication = new FeatureImplication(changedNodeParent.getCondition(), defineNode.getMacroName());
                        }
                        featureImplications.add(featureImplication);
                        feature = new Feature(defineNode.getMacroName()); //feature is the variable of the define or undef
                        solver.addClause(feature, featureImplications);
                    } else if (defineNode instanceof Undef && defineNode.getLineInfo() < changedNode.getLineFrom()) { //undef
                        featureImplication = new FeatureImplication("BASE", defineNode.getMacroName());
                        featureImplications.add(featureImplication);
                        feature = new Feature(defineNode.getMacroName()); //feature is the variable of the define or undef
                        solver.addClause(feature, featureImplications);
                    }
                }

                //adding clauses for each BASE children Nodes that has defines below the changedNode
                for (ConditionBlockNode children : changedNodeParent.getChildren()) {
                    if (children.getIfBlock().getDefineNodes().size() > 0) {
                        for (DefineNode definedNode : children.getIfBlock().getDefineNodes()) {
                            if (definedNode instanceof Define && definedNode.getLineInfo() < changedNode.getLineFrom()) {
                                if (((Define) definedNode).getMacroExpansion() != null) {
                                    featureImplication = new FeatureImplication(changedNodeParent.getCondition() + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName() + "==" + ((Define) definedNode).getMacroExpansion());
                                } else {
                                    featureImplication = new FeatureImplication(changedNodeParent.getCondition() + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName());
                                }
                                featureImplications.add(featureImplication);
                                feature = new Feature(definedNode.getMacroName()); //feature is the variable of the define or undef
                                solver.addClause(feature, featureImplications);
                            } else if (definedNode instanceof Undef && definedNode.getLineInfo() < changedNode.getLineFrom()) { //undef
                                featureImplication = new FeatureImplication(changedNodeParent.getCondition() + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName());
                                featureImplications.add(featureImplication);
                                feature = new Feature(definedNode.getMacroName()); //feature is the variable of the define or undef
                                solver.addClause(feature, featureImplications);
                            }

                        }
                    }
                    if (children.getIfBlock().getLineTo() < changedNode.getLineFrom()) {
                        searchingDefineNodes(children, solver, featureImplications, changedNodeParent.getCondition());
                    }
                }

                //after added all clauses the solver returns a solution
                Map<Feature, Integer> result = solver.solve();
                solver.reset();
                pph.generateVariants(result, gitFolder, eccoFolder);
                System.out.println("\nBlock: " + changedNode.getCondition() + " has change!");
                result.entrySet().forEach(x -> System.out.print(x.getKey() + " = " + x.getValue() + "; "));
                System.out.println("\n");
            }
        }
    }


    public void searchingDefineNodes(ConditionBlockNode children, ExpressionSolver solver, Queue<FeatureImplication> featureImplications, String changedNodeParent) {
        FeatureImplication featureImplication;
        Feature feature = null;

        if (children.getIfBlock().getChildren().size() > 0) {
            for (ConditionBlockNode childrenChildren : children.getIfBlock().getChildren()) {
                searchingDefineNodes(childrenChildren, solver, featureImplications, changedNodeParent);
            }
        } else {
            if (children.getIfBlock().getDefineNodes().size() > 0) {
                for (DefineNode definedNode : children.getIfBlock().getDefineNodes()) {
                    if (definedNode instanceof Define) {
                        if (!children.getIfBlock().getParent().getParent().getCondition().contains("BASE"))
                            featureImplication = new FeatureImplication(changedNodeParent + " && " + children.getIfBlock().getParent().getParent().getCondition() + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName() + "==" + ((Define) definedNode).getMacroExpansion());
                        else
                            featureImplication = new FeatureImplication(changedNodeParent + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName() + "==" + ((Define) definedNode).getMacroExpansion());
                        featureImplications.add(featureImplication);

                    } else { //undef
                        if (!children.getIfBlock().getParent().getParent().getCondition().contains("BASE"))
                            featureImplication = new FeatureImplication(changedNodeParent + " && " + children.getIfBlock().getParent().getParent().getCondition() + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName());
                        else
                            featureImplication = new FeatureImplication(changedNodeParent + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName());
                        featureImplications.add(featureImplication);
                    }
                    feature = new Feature(definedNode.getMacroName()); //feature is the variable of the define or undef
                    solver.addClause(feature, featureImplications);
                }
            }
        }
    }
}

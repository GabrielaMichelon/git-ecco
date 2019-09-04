package at.jku.isse.gitecco.translation.constraintcomputation.util;

import at.jku.isse.gitecco.core.git.Change;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.solver.ExpressionSolver;
import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.core.type.FeatureImplication;
import org.chocosolver.solver.variables.Variable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class ChangeConstraint {

    private ArrayList<String> featureList;

    public ArrayList<String> getFeatureList() {
        return featureList;
    }

    public void setFeatureList(ArrayList<String> featureList) {
        this.featureList = featureList;
    }

    public Map<String, Boolean> featureChangedNode = new HashMap<>();

    public void constructConstraintPerFeature(ArrayList<ConditionalNode> classUntilChange, Set<ConditionalNode> changedNodes, GitHelper gitHelper, Change change, GetNodesForChangeVisitor visitor, FileNode child, File outputDirectory, RootNode tree) {
        Feature feature = null;
        ExpressionSolver solver = new ExpressionSolver();
        PreprocessorHelper pph = new PreprocessorHelper();
        final File gitFolder = new File(gitHelper.getPath());
        final File eccoFolder = new File(gitFolder.getParent(), "ecco");
        Queue<FeatureImplication> featureImplications = new LinkedList<>();
        FeatureImplication featureImplication;
        String elsePartFeature = null;

        for (ConditionalNode changedNode : changedNodes) {

            GetAllIncludesVisitor getAllIncludesVisitor = new GetAllIncludesVisitor(changedNode.getLineFrom());
            GetAllDefinesVisitorTranslation definesVisitor = new GetAllDefinesVisitorTranslation(changedNode.getLineFrom());
            Set<DefineNode> allDefines = new TreeSet<>();

            //getting includes before the changedNode inside BASE
            for (IncludeNode includeNode : ((SourceFileNode) child).getIncludesBase()) {
                getAllIncludesVisitor.visit(includeNode);
            }

            //getting includes inside each block above changedNode
            for (ConditionalNode conditionalNode : visitor.getchangedNodes()) {
                for (IncludeNode includesBlocks : conditionalNode.getIncludeNodes()) {
                    getAllIncludesVisitor.visit(includesBlocks);
                }
            }

            //getting defineNodes inside includeFiles
            for (IncludeNode includeInsideIncludesChangedNode : getAllIncludesVisitor.getIncludeNodes()) {
                FileNode tmpF = tree.getChild(includeInsideIncludesChangedNode.getFileName());
                if (tmpF != null) tmpF.accept(definesVisitor);
                for (DefineNode defineNode : definesVisitor.getDefines()) {
                    //add define
                    if (defineNode instanceof Define) {
                        if (!defineNode.getParent().equals(null)) {
                            allDefines.add(new Define(defineNode.getMacroName(), ((Define) defineNode).getMacroExpansion(), includeInsideIncludesChangedNode.getLineInfo(), (ConditionalNode) includeInsideIncludesChangedNode.getParent(), defineNode.getParent()));
                        } else {
                            allDefines.add(new Define(defineNode.getMacroName(), ((Define) defineNode).getMacroExpansion(), includeInsideIncludesChangedNode.getLineInfo(), (ConditionalNode) includeInsideIncludesChangedNode.getParent()));
                        }
                    } else { //add undef
                        if (!defineNode.getParent().equals(null)) {
                            allDefines.add(new Define(defineNode.getMacroName(), null, includeInsideIncludesChangedNode.getLineInfo(), (ConditionalNode) includeInsideIncludesChangedNode.getParent().getParent(), defineNode.getParent()));
                        } else {
                            allDefines.add(new Define(defineNode.getMacroName(), null, includeInsideIncludesChangedNode.getLineInfo(), (ConditionalNode) includeInsideIncludesChangedNode.getParent()));
                        }
                    }
                }
                definesVisitor.reset();
            }


            if (!changedNode.getLocalCondition().contains("BASE")) {
                ConditionalNode changedNodeParent = changedNode.getParent().getIfBlock().getParent().getParent();
                ConditionalNode conditionalNode = changedNode.getParent().getParent();
                String expression = "";
                expression = changedNode.getCondition();
                solver.setExpr(expression);


                //we need the root BASE to look for its every children that are above the changed node
                if (!changedNodeParent.getLocalCondition().contains("BASE")) {
                    conditionalNode = changedNodeParent;

                    while (!(conditionalNode.getParent().getParent().getLocalCondition().contains("BASE"))) {
                        conditionalNode = conditionalNode.getParent().getParent();
                    }

                    changedNodeParent = conditionalNode.getParent().getParent();
                }
                definesVisitor = new GetAllDefinesVisitorTranslation(changedNode.getLineFrom());

                //getting defines for each BASE children Nodes above the changedNode
                for (int i = changedNodeParent.getChildren().size() - 1; i >= 0; i--) {
                    if (changedNodeParent.getChildren().get(i).getIfBlock().getLineFrom() < changedNode.getLineFrom()) {
                        //getting defineNodes inside block
                        FileNode tmpF = tree.getChild(child.getFilePath());
                        if (tmpF != null) tmpF.accept(definesVisitor);
                        for (DefineNode defineNode : definesVisitor.getDefines()) {
                            //add define
                            if (defineNode instanceof Define) {
                                allDefines.add(new Define(defineNode.getMacroName(), ((Define) defineNode).getMacroExpansion(), defineNode.getLineInfo(), (ConditionalNode) defineNode.getParent()));
                            } else { //add undef
                                allDefines.add(new Define(defineNode.getMacroName(), null, defineNode.getLineInfo(), (ConditionalNode) defineNode.getParent()));
                            }
                        }
                    }
                }

                //getting the defineNodes in desc form and adding it feature of defineNode in the set to consider to add clauses
                DefineNode[] arrayDefine = new DefineNode[allDefines.size()];
                int k = allDefines.size() - 1;
                for (DefineNode defineNode : allDefines) {
                    arrayDefine[k] = defineNode;
                    if (!(featureChangedNode.containsKey(defineNode.getMacroName())))
                        featureChangedNode.put(defineNode.getMacroName(), true);
                    k--;
                }


                //adding clauses
                for (Map.Entry<String, Boolean> literal : featureChangedNode.entrySet()) {
                        featureImplications = new LinkedList<>();
                        featureImplication = null;
                        for (int i = 0; i < allDefines.size(); i++) {
                            if (arrayDefine[i].getMacroName().contains(literal.getKey())) {
                                if (arrayDefine[i] instanceof Define) {
                                    if (!(((Define) arrayDefine[i]).getMacroExpansion().contains("(") && ((Define) arrayDefine[i]).getMacroExpansion().contains(")"))) {//not add macro function
                                        featureImplication = new FeatureImplication(arrayDefine[i].getCondition(), arrayDefine[i].getMacroName() + " == " +((Define) arrayDefine[i]).getMacroExpansion());
                                    }
                                } else {
                                    featureImplication = new FeatureImplication(arrayDefine[i].getCondition(), arrayDefine[i].getMacroName());
                                }
                                if (featureImplication != null) {
                                    featureImplications.add(featureImplication);
                                }
                                feature = new Feature(arrayDefine[i].getMacroName()); //feature is the variable of the define or undef
                            }
                        }
                        if (feature != null && featureImplications.size() != 0)
                            solver.addClause(feature, featureImplications);
                }


                //after added all clauses the solver returns a solution
                Map<Feature, Integer> result = solver.solve();
                try {
                    String text = "\nBlock: " + changedNode.getLocalCondition() + " has change!\n";
                    Files.write(Paths.get(String.valueOf(outputDirectory)), text.getBytes(), new StandardOpenOption[]{StandardOpenOption.APPEND});
                    System.out.println("\nBlock: " + changedNode.getLocalCondition() + " has change!");
                    ArrayList<String> featuresVersioned = new ArrayList<>();
                    if (!result.entrySet().isEmpty()) {
                        pph.generateVariants(result, gitFolder, eccoFolder, gitHelper.getDirFiles());
                        System.out.println("Solution ChocoSolver: ");
                        result.entrySet().forEach(x -> System.out.print(x.getKey() + " = " + x.getValue() + "; "));
                        Variable[] getVars = solver.getModel().getVars();
                        System.out.println("\nConfiguration that has impact on this change:");
                        for (int j = 0; j < getVars.length; j++) {
                            if (featureList.contains(getVars[j].getName())) {
                                System.out.println("\n" + getVars[j].getName() + " = " + getVars[j].asIntVar().getValue());
                            }
                        }
                    }
                    text = "________________________\n";
                    Files.write(Paths.get(String.valueOf(outputDirectory)), text.getBytes(), new StandardOpenOption[]{StandardOpenOption.APPEND});
                    System.out.println("________________________");
                    solver.reset();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}

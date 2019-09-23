package at.jku.isse.gitecco.translation.constraintcomputation.util;

import at.jku.isse.gitecco.core.git.Change;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.solver.ExpressionSolver;
import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.core.type.FeatureImplication;
import at.jku.isse.gitecco.translation.visitor.GetAllDefinesVisitor;
import at.jku.isse.gitecco.translation.visitor.GetAllIncludesVisitor;
import at.jku.isse.gitecco.translation.visitor.GetNodesForChangeVisitor;
import org.chocosolver.solver.variables.Variable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class ChangeConstraint {

    //changed:
    // - used visitor pattern correctly
    // - removed unused stuff
    //

    private ArrayList<String> featureList;

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
        Queue<FeatureImplication> featureImplications;
        FeatureImplication featureImplication;

        //for each changed node execute this process
        for (ConditionalNode changedNode : changedNodes) {
            //set that will contain all the defines, also the defines from included files. --> treeset for automatic sorting
            Set<DefineNode> allDefines = new TreeSet<>();

            //collecting all include statements above the changed node
            GetAllIncludesVisitor getAllIncludesVisitor = new GetAllIncludesVisitor(changedNode.getLineFrom());
            //collecting all define statements above the changed node
            GetAllDefinesVisitor definesVisitor = new GetAllDefinesVisitor(changedNode.getLineFrom());

            //get all include nodes recursively
            changedNode.getContainingFile().accept(getAllIncludesVisitor);
            //get all defines of the changed file
            changedNode.getContainingFile().accept(definesVisitor);
            allDefines.addAll(definesVisitor.getDefines());
            //reset clears the collected list of defines and cancels the linenumber limitation
            definesVisitor.reset();

            //for each found includenode collect all the define nodes
            for (IncludeNode includeNode : getAllIncludesVisitor.getIncludeNodes()) {
                //TODO: assign the define the linenumber of the include node
                //TODO: conditional includes --> include nodes must be walked up (except base. if base is hit stop walking up the condition tree)
                tree.getChild(includeNode.getFileName()).accept(definesVisitor);
            }

            /*//getting includes inside each block above changedNode
            for (ConditionalNode conditionalNode : visitor.getchangedNodes()) {
                for (IncludeNode includesBlocks : conditionalNode.getIncludeNodes()) {
                    getAllIncludesVisitor.visit(includesBlocks);
                }
            }*/

            //getting defineNodes inside includeFiles
            for (IncludeNode includeInsideIncludesChangedNode : getAllIncludesVisitor.getIncludeNodes()) {
                FileNode tmpF = tree.getChild(includeInsideIncludesChangedNode.getFileName());
                if (tmpF != null) tmpF.accept(definesVisitor);
                for (DefineNode defineNode : definesVisitor.getDefines()) {
                    ConditionalNode conditionalNode;
                    if (includeInsideIncludesChangedNode.getParent().getParent() instanceof ConditionBlockNode) {
                        ConditionBlockNode conditionBlockNode = (ConditionBlockNode) includeInsideIncludesChangedNode.getParent().getParent();
                        conditionalNode = conditionBlockNode.getIfBlock();
                    } else {
                        conditionalNode = (ConditionalNode) includeInsideIncludesChangedNode.getParent();
                    }
                    //add define
                    if (defineNode instanceof Define) {
                        if (!defineNode.getParent().equals(null)) {
                            allDefines.add(new Define(defineNode.getMacroName(), ((Define) defineNode).getMacroExpansion(), includeInsideIncludesChangedNode.getLineInfo(), conditionalNode, defineNode.getParent()));
                        } else {
                            allDefines.add(new Define(defineNode.getMacroName(), ((Define) defineNode).getMacroExpansion(), includeInsideIncludesChangedNode.getLineInfo(), conditionalNode));
                        }
                    } else { //add undef
                        if (!defineNode.getParent().equals(null)) {
                            allDefines.add(new Define(defineNode.getMacroName(), null, includeInsideIncludesChangedNode.getLineInfo(), conditionalNode, defineNode.getParent()));
                        } else {
                            allDefines.add(new Define(defineNode.getMacroName(), null, includeInsideIncludesChangedNode.getLineInfo(), conditionalNode));
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
                //System.out.println(expression);
                solver.setExpr(expression);


                //we need the root BASE to look for its every children that are above the changed node
                if (!changedNodeParent.getLocalCondition().contains("BASE")) {
                    conditionalNode = changedNodeParent;

                    while (!(conditionalNode.getParent().getParent().getLocalCondition().contains("BASE"))) {
                        conditionalNode = conditionalNode.getParent().getParent();
                    }

                    changedNodeParent = conditionalNode.getParent().getParent();
                }
                definesVisitor = new GetAllDefinesVisitor(changedNode.getLineFrom());

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

                Boolean localConditionContainsFeature = false;
                ArrayList<Feature> changedNodeLocalVars = feature.parseConditionArray(changedNode.getLocalCondition());
                for (Feature feat:changedNodeLocalVars) {
                    if(featureList.contains(feat)){
                        localConditionContainsFeature = true;
                    }
                }
                ArrayList<Feature> changedNodeVars;
                if(!localConditionContainsFeature) {
                    changedNodeVars = feature.parseConditionArray(changedNode.getCondition());
                }
                else{
                    changedNodeVars = changedNodeLocalVars;
                }

                //adding clauses for each literal (in desc order --> the arrayDefine has the last define in position 0)
                for (int j = 0; j < changedNodeVars.size(); j++) {
                    Feature literal = changedNodeVars.get(j);
                    featureImplications = new LinkedList<>();
                    featureImplication = null;
                    for (int i = 0; i < allDefines.size(); i++) {
                        if (arrayDefine[i].getMacroName().contains(literal.getName())) {
                            if (arrayDefine[i] instanceof Define) {
                                if (!(((Define) arrayDefine[i]).getMacroExpansion().contains("(") && ((Define) arrayDefine[i]).getMacroExpansion().contains(")"))) {//not add macro function
                                    featureImplication = new FeatureImplication(arrayDefine[i].getCondition(), arrayDefine[i].getMacroName() + " == " + ((Define) arrayDefine[i]).getMacroExpansion());
                                }
                            } else {
                                featureImplication = new FeatureImplication(arrayDefine[i].getCondition(), arrayDefine[i].getMacroName());
                            }
                            if (featureImplication != null) {
                                featureImplications.add(featureImplication);
                            }
                            feature = new Feature(arrayDefine[i].getMacroName()); //feature is the variable of the define or undef
                            HashSet<Feature> splitDefineVars = (HashSet<Feature>) feature.parseCondition(arrayDefine[i].getCondition());
                            for (Feature stringVar : splitDefineVars) {
                                if (!changedNodeVars.contains(stringVar)) {
                                    changedNodeVars.add(stringVar);
                                }
                            }
                        }
                    }
                    if (feature != null && featureImplications.size() != 0)
                        solver.addClause(feature, featureImplications);
                }
                solver.setExpr(changedNode.getLocalCondition());

                //after added all clauses the solver returns a solution
                Map<Feature, Integer> result = solver.solve();
                try {
                    String text = "\nBlock: " + changedNode.getLocalCondition() + " has change!\n";
                    Files.write(Paths.get(String.valueOf(outputDirectory)), text.getBytes(), new StandardOpenOption[]{StandardOpenOption.APPEND});
                    System.out.println("\nLOCAL CONDITION Block: " + changedNode.getLocalCondition() + " has change!");
                    ArrayList<String> featuresVersioned = new ArrayList<>();
                    if (!result.entrySet().isEmpty()) {
                        pph.generateVariants(result, gitFolder, eccoFolder, gitHelper.getDirFiles());
                        System.out.println("Solution ChocoSolver: ");
                        text = "Solution ChocoSolver: ";
                        Files.write(Paths.get(String.valueOf(outputDirectory)), text.getBytes(), new StandardOpenOption[]{StandardOpenOption.APPEND});
                        result.entrySet().forEach(x -> System.out.print(x.getKey() + " = " + x.getValue() + "; "));
                        Variable[] getVars = solver.getModel().getVars();
                        System.out.println("\nFeatures that has impact on this change:");
                        text = "\nFeatures that has impact on this change:";
                        Files.write(Paths.get(String.valueOf(outputDirectory)), text.getBytes(), new StandardOpenOption[]{StandardOpenOption.APPEND});
                        for (int j = 0; j < getVars.length; j++) {
                            if (featureList.contains(getVars[j].getName())) {
                                featuresVersioned.add(getVars[j].getName());
                            }
                        }
                        if(featuresVersioned.size()>0) {
                            for (String featVersioned : featuresVersioned) {
                                System.out.println("\n" + featVersioned);
                                text = "\n" + featVersioned;
                                Files.write(Paths.get(String.valueOf(outputDirectory)), text.getBytes(), new StandardOpenOption[]{StandardOpenOption.APPEND});
                            }
                        }else{
                            text = "\n No features, just BASE";
                            Files.write(Paths.get(String.valueOf(outputDirectory)), text.getBytes(), new StandardOpenOption[]{StandardOpenOption.APPEND});
                            System.out.println("\n No features, just BASE");
                        }
                    }
                    text = "________________________\n";
                    Files.write(Paths.get(String.valueOf(outputDirectory)), text.getBytes(), new StandardOpenOption[]{StandardOpenOption.APPEND});
                    System.out.println("________________________");

                    solver.reset();
                    solver.setExpr(changedNode.getCondition());
                    changedNodeVars = feature.parseConditionArray(changedNode.getCondition());
                    //adding clauses for each literal (in desc order --> the arrayDefine has the last define in position 0)
                    for (int j = 0; j < changedNodeVars.size(); j++) {
                        Feature literal = changedNodeVars.get(j);
                        featureImplications = new LinkedList<>();
                        featureImplication = null;
                        for (int i = 0; i < allDefines.size(); i++) {
                            if (arrayDefine[i].getMacroName().contains(literal.getName())) {
                                if (arrayDefine[i] instanceof Define) {
                                    if (!(((Define) arrayDefine[i]).getMacroExpansion().contains("(") && ((Define) arrayDefine[i]).getMacroExpansion().contains(")"))) {//not add macro function
                                        featureImplication = new FeatureImplication(arrayDefine[i].getCondition(), arrayDefine[i].getMacroName() + " == " + ((Define) arrayDefine[i]).getMacroExpansion());
                                    }
                                } else {
                                    featureImplication = new FeatureImplication(arrayDefine[i].getCondition(), arrayDefine[i].getMacroName());
                                }
                                if (featureImplication != null) {
                                    featureImplications.add(featureImplication);
                                }
                                feature = new Feature(arrayDefine[i].getMacroName()); //feature is the variable of the define or undef
                                HashSet<Feature> splitDefineVars = (HashSet<Feature>) feature.parseCondition(arrayDefine[i].getCondition());
                                for (Feature stringVar : splitDefineVars) {
                                    if (!changedNodeVars.contains(stringVar)) {
                                        changedNodeVars.add(stringVar);
                                    }
                                }
                            }
                        }
                        if (feature != null && featureImplications.size() != 0)
                            solver.addClause(feature, featureImplications);
                    }

                    Map<Feature, Integer> result2 = solver.solve();
                    System.out.println("GLOBAL CONDITION: ");
                    String text2 = "\nBlock: " + changedNode.getCondition() + " Configuration:\n";
                    Files.write(Paths.get(String.valueOf(outputDirectory)), text.getBytes(), new StandardOpenOption[]{StandardOpenOption.APPEND});
                    System.out.println("\nBlock: " + changedNode.getCondition() + " Configuration:\n");
                    ArrayList<String> featuresVersioned2 = new ArrayList<>();
                    if (!result.entrySet().isEmpty()) {
                        pph.generateVariants(result2, gitFolder, eccoFolder, gitHelper.getDirFiles());
                        System.out.println("Solution ChocoSolver: ");
                        text2 = "Solution ChocoSolver: ";
                        Files.write(Paths.get(String.valueOf(outputDirectory)), text.getBytes(), new StandardOpenOption[]{StandardOpenOption.APPEND});
                        result2.entrySet().forEach(x -> System.out.print(x.getKey() + " = " + x.getValue() + "; "));
                        Variable[] getVars = solver.getModel().getVars();
                        System.out.println("\nConfiguration that has impact on this change:");
                        text = "\nConfiguration that has impact on this change:";
                        Files.write(Paths.get(String.valueOf(outputDirectory)), text.getBytes(), new StandardOpenOption[]{StandardOpenOption.APPEND});
                        for (int j = 0; j < getVars.length; j++) {
                            if ((featureList.contains(getVars[j].getName()) && changedNodeVars.contains(getVars[j].getName())) || getVars[j].getName().contains("BASE")) {
                                text = "\n" + getVars[j].getName() + " = " + getVars[j].asIntVar().getValue();
                                Files.write(Paths.get(String.valueOf(outputDirectory)), text.getBytes(), new StandardOpenOption[]{StandardOpenOption.APPEND});
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

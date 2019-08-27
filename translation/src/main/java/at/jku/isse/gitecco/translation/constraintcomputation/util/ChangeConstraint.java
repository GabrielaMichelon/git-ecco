package at.jku.isse.gitecco.translation.constraintcomputation.util;

import at.jku.isse.gitecco.core.git.Change;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.solver.ExpressionSolver;
import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.core.type.FeatureImplication;
import org.chocosolver.solver.variables.IntVar;
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

    public void constructConstraintPerFeature(ArrayList<ConditionalNode> classUntilChange, Set<ConditionalNode> changedNodes, GitHelper gitHelper, Change change, GetNodesForChangeVisitor visitor, FileNode child, String[] dirFiles, File outputDirectory) {
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
                solver.setExpr(expression);

                //obtains the literals that we need to search its parents and defines to add to the queue implication
                featureChangedNode = searchingFeatureCorrespondingtoChangedNode(changedNode, featureList, expression, solver);


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
                                    elsePartFeature = "!(" + feature + ")";
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
                        featureImplication = new FeatureImplication(changedNodeParent.getCondition(), "!(" + defineNode.getMacroName() + ")");
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
                //we need the root BASE to look for its every children that are above the changed node
                if (!changedNodeParent.getCondition().contains("BASE")) {
                    conditionalNode = changedNodeParent;

                    while (!(conditionalNode.getParent().getParent().getCondition().contains("BASE"))) {
                        conditionalNode = conditionalNode.getParent().getParent();
                    }

                    changedNodeParent = conditionalNode.getParent().getParent();
                }

                //adding clauses for each BASE children Nodes that has defines above the changedNode that has literal which implies on the changedNode
                for (int i = changedNodeParent.getChildren().size() - 1; i >= 0; i--) {
                    if (changedNodeParent.getChildren().get(i).getIfBlock().getLineFrom() < changedNode.getLineFrom()) {
                        searchingDefineNodes(changedNodeParent.getChildren().get(i), solver, featureImplications, changedNodeParent.getCondition(), changedNode.getLineFrom(), featureChangedNode);
                    }
                }


                //after added all clauses the solver returns a solution
                Map<Feature, Integer> result = solver.solve();
                try {
                    String text = "\nBlock: " + changedNode.getLocalCondition() + " has change!\n";
                    Files.write(Paths.get(String.valueOf(outputDirectory)), text.getBytes(), new StandardOpenOption[]{StandardOpenOption.APPEND});
                    System.out.println("\nBlock: " + changedNode.getLocalCondition() + " has change!");
                    ArrayList<String> featuresVersioned = new ArrayList<>();
                    if (!result.entrySet().isEmpty()) {
                        pph.generateVariants(result, gitFolder, eccoFolder, dirFiles);
                        System.out.println("Solution ChocoSolver: ");
                        result.entrySet().forEach(x -> System.out.print(x.getKey() + " = " + x.getValue() + "; "));
                        Variable[] getVars = solver.getModel().getVars();
                        System.out.println("\nConfiguration that has impact on this change:");
                        for(int j =0; j< getVars.length; j++) {
                            if (featureList.contains(getVars[j].getName())) {
                                System.out.println("\n"+getVars[j].getName() +" = "+ getVars[j].asIntVar().getValue());
                            }
                        }
                        text = "Solution ChocoSolver: \n";
                        Files.write(Paths.get(String.valueOf(outputDirectory)), text.getBytes(), new StandardOpenOption[]{StandardOpenOption.APPEND});
                       // result.entrySet().forEach(x -> {
                           // try {
                             //   Files.write(Paths.get(String.valueOf(outputDirectory)), (x.getKey() + " = " + x.getValue() + "; ").toString().getBytes(), new StandardOpenOption[]{StandardOpenOption.APPEND});
                        //    } catch (IOException e) {
                        //        e.printStackTrace();
                        //    }
                       // });
                        /*Feature key;
                        Integer value;
                        Set<Map.Entry<Feature, Integer>> setRetornado = result.entrySet();
                        boolean flag = true;
                        for (Map.Entry<Feature, Integer> literalsValues : setRetornado) {
                            if (flag) {
                                text = "\nConfiguration that has impact on this change (0 is false and others integers true):\n";
                                Files.write(Paths.get(String.valueOf(outputDirectory)), text.getBytes(), new StandardOpenOption[]{StandardOpenOption.APPEND});
                                System.out.println("\nConfiguration that has impact on this change (0 is false and others integers true):");
                                flag = false;
                            }
                            key = literalsValues.getKey();
                            value = literalsValues.getValue();
                            if (value != 0 && (featureList.contains(key.toString()) || key.toString().contains("BASE"))) {
                                featuresVersioned.add(key.toString());
                                System.out.println("Feature " + key.toString() + " == " + value);
                                text = "Feature " + key.toString() + " == " + value + "\n";
                                Files.write(Paths.get(String.valueOf(outputDirectory)), text.getBytes(), new StandardOpenOption[]{StandardOpenOption.APPEND});
                            }
                        }*/
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

    //we need to look if the changedNode is a feature and look if exists any parent besides BASE
    public ArrayList<String> searchingFeatureCorrespondingtoChangedNode(ConditionalNode
                                                                                changedNode, ArrayList<String> featureList, String expression, ExpressionSolver solver) {
        ArrayList<String> features = new ArrayList<>();
        String[] literals;
        Variable[] getVars = solver.getModel().getVars();
        for(int j =0; j< getVars.length; j++) {
            if (expression.contains(getVars[j].getName())) {
                features.add(getVars[j].getName());
            }
        }

        if (changedNode.getLocalCondition().contains("&&") && changedNode.getLocalCondition().contains("||")) {
            literals = changedNode.getLocalCondition().split("&&");
            for (int i = 0; i < literals.length; i++) {
                if (literals[i].contains("||")) {
                    String[] literalsAux = literals[i].split("\\|\\|");
                    for (int j = 0; j < literalsAux.length; j++) {
                        if (literalsAux[j].contains("!") && !(features.contains(literalsAux[j].replace("!", ""))))
                            literalsAux[j] = literalsAux[j].replace("!", "");
                        if (featureList.contains(literalsAux[j]))
                            features.add(literalsAux[j]);
                    }
                } else {
                    if (literals[i].contains("!") && !(features.contains(literals[i].replace("!", ""))))
                        literals[i] = literals[i].replace("!", "");
                    if (featureList.contains(literals[i]))
                        features.add(literals[i]);
                }
            }
        } else if (changedNode.getLocalCondition().contains("&&") && !changedNode.getLocalCondition().contains("||")) {
            literals = changedNode.getLocalCondition().split("&&");
            for (int i = 0; i < literals.length; i++) {
                if (literals[i].contains("!") && !(features.contains(literals[i].replace("!", ""))))
                    literals[i] = literals[i].replace("!", "");
                if (featureList.contains(literals[i]))
                    features.add(literals[i]);
            }
        } else if (!changedNode.getLocalCondition().contains("&&") && changedNode.getLocalCondition().contains("||")) {
            literals = changedNode.getLocalCondition().split("\\|\\|");
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


    public void searchingDefineNodes(ConditionBlockNode children, ExpressionSolver
            solver, Queue<FeatureImplication> featureImplications, String changedNodeParent, Integer
                                             changedNodeLineFrom, ArrayList<String> featureChangedNode) {
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
                                    elsePartFeature = "!(" + feature + ")"; //feature + "==0";
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
                    } else if (definedNode instanceof Undef && definedNode.getLineInfo() < changedNodeLineFrom && featureChangedNode.contains(definedNode.getMacroName())) { //undef
                        if (!children.getIfBlock().getParent().getParent().getCondition().contains("BASE")) {
                            featureImplication = new FeatureImplication(changedNodeParent + " && " + children.getIfBlock().getCondition(), "!(" + definedNode.getMacroName() + ")");
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
                                    elsePartFeature = "!(" + feature + ")";//feature + "==0";
                                } else {
                                    elsePartFeature = null;
                                }
                                solver.addClause(feature, featureImplications, elsePartFeature);
                                for (int i = 0; i < solver.getVars().size(); i++) {
                                    if (!(featureChangedNode.contains(solver.getVars().get(i).getName()))) {
                                        featureChangedNode.add(solver.getVars().get(i).getName());
                                    }
                                }
                            } else {//if(!(children.getIfBlock().getParent().getParent() instanceof IFNDEFCondition && children.getIfBlock().getParent().getParent().getLocalCondition().contains(definedNode.getMacroName()))){
                                featureImplication = new FeatureImplication(changedNodeParent + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName() + ((Define) definedNode).getMacroExpansion());
                                featureImplications.add(featureImplication);
                                feature = new Feature(definedNode.getMacroName());
                                if (!featureList.contains(feature)) {
                                    elsePartFeature = "!(" + feature + ")";//feature + "==0";
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
                            featureImplication = new FeatureImplication(changedNodeParent + " && " + children.getIfBlock().getCondition(), "!(" + definedNode.getMacroName() + ")");
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

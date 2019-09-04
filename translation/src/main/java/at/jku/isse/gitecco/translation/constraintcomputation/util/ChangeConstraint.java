package at.jku.isse.gitecco.translation.constraintcomputation.util;

import at.jku.isse.gitecco.core.git.Change;
import at.jku.isse.gitecco.core.git.GitCommitListener;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.solver.ExpressionSolver;
import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.core.type.FeatureImplication;
import at.jku.isse.gitecco.featureid.featuretree.visitor.GetAllDefinesVisitor;
import com.sun.org.apache.xpath.internal.operations.Bool;
import com.sun.research.ws.wadl.Include;
import org.anarres.cpp.Source;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static scala.collection.immutable.Nil.indexOf;

public class ChangeConstraint {

    private ArrayList<String> featureList;

    public ArrayList<String> getFeatureList() {
        return featureList;
    }

    public void setFeatureList(ArrayList<String> featureList) {
        this.featureList = featureList;
    }

    public Map<String, Boolean> featureChangedNode = new HashMap<>();

    public void constructConstraintPerFeature(ArrayList<ConditionalNode> classUntilChange, Set<ConditionalNode> changedNodes, GitHelper gitHelper, Change change, GetNodesForChangeVisitor visitor, FileNode child, String[] dirFiles, File outputDirectory, RootNode tree) {
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
            for (ConditionalNode conditionalNode: visitor.getchangedNodes()) {
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
                /*for (DefineNode defineNode : changedNodeParent.getDefineNodes()) {
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
                                solver.addClause(feature, featureImplications);
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
                        solver.addClause(feature, featureImplications);
                        for (int i = 0; i < solver.getVars().size(); i++) {
                            if (!(featureChangedNode.contains(solver.getVars().get(i).getName()))) {
                                featureChangedNode.add(solver.getVars().get(i).getName());
                            }
                        }
                    }
                }*/
                //we need the root BASE to look for its every children that are above the changed node
                if (!changedNodeParent.getCondition().contains("BASE")) {
                    conditionalNode = changedNodeParent;

                    while (!(conditionalNode.getParent().getParent().getCondition().contains("BASE"))) {
                        conditionalNode = conditionalNode.getParent().getParent();
                    }

                    changedNodeParent = conditionalNode.getParent().getParent();
                }
                definesVisitor = new GetAllDefinesVisitorTranslation(changedNode.getLineFrom());

                //adding clauses for each BASE children Nodes that has defines above the changedNode that has literal which implies on the changedNode
                for (int i = changedNodeParent.getChildren().size() - 1; i >= 0; i--) {
                    if (changedNodeParent.getChildren().get(i).getIfBlock().getLineFrom() < changedNode.getLineFrom()) {
                        //getting defineNodes inside includeFiles
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

                        //searchingDefineNodes(changedNodeParent.getChildren().get(i), solver, featureImplications, changedNodeParent.getCondition(), changedNode.getLineFrom(), featureChangedNode, tree);
                    }
                }

                //getting the defineNodes in desc form
                DefineNode[] arrayDefine = new DefineNode[allDefines.size()];
                int k = allDefines.size() - 1;
                for (DefineNode defineNode : allDefines) {
                    arrayDefine[k] = defineNode;
                    k--;
                }


                //add clauses
                for (Map.Entry<String, Boolean> literal : featureChangedNode.entrySet()) {
                    if (literal.getValue()) {
                        featureImplications = new LinkedList<>();
                        featureImplication = null;
                        for (int i = 0; i < allDefines.size(); i++) {
                            if (arrayDefine[i].getMacroName().contains(literal.getKey())) {
                                //ConditionalNode parent = (ConditionalNode) defineNode.getParent();
                                if (arrayDefine[i] instanceof Define) {
                                    if (!(((Define) arrayDefine[i]).getMacroExpansion().contains("(") && ((Define) arrayDefine[i]).getMacroExpansion().contains(")"))) {//not add macro function
                                        featureImplication = new FeatureImplication(arrayDefine[i].getCondition(), arrayDefine[i].getMacroName() + ((Define) arrayDefine[i]).getMacroExpansion());
                                    }
                                } else {
                                    featureImplication = new FeatureImplication(arrayDefine[i].getCondition(), arrayDefine[i].getMacroName());
                                }
                                if (featureImplication != null) {
                                    featureImplications.add(featureImplication);
                                }
                                feature = new Feature(arrayDefine[i].getMacroName()); //feature is the variable of the define or undef
                                if (!featureList.contains(feature)) {
                                    elsePartFeature = "!(" + feature + ")"; //feature + "==0";
                                } else {
                                    elsePartFeature = null;
                                }
                                for (int j = 0; j < solver.getVars().size(); j++) {
                                    if (!(featureChangedNode.entrySet().contains(solver.getVars().get(j).getName()))) {
                                        featureChangedNode.put(solver.getVars().get(j).getName(), true);
                                    }
                                }
                            }
                        }
                        if (feature != null && featureImplications.size() != 0)
                            solver.addClause(feature, featureImplications);
                        featureChangedNode.entrySet().remove(literal);
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


    //we need to look if the changedNode is a feature and look if exists any parent besides BASE
    public Map<String,Boolean> searchingFeatureCorrespondingtoChangedNode(ConditionalNode
                                                                                changedNode, ArrayList<String> featureList, String expression, ExpressionSolver solver) {
        ArrayList<String> features = new ArrayList<>();
        String[] literals;
        Variable[] getVars = solver.getModel().getVars();
        for (int j = 0; j < getVars.length; j++) {
            if (expression.contains(getVars[j].getName())) {
                features.add(getVars[j].getName());
            }
        }

        if (changedNode.getCondition().contains("&&") && changedNode.getCondition().contains("||")) {
            literals = changedNode.getCondition().split("&&");
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
        } else if (changedNode.getCondition().contains("&&") && !changedNode.getCondition().contains("||")) {
            literals = changedNode.getCondition().split("&&");
            for (int i = 0; i < literals.length; i++) {
                if (literals[i].contains("!") && !(features.contains(literals[i].replace("!", ""))))
                    literals[i] = literals[i].replace("!", "");
                if (featureList.contains(literals[i]))
                    features.add(literals[i]);
            }
        } else if (!changedNode.getCondition().contains("&&") && changedNode.getCondition().contains("||")) {
            literals = changedNode.getCondition().split("\\|\\|");
            for (int i = 0; i < literals.length; i++) {
                if (literals[i].contains("!") && !(features.contains(literals[i].replace("!", ""))))
                    literals[i] = literals[i].replace("!", "");
                if (featureList.contains(literals[i]))
                    features.add(literals[i]);
            }
        } else if (!(features.contains(changedNode.getCondition()))) {
            features.add(changedNode.getCondition());
        }
        Map<String, Boolean> literalAndFeatures = new HashMap<>();
        for (String feat : features) {
            literalAndFeatures.put(feat, true);
        }


        return literalAndFeatures;
    }


    public void searchingDefineNodes(ConditionBlockNode children, ExpressionSolver
            solver, Queue<FeatureImplication> featureImplications, String changedNodeParent, Integer
                                             changedNodeLineFrom, ArrayList<String> featureChangedNode, RootNode tree) {
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
                                solver.addClause(feature, featureImplications);
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
                            solver.addClause(feature, featureImplications);
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
                searchingDefineNodes(childrenChildren, solver, featureImplications, changedNodeParent, changedNodeLineFrom, featureChangedNode, tree);
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
                                solver.addClause(feature, featureImplications);
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
                                solver.addClause(feature, featureImplications);
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
                            solver.addClause(feature, featureImplications);
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

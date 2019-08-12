package at.jku.isse.gitecco.translation;

import at.jku.isse.gitecco.core.cdt.CDTHelper;
import at.jku.isse.gitecco.core.git.Change;
import at.jku.isse.gitecco.core.git.GitCommitList;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.solver.ExpressionSolver;
import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.core.type.FeatureImplication;
import at.jku.isse.gitecco.translation.constraintcomputation.util.GetNodesForChangeVisitor;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.core.runtime.CoreException;
import scala.util.parsing.combinator.testing.Str;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.chocosolver.solver.constraints.nary.cnf.LogOp.ifThenElse;
import static org.chocosolver.solver.constraints.nary.cnf.LogOp.implies;

public class App {


    public static void main(String... args) throws Exception {
        final boolean debug = true;
        //TODO: planned arguments: DEBUG, dispose tree, max commits, repo path, csv path(feature id), outpath for ecco
        //maybe even start commit and/or end commit (hashes or numbers)
        //String repoPath = "C:\\obermanndavid\\git-ecco-test\\appimpleTest\\marlin\\Marlin";
        //String repoPath = "C:\\Users\\gabil\\Desktop\\ECCO_Work\\Marlin\\Marlin";
        String repoPath = "C:\\Users\\gabil\\Desktop\\ECCO_Work\\test4";
        //String repoPath = "C:\\Users\\gabil\\Desktop\\Test";
        final GitHelper gitHelper = new GitHelper(repoPath);
        final GitCommitList commitList = new GitCommitList(repoPath);

        commitList.addGitCommitListener((gc, gcl) -> {

            //TODO: do the git commit and measure time or whatever
            //for a guide how to make a git commit with jgit: git-ecco commit a071bdd677d9a7555f60e026a4b0ba975be09792
            //file GitCommitList.java method: enableAutoCommitConfig()

            GetNodesForChangeVisitor visitor = new GetNodesForChangeVisitor();
            Set<ConditionalNode> changedNodes = new HashSet<>();
            List<String> changedFiles = gitHelper.getChangedFiles(gc);

            //retrieve changed nodes
            for (FileNode child : gc.getTree().getChildren()) {
                //System.out.println((gc.getTree().getChildren().indexOf(child)));
                if (child instanceof SourceFileNode && changedFiles.contains(child.getFilePath().replace("/", "\\"))) {
                    Change[] changes = null;
                    try {
                        changes = gitHelper.getFileDiffs(gc, child);
                    } catch (Exception e) {
                        System.err.println("error while executing the file diff: " + child.getFilePath());
                        e.printStackTrace();
                    }
                    String fileContent = null;
                    Path path = Paths.get(gitHelper.getPath() + File.separator + child.getFilePath());
                    try {
                        fileContent = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    for (Change change : changes) {
                        visitor.setChange(change);
                        child.accept(visitor);
                        changedNodes.addAll(visitor.getchangedNodes());


                        //due to problems in receiving all the ppstatements (some times de defines comes and undefs never comes)
                        /*for (ConditionalNode changedNode : changedNodes) {
                            int init = changedNode.getLineFrom();
                            int end = changedNode.getLineTo();

                            String[] lines = fileContent.split("\\r?\\n");
                            String code = "";
                            if (init != 0) {
                                for (int i = init + 1; i < end; i++) {
                                    code += lines[i - 1] + "\n";
                                }
                            }
                            IASTTranslationUnit translationUnit = null;
                            try {
                                translationUnit = CDTHelper.parse(code.toCharArray());
                            } catch (CoreException e) {
                                e.printStackTrace();
                            }


                            final IASTPreprocessorStatement[] ppstatements = translationUnit.getAllPreprocessorStatements();
                            ArrayList<DefineNode> definedNodes = new ArrayList<>();
                            for (IASTPreprocessorStatement ppstatementsInsideFeature : ppstatements) {
                                if (ppstatementsInsideFeature instanceof IASTPreprocessorMacroDefinition) {
                                    Define defineNodeInsideFeature = new Define(((IASTPreprocessorMacroDefinition) ppstatementsInsideFeature).getName().toString(), ((IASTPreprocessorMacroDefinition) ppstatementsInsideFeature).getExpansion().toString(), 1);
                                    Boolean alreadyExists = false;
                                    for (DefineNode defineNode:changedNode.getDefineNodes()) {
                                        if (defineNode.getLineInfo() == defineNodeInsideFeature.getLineInfo()){
                                            alreadyExists = true;
                                        }
                                    }
                                    if(!alreadyExists){
                                        changedNode.addDefineNode(defineNodeInsideFeature);
                                        definedNodes.add(defineNodeInsideFeature);
                                    }
                                } else if (ppstatementsInsideFeature instanceof IASTPreprocessorUndefStatement) {
                                    Undef undefNodeInsideFeature = new Undef(((IASTPreprocessorUndefStatement) ppstatementsInsideFeature).getMacroName().toString(), 1);
                                    Boolean alreadyExists = false;
                                    for (DefineNode defineNode:changedNode.getDefineNodes()) {
                                        if (defineNode.getLineInfo() == undefNodeInsideFeature.getLineInfo()){
                                            alreadyExists = true;
                                        }
                                    }
                                    if(!alreadyExists){
                                        changedNode.addDefineNode(undefNodeInsideFeature);
                                        definedNodes.add(undefNodeInsideFeature);
                                    }
                                }
                            }
                            if(changedNode.getChildren().size() > 0){
                                for (ConditionBlockNode changeChild:changedNode.getChildren()) {
                                    if(changeChild.getIfBlock().getDefineNodes().size()>0){
                                        for (DefineNode defineAux:changeChild.getIfBlock().getDefineNodes() ) {
                                            for (DefineNode defineAux2 :changedNode.getDefineNodes()) {
                                                if(defineAux.getLineInfo()==defineAux2.getLineInfo()){
                                                    changedNode.deleteDefineNode(defineAux2);
                                                }
                                            }

                                        }
                                    }
                                }
                            }
                        }*/

                        Change allClass = new Change(0, change.getTo());
                        visitor.setChange(allClass);
                        child.accept(visitor);
                        ArrayList<ConditionalNode> classNodes = new ArrayList<>();

                        classNodes.addAll(visitor.getchangedNodes());
                        App app = new App();
                        app.constructConstraintPerFeature(classNodes, changedNodes, gitHelper, change, visitor, child);


                    }
                }

            }
/*
            //compute assignment for preprocessing and generate variants
            ExpressionSolver solver = new ExpressionSolver();
            PreprocessorHelper pph = new PreprocessorHelper();
            final File gitFolder = new File(gitHelper.getPath());
            final File eccoFolder = new File(gitFolder.getParent(), "ecco");

            //for each changed node:
            for (ConditionalNode changedNode : changedNodes) {

                //TODO: previous constraints and affected constraints
                //new class that takes a changed node and walks up the tree and build the implication queue.
                //same for the affected blocks --> tree might need additional methods
                //for retrieving conjunctive conditions that are affected by a changed block.



                solver.setExpr(changedNode.getCondition());
                Map<Feature, Integer> result = solver.solve();
                solver.reset();
                pph.generateVariants(result, gitFolder, eccoFolder);
                System.out.println("CONFIG FOR PREPROCESSING:");
                result.entrySet().forEach(x -> System.out.print(x.getKey() + " = " + x.getValue() + "; "));

                //TODO: ecco commit with solution + marked as changed
            }*/

        });

        gitHelper.getAllCommits(commitList);

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
        for (ConditionalNode changedNode : changedNodes) {
            if (!changedNode.getCondition().contains("BASE")) {
                IFCondition changedNodeParent = (IFCondition) changedNode.getParent().getIfBlock().getParent().getParent();

                //adding the expression of the changedNode that solver will use to give a solution
                if (!changedNode.getParent().getIfBlock().getParent().getParent().getCondition().contains("BASE")) {
                    solver.setExpr(changedNode.getCondition() + " && " + changedNode.getParent().getIfBlock().getParent().getIfBlock().getParent().getParent().getCondition() + " && " + changedNodeParent.getCondition());
                    //System.out.println(changedNode.getCondition()+ " && " +changedNode.getParent().getIfBlock().getParent().getParent().getCondition()+ " && BASE");
                } else {
                    solver.setExpr(changedNode.getCondition() + " && " + changedNodeParent.getCondition());
                }

                //adding clauses first for definedNodes children of the changed node
                for (DefineNode definedNode : changedNode.getDefineNodes()) {
                    if (definedNode instanceof Define) {
                        if (!changedNode.getParent().getParent().getCondition().contains("BASE"))
                            featureImplication = new FeatureImplication(changedNodeParent.getParent().getParent().getCondition() + " && " + changedNodeParent.getCondition() + " && " + changedNode.getCondition(), definedNode.getMacroName() + " " + ((Define) definedNode).getMacroExpansion());
                        else
                            featureImplication = new FeatureImplication(changedNodeParent.getCondition() + " && " + changedNode.getCondition(), definedNode.getMacroName() + " " + ((Define) definedNode).getMacroExpansion());
                        featureImplications.add(featureImplication);

                    } else { //undef
                        if (!changedNode.getParent().getParent().getCondition().contains("BASE"))
                            featureImplication = new FeatureImplication(changedNodeParent.getParent().getParent().getCondition() + " && " + changedNodeParent.getCondition() + " && " + changedNode.getCondition(), definedNode.getMacroName());
                        else
                            featureImplication = new FeatureImplication(changedNodeParent.getCondition() + " && " + changedNode.getCondition(), definedNode.getMacroName());
                        featureImplications.add(featureImplication);
                    }
                    feature = new Feature(definedNode.getMacroName()); //feature is the variable of the define or undef
                    solver.addClause(feature, featureImplications);
                }

                //adding clauses to the defines children of BASE
                for (DefineNode defineNode : changedNodeParent.getDefineNodes()) {
                    if (defineNode instanceof Define && defineNode.getLineInfo() < changedNode.getLineFrom()) {
                        if (((Define) defineNode).getMacroExpansion() != null) {
                            featureImplication = new FeatureImplication(changedNodeParent.getCondition(), defineNode.getMacroName() + " " + ((Define) defineNode).getMacroExpansion());
                        } else {
                            featureImplication = new FeatureImplication(changedNodeParent.getCondition(), defineNode.getMacroName());
                        }
                        featureImplications.add(featureImplication);
                        feature = new Feature(defineNode.getMacroName()); //feature is the variable of the define or undef
                        solver.addClause(feature, featureImplications);
                    } else if (defineNode instanceof Undef && defineNode.getLineInfo() < changedNode.getLineFrom()) { //undef
                        featureImplication = new FeatureImplication("BASE",   defineNode.getMacroName());
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
                                    featureImplication = new FeatureImplication(changedNodeParent.getCondition() + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName() + " " + ((Define) definedNode).getMacroExpansion());
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
                if (solver.getVars().size() > 0) {
                    Map<Feature, Integer> result = solver.solve();
                    solver.reset();
                    pph.generateVariants(result, gitFolder, eccoFolder);
                    System.out.println("\nBlock: " + changedNode.getCondition() + " has change!");
                    result.entrySet().forEach(x -> System.out.print(x.getKey() + " = " + x.getValue() + "; "));
                    System.out.println("\n");
                } else {
                    System.out.println("\nBlock " + changedNode.getCondition() + " has change!");
                }
            }

                /*
                Change allClass = new Change(0, changedNode.getLineFrom());
                visitor.setChange(allClass);
                child.accept(visitor);
                ArrayList<ConditionalNode> classNodes = new ArrayList<>();
                classNodes.addAll(visitor.getchangedNodes());
                solver.setExpr(changedNode.getCondition() + " && BASE");
                for (int i = classNodes.size() - 1; i >= 0; i--) {
                    if (classNodes.get(i).getLineFrom() > changedNode.getLineTo()) {
                        i--;
                    } else {
                        featureImplications = new LinkedList<FeatureImplication>();
                        FeatureImplication featureImplication;

                        if (classNodes.get(i).getDefineNodes().size() > 0) {
                            for (int k = 0; k < classNodes.get(i).getDefineNodes().size(); k++) {
                                if (classNodes.get(i).getDefineNodes().get(k) instanceof Undef) {
                                    featureImplication = new FeatureImplication("BASE && " + classNodes.get(i).getCondition(), "!" + classNodes.get(i).getDefineNodes().get(k).getMacroName());
                                    featureImplications.add(featureImplication);
                                } else {
                                    featureImplication = new FeatureImplication("BASE && " + classNodes.get(i).getCondition(), classNodes.get(i).getDefineNodes().get(k).getMacroName());
                                    featureImplications.add(featureImplication);
                                }
                                feature = new Feature(classNodes.get(i).getDefineNodes().get(k).getMacroName()); //feature is the variable of the define or undef
                                solver.addClause(feature, featureImplications);
                            }
                        }
                        if (classNodes.get(i).getChildren().size() > 0) {
                            for (int j = 0; j < classNodes.get(i).getChildren().size() - 1; j++) {
                                if (classNodes.get(i).getChildren().get(j).getIfBlock().getDefineNodes().size() > 0) {
                                    for (int k = 0; k < classNodes.get(i).getChildren().get(j).getIfBlock().getDefineNodes().size(); k++) {
                                        if (classNodes.get(i).getChildren().get(j).getIfBlock().getDefineNodes().get(k) instanceof Undef) {
                                            featureImplication = new FeatureImplication("BASE && " + classNodes.get(i).getChildren().get(j).getIfBlock().getCondition(), "!" + classNodes.get(i).getChildren().get(j).getIfBlock().getDefineNodes().get(k).getMacroName());
                                            featureImplications.add(featureImplication);
                                        } else {
                                            featureImplication = new FeatureImplication("BASE && " + classNodes.get(i).getChildren().get(j).getIfBlock().getCondition(), classNodes.get(i).getChildren().get(j).getIfBlock().getDefineNodes().get(k).getMacroName());
                                            featureImplications.add(featureImplication);
                                        }
                                        feature = new Feature(classNodes.get(i).getChildren().get(j).getIfBlock().getDefineNodes().get(k).getMacroName()); //feature is the variable of the define or undef
                                        solver.addClause(feature, featureImplications);
                                    }
                                }
                            }
                        }


                    }
                }*/

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
                            featureImplication = new FeatureImplication(changedNodeParent + " && " + children.getIfBlock().getParent().getParent().getCondition() + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName() + " " + ((Define) definedNode).getMacroExpansion());
                        else
                            featureImplication = new FeatureImplication(changedNodeParent + " && " + children.getIfBlock().getCondition(), definedNode.getMacroName() + " " + ((Define) definedNode).getMacroExpansion());
                        featureImplications.add(featureImplication);

                    } else { //undef
                        if (!children.getIfBlock().getParent().getParent().getCondition().contains("BASE"))
                            featureImplication = new FeatureImplication(changedNodeParent + " && " + children.getIfBlock().getParent().getParent().getCondition() + " && " + children.getIfBlock().getCondition(),  definedNode.getMacroName());
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

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
        String repoPath = "C:\\Users\\gabil\\Desktop\\ECCO_Work\\test-featureid";
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
                                    changedNode.addDefineNode(defineNodeInsideFeature);
                                    definedNodes.add(defineNodeInsideFeature);
                                } else if (ppstatementsInsideFeature instanceof IASTPreprocessorUndefStatement) {
                                    Undef undefNodeInsideFeature = new Undef(((IASTPreprocessorUndefStatement) ppstatementsInsideFeature).getMacroName().toString(), 1);
                                    changedNode.addDefineNode(undefNodeInsideFeature);
                                    definedNodes.add(undefNodeInsideFeature);
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
            if (changedNode.getCondition() != "BASE") {
                IFCondition changedNodeParent = (IFCondition) changedNode.getParent().getIfBlock().getParent().getParent();
                for (ConditionBlockNode children : changedNodeParent.getChildren()) {
                    if (children.getIfBlock().getDefineNodes().size() > 0) {
                            for (DefineNode definedNode : children.getIfBlock().getDefineNodes()) {
                                if (definedNode instanceof Define && definedNode.getLineInfo()<changedNode.getLineFrom()) {
                                    featureImplication = new FeatureImplication("BASE && " + children.getIfBlock().getCondition(), definedNode.getMacroName());
                                    featureImplications.add(featureImplication);
                                    feature = new Feature(definedNode.getMacroName()); //feature is the variable of the define or undef
                                    solver.addClause(feature, featureImplications);
                                } else if(definedNode instanceof Undef && definedNode.getLineInfo()<changedNode.getLineFrom()){ //undef
                                    featureImplication = new FeatureImplication("BASE && " + children.getIfBlock().getCondition(), "!" + definedNode.getMacroName());
                                    featureImplications.add(featureImplication);
                                    feature = new Feature(definedNode.getMacroName()); //feature is the variable of the define or undef
                                    solver.addClause(feature, featureImplications);
                                }

                            }
                    }
                    if (children.getIfBlock().getLineTo() < changedNode.getLineFrom()) {
                        searchingDefineNodes(children, solver, featureImplications);
                    }
                }
                if(solver.getVars().size() > 0) {
                    solver.setExpr(changedNode.getCondition() + " && BASE");
                    Map<Feature, Integer> result = solver.solve();
                    solver.reset();
                    pph.generateVariants(result, gitFolder, eccoFolder);
                    System.out.println("\nCONFIG FOR PREPROCESSING CHANGEDNODE: " + changedNode.getCondition());
                    result.entrySet().forEach(x -> System.out.print(x.getKey() + " = " + x.getValue() + "; "));
                    System.out.println("\n");
                }else{
                    System.out.println("\nThe Feature "+changedNode.getCondition()+" has no implications in its change!");
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
       /*
        featureImplications = new LinkedList<FeatureImplication>();
        FeatureImplication featureImplication;
        solver.setExpr("A");
        featureImplication = new FeatureImplication("A", "B");
        featureImplications.add(featureImplication);
        feature = new Feature("A");
        solver.addClause(feature, featureImplications);
       -----------
       Model model = new Model("test1");
        BoolVar a = model.boolVar("A");
        BoolVar b = model.boolVar("B");
        model.addClauses(implies(a,b));
        model.post(b.extension());

        Solution s = model.getSolver().findSolution();
        System.out.println(s);
        */

    }

    public void searchingDefineNodes(ConditionBlockNode children, ExpressionSolver solver, Queue<FeatureImplication> featureImplications) {
        FeatureImplication featureImplication;
        Feature feature = null;

        if (children.getIfBlock().getChildren().size() > 0) {
            for (ConditionBlockNode childrenChildren : children.getIfBlock().getChildren()) {
                searchingDefineNodes(childrenChildren, solver, featureImplications);
            }
        } else {
            if (children.getIfBlock().getDefineNodes().size() > 0) {
                for (DefineNode definedNode : children.getIfBlock().getDefineNodes()) {
                    if (definedNode instanceof Define) {
                        featureImplication = new FeatureImplication("BASE && " + children.getIfBlock().getCondition(), definedNode.getMacroName()+" "+((Define) definedNode).getMacroExpansion());
                        featureImplications.add(featureImplication);
                    } else { //undef
                        featureImplication = new FeatureImplication("BASE && " + children.getIfBlock().getCondition(), "!" + definedNode.getMacroName());
                        featureImplications.add(featureImplication);
                    }
                    feature = new Feature(definedNode.getMacroName()); //feature is the variable of the define or undef
                    solver.addClause(feature, featureImplications);
                }
            }
        }
    }

}

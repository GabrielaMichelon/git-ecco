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
import at.jku.isse.gitecco.translation.constraintcomputation.util.ChangeConstraint;
import at.jku.isse.gitecco.translation.constraintcomputation.util.GetNodesForChangeVisitor;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.anarres.cpp.featureExpr.CondExpr;
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
        String repoPath = "C:\\Users\\gabil\\Desktop\\ECCO_Work\\test12";
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

                        Change allClass = new Change(0, change.getTo());
                        visitor.setChange(allClass);
                        child.accept(visitor);
                        ArrayList<ConditionalNode> classNodes = new ArrayList<>();

                        classNodes.addAll(visitor.getchangedNodes());
                        ChangeConstraint changeConstraint = new ChangeConstraint();
                        changeConstraint.constructConstraintPerFeature(classNodes, changedNodes, gitHelper, change, visitor, child);


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

}

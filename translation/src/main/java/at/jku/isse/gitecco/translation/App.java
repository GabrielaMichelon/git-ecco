package at.jku.isse.gitecco.translation;

import at.jku.isse.gitecco.core.cdt.CDTHelper;
import at.jku.isse.gitecco.core.git.Change;
import at.jku.isse.gitecco.core.git.GitCommitList;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.solver.ExpressionSolver;
import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.translation.constraintcomputation.util.GetNodesForChangeVisitor;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.core.runtime.CoreException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                if (child instanceof SourceFileNode && changedFiles.contains(child.getFilePath().replace("/", "\\"))) {
                    Change[] changes = null;
                    try {
                        changes = gitHelper.getFileDiffs(gc, child);
                    } catch (Exception e) {
                        System.err.println("error while executing the file diff: " + child.getFilePath());
                        e.printStackTrace();
                    }

                    for (Change change : changes) {
                        visitor.setChange(change);
                        child.accept(visitor);
                        changedNodes.addAll(visitor.getchangedNodes());
                        for (ConditionalNode changedNode : changedNodes) {
                            int init = changedNode.getLineFrom();
                            int end = changedNode.getLineTo();

                            String fileContent = null;
                            Path path = Paths.get(gitHelper.getPath() + File.separator + child.getFilePath());
                            try {
                                fileContent = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            String[] lines = fileContent.split("\\r?\\n");
                            String code="";
                            if (init == 0) {
                                for (int i = init; i < end; i++) {
                                    //System.out.println("Line " + i + ": " + lines[i]);
                                }
                            } else {
                                for (int i = init+1; i < end; i++) {
                                    code+=lines[i-1]+"\n";
                                 //   System.out.println("Line " + i + ": " + lines[i-1]);
                                }
                            }
                            IASTTranslationUnit translationUnit = null;
                            try {
                                translationUnit = CDTHelper.parse(code.toCharArray());
                            } catch (CoreException e) {
                                e.printStackTrace();
                            }
                            final IASTPreprocessorStatement[] ppstatements = translationUnit.getAllPreprocessorStatements();
                            for (IASTPreprocessorStatement ppstatementsInsideFeature : ppstatements) {
                                if( ppstatementsInsideFeature instanceof IASTPreprocessorMacroDefinition){
                                    Define defineNodeInsideFeature = new Define(((IASTPreprocessorMacroDefinition) ppstatementsInsideFeature).getName().toString(), ((IASTPreprocessorMacroDefinition) ppstatementsInsideFeature).getExpansion().toString(),1);
                                    changedNode.addDefineNode(defineNodeInsideFeature);
                                }
                            }

                            System.out.println(ppstatements.toString());
                        }
                    }
                }
            }

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
            }

        });

        gitHelper.getAllCommits(commitList);

    }

}

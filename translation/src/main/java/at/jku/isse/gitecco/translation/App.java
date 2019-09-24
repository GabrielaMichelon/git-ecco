package at.jku.isse.gitecco.translation;

import at.jku.isse.gitecco.core.git.Change;
import at.jku.isse.gitecco.core.git.GitCommitList;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.tree.nodes.ConditionalNode;
import at.jku.isse.gitecco.core.tree.nodes.FileNode;
import at.jku.isse.gitecco.core.tree.nodes.SourceFileNode;
import at.jku.isse.gitecco.translation.constraintcomputation.util.ChangeConstraint;
import at.jku.isse.gitecco.translation.visitor.GetNodesForChangeVisitor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class App {


    public static void main(String... args) throws Exception {
        final boolean debug = true;
        //TODO: planned arguments: DEBUG, dispose tree, max commits, repo path, csv path(feature id), outpath for ecco
        //maybe even start commit and/or end commit (hashes or numbers)
        //String repoPath = "C:\\obermanndavid\\git-ecco-test\\test_repo_gabi";
        String repoPath = "C:\\obermanndavid\\git-ecco-test\\test_featureid\\Marlin";

        //optional features of the project obtained by the featureID (chosen that which is in almost cases external feature)
        String[] featuresToAdd = {"BASE","F_FILE_DIR_DIRTY", "F_UNUSED", "F_FILE_UNBUFFERED_READ", "RAMPS_V_1_0", "__AVR_ATmega2560__", "F_CPU", "F_OFLAG", "WATCHPERIOD",
                                  "THERMISTORHEATER","THERMISTORBED", "TSd2PinMap_hHERMISTORHEATER", "PID_DEBUG", "HEATER_USES_THERMISTOR", "__AVR_ATmega328P__", "__AVR_ATmega1280__", "__AVR_ATmega168__",
                                  "ADVANCE", "PID_OPENLOOP", "SDSUPPORT", "BED_USES_THERMISTOR", "SIMPLE_LCD", "NEWPANEL", "DEBUG_STEPS", "BED_USES_AD595", "ARDUINO",
                                  "HEATER_1_USES_THERMISTOR", "THERMISTORHEATER_1", "HEATER_USES_THERMISTOR_1", "HEATER_2_USES_AD595", "HEATER_1_MAXTEMP", "THERMISTORHEATER_0",
                                   "HEATER_1_MINTEMP", "HEATER_0_USES_THERMISTOR", "RESET_MANUAL", "PID_PID"};
        ArrayList<String> featureList = new ArrayList<>();
        for(String feat : featuresToAdd) {
            featureList.add(feat);
        }
        //add directories that we need to include manually to get all the files to create a clean version because "/usr/local/include"
        // and "/usr/include")does not includes files outside the root path
        final List<String> dirFiles = new ArrayList<>();
        //dirFiles.add("C:\\Users\\gabil\\Desktop\\ECCO_Work\\TestMarlin\\Marlin\\Marlin\\Marlin\\Marlin");
        final GitHelper gitHelper = new GitHelper(repoPath, dirFiles);
        final GitCommitList commitList = new GitCommitList(gitHelper);

        commitList.addGitCommitListener((gc, gcl) -> {

            //TODO: do the git commit and measure time or whatever
            //for a guide how to make a git commit with jgit: git-ecco commit a071bdd677d9a7555f60e026a4b0ba975be09792
            //file GitCommitList.java method: enableAutoCommitConfig()

            GetNodesForChangeVisitor visitor = new GetNodesForChangeVisitor();
            Set<ConditionalNode> changedNodes = new HashSet<>();
            List<String> changedFiles = gitHelper.getChangedFiles(gc);
            //directory to save the output
            final File outputDirectory = new File("C:\\obermanndavid\\git-ecco-test\\test_repo_gabi_log"+gcl.getBranch(gc,gcl));


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
                        changeConstraint.setFeatureList(featureList);
                        //create the constraints for each changed node and generates the variant
                        changeConstraint.constructConstraintPerFeature(classNodes, changedNodes, gitHelper, change, visitor, child, outputDirectory, gc.getTree());

                        //TODO: variant generation
                        //TODO: Map<Feature, Integer> result --> config for preprocssing
                        //TODO: ecco stuff

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

                //previous constraints and affected constraints
                //new class that takes a changed node and walks up the tree and build the implication queue.
                //same for the affected blocks --> tree might need additional methods
                //for retrieving conjunctive conditions that are affected by a changed block.



                solver.setExpr(changedNode.getCondition());
                Map<Feature, Integer> result = solver.solve();
                solver.reset();
                pph.generateVariants(result, gitFolder, eccoFolder);
                System.out.println("CONFIG FOR PREPROCESSING:");
                result.entrySet().forEach(x -> System.out.print(x.getKey() + " = " + x.getValue() + "; "));

                //ecco commit with solution + marked as changed
            }*/

        });

        gitHelper.getAllCommits(commitList);

    }

}

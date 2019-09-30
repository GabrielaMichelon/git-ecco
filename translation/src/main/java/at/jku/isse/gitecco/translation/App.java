package at.jku.isse.gitecco.translation;

import at.jku.isse.gitecco.core.git.Change;
import at.jku.isse.gitecco.core.git.GitCommitList;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.tree.nodes.ConditionalNode;
import at.jku.isse.gitecco.core.tree.nodes.FileNode;
import at.jku.isse.gitecco.core.tree.nodes.SourceFileNode;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.core.type.FeatureImplication;
import at.jku.isse.gitecco.translation.constraintcomputation.util.ConstraintComputer;
import at.jku.isse.gitecco.translation.visitor.BuildImplicationsVisitor;
import at.jku.isse.gitecco.translation.visitor.GetNodesForChangeVisitor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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

            GetNodesForChangeVisitor visitor = new GetNodesForChangeVisitor();
            Set<ConditionalNode> changedNodes = new HashSet<>();
            List<String> changedFiles = gitHelper.getChangedFiles(gc);

            //retrieve changed nodes
            for (FileNode child : gc.getTree().getChildren()) {
                if(child instanceof SourceFileNode && changedFiles.contains(child.getFilePath().replace("/","\\"))) {
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
                    }
                }
            }


            final File gitFolder = new File(gitHelper.getPath());
            final File eccoFolder = new File(gitFolder.getParent(), "ecco");
            final ConstraintComputer constraintComputer = new ConstraintComputer(featureList);
            final PreprocessorHelper pph = new PreprocessorHelper();
            Map<Feature, Integer> config;

            //for each changed node:
            //compute config
            //compute changed
            //do preprocessing/variant gen
            //ecco commit
            for (ConditionalNode changedNode : changedNodes) {
                //compute the config for the var gen
                config = constraintComputer.computeConfig(changedNode, gc.getTree());
                //generate the variant for this config
                pph.generateVariants(config, gitFolder, eccoFolder, gitHelper.getDirFiles());
                //TODO: compute the marked as changed features.
                //TODO: test
                //TODO: ecco commit with solution + marked as changed
            }

        });

        gitHelper.getAllCommits(commitList);

    }

}

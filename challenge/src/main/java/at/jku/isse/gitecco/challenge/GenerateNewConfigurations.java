package at.jku.isse.gitecco.challenge;

import at.jku.isse.gitecco.core.git.GitCommit;
import at.jku.isse.gitecco.core.git.GitCommitList;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.type.Feature;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenerateNewConfigurations {
    static boolean EVERYCOMMIT = false;
    static int EVERY_NTH_COMMIT = 1;
    static ArrayList<String> featureNamesList = new ArrayList<String>();
    static Integer commitInit = 0;
    static Integer commitEnd = 1;
    static String REPO_PATH = "";//"C:\\Users\\gabil\\Desktop\\PHD\\ChallengePaper\\TestScript\\LibSSH\\libssh";
    static String FEATURES_PATH = "";//"C:\\Users\\gabil\\Desktop\\PHD\\ChallengePaper\\TestScript\\LibSSH";
    static GitCommit gcPrevious = null;
    static Boolean previous = true;
    static List<String> configurations = new ArrayList<>();
    static Map<Feature, Integer> featureVersions = new HashMap<>();


    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            REPO_PATH = args[0];
            FEATURES_PATH = args[1];
            if (REPO_PATH.contains("//")) {
                REPO_PATH.replaceAll("//", File.separator);
            }
            if (REPO_PATH.contains("\\\\")) {
                REPO_PATH.replaceAll("\\\\", File.separator);
            }
            if (FEATURES_PATH.contains("//")) {
                FEATURES_PATH.replaceAll("//", File.separator);
            }
            if (FEATURES_PATH.contains("\\\\")) {
                FEATURES_PATH.replaceAll("\\\\", File.separator);
            }
            commitInit = Integer.valueOf(args[2]);
            commitEnd = Integer.valueOf(args[3]);

            System.out.println("\u001B[32m" + "Mining process started. It can take minutes or hours...");
            identification();
            System.out.println("\u001B[32m" + "Process finished!!");
        }
    }

    public static void identification() throws Exception {
        String repoPath;
        repoPath = REPO_PATH;

        final GitHelper gitHelper = new GitHelper(repoPath, null);
        GitCommitList commitList = new GitCommitList(gitHelper);

        if (EVERYCOMMIT) {
            //gitHelper.getAllCommits(commitList);
        } else {
            gitHelper.getEveryNthCommit3(commitList, null, commitInit, commitEnd, EVERY_NTH_COMMIT);
            String folderRelease = FEATURES_PATH;
            initVars();
            for (GitCommit commits : commitList) {
                configurations.clear();
                generateVariants(gitHelper, commits, featureNamesList);
                //dispose tree if it is not needed -> for memory saving reasons.
                commits.disposeTree();
            }

        }

        System.out.println("finished analyzing repo");

    }


    public static void initVars() {
        gcPrevious = null;
        previous = true;
        configurations.clear();
    }

    public static void generateVariants(GitHelper gitHelper, GitCommit gc, ArrayList<String> featureNamesList) throws Exception {
        final File gitFolder = new File(gitHelper.getPath());
        final File randomVariantFolder = new File(gitFolder.getParent(), "newConfigurations");
        final PreprocessorHelper pph = new PreprocessorHelper();

        //generate the variant for this config
        for (Map.Entry<Map<Feature, Integer>, String> variant : configsToGenerateVariant.entrySet()) {
            String eccoConfig = variant.getValue();
            System.out.println("------ Variant to generate with config: " + eccoConfig);
            Map<Feature, Integer> test = new HashMap<>();
            for (Map.Entry<Feature, Integer> mapconfig : variant.getKey().entrySet()) {
                    test.put(mapconfig.getKey(), 1);
            }
            gitHelper.checkOutCommit(gc);
            pph.generateVariants(test, gitFolder, randomVariantFolder, gitHelper.getDirFiles(), eccoConfig);
            System.out.println("Variant generated with config: " + eccoConfig);

        }
    }


}

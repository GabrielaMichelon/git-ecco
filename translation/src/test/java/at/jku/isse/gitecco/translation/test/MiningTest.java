package at.jku.isse.gitecco.translation.test;

import at.jku.isse.gitecco.core.git.GitCommitList;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.featureid.type.TraceableFeature;
import at.jku.isse.gitecco.translation.mining.ComputeRQMetrics;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.lang.Thread.sleep;

public class MiningTest {

    private final static boolean DISPOSE = true;
    private final static int MAX_COMMITS = 500;
    private final static boolean EVERYCOMMIT = false;
    private final static int STARTCOMMIT = 0;
    private final static int ENDCOMMIT = 50;
    private final static int EVERY_NTH_COMMIT = 1;
    private final static boolean MAX_COMMITS_ENA = true;
    private final static boolean PARALLEL = false;
    private final static ArrayList<Feature> featureList = new ArrayList<>();
    private final static ArrayList<String> featureNamesList = new ArrayList<String>();
    private final static String REPO_PATH = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\systems\\Bison\\bison";
    String[] features = {"BASE", "TRACE", "DEBUG", "MSDOS", "eta10", "__GO32__", "DONTDEF", "VMS", "HAVE_ALLOCA_H", "__GNUC__", "_AIX", "__STDC__", "HAVE_STDLIB_H", "HAVE_MEMORY_H", "STDC_HEADERS"};


    @Test
    public void getFeatureCharacteristics() throws Exception {

        final GitHelper gitHelper = new GitHelper(REPO_PATH, null);
        final GitCommitList commitList = new GitCommitList(gitHelper);
        final List<Future<?>> tasks = new ArrayList<>();
        final ExecutorService executorService = Executors.newFixedThreadPool(30);
        addFeatures();

        commitList.addGitCommitListener(
                (gc, gcl) -> {
                    if(gcl.size() > MAX_COMMITS && MAX_COMMITS_ENA) {
                        // writeToCsv(evaluation, csvPath);
                        executorService.shutdownNow();
                        System.exit(0);
                    }

                    if(PARALLEL) {
                        tasks.add(
                                executorService.submit(() -> {
                                    ComputeRQMetrics.CharacteristicsFeature(gc.getTree(), featureList,featureNamesList);
                                    //dispose tree if it is not needed -> for memory saving reasons.
                                    if (DISPOSE) gc.disposeTree();
                                })
                        );
                    } else {
                        ComputeRQMetrics.CharacteristicsFeature(gc.getTree(), featureList,featureNamesList);
                        //dispose tree if it is not needed -> for memory saving reasons.
                        if (DISPOSE) gc.disposeTree();
                    }
                }
        );


        if(EVERYCOMMIT) {
            gitHelper.getAllCommits(commitList);
        } else {
            gitHelper.getEveryNthCommit(commitList, null,STARTCOMMIT, ENDCOMMIT, EVERY_NTH_COMMIT);
        }

        while(PARALLEL && !isDone(tasks)) sleep(100);
        executorService.shutdownNow();

        //print to CSV:
        //writeToCsv(evaluation, csvPath);

        System.out.println("finished analyzing repo");
    }


    public void addFeatures(){
        for (String feat : features) {
            Feature feature = new Feature(feat);
            featureNamesList.add(feat);
            featureList.add(feature);
        }
    }

    /**
     * Helper method to check if all tasks are done.
     * @param tasks
     * @return
     */
    private static boolean isDone(List<Future<?>> tasks) {
        for (Future task : tasks)
            if(!task.isDone()) return false;

        return true;
    }
}

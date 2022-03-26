package at.jku.isse.gitecco.translation.test;

import at.jku.isse.gitecco.core.git.GitCommitList;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.type.Feature;
import com.google.common.collect.Multimap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PropagationFeatureRevisionsReleasesTest {

    private static String REPO_PATH = "C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\ChangePropagation\\Bison";
    private static String FEATURES_PATH = "C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\ICSME or ASE 2022\\dataset\\Bison";
    private static String MINING_DATASET = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\LOC_fix\\Bison\\Mining";
    private static String stopRelease = "v3.5.3";
    private static ArrayList<String> featureNamesListDestinationRelease = new ArrayList<String>();
    static ArrayList<Feature> featurespropagate = new ArrayList<>();
    private static String pathFeatures = MINING_DATASET+File.separator+"features-";
    private static String FEATURES_TXT = pathFeatures;
    private static String FEATURESPROPRAGATE_TXT = pathFeatures;


    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            REPO_PATH = args[0];
            if (REPO_PATH.contains("//")) {
                REPO_PATH.replaceAll("//", File.separator);
            }
            if (REPO_PATH.contains("\\\\")) {
                REPO_PATH.replaceAll("\\\\", File.separator);
            }
            identification();
        } else {
            identification();
        }
    }


    public static void identification() throws Exception {
        long measure = System.currentTimeMillis();
        String repoPath = REPO_PATH;

        final GitHelper gitHelper = new GitHelper(repoPath, null);
        GitCommitList commitList = new GitCommitList(gitHelper);

        Multimap<Long, String> mapTags = gitHelper.getCommitHashTag();
        Iterator<Long> keyIt = mapTags.keySet().iterator();

        long key1 = keyIt.next();
        long key2 = keyIt.next();
        List<String> val1 = new ArrayList<>(mapTags.get(key1));
        List<String> val2 = new ArrayList<>(mapTags.get(key2));
        System.out.println(key1 + " " + val1.get(0) + " " + val1.get(1));
        System.out.println(key2 + " " + val2.get(0) + " " + val2.get(1));
        FEATURES_TXT += val1.get(0) + ".txt";
        featureNamesListDestinationRelease = new ArrayList<>();
        addFeaturesReleaseDestination();
        FEATURESPROPRAGATE_TXT += val2.get(0) + ".txt";;
        featurespropagate = new ArrayList<>();
        addFeaturesPropagate();

        while (keyIt.hasNext()) {
            key1 = keyIt.next();
            key2 = keyIt.next();
            val1 = new ArrayList<>(mapTags.get(key1));
            val2 = new ArrayList<>(mapTags.get(key2));
            if(val1.get(0).equals(stopRelease))
                break;
            System.out.println(key1 + " " + val1.get(0) + " " + val1.get(1));
            System.out.println(key2 + " " + val2.get(0) + " " + val2.get(1));
            FEATURES_TXT = pathFeatures+val1.get(0) + ".txt";
            featureNamesListDestinationRelease = new ArrayList<>();
            addFeaturesReleaseDestination();
            FEATURESPROPRAGATE_TXT = pathFeatures+val2.get(0) + ".txt";;
            featurespropagate = new ArrayList<>();
            addFeaturesPropagate();
        }
    }

    public static void addFeaturesPropagate() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(FEATURESPROPRAGATE_TXT));
            String line = "";
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] cols = line.split(",");
                if (!featurespropagate.contains(cols[0].substring(1).replace("\"", ""))) {
                    Feature featpropagate = new Feature(cols[0].substring(1).replace("\"", ""));
                    featurespropagate.add(featpropagate);
                }
                for (int i = 1; i < cols.length - 1; i++) {
                    if (!featurespropagate.contains(cols[i].replace("\"", ""))) {
                        Feature featpropagate = new Feature(cols[i].replace("\"", ""));
                        featurespropagate.add(featpropagate);
                    }
                }
                String lastfeature = cols[cols.length - 1].replace("\"", "");
                if (!featurespropagate.contains(lastfeature.replace("}", ""))) {
                    Feature featpropagate = new Feature(lastfeature.replace("}", ""));
                    featurespropagate.add(featpropagate);
                }
                if (!featurespropagate.contains("BASE")) {
                    Feature featpropagate = new Feature("BASE");
                    featurespropagate.add(featpropagate);
                }
            }
        } catch (IOException ex) {
            System.out.println((char) 27 + "[31m" + "Error reading the features text file!\nVerify if you wrote in the correct format -> {\"WITH_SERVER\",\"HAVE_LIBZ\",...}");
            System.exit(0);
        }
    }

    public static void addFeaturesReleaseDestination() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(FEATURES_TXT));
            String line = "";
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] cols = line.split(",");
                if (!featureNamesListDestinationRelease.contains(cols[0].substring(1).replace("\"", "")))
                    featureNamesListDestinationRelease.add(cols[0].substring(1).replace("\"", ""));
                for (int i = 1; i < cols.length - 1; i++) {
                    if (!featureNamesListDestinationRelease.contains(cols[i].replace("\"", "")))
                        featureNamesListDestinationRelease.add(cols[i].replace("\"", ""));
                }
                String lastfeature = cols[cols.length - 1].replace("\"", "");
                if (!featureNamesListDestinationRelease.contains(lastfeature.replace("}", "")))
                    featureNamesListDestinationRelease.add(lastfeature.replace("}", ""));
                if (!featureNamesListDestinationRelease.contains("BASE"))
                    featureNamesListDestinationRelease.add("BASE");
            }
        } catch (IOException ex) {
            System.out.println((char) 27 + "[31m" + "Error reading the features text file!\nVerify if you wrote in the correct format -> {\"WITH_SERVER\",\"HAVE_LIBZ\",...}");
            System.exit(0);
        }
    }

}

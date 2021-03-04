package at.jku.isse.gitecco.challenge;

import at.jku.isse.gitecco.core.git.Change;
import at.jku.isse.gitecco.core.git.GitCommit;
import at.jku.isse.gitecco.core.git.GitCommitList;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.tree.nodes.BaseNode;
import at.jku.isse.gitecco.core.tree.nodes.ConditionalNode;
import at.jku.isse.gitecco.core.tree.nodes.FileNode;
import at.jku.isse.gitecco.core.tree.nodes.SourceFileNode;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.featureid.identification.ID;
import at.jku.isse.gitecco.featureid.type.TraceableFeature;
import at.jku.isse.gitecco.translation.constraintcomputation.util.ConstraintComputer;
import at.jku.isse.gitecco.translation.visitor.GetNodesForChangeVisitor;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import org.glassfish.grizzly.http.server.accesslog.FileAppender;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ChallengeCase {

    static boolean EVERYCOMMIT = false;
    static int EVERY_NTH_COMMIT = 1;
    static ArrayList<Feature> featureList = new ArrayList<>();
    static ArrayList<String> featureNamesList = new ArrayList<String>();
    //set as true to generate random variants or false to just generate original variants
    static boolean generateRandomVariants = false;
    //set as true to generate PP variants or false to just generate configurations to generate random variants
    static boolean generateOriginalVariants = true;
    static Integer commitInit = 0;
    static Integer commitEnd = 1;
    static String REPO_PATH = "";//"C:\\Users\\gabil\\Desktop\\PHD\\ChallengePaper\\TestScript\\LibSSH\\libssh";
    static String FEATURES_PATH = "";//"C:\\Users\\gabil\\Desktop\\PHD\\ChallengePaper\\TestScript\\LibSSH";
    static String FEATURES_TXT = "";
    static String fileReportFeature = "features_report_each_project_commit.csv";
    static String fileStoreConfig = "inputConfigurations.csv";
    static String fileStoreRandomConfig = "newConfigurations.csv";
    static String featuretxt = File.separator + "features.txt";
    //String featuretxt = "\\features-first500commits.txt";
    static List<String> changedFiles = new ArrayList<>();
    static List<String> changedFilesNext = new ArrayList<>();
    static GitCommit gcPrevious = null;
    static Boolean previous = true;
    static List<String> configurations = new ArrayList<>();
    static Map<Feature, Integer> featureVersions = new HashMap<>();
    static String feats;


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
            if (args[4].equals("true")) {
                generateRandomVariants = true;
                generateOriginalVariants = false;
            }
            FEATURES_TXT = args[5];
            identification();
        }
    }

    public static void identification() throws Exception {

        //long measure = System.currentTimeMillis();
        String repoPath;
        repoPath = REPO_PATH;

        final GitHelper gitHelper = new GitHelper(repoPath, null);
        GitCommitList commitList = new GitCommitList(gitHelper);

        if (EVERYCOMMIT) {
            //gitHelper.getAllCommits(commitList);
        } else {
            Map<Long, String> mapTags = gitHelper.getCommitNumberTag();
            LinkedHashMap<Long, String> orderedMap = mapTags.entrySet() //
                    .stream() //
                    .sorted(Map.Entry.comparingByKey()) //
                    .collect(Collectors.toMap(Map.Entry::getKey, //
                            Map.Entry::getValue, //
                            (key, content) -> content, //
                            LinkedHashMap::new)); //

            int i = 0;
            //Boolean analyze = false;
            //for (Map.Entry<Long, String> releases : orderedMap.entrySet()) {
            //    System.out.println("TAG: " + releases.getValue());
            //if (releases.getValue().equals("refs/tags/3.1.5") || analyze) {
            //analyze = true;
            //gitHelper.getEveryNthCommit2(commitList, releases.getValue(), null, i, Math.toIntExact(releases.getKey()), EVERY_NTH_COMMIT);
            gitHelper.getEveryNthCommit3(commitList, null, commitInit, commitEnd, EVERY_NTH_COMMIT);
            //i = Math.toIntExact(releases.getKey()) + 1;
            String folderRelease = FEATURES_PATH;
            //final File folder = new File(FEATURES_PATH, "FeatureCharacteristic");
            // if the directory does not exist, create it
            //File foldereachRelease = new File(folderRelease + File.separator + releases.getValue().substring(10));
            //String releases = "first500commits";
            String releases = "allcommits";
            File foldereachRelease = new File(folderRelease);
            if (!foldereachRelease.exists())
                foldereachRelease.mkdir();
            final File idFeatsfolder = new File(foldereachRelease, "IdentifiedFeatures");
            if (!idFeatsfolder.exists())
                idFeatsfolder.mkdir();
            //feature identification
            if(FEATURES_TXT.equals("false")) {
                identifyFeatures(commitList, releases, idFeatsfolder);
            }
            addFeatures();
            initVars(foldereachRelease.getAbsolutePath());
            File fileconfig = new File(folderRelease, fileStoreConfig);
            if (fileconfig.exists())
                fileconfig.delete();
            for (GitCommit commits : commitList) {
                //ComputeRQMetrics.characteristicsFeature(folder, commits.getNumber(), commits.getTree(), featureNamesList);
                configurations.clear();
                generateVariants(gitHelper, commits, featureNamesList, foldereachRelease.getAbsolutePath());
                //characteristicsChange2(gitHelper, changeFolder, commits, featureNamesList);
                //dispose tree if it is not needed -> for memory saving reasons.
                commits.disposeTree();
            }
            //RQ.2 How many times one feature changed along a number of Git commits?
            File filetxt = new File(foldereachRelease, "TimesEachFeatureChanged.txt");
            PrintWriter writerTXT = new PrintWriter(filetxt.getAbsolutePath(), "UTF-8");
            for (Map.Entry<Feature, Integer> featureRevision : featureVersions.entrySet()) {
                if (featureRevision.getValue() > 1)
                    writerTXT.println(featureRevision.getKey() + " Changed " + (featureRevision.getValue() - 1) + " times.");
            }
            writerTXT.close();
            //commitList = new GitCommitList(gitHelper);
            //} else {
            //    i = Math.toIntExact(releases.getKey()) + 1;
            //}
            //}
        }

        System.out.println("finished analyzing repo");

    }


    public static void initVars(String folderRelease) {
        changedFiles.clear();
        changedFilesNext.clear();
        gcPrevious = null;
        previous = true;
        configurations.clear();
        featureVersions.clear();

        //csv to report new features and features changed per git commit of the project
        //RQ.2 How many features changed per Git commit?
        try {
            FileWriter csvWriter = new FileWriter(folderRelease + File.separator + fileReportFeature);
            List<List<String>> headerRows = Arrays.asList(
                    Arrays.asList("CommitNumber", "NewFeatures", "ChangedFeatures")
            );
            for (List<String> rowData : headerRows) {
                csvWriter.append(String.join(",", rowData));
                csvWriter.append("\n");
            }
            csvWriter.flush();
            csvWriter.close();
        } catch (
                IOException e) {
            e.printStackTrace();
        }
        // end csv to report new features and features changed
    }

    public static void addFeatures() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(FEATURES_TXT));
        String line = "";
        while ((line = br.readLine()) != null) {
            // use comma as separator
            String[] cols = line.split(",");
            featureNamesList.add(cols[0].substring(1).replace("\"", ""));
            for (int i = 2; i < cols.length - 1; i++) {
                featureNamesList.add(cols[i].replace("\"", ""));
            }
            String lastfeature = cols[cols.length - 1].replace("\"", "");
            featureNamesList.add(lastfeature.replace("}", ""));
            if (!featureNamesList.contains("BASE"))
                featureNamesList.add("BASE");
        }
    }

    public static void generateVariants(GitHelper gitHelper, GitCommit gc, ArrayList<String> featureNamesList, String folder) throws Exception {
        final File gitFolder = new File(gitHelper.getPath());
        final File eccoFolder = new File(gitFolder.getParent(), "inputConfigurations");
        final File randomVariantFolder = new File(gitFolder.getParent(), "newConfigurations");

        Integer countFeaturesChanged = 0; //COUNT PER GIT COMMIT
        Integer newFeatures = 0; //COUNT PER GIT COMMIT

        GetNodesForChangeVisitor visitor = new GetNodesForChangeVisitor();
        ArrayList<ConditionalNode> changedNodes = new ArrayList<>();
        ArrayList<ConditionalNode> deletedNodes = new ArrayList<>();

        if (gc.getNumber() == Long.valueOf(0)) {
            gcPrevious = new GitCommit(gc.getCommitName(), gc.getNumber(), gc.getDiffCommitName(), gc.getBranch(), gc.getRevCommit());
            gcPrevious.setTree(gc.getTree());
        } else if (previous || gcPrevious != null) {
            if (gc.getNumber() - 1 < 1) {
                gcPrevious = new GitCommit(gc.getCommitName(), gc.getNumber(), gc.getDiffCommitName(), gc.getBranch(), gc.getRevCommit());
                gcPrevious.setTree(gc.getTree());
                System.out.println("---- commit name " + gc.getCommitName());
                previous = false;
            } else {
                if (gc.getNumber() == 0) {
                    gcPrevious = new GitCommit(gc.getCommitName(), gc.getNumber(), gc.getDiffCommitName(), gc.getBranch(), gc.getRevCommit());
                    gcPrevious.setTree(gc.getTree());
                } else if (previous || previous != null) {
                    if (gc.getNumber() - 1 < 1) {
                        previous = false;
                    } else {
                        gcPrevious = new GitCommit(gc.getRevCommit().getParent(0).getName(), gc.getNumber() - 1, gc.getRevCommit().getParent(0).getParent(0).getName(), gc.getBranch(), gc.getRevCommit().getParent(0));
                        GitCommitList gcl = new GitCommitList(gitHelper);
                        gcl.addTreeParent(gcPrevious, gc.getCommitName());
                    }
                }
            }
        }

        Map<Change, FileNode> changesDelete = new HashMap<>();
        //retrieve changed nodes
        for (FileNode child : gc.getTree().getChildren()) {
            if (child instanceof SourceFileNode) {

                Change[] changes = null;
                try {
                    changes = gitHelper.getFileDiffs(gc, child, false);
                } catch (Exception e) {
                    System.err.println("error while executing the file diff: " + child.getFilePath());
                    e.printStackTrace();
                }

                for (Change change : changes) {
                    if (change.getChangeType().equals("INSERT")) {
                        visitor.setChange(change);
                        child.accept(visitor, null);
                        changedNodes.addAll(visitor.getchangedNodes());
                    } else if (change.getChangeType().equals("DELETE")) {
                        changesDelete.put(change, child);
                    } else if (change.getChangeType().equals("CHANGE")) {
                        visitor.setChange(change);
                        child.accept(visitor, null);
                        changedNodes.addAll(visitor.getchangedNodes());
                    }

                }
            }
            if (gc.getNumber() == 0) {
                changedFiles.add(child.getFilePath());
                previous = false;
            } else {
                changedFilesNext.add(child.getFilePath());
            }
        }

        if (previous) {
            for (FileNode child : gcPrevious.getTree().getChildren()) {
                String start = child.getFilePath().replace("arent" + File.separator, "");
                changedFiles.add(start);
                previous = false;
            }
        }

        if (gc.getNumber() == 0 || previous) {
            previous = false;
        } else {
            //to retrieve changed nodes of deleted files
            if (gcPrevious != null) {
                for (String file : changedFiles) {
                    if (!changedFilesNext.contains(file)) {
                        FileNode child = gcPrevious.getTree().getChild(file);
                        if (child instanceof SourceFileNode) {
                            Change[] changes = null;
                            try {
                                changes = gitHelper.getFileDiffs(gc, child, true);
                            } catch (Exception e) {
                                System.err.println("error while executing the file diff: " + child.getFilePath());
                                e.printStackTrace();
                            }

                            for (Change change : changes) {
                                visitor.setChange(change);
                                child.accept(visitor, null);
                                deletedNodes.addAll(visitor.getchangedNodes());
                            }
                        }
                    }
                }
                for (Map.Entry<Change, FileNode> changeInsert : changesDelete.entrySet()) {
                    Change change = changeInsert.getKey();
                    FileNode childAux = changeInsert.getValue();
                    FileNode child = gcPrevious.getTree().getChild(childAux.getFilePath());
                    visitor.setChange(change);
                    child.accept(visitor, null);
                    deletedNodes.addAll(visitor.getchangedNodes());
                }
            }
            //next is changedFiles for the next commit
            changedFiles.removeAll(changedFiles);
            changedFiles.addAll(changedFilesNext);
            changedFilesNext.removeAll(changedFilesNext);
        }


        final ConstraintComputer constraintComputer = new ConstraintComputer(featureNamesList);
        final PreprocessorHelper pph = new PreprocessorHelper();
        Map<Feature, Integer> config;
        Set<Feature> changed;
        Set<Feature> alreadyComitted = new HashSet<>();
        Map<Integer, Map<Feature, Integer>> mapConfigToGenerateRandomVariants = new HashMap<>();
        Map<Integer, String> mapRevisionToGenerateRandomVariants = new HashMap<>();
        Map<Integer, String> mapRevisionToGenerateRandomVariantsFinal = new HashMap<>();
        Integer count = 0;

        //if there is no changed node then there must be a change in the binary files --> commit base.
        if ((changedNodes.size() == 0) && (deletedNodes.size() == 0) && (gc.getTree().getChildren().size() > 0))
            changedNodes.add(new BaseNode(null, 0));

        Boolean baseChanged = false;
        for (ConditionalNode nods : changedNodes) {
            if (nods instanceof BaseNode) {
                baseChanged = true;
            }
        }
        if (!baseChanged) {
            for (ConditionalNode nods : deletedNodes) {
                if (nods instanceof BaseNode) {
                    baseChanged = true;
                }
            }
        }

        ArrayList<String> configsToCommit = new ArrayList<>();
        Map<Map<Feature, Integer>, String> configsToGenerateVariant = new HashMap<>();
        Feature base = new Feature("BASE");
        //changedNodes = changedNodes.stream().filter(x -> x.getLocalCondition().equals("__AVR_ATmega644P__ || __AVR_ATmega644__")).collect(Collectors.toSet());
        for (ConditionalNode changedNode : changedNodes) {
            //compute the config for the var gen
            config = constraintComputer.computeConfig(changedNode, gc.getTree());
            if (config != null && !config.isEmpty()) {
                //compute the marked as changed features.
                changed = constraintComputer.computeChangedFeatures(changedNode, config);
                if (!changed.contains(base))
                    config.remove(base);
                //map with the name of the feature and version and a boolean to set true when it is already incremented in the analysed commit
                String eccoConfig = "";
                //if (changedNode.getContainingFile() != null)
                for (Map.Entry<Feature, Integer> configFeature : config.entrySet()) {
                    int version = 0;
                    if (featureNamesList.contains(configFeature.getKey().getName()) && configFeature.getValue() != 0) {
                        if (featureVersions.containsKey(configFeature.getKey())) {
                            version = featureVersions.get(configFeature.getKey());
                        }
                        if (!alreadyComitted.contains(configFeature.getKey())) {
                            alreadyComitted.add(configFeature.getKey());
                            //for marlin: first commit is empty, thus we need the < 2. otherwise <1 should be enough.
                            if (changed.contains(configFeature.getKey())) { //|| gcl.size() < 2) {
                                version++;
                                if (version == 1)
                                    newFeatures++;
                                else
                                    countFeaturesChanged++;
                            }
                            if (version == 0 && !(changed.contains(configFeature.getKey()))) {
                                newFeatures++;
                                version = 1;
                            }
                            featureVersions.put(configFeature.getKey(), version);
                        }
                        if (!configFeature.getKey().toString().equals("BASE"))
                            eccoConfig += "," + configFeature.getKey().toString() + "." + version;
                        else
                            eccoConfig += "," + configFeature.getKey().toString() + ".$$";
                    }
                }
                if (!eccoConfig.contains("BASE")) {
                    eccoConfig += "," + "BASE.$$";
                }
                eccoConfig = eccoConfig.replaceFirst(",", "");

                if (configurations.contains(eccoConfig)) {
                    System.out.println("Config already used to generate a variant: " + eccoConfig);
                    //don't need to generate variant and commit it again at the same commit of the project git repository
                } else {
                    mapConfigToGenerateRandomVariants.put(count, config);
                    mapRevisionToGenerateRandomVariants.put(count, eccoConfig);
                    count++;
                    //configuration that will be used to generate the variant of this changed node
                    configsToGenerateVariant.put(config, eccoConfig);
                    configurations.add(eccoConfig);
                    //folder where the variant is stored
                    File variantsrc = new File(eccoFolder, eccoConfig);

                }
            }
        }

        if (deletedNodes.size() != 0) {
            for (ConditionalNode deletedNode : deletedNodes) {
                //compute the config for the var gen
                config = constraintComputer.computeConfig(deletedNode, gcPrevious.getTree());
                if (config != null && !config.isEmpty()) {
                    //compute the marked as changed features.
                    changed = constraintComputer.computeChangedFeatures(deletedNode, config);
                    int tanglingDegree = 0;
                    for (Map.Entry<Feature, Integer> feat : config.entrySet()) {
                        if (featureNamesList.contains(feat.getKey().getName()))
                            tanglingDegree++;
                    }
                    if (!changed.contains(base))
                        config.remove(base);
                    //map with the name of the feature and version and a boolean to set true when it is already incremented in the analysed commit
                    String eccoConfig = "";
                    for (Map.Entry<Feature, Integer> configFeature : config.entrySet()) {
                        int version = 0;
                        if (featureNamesList.contains(configFeature.getKey().getName()) && configFeature.getValue() != 0) {
                            if (featureVersions.containsKey(configFeature.getKey())) {
                                version = featureVersions.get(configFeature.getKey());
                            }
                            if (!alreadyComitted.contains(configFeature.getKey())) {
                                alreadyComitted.add(configFeature.getKey());
                                //for marlin: first commit is empty, thus we need the < 2. otherwise <1 should be enough.
                                if (changed.contains(configFeature.getKey())) { //gcl.size() < 2 ||
                                    version++;
                                    if (version == 1)
                                        newFeatures++;
                                    else
                                        countFeaturesChanged++;
                                }
                                featureVersions.put(configFeature.getKey(), version);
                            }
                            if (!configFeature.getKey().toString().equals("BASE"))
                                eccoConfig += "," + configFeature.getKey().toString() + "." + version;
                            else
                                eccoConfig += "," + configFeature.getKey().toString() + ".$$";
                        }
                    }
                    if (!eccoConfig.contains("BASE")) {
                        eccoConfig += "," + "BASE.$$";
                    }
                    eccoConfig = eccoConfig.replaceFirst(",", "");

                    if (configurations.contains(eccoConfig)) {
                        System.out.println("Config already used to generate a variant: " + eccoConfig);
                        //don't need to generate variant and commit it again at the same commit of the project git repository
                    } else {
                        mapConfigToGenerateRandomVariants.put(count, config);
                        mapRevisionToGenerateRandomVariants.put(count, eccoConfig);
                        count++;
                        //configuration that will be used to generate the variant of this changed node
                        configsToGenerateVariant.put(config, eccoConfig);
                        configurations.add(eccoConfig);
                    }
                }
            }
        }

        String baseVersion = "";
        for (Map.Entry<Feature, Integer> configFeature : featureVersions.entrySet()) {
            if (configFeature.getKey().equals(base))
                baseVersion = configFeature.getValue().toString();
        }

        //generate the variant for this config
        for (Map.Entry<Map<Feature, Integer>, String> variant : configsToGenerateVariant.entrySet()) {
            String eccoConfig = variant.getValue().replace("$$", baseVersion);
            if (generateOriginalVariants) {
                System.out.println("------ Variant to generate with config: " + eccoConfig);
                Map<Feature, Integer> test = new HashMap<>();
                for (Map.Entry<Feature, Integer> mapconfig : variant.getKey().entrySet()) {
                    if (mapconfig.getValue() == 1)
                        test.put(mapconfig.getKey(), 1);
                }
                gitHelper.checkOutCommit(gc);
                pph.generateVariants(test, gitFolder, eccoFolder, gitHelper.getDirFiles(), eccoConfig);
                System.out.println("Variant generated with config: " + eccoConfig);
            } else if (generateRandomVariants) {
                for (Map.Entry<Integer, String> map : mapRevisionToGenerateRandomVariants.entrySet()) {
                    int auxkey = map.getKey();
                    String newBase = map.getValue();
                    if (map.getValue().contains("BASE.$$")) {
                        newBase = map.getValue().replace("BASE.$$", "BASE." + baseVersion);
                    }
                    mapRevisionToGenerateRandomVariantsFinal.put(auxkey, newBase);
                }

            }
            //config that will be used to commit the variant generated with this changed node in ecco
            configsToCommit.add(eccoConfig);
        }

        //generate random variants
        if (generateRandomVariants) {
            if (countFeaturesChanged + newFeatures > 0) {
                Random random = new Random();
                int numberConfigs = 1;
                if (countFeaturesChanged + newFeatures > 1) {
                    if ((countFeaturesChanged + newFeatures) > 6)
                        numberConfigs = random.nextInt(6);
                    else
                        numberConfigs = random.nextInt((countFeaturesChanged + newFeatures) - 1) + (countFeaturesChanged + newFeatures);
                }
                Map<Feature, Integer> mapNewConfig = new HashMap<>();
                String featurerevision = "";
                ArrayList<Integer> positionsMap = new ArrayList<>();
                int positionMapToSelectConfig = random.nextInt(count);
                positionsMap.add(positionMapToSelectConfig);
                System.out.println(mapConfigToGenerateRandomVariants.entrySet());
                for (int i = 0; i < numberConfigs; i++) {
                    for (Map.Entry<Feature, Integer> map : mapConfigToGenerateRandomVariants.get(positionMapToSelectConfig).entrySet()) {
                        if (map.getValue() == 1)
                            mapNewConfig.put(map.getKey(), map.getValue());
                    }
                    featurerevision += "," + mapRevisionToGenerateRandomVariantsFinal.get(positionMapToSelectConfig);
                    positionMapToSelectConfig = random.nextInt(count);
                    if (positionsMap.contains(positionMapToSelectConfig)) {
                        while (!positionsMap.contains(positionMapToSelectConfig)) {
                            positionMapToSelectConfig = random.nextInt(count);
                        }
                    } else {
                        positionsMap.add(positionMapToSelectConfig);
                    }

                }
                featurerevision = featurerevision.replaceFirst(",", "");
                if (featurerevision.contains("$$")) {
                    featurerevision = featurerevision.replace("$$", baseVersion);
                }
                if (featurerevision.contains(",")) {
                    String[] featurerevisions = featurerevision.split(",");
                    ArrayList<String> allfeatures = new ArrayList<>();
                    String featuresToVariant = "";
                    for (String featRevision : featurerevisions) {
                        if (!allfeatures.contains(featRevision)) {
                            allfeatures.add(featRevision);
                            featuresToVariant += "," + featRevision;
                        }
                    }
                    featuresToVariant = featuresToVariant.replaceFirst(",", "");
                    featurerevision = featuresToVariant;
                    gitHelper.checkOutCommit(gc);
                    pph.generateVariants(mapNewConfig, gitFolder, randomVariantFolder, gitHelper.getDirFiles(), featuresToVariant);
                } else {
                    gitHelper.checkOutCommit(gc);
                    pph.generateVariants(mapNewConfig, gitFolder, randomVariantFolder, gitHelper.getDirFiles(), featurerevision);
                }
                //append random configuration to the csv file
                try {
                    String fileStr = gitFolder.getParent() + File.separator + fileStoreRandomConfig;
                    FileAppender csvWriter = new FileAppender(new File(fileStr));
                    List<List<String>> headerRows = Arrays.asList(
                            Arrays.asList(Long.toString(gc.getNumber()), gc.getCommitName(), featurerevision)
                    );
                    for (List<String> rowData : headerRows) {
                        csvWriter.append(String.join(",", rowData));
                    }
                    csvWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //end append random configuration
            }
        }
        //end generate random variants

        //appending to the config csv
        try {
            FileAppender csvWriter = new FileAppender(new File(folder, fileStoreConfig));
            for (String configs : configsToCommit) {
                List<List<String>> headerRows = Arrays.asList(
                        Arrays.asList(Long.toString(gc.getNumber()), gc.getCommitName(), configs)
                );
                for (List<String> rowData : headerRows) {
                    csvWriter.append(String.join(",", rowData));
                }
            }
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        //append results to the feature report csv
        try {
            FileAppender csvAppender = new FileAppender(new File(folder, fileReportFeature));
            List<List<String>> contentRows = Arrays.asList(
                    Arrays.asList(Long.toString(gc.getNumber()), newFeatures.toString(), countFeaturesChanged.toString())
            );
            for (List<String> rowData : contentRows) {
                csvAppender.append(String.join(",", rowData));
            }
            csvAppender.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //end append results to the feature report csv

        for (Map.Entry<Feature, Integer> featureRevision : featureVersions.entrySet()) {
            System.out.println(featureRevision.getKey() + "." + featureRevision.getValue());
        }

        changedNodes.clear();
        deletedNodes.clear();

    }

    public static void identifyFeatures(GitCommitList commitList, String release, File idFeatFolder) throws IOException {
        final List<TraceableFeature> evaluation = Collections.synchronizedList(new ArrayList<>());
        String csvFile = release.substring(release.lastIndexOf("/") + 1);
        int count = 0;
        for (GitCommit commit : commitList) {
            System.out.println("for git identifyFeatures" + commit.getCommitName());
            ID.evaluateFeatureMap(evaluation, ID.id(commit.getTree()), commit.getNumber());
            //dispose tree if it is not needed -> for memory saving reasons.
            if (count == commitList.size() - 1) {
                //commits.disposeTree();
                writeToCsv(evaluation, csvFile, idFeatFolder);
                getFeatureDeletedTimes(idFeatFolder);
            } //else
            //commits.disposeTree();
            count++;
        }
        evaluation.clear();


    }

    private static void writeToCsv(List<TraceableFeature> features, String fileName, File idFeatsFolder) throws IOException {

        System.out.println("writing to CSV");
        FileWriter outputfile = null;
        File csvFile = new File(FEATURES_PATH, "features-" + fileName + ".csv");
        featuretxt = File.separator + "features-" + fileName + ".txt";
        //second parameter is boolean for appending --> never append
        outputfile = new FileWriter(csvFile, false);
        feats = "{";

        // create CSVWriter object file writer object as parameter
        //deprecated but no other way available --> it still works anyways
        @SuppressWarnings("deprecation") CSVWriter writer = new CSVWriter(outputfile, ',', CSVWriter.NO_QUOTE_CHARACTER);

        //adding header to csv
        writer.writeNext(new String[]{"Label/FeatureName", "#total", "#external", "#internal", "#transient"});

        PrintWriter writerTXT = new PrintWriter(FEATURES_PATH + File.separator + "features-" + fileName + ".txt", "UTF-8");

        //write each feature/label with: Name, totalOcc, InternalOcc, externalOcc, transientOcc.
        for (TraceableFeature feature : features) {
            if (feature.getExternalOcc() == feature.getTotalOcc() || feature.getExternalOcc() == feature.getCommitList().size()) {
                feats += "\"" + feature.getName() + "\",";
                Feature feat = new Feature(feature.getName());
                featureList.add(feat);
                featureNamesList.add(feature.getName());
            }
            writer.writeNext(
                    new String[]{
                            feature.getName(),
                            feature.getTotalOcc().toString(),
                            feature.getExternalOcc().toString(),
                            feature.getInternalOcc().toString(),
                            feature.getTransientOcc().toString()
                    });
            FileWriter commitList = null;

            final File featureFile = new File(idFeatsFolder, feature.getName() + ".csv");
            commitList = new FileWriter(featureFile, false);
            CSVWriter writerFeature = new CSVWriter(commitList, ',', CSVWriter.NO_QUOTE_CHARACTER);
            writerFeature.writeNext(new String[]{"commitNumber", "Present"});
            for (Map.Entry<Long, Boolean> commit : feature.getCommitList().entrySet()) {
                writerFeature.writeNext(new String[]{String.valueOf(commit.getKey()), String.valueOf(commit.getValue())});
            }
            writerFeature.close();

        }

        writerTXT.println(feats.substring(0, feats.length() - 1) + "}");
        writerTXT.close();
        System.out.println(feats.substring(0, feats.length() - 1) + "}");
        // closing writer connection
        writer.close();
    }


    public static void getFeatureDeletedTimes(File featureFolder) throws IOException {
        File[] lista = featureFolder.listFiles();
        int deletedTimes = 0;
        Map<Integer, Integer> deletePerGitCommit = new HashMap<>();
        if (lista.length > 0) {
            for (File file : lista) {
                if (featureNamesList.contains(file.getName().substring(0, file.getName().indexOf(".csv")))) {
                    Reader reader = Files.newBufferedReader(Paths.get(file.getAbsolutePath()));
                    CSVReader csvReader = new CSVReaderBuilder(reader).build();
                    List<String[]> lines = csvReader.readAll();
                    int count = 0;
                    for (int i = 1; i < lines.size(); i++) {
                        String[] line = lines.get(i);
                        String[] split = line[0].split(",");
                        if (i == 1) {
                            int first = Integer.valueOf(split[0]);
                            count = first;
                        } else {
                            int commitNumber = Integer.valueOf(split[0]);
                            if (commitNumber != (count + 1)) {
                                deletedTimes++;
                                Integer alreadyExist = deletePerGitCommit.computeIfPresent(commitNumber, (k, v) -> v + 1);
                                if (alreadyExist == null)
                                    deletePerGitCommit.put(commitNumber, 1);
                            }
                            count = commitNumber;
                        }

                    }
                    //RQ.2 How many times one feature were deleted along a number of Git commits?
                    File featureCSV = new File(featureFolder.getParent(), "deleteFeatures.csv");
                    if (!featureCSV.exists()) {
                        try {
                            FileWriter csvWriter = new FileWriter(featureCSV);
                            List<List<String>> headerRows = Arrays.asList(
                                    Arrays.asList("Feature", "Deleted Times")
                            );
                            for (List<String> rowData : headerRows) {
                                csvWriter.append(String.join(",", rowData));
                                csvWriter.append("\n");
                            }
                            csvWriter.flush();
                            csvWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        FileAppender csvAppender = new FileAppender(featureCSV);
                        List<List<String>> contentRows = Arrays.asList(
                                Arrays.asList(file.getName().substring(0, file.getName().indexOf(".csv")), String.valueOf(deletedTimes)));
                        for (List<String> rowData : contentRows) {
                            csvAppender.append(String.join(",", rowData));
                        }
                        csvAppender.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //System.out.println("Feature: " + file.getName() + " deleted times: " + deletedTimes);
                    deletedTimes = 0;
                }
            }

            File featureCSV = new File(featureFolder.getParent(), "delFeatsGitCommit.csv");
            if (!featureCSV.exists()) {
                try {
                    FileWriter csvWriter = new FileWriter(featureCSV);
                    List<List<String>> headerRows = Arrays.asList(
                            Arrays.asList("Commit Nr.", "Nr. Feat Del")
                    );
                    for (List<String> rowData : headerRows) {
                        csvWriter.append(String.join(",", rowData));
                        csvWriter.append("\n");
                    }
                    csvWriter.flush();
                    csvWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //RQ.2 How many features were deleted per Git commit?
            for (Map.Entry<Integer, Integer> commit : deletePerGitCommit.entrySet()) {
                try {
                    FileAppender csvAppender = new FileAppender(featureCSV);
                    List<List<String>> contentRows = Arrays.asList(
                            Arrays.asList(String.valueOf(commit.getKey()), String.valueOf(commit.getValue())));
                    for (List<String> rowData : contentRows) {
                        csvAppender.append(String.join(",", rowData));
                    }
                    csvAppender.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //System.out.println("Commit Number: " + commit.getKey() + " features deleted: " + commit.getValue());
            }
        }
    }

}



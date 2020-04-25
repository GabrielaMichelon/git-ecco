package at.jku.isse.gitecco.translation.test;

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
import at.jku.isse.gitecco.translation.mining.ChangeCharacteristic;
import at.jku.isse.gitecco.translation.mining.ComputeRQMetrics;
import at.jku.isse.gitecco.translation.visitor.GetNodesForChangeVisitor;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import org.glassfish.grizzly.http.server.accesslog.FileAppender;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


public class MiningMetricsTest {

    private final static boolean EVERYCOMMIT = false;
    private final static int EVERY_NTH_COMMIT = 1;
    private final static ArrayList<Feature> featureList = new ArrayList<>();
    private final static ArrayList<String> featureNamesList = new ArrayList<String>();
    private final static String REPO_PATH = "D:\\Mining\\Gnome\\libxml2";
    private final static String FEATURES_PATH = "D:\\Mining\\Gnome\\Mining\\test";
    String fileReportFeature = "features_report_each_project_commit.csv";
    String fileStoreConfig = "configurations.csv";
    List<String> changedFiles = new ArrayList<>();
    List<String> changedFilesNext = new ArrayList<>();
    GitCommit gcPrevious = null;
    Boolean previous = true;
    List<String> configurations = new ArrayList<>();
    Map<Feature, Integer> featureVersions = new HashMap<>();
    String feats;

    @Test
    public void identification() throws Exception {
        long measure = System.currentTimeMillis();
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
            Boolean analyze = false;
            for (Map.Entry<Long, String> releases : orderedMap.entrySet()) {
                System.out.println("TAG: " + releases.getValue());
                if (releases.getValue().contains("LIBXML_2_4_25") || analyze) {
                    analyze = true;
                    gitHelper.getEveryNthCommit2(commitList, releases.getValue(), null, i, Math.toIntExact(releases.getKey()), EVERY_NTH_COMMIT);
                    i = Math.toIntExact(releases.getKey()) + 1;
                    File file = new File(FEATURES_PATH, releases.getValue().substring(releases.getValue().lastIndexOf("/") + 1));
                    if (!file.exists())
                        file.mkdir();
                    String folderRelease = file.getAbsolutePath();
                    final File changeFolder = new File(file, "ChangeCharacteristic");
                    final File folder = new File(file, "FeatureCharacteristic");
                    final File idFeatsfolder = new File(file, "IdentifiedFeatures");
                    // if the directory does not exist, create it
                    if (!changeFolder.exists())
                        changeFolder.mkdir();
                    if (!folder.exists())
                        folder.mkdir();
                    if (!idFeatsfolder.exists())
                        idFeatsfolder.mkdir();
                    //feature identification
                    identifyFeatures(commitList, releases.getValue(), idFeatsfolder);
                    initVars(folderRelease);
                    for (GitCommit commits : commitList) {
                        ComputeRQMetrics.characteristicsFeature(folder, commits.getNumber(), commits.getTree(), featureNamesList);
                        configurations.clear();
                        characteristicsChange2(gitHelper, changeFolder, commits, featureNamesList);
                        //dispose tree if it is not needed -> for memory saving reasons.
                        commits.disposeTree();
                    }
                    //RQ.2 How many times one feature changed along a number of Git commits?
                    File filetxt = new File(folder.getParent(), "TimesEachFeatureChanged.txt");
                    PrintWriter writerTXT = new PrintWriter(filetxt.getAbsolutePath(), "UTF-8");
                    for (Map.Entry<Feature, Integer> featureRevision : featureVersions.entrySet()) {
                        if (featureRevision.getValue() > 1)
                            writerTXT.println(featureRevision.getKey() + " Changed " + (featureRevision.getValue() - 1) + " times.");
                    }
                    writerTXT.close();
                    commitList = new GitCommitList(gitHelper);
                } else {
                    i = Math.toIntExact(releases.getKey()) + 1;
                }
            }
        }

        System.out.println("finished analyzing repo");
    }


    public void initVars(String folderRelease) {
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

    //RQ1
    public void characteristicsChange2(GitHelper gitHelper, File changeFolder, GitCommit gc, ArrayList<String> featureNamesList) throws Exception {

        final File gitFolder = new File(gitHelper.getPath());
        final File eccoFolder = new File(gitFolder.getParent(), "ecco");

        Integer countFeaturesChanged = 0; //COUNT PER GIT COMMIT
        Integer newFeatures = 0; //COUNT PER GIT COMMIT

        GetNodesForChangeVisitor visitor = new GetNodesForChangeVisitor();
        ArrayList<ConditionalNode> changedNodes = new ArrayList<>();
        ArrayList<ConditionalNode> deletedNodes = new ArrayList<>();
        Map<Feature, ChangeCharacteristic> featureMap = new HashMap<>();

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
                gcPrevious = new GitCommit(gc.getRevCommit().getParent(0).getName(), gc.getNumber() - 1, gc.getRevCommit().getParent(0).getParent(0).getName(), gc.getBranch(), gc.getRevCommit().getParent(0));
                GitCommitList gcl = new GitCommitList(gitHelper);
                gcl.addTreeParent(gcPrevious, gc.getCommitName());
            }
        }

        Map<Change, FileNode> changesDelete = new HashMap<>();
        //retrieve changed nodes
        for (FileNode child : gc.getTree().getChildren()) {
            if (child instanceof SourceFileNode && !(child.getFilePath().contains("system.h"))) {

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
                int tanglingDegree = 0;
                for (Map.Entry<Feature, Integer> feat : config.entrySet()) {
                    if (featureNamesList.contains(feat.getKey().getName()))
                        tanglingDegree++;
                }
                if (!changed.contains(base))
                    config.remove(base);
                //map with the name of the feature and version and a boolean to set true when it is already incremented in the analysed commit
                String eccoConfig = "";
                String file = "";
                if (changedNode.getContainingFile() != null)
                    file = changedNode.getContainingFile().getFilePath();
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

                        //RQ1.
                        ChangeCharacteristic changeCharacteristic;
                        if (featureMap.get(configFeature.getKey()) == null) {
                            changeCharacteristic = new ChangeCharacteristic();
                            featureMap.put(configFeature.getKey(), new ChangeCharacteristic());
                        } else {
                            changeCharacteristic = featureMap.get(configFeature.getKey());
                        }
                        int aux = 0;
                        if (changedNode.getLineNumberInserts().size() > 0) {
                            for (int i = 2; i < changedNode.getLineNumberInserts().size(); i += 3) {
                                aux += changedNode.getLineNumberInserts().get(i);
                            }
                            changeCharacteristic.setLinesOfCodeAdded(changeCharacteristic.getLinesOfCodeAdded() + aux);
                            changedNode.getLineNumberInserts().removeAll(changedNode.getLineNumberInserts());
                        }

                        changeCharacteristic.addTanglingDegree(tanglingDegree);
                        if (!changeCharacteristic.getScatteringDegreeFiles().contains(file)) {
                            changeCharacteristic.addScatteringDegreeFiles(file);
                        }
                        if (!(changedNode instanceof BaseNode))
                            changeCharacteristic.setScatteringDegreeIfs(changeCharacteristic.getScatteringDegreeIfs() + 1);
                        ChangeCharacteristic finalChangeCharacteristic = changeCharacteristic;
                        featureMap.computeIfAbsent(configFeature.getKey(), v -> finalChangeCharacteristic);
                        featureMap.computeIfPresent(configFeature.getKey(), (k, v) -> finalChangeCharacteristic);
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
                    count++;
                    //configuration that will be used to generate the variant of this changed node
                    configsToGenerateVariant.put(config, eccoConfig);

                    configurations.add(eccoConfig);
                    //folder where the variant is stored
                    File variantsrc = new File(eccoFolder, eccoConfig);
                    String outputCSV = variantsrc.getParentFile().getParentFile().getAbsolutePath();
                    final Path variant_dir = Paths.get(String.valueOf(variantsrc));

                }
            }
        }

        if (deletedNodes.size() != 0) {
            String file = "";
            for (ConditionalNode deletedNode : deletedNodes) {
                file = deletedNode.getContainingFile().getFilePath();
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


                            //RQ1. deleted lines
                            ChangeCharacteristic changeCharacteristic = featureMap.get(configFeature.getKey());
                            if (changeCharacteristic == null)
                                changeCharacteristic = new ChangeCharacteristic();
                            int aux = 0;
                            if (deletedNode.getLineNumberDeleted().size() > 0) {
                                for (int i = 2; i < deletedNode.getLineNumberDeleted().size(); i += 3) {
                                    aux += deletedNode.getLineNumberDeleted().get(i);
                                }
                                changeCharacteristic.setLinesOfCodeRemoved(changeCharacteristic.getLinesOfCodeRemoved() + aux);
                                deletedNode.getLineNumberDeleted().removeAll(deletedNode.getLineNumberDeleted());
                            }
                            changeCharacteristic.addTanglingDegree(tanglingDegree);
                            if (!changeCharacteristic.getScatteringDegreeFiles().contains(file)) {
                                changeCharacteristic.addScatteringDegreeFiles(file);
                            }
                            if (!(deletedNode instanceof BaseNode))
                                changeCharacteristic.setScatteringDegreeIfs(changeCharacteristic.getScatteringDegreeIfs() + 1);
                            ChangeCharacteristic finalChangeCharacteristic = changeCharacteristic;
                            featureMap.computeIfAbsent(configFeature.getKey(), v -> finalChangeCharacteristic);
                            featureMap.computeIfPresent(configFeature.getKey(), (k, v) -> finalChangeCharacteristic);
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
            //config that will be used to commit the variant generated with this changed node in ecco
            configsToCommit.add(eccoConfig);
        }


        //appending to the config csv
        try {

            FileAppender csvWriter = new FileAppender(new File(changeFolder.getParent(), fileStoreConfig));

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
            FileAppender csvAppender = new FileAppender(new File(changeFolder.getParent(), fileReportFeature));
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


        countFeaturesChanged = 0;
        newFeatures = 0;
        for (Map.Entry<Feature, Integer> featureRevision : featureVersions.entrySet()) {
            System.out.println(featureRevision.getKey() + "." + featureRevision.getValue());
        }


        for (Map.Entry<Feature, ChangeCharacteristic> changes : featureMap.entrySet()) {
            ChangeCharacteristic changeCharacteristic = changes.getValue();
            Collections.sort(changeCharacteristic.getTanglingDegree());
            File featureCSV = new File(changeFolder, changes.getKey().getName() + ".csv");
            if (!featureCSV.exists()) {
                try {
                    FileWriter csvWriter = new FileWriter(featureCSV);
                    List<List<String>> headerRows = Arrays.asList(
                            Arrays.asList("Commit Nr", "LOC A", "LOC R", "SD IF", "SD File", "TD IF")
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
                int tangling = 0;
                if (changeCharacteristic.getTanglingDegree().size() > 0)
                    tangling = changeCharacteristic.getTanglingDegree().get(changeCharacteristic.getTanglingDegree().size() - 1);
                List<List<String>> contentRows = Arrays.asList(
                        Arrays.asList(Long.toString(gc.getNumber()), String.valueOf(changeCharacteristic.getLinesOfCodeAdded()), String.valueOf(changeCharacteristic.getLinesOfCodeRemoved()), String.valueOf(changeCharacteristic.getScatteringDegreeIfs()), String.valueOf(changeCharacteristic.getScatteringDegreeFiles().size()),
                                String.valueOf(tangling)));
                for (List<String> rowData : contentRows) {
                    csvAppender.append(String.join(",", rowData));
                }
                csvAppender.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        changedNodes.clear();
        deletedNodes.clear();
        featureMap.clear();

    }


    public void identifyFeatures(GitCommitList commitList, String release, File idFeatFolder) throws IOException {
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

    private void writeToCsv(List<TraceableFeature> features, String fileName, File idFeatsFolder) throws IOException {

        System.out.println("writing to CSV");
        FileWriter outputfile = null;
        File csvFile = new File(FEATURES_PATH, "features-" + fileName + ".csv");
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


    public void getFeatureDeletedTimes(File featureFolder) throws IOException {
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

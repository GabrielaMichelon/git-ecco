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
import at.jku.isse.gitecco.translation.visitor.GetNodesForChangeVisitor;
import org.glassfish.grizzly.http.server.accesslog.FileAppender;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MineCherryPickTest {

    private final static boolean EVERYCOMMIT = false;
    private final static int EVERY_NTH_COMMIT = 1;
    private final static String REPO_PATH = "C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\ChangePropagation\\Bison";//"C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\ChangePropagation\\libssh";//"C:\\Users\\gabil\\Desktop\\PHD\\Mining\\CaseStudies\\Irssi\\irssi";//"C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\ChangePropagation\\sqlite\\sqlite";
    private final static String FEATURES_PATH = "C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\cherrypick-results\\cherry-pick-bison";
    int commitsAnalyze = 8590;//Marlin 22351,  bison 8590,   curl 28103,  sqlite 25167,  irssi 6503,  libssh 6918
    GitCommit gcPrevious = null;
    Boolean previous = true;
    List<String> configurations = new ArrayList<>();
    private final static ArrayList<String> featureNamesList = new ArrayList<String>();
    Map<Feature, Integer> featureVersions = new HashMap<>();
    List<String> changedFiles = new ArrayList<>();
    List<String> changedFilesNext = new ArrayList<>();
    String fileStoreConfig = "configurations.csv";
    String fileReportFeature = "features_report_each_project_commit.csv";

    @Test
    public void identification() throws Exception {
        String repoPath;
        repoPath = REPO_PATH;
        final GitHelper gitHelper = new GitHelper(repoPath, null);
        GitCommitList commitList = new GitCommitList(gitHelper);

        //gitHelper.fetchGitBranches("https://github.com/sqlite/sqlite.git");

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
            //System.out.println("TAG: " + releases.getValue());
            //if (releases.getValue().contains("version-3.18.1") || analyze) {
            //    analyze = true;
            gitHelper.getEveryNthCommitMessage(commitList, null, i, commitsAnalyze, EVERY_NTH_COMMIT);
            //i = Math.toIntExact(releases.getKey()) + 1;
            //File file = new File(FEATURES_PATH, releases.getValue().substring(releases.getValue().lastIndexOf("/") + 1));
            //if (!file.exists())
            //    file.mkdir();
            //String folderRelease = file.getAbsolutePath();
            gcPrevious = null;
            previous = true;
            File file = new File(FEATURES_PATH);
            identifyFeatures(commitList);
            final File changeFolder = new File(file, "Changes");
            // if the directory does not exist, create it
            if (!changeFolder.exists())
                changeFolder.mkdir();
            initVars(changeFolder.getParent());
            for (GitCommit commits : commitList) {
                configurations.clear();
                characteristicsChange2(gitHelper, changeFolder, commits, featureNamesList);
                //dispose tree if it is not needed -> for memory saving reasons.
                commits.disposeTree();
            }
            commitList = new GitCommitList(gitHelper);
            //} else {
            //    i = Math.toIntExact(releases.getKey()) + 1;
            //}
            //}
        }

        System.out.println("finished analyzing repo");
    }

    public void identifyFeatures(GitCommitList commitList) throws IOException {
        final List<TraceableFeature> evaluation = Collections.synchronizedList(new ArrayList<>());
        //String csvFile = release.substring(release.lastIndexOf("/") + 1);
        int count = 0;
        for (GitCommit commit : commitList) {
            System.out.println("for git identifyFeatures: " + commit.getCommitName());
            ID.evaluateFeatureMap(evaluation, ID.id(commit.getTree()), commit.getNumber());
            if (count == commitList.size() - 1) {
                //writeToCsv(evaluation, csvFile, idFeatFolder);
                for (TraceableFeature feature : evaluation) {
                    if (feature.getExternalOcc() == feature.getTotalOcc() || feature.getExternalOcc() == feature.getCommitList().size()) {
                        featureNamesList.add(feature.getName());
                    }
                }
            }
            count++;
        }
        evaluation.clear();
    }

    public void initVars(String folder) {
        changedFiles.clear();
        changedFilesNext.clear();
        gcPrevious = null;
        previous = true;
        configurations.clear();
        featureVersions.clear();
        try {
            FileWriter csvWriter = new FileWriter(folder + File.separator + fileReportFeature);
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
                        eccoConfig += "," + configFeature.getKey().toString();
                    }
                }
                if (!eccoConfig.contains("BASE")) {
                    eccoConfig += "," + "BASE";
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

        if (deletedNodes.size() != 0) {
            String file = "";
            for (ConditionalNode deletedNode : deletedNodes) {
                file = deletedNode.getContainingFile().getFilePath();
                //compute the config for the var gen
                config = constraintComputer.computeConfig(deletedNode, gcPrevious.getTree());
                if (config != null && !config.isEmpty()) {
                    //compute the marked as changed features.
                    changed = constraintComputer.computeChangedFeatures(deletedNode, config);
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
                            eccoConfig += "," + configFeature.getKey().toString();
                        }
                    }
                    if (!eccoConfig.contains("BASE")) {
                        eccoConfig += "," + "BASE";
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
            String eccoConfig = variant.getValue();
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
            System.out.println(featureRevision.getKey());
        }
        changedNodes.clear();
        deletedNodes.clear();
        featureMap.clear();

    }


}

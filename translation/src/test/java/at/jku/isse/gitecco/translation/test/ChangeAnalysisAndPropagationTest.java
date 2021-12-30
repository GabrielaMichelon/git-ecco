package at.jku.isse.gitecco.translation.test;


import at.jku.isse.gitecco.core.git.*;
import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.featureid.type.TraceableFeature;
import at.jku.isse.gitecco.translation.changepropagation.AddedFile;
import at.jku.isse.gitecco.translation.changepropagation.ChangedFile;
import at.jku.isse.gitecco.translation.changepropagation.Changes;
import at.jku.isse.gitecco.translation.changepropagation.DeletedFile;
import at.jku.isse.gitecco.translation.constraintcomputation.util.ConstraintComputer;
import at.jku.isse.gitecco.translation.mining.ChangeCharacteristic;
import at.jku.isse.gitecco.translation.mining.ComputeRQMetrics;
import at.jku.isse.gitecco.translation.visitor.GetNodesForChangeVisitor;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import org.glassfish.grizzly.http.server.accesslog.FileAppender;

import java.io.*;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ChangeAnalysisAndPropagationTest {

    private List<String> fileTypes = new LinkedList<String>();
    private String RESULTS_PATH = "C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\automated_evaluation\\bugfix\\Bison";
    String fileReportFeature = "report_each_commit.csv";
    List<String> configurations = new ArrayList<>();
    private ArrayList<String> featureNamesList = new ArrayList<String>();
    private ArrayList<Feature> featureList = new ArrayList<>();
    Map<Feature, Integer> featureVersions = new HashMap<>();
    List<String> changedFiles = new ArrayList<>();
    List<String> changedFilesNext = new ArrayList<>();
    List<String> changedFilesPropagate = new ArrayList<>();
    GitCommit gcPrevious = null;
    Boolean previous = true;
    String feats;
    private String changeAnalysisTime = "";
    private Boolean computeChangeCharacteristics = true;
    Map<String,Integer> patchesPerCommit = new HashMap<>();

    @org.testng.annotations.Test
    public void TestChangePropagation() throws IOException {
        try {
            //csv file containing data
            String strFile = "C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\automated_evaluation\\Bison.csv";
            String repoFeatures = "C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\automated_evaluation\\bugfix\\Bison";//"C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\ChangePropagation\\features-libssh-0.9.4.txt";
            String repoGit = "C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\ChangePropagation\\Bison";//"C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\ChangePropagation\\sqlite\\sqlite";// "C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\automated_evaluation\\test";
            CSVReader reader = new CSVReader(new FileReader(strFile));
            String[] nextLine;
            int lineNumber = 0;
            try {
                FileWriter csvWriter = new FileWriter(RESULTS_PATH + File.separator + fileReportFeature);
                List<List<String>> headerRows = Arrays.asList(
                        Arrays.asList("CommitHash", "NFeatures", "NFiles", "NLinesAdded", "NLinesRemoved", "Runtime")
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

            while ((nextLine = reader.readNext()) != null) {
                if (lineNumber == 0) {
                    lineNumber++;
                } else {
                    lineNumber++;
                    System.out.println("Line # " + lineNumber);
                    System.out.println(nextLine[1]);
                    if (!nextLine[6].equals("BASE")) {
                        //String[] features = nextLine[6].split(",");
                        //if (featuresPerCommit.get(Integer.valueOf(features.length)) == null) {
                        //    featuresPerCommit.put(Integer.valueOf(features.length), 1);
                        //} else {
                           // featuresPerCommit.put(Integer.valueOf(features.length), featuresPerCommit.get(Integer.valueOf(features.length)) + 1);
                        //}
                        //uncomment
                        identification(repoFeatures, repoGit, nextLine[1], nextLine[6]);
                    }
                }
            }
            System.out.println("Qtde patches:");
            for (Map.Entry<String, Integer> patch: patchesPerCommit.entrySet()) {
                System.out.println(patch.getKey()+" "+patch.getValue());
            }


            //identification(repoFeatures, repoGit, "e35940027baebf3d81bd97ee7befacf1e2e079a5", "SQLITE_TEST, SQLITE_ENABLE_STAT4, BASE"); featuretestadd
            //identification(repoFeatures, repoGit, "188f52d4f3ee73e82d9efebbed5e9cc85ba6977c", "OPENSSL_VERSION_NUMBER, BASE");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public Map<Map<String, List<String>>, Changes> identification(String featuresRepo, String REPO_PATH, String secondcommit, String featpropagatename) throws Exception {
        String[] featspropagate = featpropagatename.replaceAll(" ", "").split(",");
        ArrayList<Feature> featurespropagate = new ArrayList<>();
        for (String feature : featspropagate) {
            Feature featpropagate = new Feature(feature);
            featurespropagate.add(featpropagate);
        }

        // Feature featpropagate = new Feature(featpropagatename);
        Map<Map<String, List<String>>, Changes> results = new HashMap<>();

        String repoPath = REPO_PATH;

        final GitHelper gitHelper = new GitHelper(repoPath, null);
        GitCommitList commitList = new GitCommitList(gitHelper);

        gitHelper.getCommitAndItsPattern(commitList, secondcommit);
        File file = new File(RESULTS_PATH, secondcommit);
        if (!file.exists())
            file.mkdir();
        final File originalFolder = new File(file, "original");
        // if the directory does not exist, create it
        if (!originalFolder.exists())
            originalFolder.mkdir();
        final File originalcommitFolder = new File(originalFolder, featpropagatename.replaceAll(" ", ""));
        if (!originalcommitFolder.exists())
            originalcommitFolder.mkdir();
        GitHelper.copyCommitFilesToAFolder(REPO_PATH, originalcommitFolder, secondcommit);

        String folderRelease = file.getAbsolutePath();
        final File changeFolder = new File(file, "ChangeCharacteristic");
        final File folder = new File(file, "FeatureCharacteristic");
        final File idFeatsfolder = new File(file, "IdentifiedFeatures");
        final File checkoutFolder = new File(file, "checkout");
        // if the directory does not exist, create it
        if (!checkoutFolder.exists())
            checkoutFolder.mkdir();
        final File featurescheckout = new File(checkoutFolder, featpropagatename.replaceAll(" ", ""));
        if (!featurescheckout.exists())
            featurescheckout.mkdir();
        // if the directory does not exist, create it
        if (!changeFolder.exists())
            changeFolder.mkdir();
        if (!folder.exists())
            folder.mkdir();
        //feature identification
        final File featuresTXT = new File(featuresRepo, "features-" + secondcommit + ".txt");
        if (featuresTXT.exists() && featuresTXT.getName().contains(".txt"))
            addFeatures(featuresTXT.getAbsolutePath());
        else {
            //if (!idFeatsfolder.exists())
            //    idFeatsfolder.mkdir();
            identifyFeatures(commitList, secondcommit, idFeatsfolder, featuresRepo);
        }
        //uncomment
        initVars(folderRelease);

        //for (GitCommit commits : commitList) {
        //ComputeRQMetrics.characteristicsFeature(folder, commits.getNumber(), commits.getTree(), featureNamesList);

        patchesCommit(patchesPerCommit, gitHelper, commitList);
        //uncomment
        configurations.clear();
        //ComputeRQMetrics.characteristicsFeature(folder, commitList.get(0).getNumber(), commitList.get(0).getTree(), featureNamesList);
        //ComputeRQMetrics.characteristicsFeature(folder, commitList.get(1).getNumber(), commitList.get(1).getTree(), featureNamesList);
        characteristicsChange(gitHelper, changeFolder, commitList, featureNamesList, featurespropagate, results, featurescheckout);
        //System.out.println(changeAnalysisTime);
        //uncomment
        compare(originalcommitFolder, featurescheckout);


        //dispose tree if it is not needed -> for memory saving reasons.
        //commits.disposeTree();
        //}
        //RQ.2 How many times one feature changed along a number of Git commits?
       /* File filetxt = new File(folder.getParent(), "TimesEachFeatureChanged.txt");
        PrintWriter writerTXT = new PrintWriter(filetxt.getAbsolutePath(), "UTF-8");
        for (Map.Entry<Feature, Integer> featureRevision : featureVersions.entrySet()) {
            if (featureRevision.getValue() > 1)
                writerTXT.println(featureRevision.getKey() + " Changed " + (featureRevision.getValue() - 1) + " times.");
        }
        writerTXT.close();
        */

        System.out.println("finished analyzing repo");
        return results;
    }

    public void initVars(String folderRelease) {
        changedFiles.clear();
        changedFilesNext.clear();
        gcPrevious = null;
        previous = true;
        configurations.clear();
        featureVersions.clear();
        changedFilesPropagate.clear();

        //csv to report new features and features changed per git commit of the project
        //RQ.2 How many features changed per Git commit?
        /*try {
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
         */
    }

    public void addFeatures(String featuresRepo) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(featuresRepo));
            String line = "";
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] cols = line.split(",");
                if (!featureNamesList.contains(cols[0].substring(1).replace("\"", "")))
                    featureNamesList.add(cols[0].substring(1).replace("\"", ""));
                for (int i = 1; i < cols.length - 1; i++) {
                    if (!featureNamesList.contains(cols[i].replace("\"", "")))
                        featureNamesList.add(cols[i].replace("\"", ""));
                }
                String lastfeature = cols[cols.length - 1].replace("\"", "");
                if (!featureNamesList.contains(lastfeature.replace("}", "")))
                    featureNamesList.add(lastfeature.replace("}", ""));
                if (!featureNamesList.contains("BASE"))
                    featureNamesList.add("BASE");
            }
        } catch (IOException ex) {
            System.out.println((char) 27 + "[31m" + "Error reading the features text file!\nVerify if you wrote in the correct format -> {\"WITH_SERVER\",\"HAVE_LIBZ\",...}");
            System.exit(0);
        }
    }

    public void identifyFeatures(GitCommitList commitList, String release, File idFeatFolder, String featuresRepo) throws IOException {
        final List<TraceableFeature> evaluation = Collections.synchronizedList(new ArrayList<>());
        String csvFile = release.substring(release.lastIndexOf("/") + 1);
        int count = 0;
        for (GitCommit commit : commitList) {
            System.out.println("for git identifyFeatures" + commit.getCommitName());
            at.jku.isse.gitecco.featureid.identification.ID.evaluateFeatureMap(evaluation, at.jku.isse.gitecco.featureid.identification.ID.id(commit.getTree()), commit.getNumber());
            //dispose tree if it is not needed -> for memory saving reasons.
            if (count == commitList.size() - 1) {
                //commits.disposeTree();
                writeToCsv(evaluation, csvFile, idFeatFolder, featuresRepo);
                //getFeatureDeletedTimes(idFeatFolder);
            } //else
            //commits.disposeTree();
            count++;
        }
        evaluation.clear();
    }

    private void writeToCsv(List<TraceableFeature> features, String fileName, File idFeatsFolder, String featuresRepo) throws IOException {

        //System.out.println("writing to CSV");
        //FileWriter outputfile = null;
        //File csvFile = new File(featuresRepo, "features-" + fileName + ".csv");
        //second parameter is boolean for appending --> never append
        //outputfile = new FileWriter(csvFile, false);
        feats = "{";

        // create CSVWriter object file writer object as parameter
        //deprecated but no other way available --> it still works anyways
        //@SuppressWarnings("deprecation") CSVWriter writer = new CSVWriter(outputfile, ',', CSVWriter.NO_QUOTE_CHARACTER);

        //adding header to csv
        //writer.writeNext(new String[]{"Label/FeatureName", "#total", "#external", "#internal", "#transient"});

        PrintWriter writerTXT = new PrintWriter(featuresRepo + File.separator + "features-" + fileName + ".txt", "UTF-8");
        //String introduced = "";
        //String removed = "";


        //write each feature/label with: Name, totalOcc, InternalOcc, externalOcc, transientOcc.
        for (TraceableFeature feature : features) {
            if (feature.getExternalOcc() == feature.getTotalOcc() || feature.getExternalOcc() == feature.getCommitList().size()) {
                feats += "\"" + feature.getName() + "\",";
                Feature feat = new Feature(feature.getName());
                featureList.add(feat);
                featureNamesList.add(feature.getName());
            }
           /* writer.writeNext(
                    new String[]{
                            feature.getName(),
                            feature.getTotalOcc().toString(),
                            feature.getExternalOcc().toString(),
                            feature.getInternalOcc().toString(),
                            feature.getTransientOcc().toString()
                    });
            FileWriter commitList = null;
            */
            //final File featureFile = new File(idFeatsFolder, feature.getName() + ".csv");
            //commitList = new FileWriter(featureFile, false);
            //CSVWriter writerFeature = new CSVWriter(commitList, ',', CSVWriter.NO_QUOTE_CHARACTER);
            //writerFeature.writeNext(new String[]{"commitNumber", "Present"});
            /*for (Map.Entry<Long, Boolean> commit : feature.getCommitList().entrySet()) {
                if (feature.getCommitList().entrySet().size() < 2) {
                    if (commit.getKey().equals(Long.valueOf(1)) && featureList.contains(feature)) {
                        System.out.println("Removed: " + feature.getName());
                        removed += "," + feature.getName();
                    } else if (featureList.contains(feature)) {
                        introduced += "," + feature.getName();
                        System.out.println("Introduced: " + feature.getName());
                    }
                }
                //writerFeature.writeNext(new String[]{String.valueOf(commit.getKey()), String.valueOf(commit.getValue())});
            }
            //writerFeature.close();*/

        }
        //introduced = introduced.replaceFirst(",", "");
        //removed = removed.replaceFirst(",", "");

        writerTXT.println(feats.substring(0, feats.length() - 1) + "}");
        writerTXT.close();

        /*if (!introduced.equals("")) {
            PrintWriter writerTXT2 = new PrintWriter(featuresRepo + File.separator + "featuresIntroduced-" + fileName + ".txt", "UTF-8");
            writerTXT2.println(introduced);
            writerTXT2.close();
        }

        if (!removed.equals("")) {
            PrintWriter writerTXT3 = new PrintWriter(featuresRepo + File.separator + "featuresRemoved-" + fileName + ".txt", "UTF-8");
            writerTXT3.println(removed);
            writerTXT3.close();
        }
        */

        System.out.println(feats.substring(0, feats.length() - 1) + "}");
        // closing writer connection
        //writer.close();
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


    public void patchesCommit(Map<String,Integer> patchesPerCommit, GitHelper gitHelper, GitCommitList gcs) throws Exception {

        GitCommit gc1 = gcs.get(0);
        GitCommit gc2 = gcs.get(1);

        gcPrevious = new GitCommit(gc2.getCommitName(), gc2.getNumber(), gc1.getCommitName(), gc2.getBranch(), gc2.getRevCommit());
        gcPrevious.setTree(gc2.getTree());
        gc2 = gcPrevious;

        GitCommitList gcl = new GitCommitList(gitHelper);
        gcl.addTreeParent(gcPrevious, gc2.getCommitName());

        for (FileNode nod : gc1.getTree().getChildren()) {
            if (nod instanceof SourceFileNode) {
                changedFiles.add(nod.getFilePath());
            }
        }

        //retrieve changed nodes
        for (FileNode child : gc2.getTree().getChildren()) {
            if (child instanceof SourceFileNode) {
                try {

                    GitHelper.getPatchesPerCommit(patchesPerCommit, gc2, child, false);
                } catch (Exception e) {
                    System.err.println("error while executing the file diff: " + child.getFilePath());
                    e.printStackTrace();
                }
            }
        }
    }


        //RQ1
    public void characteristicsChange(GitHelper gitHelper, File changeFolder, GitCommitList gcs, ArrayList<String> featureNamesList, ArrayList<Feature> featurespropagate, Map<Map<String, List<String>>, Changes> results, File featurescheckout) throws Exception {

        final File gitFolder = new File(gitHelper.getPath());
        final File eccoFolder = new File(gitFolder.getParent(), "ecco");

        Integer countFeaturesChanged = 0; //COUNT PER GIT COMMIT
        Integer newFeatures = 0; //COUNT PER GIT COMMIT

        GetNodesForChangeVisitor visitor = new GetNodesForChangeVisitor();
        ArrayList<ConditionalNode> changedNodes = new ArrayList<>();
        ArrayList<ConditionalNode> deletedNodes = new ArrayList<>();
        ArrayList<ConditionalNode> propagateChangedNodes = new ArrayList<>();
        ArrayList<ConditionalNode> propagateDeletedNodes = new ArrayList<>();
        ArrayList<String> filesAdded = new ArrayList<>();
        ArrayList<String> filesRemoved = new ArrayList<>();
        Map<String, Map<Integer, FileChange>> filesChanged = new HashMap<>();
        Map<Feature, ChangeCharacteristic> featureMap = new HashMap<>();
        GitCommit gc1 = gcs.get(0);
        GitCommit gc2 = gcs.get(1);
        Changes mapAux = new Changes();

        int nrChunksInsert, nrChunksRemove;

        gcPrevious = new GitCommit(gc2.getCommitName(), gc2.getNumber(), gc1.getCommitName(), gc2.getBranch(), gc2.getRevCommit());
        gcPrevious.setTree(gc2.getTree());
        gc2 = gcPrevious;

        GitCommitList gcl = new GitCommitList(gitHelper);
        gcl.addTreeParent(gcPrevious, gc2.getCommitName());

        Map<Change, FileNode> changesDelete = new HashMap<>();
        Map<Change, FileNode> changesInsertAndDelete = new HashMap<>();

        for (FileNode nod : gc1.getTree().getChildren()) {
            if (nod instanceof SourceFileNode) {
                changedFiles.add(nod.getFilePath());
            }
        }

        long measure = System.currentTimeMillis();

        //retrieve changed nodes
        for (FileNode child : gc2.getTree().getChildren()) {
            if (child instanceof SourceFileNode) {

                Change[] changes = null;
                try {
                    changes = gitHelper.getFileDiffsTwoCommits(gc2, child, false);
                } catch (Exception e) {
                    System.err.println("error while executing the file diff: " + child.getFilePath());
                    e.printStackTrace();
                }

                for (Change change : changes) {
                    if (!change.getChangeType().equals("DELETE")) {
                        visitor.setChange(change);
                        child.accept(visitor, null);
                        changedNodes.addAll(visitor.getchangedNodes());
                    } else if (change.getChangeType().equals("DELETE")) {
                        changesDelete.put(change, child);
                    }
                }
            }
            previous = false;
            changedFilesNext.add(child.getFilePath());
        }

        //The deleted nodes are
        for (String file : changedFiles) {
            if (!changedFilesNext.contains(file)) {
                FileNode child = gc1.getTree().getChild(file);
                if (child instanceof SourceFileNode) {
                    Change[] changes = null;
                    try {
                        changes = gitHelper.getFileDiffs(gc2, child, true);
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
            FileNode child = childAux;//gc1.getTree().getChild(childAux.getFilePath());
            visitor.setChange(change);
            child.accept(visitor, null);
            deletedNodes.addAll(visitor.getchangedNodes());
        }


        final ConstraintComputer constraintComputer = new ConstraintComputer(featureNamesList);
        final PreprocessorHelper pph = new PreprocessorHelper();
        Map<Feature, Integer> config;
        Set<Feature> changed;
        Set<Feature> alreadyComitted = new HashSet<>();
        Integer count = 0;

        //if there is no changed node then there must be a change in the binary files --> commit base.
        if ((changedNodes.size() == 0) && (deletedNodes.size() == 0) && (gc2.getTree().getChildren().size() > 0))
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

        ArrayList<ConditionalNode> changedNodesAux = new ArrayList<>();
        for (ConditionalNode changedNode : changedNodes) {
            if (!changedNodesAux.contains(changedNode))
                changedNodesAux.add(changedNode);
        }

        ArrayList<ConditionalNode> deletedNodesAux = new ArrayList<>();
        for (ConditionalNode deletedNode : deletedNodes) {
            if (!deletedNodesAux.contains(deletedNode))
                deletedNodesAux.add(deletedNode);
        }

        ArrayList<String> configsToCommit = new ArrayList<>();
        Map<Map<Feature, Integer>, String> configsToGenerateVariant = new HashMap<>();
        Feature base = new Feature("BASE");
        //changedNodes = changedNodes.stream().filter(x -> x.getLocalCondition().equals("__AVR_ATmega644P__ || __AVR_ATmega644__")).collect(Collectors.toSet());
        for (ConditionalNode changedNode : changedNodesAux) {
            //compute the config for the var gen
            config = constraintComputer.computeConfig(changedNode, gc2.getTree());
            if (config != null && !config.isEmpty()) {
                //compute the marked as changed features.
                changed = constraintComputer.computeChangedFeatures(changedNode, config);
                for (Feature feature : featurespropagate) {
                    if (changed.contains(feature)) {
                        //if (changed.contains(featpropagate)) {
                        propagateChangedNodes.add(changedNode);
                        System.out.println("CHANGED NODE CONTAINS FEATURE: " + feature.getName());
                        ArrayList<Integer> linesInsert = new ArrayList<>();
                        ArrayList<Integer> linesRemoved = new ArrayList<>();
                        ArrayList<String> featureInteraction = new ArrayList<>();
                        ArrayList<String> featureMightBeAffected = new ArrayList<>();
                        if (!changedFiles.contains(changedNode.getContainingFile().getFilePath())) {
                            filesAdded.add(changedNode.getContainingFile().getFilePath());
                            System.out.println("NEW FILE: " + changedNode.getContainingFile().getFilePath());
                            //linesInsert.addAll(changedNode.getLineNumberInserts());
                            //System.out.println("Line numbers insert: " + changedNode.getLineNumberInserts());
                            if (!changedFilesPropagate.contains(changedNode.getContainingFile().getFilePath()))
                                changedFilesPropagate.add(changedNode.getContainingFile().getFilePath());
                        } else {
                            if (!changedFilesPropagate.contains(changedNode.getContainingFile().getFilePath()))
                                changedFilesPropagate.add(changedNode.getContainingFile().getFilePath());
                            System.out.println("CHANGED FILE: " + changedNode.getContainingFile().getFilePath());
                            //linesInsert.addAll(changedNode.getLineNumberInserts());
                            //linesRemoved.addAll(changedNode.getLineNumberDeleted());
                            //System.out.println("Line numbers insert: " + changedNode.getLineNumberInserts() + " Line numbers removed: " + changedNode.getLineNumberDeleted());
                        }

                        System.out.println("Feature interactions: ");
                        for (Feature fea : changed) {
                            if (!fea.equals(feature)) {
                                featureInteraction.add(fea.getName());
                                System.out.println(fea.getName());
                            }
                        }


                        System.out.println("Feature might be affected: ");
                        for (Map.Entry<Feature, Integer> fea : config.entrySet()) {
                            if (!changed.contains(fea.getKey())) {
                                featureMightBeAffected.add(fea.getKey().getName());
                                System.out.println(fea.getKey().getName());
                            }
                        }

                        List<String> linesFile = changedNode.getContainingFileLines();

                        Boolean mapExist = false;

                        for (Map<String, List<String>> map : results.keySet()) {
                            if (map.get(changedNode.getContainingFile().getFilePath()) != null) {
                                mapExist = true;
                                mapAux = results.get(map);
                            }
                        }

                        //new file
                        if (!changedFiles.contains(changedNode.getContainingFile().getFilePath())) {
                            Map<String, List<String>> mapFile = new HashMap<>();
                            for (Deltas delta : changedNode.getLineNumberInsertsAndDeletes()) {
                                ArrayList<Integer> linesInsertArray = new ArrayList<>();
                                linesInsertArray.add(delta.getLineFromAdd());
                                linesInsertArray.add(delta.getLineToAdd());
                                linesInsertArray.add(delta.getTotalLinesAdd());
                                AddedFile addedFile = new AddedFile(linesInsertArray, featureInteraction, featureMightBeAffected, feature);
                                mapAux.addAddedFiles(addedFile);
                            }
                            mapFile.put(changedNode.getContainingFile().getFilePath(), linesFile);
                            results.put(mapFile, mapAux);

                        } else { //changed file
                            for (Deltas delta : changedNode.getLineNumberInsertsAndDeletes()) {
                                ArrayList<Integer> linesInsertArray = new ArrayList<>();
                                ArrayList<Integer> linesRemovedArray = new ArrayList<>();
                                linesInsertArray.add(delta.getLineFromAdd());
                                linesInsertArray.add(delta.getLineToAdd());
                                linesInsertArray.add(delta.getTotalLinesAdd());
                                if (delta.getLineFromRemov() != -1) {
                                    linesRemovedArray.add(delta.getLineFromRemov());
                                    linesRemovedArray.add(delta.getLineToRemov());
                                    linesRemovedArray.add(delta.getTotalLinesRemov());
                                }
                                int position = delta.getPosLinesAdd();
                                if (position == -1)
                                    position = delta.getPosLinesRemov();
                                //Map<String, Map<Integer, FileChange>> filesChanged = new HashMap<>();
                                Map<Integer, FileChange> fc = new HashMap<>();
                                FileChange fileChangeAux = null;
                                if (filesChanged.get(changedNode.getContainingFile().getFilePath()) != null) {
                                    fileChangeAux = new at.jku.isse.gitecco.core.git.FileChange("Lines added: " + linesInsertArray.toString() + " Lines removed: " + linesRemovedArray.toString(), String.valueOf(position), changedNode.getContainingFileLines(), changedNode.getContainingPreviousFileLines());
                                    Map<Integer, at.jku.isse.gitecco.core.git.FileChange> finalFileChangeAux = filesChanged.get(changedNode.getContainingFile().getFilePath());
                                    finalFileChangeAux.put(position, fileChangeAux);
                                    filesChanged.computeIfPresent(changedNode.getContainingFile().getFilePath(), (k, v) -> finalFileChangeAux);
                                } else {
                                    fileChangeAux = new at.jku.isse.gitecco.core.git.FileChange("Lines added: " + linesInsertArray.toString() + " Lines removed: " + linesRemovedArray.toString(), String.valueOf(position), changedNode.getContainingFileLines(), changedNode.getContainingPreviousFileLines());
                                    Map<Integer, at.jku.isse.gitecco.core.git.FileChange> finalFileChangeAux = new HashMap<>();
                                    finalFileChangeAux.put(Integer.valueOf(position), fileChangeAux);
                                    filesChanged.put(changedNode.getContainingFile().getFilePath(), finalFileChangeAux);
                                }
                                ChangedFile changedFile = new ChangedFile(linesInsertArray, linesRemovedArray, position, featureInteraction, featureMightBeAffected, changedNode.getContainingFileLines(), changedNode.getContainingPreviousFileLines(), feature);
                                changedFile.setPreviousLines(changedNode.getContainingPreviousFileLines());
                                mapAux.addChangedFiles(changedFile);
                            }
                            Map<String, List<String>> mapFile = new HashMap<>();
                            mapFile.put(changedNode.getContainingFile().getFilePath(), linesFile);
                            results.put(mapFile, mapAux);
                        }

                    }
                }
                int tanglingDegree = 0;
                if (computeChangeCharacteristics) {
                    for (Map.Entry<Feature, Integer> feat : config.entrySet()) {
                        if (featureNamesList.contains(feat.getKey().getName()))
                            tanglingDegree++;
                    }
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
                            eccoConfig += "," + configFeature.getKey().toString();
                        else
                            eccoConfig += "," + configFeature.getKey().toString();

                        if (computeChangeCharacteristics) {
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

                    if (computeChangeCharacteristics) {
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

                if (configurations.contains(eccoConfig)) {
                    //System.out.println("Config already used to generate a variant: " + eccoConfig);
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


        if (deletedNodesAux.size() != 0) {
            String file = "";
            for (ConditionalNode deletedNode : deletedNodesAux) {
                file = deletedNode.getContainingFile().getFilePath();
                //compute the config for the var gen
                config = constraintComputer.computeConfig(deletedNode, gcPrevious.getTree());
                if (config != null && !config.isEmpty()) {
                    //compute the marked as changed features.
                    changed = constraintComputer.computeChangedFeatures(deletedNode, config);
                    for (Feature feature : featurespropagate) {
                        if (changed.contains(feature)) {
                            //if (changed.contains(featpropagate)) {
                            propagateDeletedNodes.add(deletedNode);
                            System.out.println("DELETED NODE CONTAINS FEATURE: " + feature.getName());
                            ArrayList<String> featureInteraction = new ArrayList<>();
                            ArrayList<String> featureMightBeAffected = new ArrayList<>();
                            ArrayList<Integer> linesRemoved = new ArrayList<>();
                            if (!changedFilesPropagate.contains(deletedNode.getContainingFile().getFilePath()))
                                changedFilesPropagate.add(deletedNode.getContainingFile().getFilePath());
                            if (!changedFiles.contains(deletedNode.getContainingFile().getFilePath())) {
                                filesAdded.add(deletedNode.getContainingFile().getFilePath());
                                System.out.println("NEW FILE: " + deletedNode.getContainingFile().getFilePath());
                            } else if (!changedFilesNext.contains(deletedNode.getContainingFile().getFilePath())) {
                                filesRemoved.add(deletedNode.getContainingFile().getFilePath());
                                System.out.println("DELETED FILE: " + deletedNode.getContainingFile().getFilePath());
                            } else {
                                System.out.println("CHANGED FILE: " + deletedNode.getContainingFile().getFilePath());
                            }

                            System.out.println("FILE: " + deletedNode.getContainingFile().getFilePath());
                            //System.out.println("lines removed " + deletedNode.getLineNumberDeleted());
                            System.out.println("Feature interactions: ");
                            //linesRemoved.addAll(deletedNode.getLineNumberDeleted());
                            for (Feature fea : changed) {
                                if (!fea.equals(feature)) {
                                    featureInteraction.add(fea.getName());
                                    System.out.println(fea.getName());
                                }
                            }

                            System.out.println("Feature might be affected: ");
                            for (Map.Entry<Feature, Integer> fea : config.entrySet()) {
                                if (!changed.contains(fea.getKey())) {
                                    featureMightBeAffected.add(fea.getKey().getName());
                                    System.out.println(fea.getKey().getName());
                                }
                            }
                            List<String> linesFile = deletedNode.getContainingFileLines();
                            if (deletedNode.getContainingFile().getPreviousFileContent().size() == 0 && deletedNode.getContainingFile().getFileContent().size() == 0) {
                                for (ConditionalNode changedNode : changedNodesAux) {
                                    if (changedNode.getContainingFile().getFilePath().equals(deletedNode.getContainingFile().getFilePath()))
                                        linesFile = changedNode.getContainingFile().getPreviousFileContent();
                                }
                            } else {
                                linesFile = deletedNode.getContainingPreviousFileLines();
                            }


                            Boolean mapExist = false;

                            for (Map<String, List<String>> map : results.keySet()) {
                                if (map.get(deletedNode.getContainingFile().getFilePath()) != null) {
                                    mapExist = true;
                                    mapAux = results.get(map);
                                }
                            }

                            //new file
                            if (!changedFiles.contains(deletedNode.getContainingFile().getFilePath())) {
                                for (Deltas delta : deletedNode.getLineNumberInsertsAndDeletes()) {
                                    ArrayList<Integer> linesRemoveArray = new ArrayList<>();
                                    linesRemoveArray.add(delta.getLineFromRemov());
                                    linesRemoveArray.add(delta.getLineToRemov());
                                    linesRemoveArray.add(delta.getTotalLinesRemov());
                                    AddedFile addedFile = new AddedFile(linesRemoveArray, featureInteraction, featureMightBeAffected, feature);
                                    mapAux.addAddedFiles(addedFile);
                                }
                                Map<String, List<String>> mapFile = new HashMap<>();
                                mapFile.put(deletedNode.getContainingFile().getFilePath(), linesFile);
                                results.put(mapFile, mapAux);
                            } else if (!changedFilesNext.contains(deletedNode.getContainingFile().getFilePath())) { //deleted file
                                for (Deltas delta : deletedNode.getLineNumberInsertsAndDeletes()) {
                                    ArrayList<Integer> linesRemoveArray = new ArrayList<>();
                                    linesRemoveArray.add(delta.getLineFromRemov());
                                    linesRemoveArray.add(delta.getLineToRemov());
                                    linesRemoveArray.add(delta.getTotalLinesRemov());
                                    DeletedFile deletedFile = new DeletedFile(linesRemoveArray, featureInteraction, featureMightBeAffected, feature);
                                    mapAux.addDeletedFiles(deletedFile);
                                }
                                Map<String, List<String>> mapFile = new HashMap<>();
                                mapFile.put(deletedNode.getContainingFile().getFilePath(), linesFile);
                                results.put(mapFile, mapAux);
                            } else {//changed file
                                for (Deltas delta : deletedNode.getLineNumberInsertsAndDeletes()) {
                                    ArrayList<Integer> linesInsertArray = new ArrayList<>();
                                    ArrayList<Integer> linesRemoveArray = new ArrayList<>();
                                    linesInsertArray.add(delta.getLineFromAdd());
                                    linesInsertArray.add(delta.getLineToAdd());
                                    linesInsertArray.add(delta.getTotalLinesAdd());
                                    int position = delta.getPosLinesAdd();
                                    if (position == -1)
                                        position = delta.getPosLinesRemov();
                                    linesRemoveArray.add(delta.getLineFromRemov());
                                    linesRemoveArray.add(delta.getLineToRemov());
                                    linesRemoveArray.add(delta.getTotalLinesRemov());
                                    Map<Integer, FileChange> fc = new HashMap<>();
                                    FileChange fileChangeAux = null;
                                    if (filesChanged.get(deletedNode.getContainingFile().getFilePath()) != null) {
                                        fileChangeAux = new at.jku.isse.gitecco.core.git.FileChange("Lines added: " + linesInsertArray.toString() + " Lines removed: " + linesRemoveArray.toString(), String.valueOf(position), deletedNode.getContainingFileLines(), deletedNode.getContainingPreviousFileLines());
                                        Map<Integer, at.jku.isse.gitecco.core.git.FileChange> finalFileChangeAux = filesChanged.get(deletedNode.getContainingFile().getFilePath());
                                        finalFileChangeAux.put(position, fileChangeAux);
                                        filesChanged.computeIfPresent(deletedNode.getContainingFile().getFilePath(), (k, v) -> finalFileChangeAux);
                                    } else {
                                        fileChangeAux = new at.jku.isse.gitecco.core.git.FileChange("Lines added: " + linesInsertArray.toString() + " Lines removed: " + linesRemoveArray.toString(), String.valueOf(position), deletedNode.getContainingFileLines(), deletedNode.getContainingPreviousFileLines());
                                        Map<Integer, at.jku.isse.gitecco.core.git.FileChange> finalFileChangeAux = new HashMap<>();
                                        finalFileChangeAux.put(Integer.valueOf(position), fileChangeAux);
                                        filesChanged.put(deletedNode.getContainingFile().getFilePath(), finalFileChangeAux);
                                    }
                                    ChangedFile changedFile = new ChangedFile(linesInsertArray, linesRemoveArray, position, featureInteraction, featureMightBeAffected, deletedNode.getContainingFileLines(), deletedNode.getContainingPreviousFileLines(), feature);
                                    mapAux.addChangedFiles(changedFile);
                                }
                                Map<String, List<String>> mapFile = new HashMap<>();
                                mapFile.put(deletedNode.getContainingFile().getFilePath(), linesFile);
                                results.put(mapFile, mapAux);
                            }


                        }
                    }
                    int tanglingDegree = 0;
                    for (Map.Entry<Feature, Integer> feat : config.entrySet()) {
                        if (featureNamesList.contains(feat.getKey().getName()))
                            tanglingDegree++;
                    }
                    if (!changed.contains(base))
                        config.remove(base);
                    //map with the name of the feature and version and a boolean to set true when it is already incremented in the analysed commit*/
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
                                eccoConfig += "," + configFeature.getKey().toString();
                            else
                                eccoConfig += "," + configFeature.getKey().toString();


                            //RQ1. deleted lines
                            if (computeChangeCharacteristics) {
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
                    }
                    if (!eccoConfig.contains("BASE")) {
                        eccoConfig += "," + "BASE.$$";
                    }
                    eccoConfig = eccoConfig.replaceFirst(",", "");

                    if (configurations.contains(eccoConfig)) {
                        //System.out.println("Config already used to generate a variant: " + eccoConfig);
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


        //generate the variant for this config
        for (Map.Entry<Map<Feature, Integer>, String> variant : configsToGenerateVariant.entrySet()) {
            String eccoConfig = variant.getValue();
            //config that will be used to commit the variant generated with this changed node in ecco
            configsToCommit.add(eccoConfig);
        }


        for (Map.Entry<Feature, Integer> featureRevision : featureVersions.entrySet()) {
            System.out.println(featureRevision.getKey() + "." + featureRevision.getValue());
        }

            /*
                //append results to the feature report csv
                try {
                    FileAppender csvAppender = new FileAppender(new File(changeFolder.getParent(), fileReportFeature));
                    List<List<String>> contentRows = Arrays.asList(
                            Arrays.asList(Long.toString(gc2.getNumber()), newFeatures.toString(), countFeaturesChanged.toString())
                    );
                    for (List<String> rowData : contentRows) {
                        csvAppender.append(String.join(",", rowData));
                    }
                    csvAppender.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //end append results to the feature report csv

                */
        if (computeChangeCharacteristics) {
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
                            Arrays.asList(Long.toString(gc2.getNumber()), String.valueOf(changeCharacteristic.getLinesOfCodeAdded()), String.valueOf(changeCharacteristic.getLinesOfCodeRemoved()), String.valueOf(changeCharacteristic.getScatteringDegreeIfs()), String.valueOf(changeCharacteristic.getScatteringDegreeFiles().size()),
                                    String.valueOf(tangling)));
                    for (List<String> rowData : contentRows) {
                        csvAppender.append(String.join(",", rowData));
                    }
                    csvAppender.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        Long timeAfter = System.currentTimeMillis();
        measure = timeAfter - measure;
        changeAnalysisTime += gc2.getCommitName() + "," + measure + "\n";
        Files.write(Paths.get(featurescheckout + File.separator + "runtime_change_analysis.txt"), String.valueOf(measure).

                getBytes());

        //uncomment
        GitHelper.changePropagation(featurescheckout, gitFolder.getAbsolutePath(), gc1.getCommitName(), gc2.getCommitName(), filesAdded, filesRemoved, filesChanged, RESULTS_PATH);
        changedNodes.clear();
        deletedNodes.clear();
        deletedNodesAux.clear();
        changedNodesAux.clear();
        featureMap.clear();

    }

    private void getFilesToProcess(File f, List<File> files) {
        if (f.isDirectory() && !f.getName().contains(".git")) {
            for (File file : f.listFiles()) {
                if (!files.contains(f) && !file.getName().equals(f.getName()))
                    files.add(f);
                getFilesToProcess(file, files);
            }
        } else if (f.isFile()) {
            if (!f.getName().equals("time.txt"))
                files.add(f);
        }
    }

    public void compare(File srcOriginal, File srcCheckout) throws IOException {
        fileTypes.add("c");
        fileTypes.add("cpp");
        fileTypes.add("h");
        fileTypes.add("hpp");

        LinkedList<File> filesOriginal = new LinkedList<>();
        LinkedList<File> filesCheckout = new LinkedList<>();
        getFilesToProcess(srcCheckout, filesCheckout);
        filesCheckout.remove(srcCheckout);
        getFilesToProcess(srcOriginal, filesOriginal);
        filesOriginal.remove(srcOriginal);
        String outputCSV = srcOriginal.getParentFile().getParentFile().getAbsolutePath();
        String fileStr = outputCSV + File.separator + srcOriginal.getName() + ".csv";
        File fWriter = new File(fileStr);
        FileWriter csvWriter = new FileWriter(fWriter);

        List<List<String>> headerRows = Arrays.asList(
                Arrays.asList("fileName", "matchFile", "truepositiveLines", "falsepositiveLines", "falsenegativeLines", "originaltotalLines", "checkouttotalLines")
        );
        for (List<String> rowData : headerRows) {
            csvWriter.write(String.join(",", rowData));
            csvWriter.write("\n");
        }

        //files that are in checked out and original variant
        for (File f : filesOriginal) {
            System.out.println("TEST PATH files to compare: " + f.getAbsolutePath().substring(f.getAbsolutePath().indexOf(srcOriginal.getName()) + srcOriginal.getName().length() + 1));
            if (changedFilesPropagate.contains(f.getAbsolutePath().substring(f.getAbsolutePath().indexOf(srcOriginal.getName()) + srcOriginal.getName().length() + 1))) {
                System.out.println("ENTROU: " + f.getAbsolutePath().substring(f.getAbsolutePath().indexOf(srcOriginal.getName()) + srcOriginal.getName().length() + 1));
                Boolean existJustCheckoutVariant = false;
                Integer truepositiveLines = 0, falsepositiveLines = 0, falsenegativeLines = 0, originaltotalLines = 0, checkouttotalLines = 0;
                Boolean matchFiles = false;
                List<String> original = new ArrayList<>();
                List<String> revised = new ArrayList<>();

                String extension = f.getName().substring(f.getName().lastIndexOf('.') + 1);
                if (fileTypes.contains(extension)) {
                    //compare text of files
                    for (File fcheckout : filesCheckout) {
                        if (f.toPath().toString().substring(f.toPath().toString().indexOf("original\\") + 9).equals(fcheckout.toPath().toString().substring(fcheckout.toPath().toString().indexOf("checkout\\") + 9))) {
                            try {
                                original = Files.readAllLines(f.toPath());
                            } catch (MalformedInputException e) {
                                StringBuilder contentBuilder = new StringBuilder();
                                File filenew = new File(String.valueOf(f.toPath()));
                                BufferedReader br = new BufferedReader(new FileReader(filenew.getAbsoluteFile()));
                                String sCurrentLine;
                                while ((sCurrentLine = br.readLine()) != null) {
                                    original.add(sCurrentLine);
                                }
                                br.close();
                            }
                            try {
                                revised = Files.readAllLines(fcheckout.toPath());
                            } catch (MalformedInputException e) {
                                StringBuilder contentBuilder = new StringBuilder();
                                File filenew = new File(String.valueOf(fcheckout.toPath()));
                                BufferedReader br = new BufferedReader(new FileReader(filenew.getAbsoluteFile()));
                                String sCurrentLine;
                                while ((sCurrentLine = br.readLine()) != null) {
                                    revised.add(sCurrentLine);
                                }
                                br.close();
                            }

                            // Compute diff. Get the Patch object. Patch is the container for computed deltas.
                            Patch<String> patch = null;
                            patch = DiffUtils.diff(original, revised);
                            ArrayList<String> insertedLines = new ArrayList<>();
                            ArrayList<String> deletedLines = new ArrayList<>();
                            if (patch.getDeltas().size() == 0) {
                                //files match
                                matchFiles = true;
                            } else {
                                //matchFiles = false;
                                String del = "", insert = "";
                                for (Delta delta : patch.getDeltas()) {
                                    Integer difLines = Math.abs(delta.getOriginal().getLines().size() - delta.getRevised().getLines().size());
                                    String line = "";
                                    if (delta.getType().toString().equals("INSERT")) {
                                        ArrayList<String> arraylines = (ArrayList<String>) delta.getRevised().getLines();
                                        for (String deltaaux : arraylines) {
                                            line = deltaaux.trim().replaceAll("\t", "").replaceAll(",", "").replaceAll(" ", "");
                                            if (!line.equals("")) {
                                                matchFiles = false;
                                                falsepositiveLines++;
                                                insert = line;
                                                insertedLines.add(insert);
                                                //falsepositiveLines += difLines;
                                            }
                                        }
                                    } else {
                                        ArrayList<String> arraylines = (ArrayList<String>) delta.getOriginal().getLines();
                                        for (String deltaaux : arraylines) {
                                            line = deltaaux.trim().replaceAll("\t", "").replaceAll(",", "").replaceAll(" ", "");
                                            if (!line.equals("")) {
                                                matchFiles = false;
                                                falsenegativeLines++;
                                                del = line;
                                                deletedLines.add(del);
                                                //falsenegativeLines += difLines;
                                            }
                                        }


                                    }
                                }
                            }
                            ArrayList<String> diffDeleted = new ArrayList<>();
                            Boolean found = false;
                            for (String line : deletedLines) {
                                for (String insertLine : insertedLines) {
                                    if (insertLine.equals(line)) {
                                        falsepositiveLines--;
                                        falsenegativeLines--;
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    diffDeleted.add(line);
                                } else {
                                    insertedLines.remove(line);
                                    found = false;
                                }
                            }

                            if (falsepositiveLines == 0 && falsenegativeLines == 0)
                                matchFiles = true;
                            else {
                                if (diffDeleted.size() > 0) {
                                    for (String line : diffDeleted) {
                                        System.out.println("file: " + fcheckout.getAbsolutePath() + " TYPE: DELETE delta: " + line);
                                    }
                                }
                                if (insertedLines.size() > 0) {
                                    for (String line : insertedLines) {
                                        System.out.println("file: " + fcheckout.getAbsolutePath() + " TYPE: INSERT delta: " + line);
                                    }
                                }
                            }

                            checkouttotalLines = (revised.size() - 1);
                            originaltotalLines = original.size() - 1;
                            truepositiveLines = checkouttotalLines - (falsepositiveLines);

                            List<List<String>> resultRows = Arrays.asList(
                                    Arrays.asList(f.toPath().toString().substring(f.toPath().toString().indexOf("original\\") + 9).replace(",", "and"), matchFiles.toString(), truepositiveLines.toString(), falsepositiveLines.toString(), falsenegativeLines.toString(), originaltotalLines.toString(), checkouttotalLines.toString())
                            );
                            for (List<String> rowData : resultRows) {
                                csvWriter.append(String.join(",", rowData));
                                csvWriter.append("\n");
                            }
                            existJustCheckoutVariant = true;
                        }

                    }
                    if (!existJustCheckoutVariant) {
                        List<List<String>> resultRows = Arrays.asList(
                                Arrays.asList(f.toPath().toString().substring(f.toPath().toString().indexOf("original\\") + 9).replace(",", "and"), "not", "0", "0", originaltotalLines.toString(), originaltotalLines.toString(), checkouttotalLines.toString())
                        );
                        for (List<String> rowData : resultRows) {
                            csvWriter.append(String.join(",", rowData));
                            csvWriter.append("\n");
                        }
                    }
                }
            }
        }
        csvWriter.close();
    }


}




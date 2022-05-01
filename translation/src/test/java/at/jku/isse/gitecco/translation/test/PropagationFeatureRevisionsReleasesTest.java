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
import at.jku.isse.gitecco.translation.visitor.GetNodesForChangeVisitor;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PropagationFeatureRevisionsReleasesTest {

    private static String REPO_PATH = "C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\ChangePropagation\\Bison";//Bison irssi\irssi sqlite\sqlite  libssh
    private static String MINING_DATASET = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\LOC_fix\\Bison\\Mining"; //"C:\\Users\\gabil\\Desktop\\PHD\\Mining\\LOC_fix\\SQLite-ready";
    private static String resultsFolder = "C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\releases-propagation\\dataset\\Bison3";
    private static String stopRelease = "v3.5.3"; //Bison:"v3.5.3"; Irssi: "1.3-dev"; LibSSH: "libssh-0.9.5"; SQLite: "version-3.31.1"
    private static Path pathFileRandomCombination = Paths.get("C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\releases-propagation\\dataset\\Bison_random_combinations.txt");
    private static Boolean aux = true;
    private static int numberOfRandomPairsOfReleases = 50;
    private static ArrayList<String> featureNamesListDestinationRelease = new ArrayList<String>();
    private static ArrayList<Feature> featurespropagate = new ArrayList<>();
    private static ArrayList<String> featurespropagateName = new ArrayList<>();
    private static String pathFeatures = MINING_DATASET + File.separator + "features-";
    private static String FEATURES_TXT = pathFeatures;
    private static String FEATURESPROPRAGATE_TXT = pathFeatures;
    private static Map<Feature, Integer> featureVersions = new HashMap<>();
    private static List<String> changedFiles = new ArrayList<>();
    private static List<String> changedFilesNext = new ArrayList<>();
    private static List<String> changedFilesPropagate = new ArrayList<>();
    private static List<String> commonFeaturesBetweenReleases = new ArrayList<>();
    private static List<String> featuresRevised = new ArrayList<>();
    private static GitCommit gcPrevious = null;
    private static Boolean previous = true;
    private static String feats;
    private static String changeAnalysisTime = "";
    private static List<String> configurations = new ArrayList<>();
    private static List<String> fileTypes = new LinkedList<String>();
    private static Boolean computeChangeCharacteristics = true;
    private static int numberFeaturesRemoved = 0;
    private static int numberFeaturesIntroduced = 0;


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


    public static List<List<Long>> getRandomPairsOfReleases(List<List<Long>> list, int totalItems) {
        Random rand = new Random();

        // create a temporary list for storing
        // selected pairs of releases
        List<List<Long>> newList = new ArrayList<>();
        for (int i = 0; i < totalItems; i++) {

            // take a random index between 0 to size
            // of given List
            int randomIndex = rand.nextInt(list.size());

            // add element in temporary list
            newList.add(list.get(randomIndex));

            // Remove selected element from original list
            list.remove(randomIndex);
        }
        return newList;
    }

    public static void identification() throws Exception {
        try {
            FileWriter csvWriter = new FileWriter(resultsFolder + File.separator + "report_each_commit.csv");
            List<List<String>> headerRows = Arrays.asList(
                    Arrays.asList("DCommitHash", "OCommitHash", "DReleaseName", "OReleaseName", "NumberOfCommitsInBetween", "NFeaturesDestination", "NFeaturesOrigin", "Patches", "NFeaturesRevised", "NFeaturesIntroduced", "NFeaturesRemoved", "NFeaturesPropagated", "NFiles", "NLinesAdded", "NLinesRemoved", "RuntimeAnalysis", "RuntimePropagation")
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


        long measure = System.currentTimeMillis();
        String repoPath = REPO_PATH;

        final GitHelper gitHelper = new GitHelper(repoPath, null);
        GitCommitList commitList = new GitCommitList(gitHelper);

        Multimap<Long, String> mapTags = gitHelper.getCommitHashTag();
        Multimap<Long, String> mapReleasesGPCE = MultimapBuilder.treeKeys().arrayListValues().build();
        Set keySet = mapTags.keySet();
        Iterator keyIterator = keySet.iterator();
        while (keyIterator.hasNext()) {
            Long key = (Long) keyIterator.next();
            Collection<String> value = mapTags.get(key);
            String nameRel = value.iterator().next();
            if (nameRel.equals(stopRelease))
                break;
            else
                mapReleasesGPCE.put(key, nameRel);
        }

        //Generates all 2-combination of the set of releases (two releases are distinct)
        /*List<List<Long>> combinations = Generator.combination(mapReleasesGPCE.keySet())
                .simple(2)
                .stream() //.forEach(System.out::println);
                .collect(toList());

        combinations.forEach(System.out::println);

        List<List<Long>> pairsOfReleases = getRandomPairsOfReleases(combinations, numberOfRandomPairsOfReleases);
        pairsOfReleases.forEach(System.out::println);*/

        List<List<Long>> pairsOfReleases = new ArrayList<>();
        try (Stream<String> lines = Files.lines(pathFileRandomCombination)) {
            List<String> line = lines.collect(Collectors.toList());
            for (String l : line) {
                Long first = Long.valueOf(l.substring(l.indexOf("[") + 1, l.indexOf(",")));
                Long second = Long.valueOf(l.substring(l.indexOf(" ") + 1, l.indexOf("]")));
                List<Long> aux = new ArrayList<>();
                aux.add(first);
                aux.add(second);
                pairsOfReleases.add(aux);
            }
        } catch (IOException e) {
            System.out.println("Failed to load file." + e);
        }


        Collection<String> releaseDestination = mapTags.get(pairsOfReleases.get(0).get(0));
        Collection<String> releaseOrigin = mapTags.get(pairsOfReleases.get(0).get(1));

        Map<Map<String, List<String>>, Changes> results = new HashMap<>();

        Iterator<String> releaseDestinationIt = releaseDestination.iterator();
        Iterator<String> releaseOriginIt = releaseOrigin.iterator();
        String nameDestination = releaseDestinationIt.next();
        String hashDestination = releaseDestinationIt.next();
        String nameOrigin = releaseOriginIt.next();
        String hashOrigin = releaseOriginIt.next();
        System.out.println(pairsOfReleases.get(0).get(0) + " Release name: " + nameDestination + " Release hash: " + hashDestination);
        System.out.println(pairsOfReleases.get(0).get(1) + " Release name: " + nameOrigin + " Release hash: " + hashOrigin);
        gitHelper.getCommitByHash(commitList, hashDestination, hashOrigin);
        String pairreleasespath = "Pair-0-";
        File releasesFolder = new File(resultsFolder, pairreleasespath+hashDestination);
        if (!releasesFolder.exists())
            releasesFolder.mkdir();
        File checkoutFolder = new File(releasesFolder, "checkout");
        if (!checkoutFolder.exists())
            checkoutFolder.mkdir();
        File originalFolder = new File(releasesFolder, "original");
        // if the directory does not exist, create it
        if (!originalFolder.exists())
            originalFolder.mkdir();
        File changeFolder = new File(releasesFolder, "ChangeCharacteristic");
        Long numberCommits = pairsOfReleases.get(0).get(1) - pairsOfReleases.get(0).get(0);

        if (aux) {
            FEATURES_TXT += nameDestination + ".txt";
            featureNamesListDestinationRelease = new ArrayList<>();
            addFeaturesReleaseDestination(commitList.get(0), nameDestination);
            FEATURESPROPRAGATE_TXT += nameOrigin + ".txt";
            featurespropagate = new ArrayList<>();
            addFeaturesPropagate(commitList.get(1), nameOrigin);
            GitHelper.copyCommitFilesToAFolder(REPO_PATH, originalFolder, hashOrigin);
            if (computeChangeCharacteristics) {
                if (!changeFolder.exists())
                    changeFolder.mkdir();
            }
            commonFeaturesBetweenReleases = new ArrayList<String>(featureNamesListDestinationRelease);
            commonFeaturesBetweenReleases.retainAll(featurespropagateName);
            numberFeaturesRemoved = featureNamesListDestinationRelease.size() - commonFeaturesBetweenReleases.size();
            numberFeaturesIntroduced = featurespropagateName.size() - commonFeaturesBetweenReleases.size();
            featuresRevised = new ArrayList<>();
            characteristicsChange(gitHelper, numberCommits, nameDestination, nameOrigin, commitList.get(0), commitList.get(1), featureNamesListDestinationRelease, featurespropagate, results, checkoutFolder, changeFolder);
            compare(originalFolder, checkoutFolder);
        }

        int iteratorPairs = 1;
        int numberpair = 1;
        while (iteratorPairs < numberOfRandomPairsOfReleases) {
            releaseDestination = mapTags.get(pairsOfReleases.get(iteratorPairs).get(0));
            releaseOrigin = mapTags.get(pairsOfReleases.get(iteratorPairs).get(1));
            releaseDestinationIt = releaseDestination.iterator();
            releaseOriginIt = releaseOrigin.iterator();
            nameDestination = releaseDestinationIt.next();
            hashDestination = releaseDestinationIt.next();
            nameOrigin = releaseOriginIt.next();
            hashOrigin = releaseOriginIt.next();
            if (nameDestination.equals(stopRelease))
                break;
            //else if (destination.get(1).equals("1c671bad33537dc91c628b982f006a47d1fdce6c"))
            //   aux = true;

            System.out.println(pairsOfReleases.get(iteratorPairs).get(0) + " " + nameDestination + " " + hashDestination);
            System.out.println(pairsOfReleases.get(iteratorPairs).get(1) + " " + nameOrigin + " " + hashOrigin);
            if (aux) {
                commitList = new GitCommitList(gitHelper);
                gitHelper.getCommitByHash(commitList, hashDestination, hashOrigin);
                FEATURES_TXT = pathFeatures + nameDestination + ".txt";
                featureNamesListDestinationRelease = new ArrayList<>();
                addFeaturesReleaseDestination(commitList.get(0), nameDestination);
                FEATURESPROPRAGATE_TXT = pathFeatures + nameDestination + ".txt";
                featurespropagate = new ArrayList<>();
                addFeaturesPropagate(commitList.get(1), nameOrigin);
                results = new HashMap<>();
                releasesFolder = new File(resultsFolder, "Pair-"+numberpair+"-"+hashDestination);
                if (!releasesFolder.exists())
                    releasesFolder.mkdir();
                checkoutFolder = new File(releasesFolder, "checkout");
                if (!checkoutFolder.exists())
                    checkoutFolder.mkdir();
                originalFolder = new File(releasesFolder, "original");
                // if the directory does not exist, create it
                if (!originalFolder.exists())
                    originalFolder.mkdir();
                GitHelper.copyCommitFilesToAFolder(REPO_PATH, originalFolder, hashOrigin);
                changeFolder = new File(releasesFolder, "ChangeCharacteristic");
                if (computeChangeCharacteristics) {
                    if (!changeFolder.exists())
                        changeFolder.mkdir();
                }

                initVars(releasesFolder.getAbsolutePath());
                commonFeaturesBetweenReleases = new ArrayList<String>(featureNamesListDestinationRelease);
                commonFeaturesBetweenReleases.retainAll(featurespropagateName);
                numberFeaturesRemoved = featureNamesListDestinationRelease.size() - commonFeaturesBetweenReleases.size();
                numberFeaturesIntroduced = featurespropagateName.size() - commonFeaturesBetweenReleases.size();
                featuresRevised = new ArrayList<>();
                numberCommits = pairsOfReleases.get(iteratorPairs).get(1) - pairsOfReleases.get(iteratorPairs).get(0);
                characteristicsChange(gitHelper, numberCommits, nameDestination, nameOrigin, commitList.get(0), commitList.get(1), featureNamesListDestinationRelease, featurespropagate, results, checkoutFolder, changeFolder);
                compare(originalFolder, checkoutFolder);
            }
            iteratorPairs++;
            numberpair++;
        }
    }

    private static void getFilesToProcess(File f, List<File> files) {
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

    public static void compare(File srcOriginal, File srcCheckout) throws IOException {
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
        String fileStr = outputCSV + File.separator + srcOriginal.getParentFile().getName() + ".csv";
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


    public static void initVars(String folderRelease) {
        changedFiles.clear();
        changedFilesNext.clear();
        gcPrevious = null;
        previous = true;
        configurations.clear();
        featureVersions.clear();
        changedFilesPropagate.clear();
    }

    public static void characteristicsChange(GitHelper gitHelper, Long numberCommits, String nameDestination, String nameOrigin, GitCommit gc1, GitCommit gc2, ArrayList<String> featureNamesList, ArrayList<Feature> featurespropagate, Map<Map<String, List<String>>, Changes> results, File checkoutFolder, File changeFolder) throws Exception {

        final File gitFolder = new File(gitHelper.getPath());

        Integer countFeaturesChanged = 0; //COUNT PER GIT COMMIT
        Integer newFeatures = 0; //COUNT PER GIT COMMIT
        ArrayList<Feature> featureschanged = new ArrayList<>();

        GetNodesForChangeVisitor visitor = new GetNodesForChangeVisitor();
        ArrayList<ConditionalNode> changedNodes = new ArrayList<>();
        ArrayList<ConditionalNode> deletedNodes = new ArrayList<>();
        //ArrayList<ConditionalNode> propagateChangedNodes = new ArrayList<>();
        ArrayList<ConditionalNode> propagateDeletedNodes = new ArrayList<>();
        ArrayList<String> filesAdded = new ArrayList<>();
        ArrayList<String> filesRemoved = new ArrayList<>();
        Map<String, Map<Integer, FileChange>> filesChanged = new HashMap<>();
        Map<Feature, ChangeCharacteristic> featureMap = new HashMap<>();
        Changes mapAux = new Changes();
        int patches = 0;

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

        System.out.println("Number of changed nodes: " + changedNodesAux.size());
        ArrayList<ConditionalNode> deletedNodesAux = new ArrayList<>();
        for (ConditionalNode deletedNode : deletedNodes) {
            if (!deletedNodesAux.contains(deletedNode))
                deletedNodesAux.add(deletedNode);
        }

        patches += changedNodes.size() + deletedNodes.size();

        ArrayList<String> configsToCommit = new ArrayList<>();
        Map<Map<Feature, Integer>, String> configsToGenerateVariant = new HashMap<>();
        Feature base = new Feature("BASE");
        //changedNodes = changedNodes.stream().filter(x -> x.getLocalCondition().equals("__AVR_ATmega644P__ || __AVR_ATmega644__")).collect(Collectors.toSet());
        for (ConditionalNode changedNode : changedNodesAux) {
            String file = "";
            if (changedNode.getContainingFile() != null) {
                file = changedNode.getContainingFile().getFilePath();
                //compute the config for the var gen
                config = constraintComputer.computeConfig(changedNode, gc2.getTree());
                if (config != null && !config.isEmpty()) {
                    //compute the marked as changed features.
                    changed = constraintComputer.computeChangedFeatures(changedNode, config);
                    for (Feature feature : featurespropagate) {
                        if (changed.contains(feature)) {
                            //if (changed.contains(featpropagate)) {
                            //propagateChangedNodes.add(changedNode);
                            if (commonFeaturesBetweenReleases.contains(feature.getName()) && !featuresRevised.contains(feature.getName()))
                                featuresRevised.add(feature.getName());
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


                            if (computeChangeCharacteristics) {
                                int tanglingDegree = 0;
                                for (Map.Entry<Feature, Integer> feat : config.entrySet()) {
                                    if (featureNamesList.contains(feat.getKey().getName()))
                                        tanglingDegree++;
                                }

                                ChangeCharacteristic changeCharacteristic;
                                if (featureMap.get(feature) == null) {
                                    changeCharacteristic = new ChangeCharacteristic();
                                    featureMap.put(feature, new ChangeCharacteristic());
                                } else {
                                    changeCharacteristic = featureMap.get(feature);
                                }
                                int aux = 0;
                               /* if (changedNode.getLineNumberInserts().size() > 0) {
                                    for (int i = 2; i < changedNode.getLineNumberInserts().size(); i += 3) {
                                        aux += changedNode.getLineNumberInserts().get(i);
                                    }
                                    changeCharacteristic.setLinesOfCodeAdded(changeCharacteristic.getLinesOfCodeAdded() + aux);
                                    changedNode.getLineNumberInserts().removeAll(changedNode.getLineNumberInserts());
                                }

                                if (changedNode.getLineNumberDeleted().size() > 0) {
                                    for (int i = 2; i < changedNode.getLineNumberDeleted().size(); i += 3) {
                                        aux += changedNode.getLineNumberDeleted().get(i);
                                    }
                                    changeCharacteristic.setLinesOfCodeRemoved(changeCharacteristic.getLinesOfCodeRemoved() + aux);
                                    changedNode.getLineNumberDeleted().removeAll(changedNode.getLineNumberDeleted());
                                }*/

                                int totalLinesAdded = 0;
                                int totallinesRemoved = 0;
                                for (Deltas delta : changedNode.getLineNumberInsertsAndDeletes()) {
                                    if (delta.getTotalLinesAdd() != -1)
                                        totalLinesAdded += delta.getTotalLinesAdd();
                                    if (delta.getTotalLinesRemov() != -1)
                                        totallinesRemoved += delta.getTotalLinesRemov();
                                }
                                changeCharacteristic.setLinesOfCodeAdded(changeCharacteristic.getLinesOfCodeAdded() + totalLinesAdded);
                                changeCharacteristic.setLinesOfCodeRemoved(changeCharacteristic.getLinesOfCodeRemoved() + totallinesRemoved);

                                changeCharacteristic.addTanglingDegree(tanglingDegree);
                                if (!changeCharacteristic.getScatteringDegreeFiles().contains(file)) {
                                    changeCharacteristic.addScatteringDegreeFiles(file);
                                }
                                if (!(changedNode instanceof BaseNode))
                                    changeCharacteristic.setScatteringDegreeIfs(changeCharacteristic.getScatteringDegreeIfs() + 1);
                                ChangeCharacteristic finalChangeCharacteristic = changeCharacteristic;
                                featureMap.computeIfAbsent(feature, v -> finalChangeCharacteristic);
                                featureMap.computeIfPresent(feature, (k, v) -> finalChangeCharacteristic);
                            }

                        }
                    }
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
                            if (!featureschanged.contains(feature))
                                featureschanged.add(feature);
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

                            int tanglingDegree = 0;
                            for (Map.Entry<Feature, Integer> feat : config.entrySet()) {
                                if (featureNamesList.contains(feat.getKey().getName()))
                                    tanglingDegree++;
                            }

                            if (computeChangeCharacteristics) {
                                ChangeCharacteristic changeCharacteristic = featureMap.get(feature);
                                if (changeCharacteristic == null)
                                    changeCharacteristic = new ChangeCharacteristic();
                                int aux = 0;
                                /*if (deletedNode.getLineNumberDeleted().size() > 0) {
                                    for (int i = 2; i < deletedNode.getLineNumberDeleted().size(); i += 3) {
                                        aux += deletedNode.getLineNumberDeleted().get(i);
                                    }
                                    changeCharacteristic.setLinesOfCodeRemoved(changeCharacteristic.getLinesOfCodeRemoved() + aux);
                                    deletedNode.getLineNumberDeleted().removeAll(deletedNode.getLineNumberDeleted());
                                }*/
                                int totalLinesAdded = 0;
                                int totallinesRemoved = 0;
                                for (Deltas delta : deletedNode.getLineNumberInsertsAndDeletes()) {
                                    if (delta.getTotalLinesRemov() != -1)
                                        totallinesRemoved += delta.getTotalLinesRemov();
                                }
                                changeCharacteristic.setLinesOfCodeRemoved(changeCharacteristic.getLinesOfCodeRemoved() + totallinesRemoved);

                                changeCharacteristic.addTanglingDegree(tanglingDegree);
                                if (!changeCharacteristic.getScatteringDegreeFiles().contains(file)) {
                                    changeCharacteristic.addScatteringDegreeFiles(file);
                                }
                                if (!(deletedNode instanceof BaseNode))
                                    changeCharacteristic.setScatteringDegreeIfs(changeCharacteristic.getScatteringDegreeIfs() + 1);
                                ChangeCharacteristic finalChangeCharacteristic = changeCharacteristic;
                                featureMap.computeIfAbsent(feature, v -> finalChangeCharacteristic);
                                featureMap.computeIfPresent(feature, (k, v) -> finalChangeCharacteristic);
                            }
                        }
                    }

                }
            }
        }

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
        Files.write(Paths.get(checkoutFolder + File.separator + "runtime_change_analysis.txt"), String.valueOf(measure).

                getBytes());
        //uncomment
        GitHelper.changePropagation(checkoutFolder, gitFolder.getAbsolutePath(), numberCommits, nameDestination, nameOrigin, gc1.getCommitName(), gc2.getCommitName(), patches, filesAdded, filesRemoved, filesChanged, resultsFolder, featureNamesList.size(), featurespropagate.size(), featuresRevised.size(), numberFeaturesIntroduced, numberFeaturesRemoved, featuresRevised.size() + numberFeaturesIntroduced + numberFeaturesRemoved, measure);
        changedNodes.clear();
        deletedNodes.clear();
        deletedNodesAux.clear();
        changedNodesAux.clear();
        featureMap.clear();

    }


    public static void addFeaturesPropagate(GitCommit commit, String release) throws IOException {
        try {
            BufferedReader br = new BufferedReader(new FileReader(FEATURESPROPRAGATE_TXT));
            String line = "";
            featurespropagateName = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] cols = line.split(",");
                if (!featurespropagate.contains(cols[0].substring(1).replace("\"", ""))) {
                    Feature featpropagate = new Feature(cols[0].substring(1).replace("\"", ""));
                    featurespropagate.add(featpropagate);
                    featurespropagateName.add(featpropagate.getName());
                }
                for (int i = 1; i < cols.length - 1; i++) {
                    if (!featurespropagate.contains(cols[i].replace("\"", ""))) {
                        Feature featpropagate = new Feature(cols[i].replace("\"", ""));
                        featurespropagate.add(featpropagate);
                        featurespropagateName.add(featpropagate.getName());
                    }
                }
                String lastfeature = cols[cols.length - 1].replace("\"", "");
                if (!featurespropagate.contains(lastfeature.replace("}", ""))) {
                    Feature featpropagate = new Feature(lastfeature.replace("}", ""));
                    featurespropagate.add(featpropagate);
                    featurespropagateName.add(featpropagate.getName());
                }
                if (!featurespropagate.contains("BASE")) {
                    Feature featpropagate = new Feature("BASE");
                    featurespropagate.add(featpropagate);
                    featurespropagateName.add(featpropagate.getName());
                }
            }
        } catch (IOException ex) {
            final List<TraceableFeature> evaluation = Collections.synchronizedList(new ArrayList<>());
            String csvFile = release.substring(release.lastIndexOf("/") + 1);
            System.out.println("for git identifyFeatures" + commit.getCommitName());
            at.jku.isse.gitecco.featureid.identification.ID.evaluateFeatureMap(evaluation, at.jku.isse.gitecco.featureid.identification.ID.id(commit.getTree()), commit.getNumber());
            writeToTXT(evaluation, release, resultsFolder);
            evaluation.clear();
            System.out.println("catch release origin");
            FEATURES_TXT = pathFeatures + release + ".txt";
            addFeaturesPropagate(commit, release);
        }
    }

    public static void addFeaturesReleaseDestination(GitCommit commit, String release) throws IOException {
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
            final List<TraceableFeature> evaluation = Collections.synchronizedList(new ArrayList<>());
            String csvFile = release.substring(release.lastIndexOf("/") + 1);
            System.out.println("for git identifyFeatures" + commit.getCommitName());
            at.jku.isse.gitecco.featureid.identification.ID.evaluateFeatureMap(evaluation, at.jku.isse.gitecco.featureid.identification.ID.id(commit.getTree()), commit.getNumber());
            writeToTXT(evaluation, release, resultsFolder);
            evaluation.clear();
            System.out.println("catch release destination");
            FEATURES_TXT = pathFeatures + release + ".txt";
            addFeaturesReleaseDestination(commit, release);
            //System.out.println((char) 27 + "[31m" + "Error reading the features release destination!\nVerify if you wrote in the correct format -> {\"WITH_SERVER\",\"HAVE_LIBZ\",...}");
        }
    }

    private static void writeToTXT(List<TraceableFeature> features, String fileName, String featuresRepo) throws
            IOException {
        feats = "{";
        PrintWriter writerTXT = new PrintWriter(featuresRepo + File.separator + "features-" + fileName + ".txt", "UTF-8");
        //write each feature/label with: Name, totalOcc, InternalOcc, externalOcc, transientOcc.
        for (TraceableFeature feature : features) {
            if (feature.getExternalOcc() == feature.getTotalOcc() || feature.getExternalOcc() == feature.getCommitList().size()) {
                feats += "\"" + feature.getName() + "\",";
            }
        }
        writerTXT.println(feats.substring(0, feats.length() - 1) + "}");
        writerTXT.close();
        System.out.println(feats.substring(0, feats.length() - 1) + "}");
    }

}

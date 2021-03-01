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
import com.opencsv.CSVWriter;
import org.glassfish.grizzly.http.server.accesslog.FileAppender;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class VariantsWithFeatures {

    static boolean EVERYCOMMIT = false;
    static int EVERY_NTH_COMMIT = 1;
    static ArrayList<Feature> featureList = new ArrayList<>();
    static ArrayList<String> featureNamesList = new ArrayList<String>();
    //set as true to generate random variants or false to just generate original variants
    static boolean generateRandomVariants = false;
    //set as true to generate PP variants or false to just generate configurations to generate random variants
    static boolean generateOriginalVariants = false;
    static Integer commitInit = 100;
    static Integer commitEnd = 100;
    static String REPO_PATH = "";//"C:\\Users\\gabil\\Desktop\\PHD\\ChallengePaper\\TestScript\\LibSSH\\libssh";
    static String FEATURES_PATH = "";//"C:\\Users\\gabil\\Desktop\\PHD\\ChallengePaper\\TestScript\\LibSSH";
    static String fileReportFeature = "features_report_each_project_commit.csv";
    static String fileStoreConfig = "configurations.csv";
    static String fileStoreRandomConfig = "newconfigurations.csv";
    static String featuretxt = File.separator + "features-allcommits.txt";
    static GitCommit gcPrevious = null;
    static Boolean previous = true;
    static ArrayList<String> configurations = new ArrayList<>();
    static ArrayList<String> randomconfigurations = new ArrayList<>();
    static ArrayList<String> arrayfeat = new ArrayList<>();
    static String feats;
    static Boolean createScenarios = false;
    static Boolean createNewConfigurations = false;
    static Integer count = 1;
    static Integer maxInputConfig = 300;
    static Integer maxNewConfig = 50;

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
            commitInit = Integer.valueOf(args[2])+1;
            commitEnd = Integer.valueOf(args[2])+1;
            if (args[3].equals("1")) {
                createScenarios = true;
            } else if (args[3].equals("2")) {
                createNewConfigurations = true;
            } else if (args[3].equals("3")) {
                createScenarios = true;
                createNewConfigurations = true;
            }
            if (args[4].equals("1")) {
                generateOriginalVariants = true;
            } else if (args[4].equals("2")) {
                generateRandomVariants = true;
            } else if (args[4].equals("3")) {
                generateOriginalVariants = true;
                generateRandomVariants = true;
            }
            if(args.length > 5){
                maxInputConfig= Integer.valueOf(args[5]);
            }
            if(args.length > 6){
                maxNewConfig= Integer.valueOf(args[6]);
            }
            identification();
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
            Map<Long, String> mapTags = gitHelper.getCommitNumberTag();
            LinkedHashMap<Long, String> orderedMap = mapTags.entrySet() //
                    .stream() //
                    .sorted(Map.Entry.comparingByKey()) //
                    .collect(Collectors.toMap(Map.Entry::getKey, //
                            Map.Entry::getValue, //
                            (key, content) -> content, //
                            LinkedHashMap::new)); //
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
            String releases = "commitToPreprocess";
            File foldereachRelease = new File(folderRelease);
            if (!foldereachRelease.exists())
                foldereachRelease.mkdir();
            final File idFeatsfolder = new File(foldereachRelease, "IdentifiedFeatures");
            if (!idFeatsfolder.exists())
                idFeatsfolder.mkdir();
            //feature identification
            identifyFeatures(commitList, releases, idFeatsfolder);
            addFeatures();
            initVars(foldereachRelease.getAbsolutePath());
            File fileconfig = new File(folderRelease, fileStoreConfig);
            if (fileconfig.exists())
                fileconfig.delete();
            //for (GitCommit commits : commitList) {3
            GitCommit commits = commitList.get(0);
            generateVariants(gitHelper, commits, featureNamesList, foldereachRelease.getAbsolutePath());
            //dispose tree if it is not needed -> for memory saving reasons.
            commits.disposeTree();
            //}
        }
        System.out.println("finished analyzing repo");
    }

    public static void initVars(String folderRelease) {
        gcPrevious = null;
        previous = true;
        configurations.clear();
    }

    public static void addFeatures() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(FEATURES_PATH + featuretxt));
        String line = "";
        while ((line = br.readLine()) != null) {
            // use comma as separator
            String[] cols = line.split(",");
            if (!featureNamesList.contains(cols[0].substring(1).replace("\"", "")))
                featureNamesList.add(cols[0].substring(1).replace("\"", ""));
            for (int i = 2; i < cols.length - 1; i++) {
                if (!featureNamesList.contains(cols[i].replace("\"", "")))
                    featureNamesList.add(cols[i].replace("\"", ""));
            }
            String lastfeature = cols[cols.length - 1].replace("\"", "");
            if (!featureNamesList.contains(lastfeature.replace("}", "")))
                featureNamesList.add(lastfeature.replace("}", ""));
            if (!featureNamesList.contains("BASE"))
                featureNamesList.add("BASE");
        }
    }

    public static void generateVariants(GitHelper gitHelper, GitCommit gc, ArrayList<String> featureNamesList, String folder) throws Exception {
        final File gitFolder = new File(gitHelper.getPath());
        final File eccoFolder = new File(gitFolder.getParent(), "inputConfigurations");
        final File randomVariantFolder = new File(gitFolder.getParent(), "newConfigurations");

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

        final PreprocessorHelper pph = new PreprocessorHelper();
        Map<Integer, Map<Feature, Integer>> mapConfigToGenerateRandomVariants = new HashMap<>();
        Map<Integer, String> mapRevisionToGenerateRandomVariants = new HashMap<>();
        Map<Integer, String> mapRevisionToGenerateRandomVariantsFinal = new HashMap<>();


        if (createScenarios) {
            while (count <= maxInputConfig) {
                createScenarios();
                arrayfeat.clear();
            }
        } else {
            FileInputStream stream = new FileInputStream(FEATURES_PATH + File.separator + "InputConfigurations.txt");
            InputStreamReader reader = new InputStreamReader(stream);
            BufferedReader br = new BufferedReader(reader);
            String linha = br.readLine();
            while (linha != null) {
                String eccoConfig = linha.substring(linha.indexOf(":") + 2);
                configurations.add(eccoConfig);
                linha = br.readLine();
            }
        }

        if (createNewConfigurations) {
            arrayfeat.clear();
            count = 1;
            while (count <= maxNewConfig) {
                createNewConfigurations();
                arrayfeat.clear();
            }
        } else {
            FileInputStream stream = new FileInputStream(FEATURES_PATH + File.separator + "NewConfigurations.txt");
            InputStreamReader reader = new InputStreamReader(stream);
            BufferedReader br = new BufferedReader(reader);
            String linha = br.readLine();
            while (linha != null) {
                String eccoConfig = linha.substring(linha.indexOf(":") + 2);
                randomconfigurations.add(eccoConfig);
                linha = br.readLine();
            }
        }

        if (generateOriginalVariants) {
            FileInputStream stream = new FileInputStream(FEATURES_PATH + File.separator + "InputConfigurations.txt");
            InputStreamReader reader = new InputStreamReader(stream);
            BufferedReader br = new BufferedReader(reader);
            String linha = br.readLine();
            while (linha != null) {
                System.out.println("------ Variant to generate with config:");
                System.out.println(linha);
                String eccoConfig = linha.substring(linha.indexOf(":") + 2);
                String variantName = linha.substring(0, linha.indexOf(":"));
                File variantsrc = new File(eccoFolder, variantName);
                Map<Feature, Integer> featurespreprocess = new HashMap<>();
                String[] features = eccoConfig.split(",");
                for (String conf : features) {
                    Feature f = new Feature(conf);
                    featurespreprocess.put(f, 1);
                }
                gitHelper.checkOutCommit(gc);
                pph.generateVariants(featurespreprocess, gitFolder, eccoFolder, gitHelper.getDirFiles(), variantsrc.getName());
                System.out.println("Variant generated with config: " + eccoConfig);
                File configfile = new File(eccoFolder, variantName + ".config");
                BufferedWriter writer = new BufferedWriter(new FileWriter(configfile));
                writer.write(eccoConfig);
                writer.close();
                linha = br.readLine();
            }
        }

        if (generateRandomVariants) {
            FileInputStream stream = new FileInputStream(FEATURES_PATH + File.separator + "NewConfigurations.txt");
            InputStreamReader reader = new InputStreamReader(stream);
            BufferedReader br = new BufferedReader(reader);
            String linha = br.readLine();
            while (linha != null) {
                System.out.println("------ NEW variant to generate with config:");
                System.out.println(linha);
                String eccoConfig = linha.substring(linha.indexOf(":") + 2);
                String variantName = linha.substring(0, linha.indexOf(":"));
                File variantsrc = new File(randomVariantFolder, variantName);
                Map<Feature, Integer> featurespreprocess = new HashMap<>();
                String[] features = eccoConfig.split(",");
                for (String conf : features) {
                    Feature f = new Feature(conf);
                    featurespreprocess.put(f, 1);
                }
                gitHelper.checkOutCommit(gc);
                pph.generateVariants(featurespreprocess, gitFolder, randomVariantFolder, gitHelper.getDirFiles(), variantsrc.getName());
                System.out.println("NEW variant generated with config: " + eccoConfig);
                File configfile = new File(randomVariantFolder, variantName + ".config");
                BufferedWriter writer = new BufferedWriter(new FileWriter(configfile));
                writer.write(eccoConfig);
                writer.close();
                linha = br.readLine();
            }
            //appending to the config csv
            try {
                FileAppender csvWriter = new FileAppender(new File(folder, fileStoreRandomConfig));
                int variantnumber = 1;
                for (String configs : randomconfigurations) {
                    List<List<String>> headerRows = Arrays.asList(
                            Arrays.asList("Variant" + Long.toString(variantnumber), Long.toString(gc.getNumber()), gc.getCommitName(), configs)
                    );
                    for (List<String> rowData : headerRows) {
                        csvWriter.append(String.join(",", rowData));
                    }
                    variantnumber++;
                }
                csvWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //appending to the config csv
        try {
            FileAppender csvWriter = new FileAppender(new File(folder, fileStoreConfig));
            int variantnumber = 1;
            for (String configs : configurations) {
                List<List<String>> headerRows = Arrays.asList(
                        Arrays.asList("Variant" + Long.toString(variantnumber), Long.toString(gc.getNumber()), gc.getCommitName(), configs)
                );
                for (List<String> rowData : headerRows) {
                    csvWriter.append(String.join(",", rowData));
                }
                variantnumber++;
            }
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void createScenarios() {
        String config = "";
        String[] list = featureNamesList.toArray(new String[0]);
        int length = list.length;
        for (int i = 0; i < length; i++) {
            int rand = (int) (Math.random() * length);
            System.out.print(list[rand]);
            System.out.print(" ");
            if (!arrayfeat.contains(list[rand])) {
                arrayfeat.add(list[rand]);
                config += "," + list[rand];
            }
        }
        if (arrayfeat.contains("BASE"))
            arrayfeat.add("BASE");
        config = config.replaceFirst(",", "");
        if (!configurations.contains(config)) {
            configurations.add(config);
        } else {
            createScenarios();
        }
        try {
            final Path path = Paths.get(FEATURES_PATH + File.separator + "InputConfigurations.txt");
            Files.write(path, Arrays.asList("Variant" + count + ": " + config), StandardCharsets.UTF_8,
                    Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
            count++;
        } catch (final IOException ioe) {
            System.out.println("Error appending scenario description in text file: " + ioe);
        }
    }

    public static void createNewConfigurations() {
        String config = "";
        String[] list = featureNamesList.toArray(new String[0]);
        int length = list.length;
        for (int i = 0; i < length; i++) {
            int rand = (int) (Math.random() * length);
            System.out.print(list[rand]);
            System.out.print(" ");
            if (!arrayfeat.contains(list[rand])) {
                arrayfeat.add(list[rand]);
                config += "," + list[rand];
            }
        }
        if (arrayfeat.contains("BASE"))
            arrayfeat.add("BASE");
        config = config.replaceFirst(",", "");
        if (!configurations.contains(config) && !randomconfigurations.contains(config)) {
            randomconfigurations.add(config);
        } else {
            createNewConfigurations();
        }
        try {
            final Path path = Paths.get(FEATURES_PATH + File.separator + "NewConfigurations.txt");
            Files.write(path, Arrays.asList("Variant" + count + ": " + config), StandardCharsets.UTF_8,
                    Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
            count++;
        } catch (final IOException ioe) {
            System.out.println("Error appending scenario description in text file: " + ioe);
        }
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


}

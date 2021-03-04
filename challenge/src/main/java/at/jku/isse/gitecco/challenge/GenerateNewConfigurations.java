package at.jku.isse.gitecco.challenge;

import at.jku.isse.gitecco.core.git.GitCommit;
import at.jku.isse.gitecco.core.git.GitCommitList;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.type.Feature;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenerateNewConfigurations {
    static String REPO_PATH = "";//""C:\\Users\\gabil\\Desktop\\PHD\\ChallengePaper\\TestScript\\test2\\libssh";
    static String FEATURES_PATH = "";//"C:\\Users\\gabil\\Desktop\\PHD\\ChallengePaper\\TestScript\\test2\\InputConfigurations.csv";


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

            System.out.println("\u001B[32m" + "Mining process started. It can take minutes or hours...");
            preprocessvariants();
            System.out.println("\u001B[32m" + "Process finished!!");
        }
        //identification();
        //countFeaturesCSV();
    }

    public static void countFeaturesTXT() throws Exception {
        Reader reader = Files.newBufferedReader(Paths.get(FEATURES_PATH));
        CSVReader csvReader = new CSVReaderBuilder(reader).build();
        List<String[]> lines = csvReader.readAll();
        ArrayList<String> features = new ArrayList<>();
        int scenario = 1;
        for (String[] line : lines) {
            int count = 0;
            for (String column : line) {
                if(column.contains(": "))
                    column=column.substring(column.indexOf(" ")+1);
                if (count >= 0) {
                    if (!features.contains(column))
                        features.add(column);
                }
                count++;
            }
            if (scenario <= Integer.valueOf(10) || scenario == Integer.valueOf(100) || scenario == Integer.valueOf(200) || scenario == Integer.valueOf(300)) {
                System.out.println("Scenario: " + scenario + " Number of features: " + features.size());
            }
            scenario++;
        }

    }

    public static void countFeaturesCSV() throws Exception {
        Reader reader = Files.newBufferedReader(Paths.get(FEATURES_PATH));
        CSVReader csvReader = new CSVReaderBuilder(reader).build();
        List<String[]> lines = csvReader.readAll();
        ArrayList<String> features = new ArrayList<>();
        int scenario = 1;
        for (String[] line : lines) {
            int count = 0;
            for (String column : line) {
                if (count >= 3) {
                    if(!features.contains(column))
                    features.add(column);
                }
                count++;
            }
            if(scenario<=Integer.valueOf(10) || scenario==Integer.valueOf(100) || scenario==Integer.valueOf(200) || scenario==Integer.valueOf(300)) {
                System.out.println("Scenario: "+scenario+" Number of features: " + features.size());
            }
            scenario++;
        }
    }

    public static void preprocessvariants() throws Exception {
        String repoPath;
        repoPath = REPO_PATH;

        final GitHelper gitHelper = new GitHelper(repoPath, null);
        final File gitFolder = new File(gitHelper.getPath());
        final File randomVariantFolder = new File(gitFolder.getParent(), "newConfigurations");
        final PreprocessorHelper pph = new PreprocessorHelper();
        Reader reader = Files.newBufferedReader(Paths.get(FEATURES_PATH));
        CSVReader csvReader = new CSVReaderBuilder(reader).build();
        List<String[]> lines = csvReader.readAll();
        String commit = "";
        ArrayList<String> features = new ArrayList<>();
        int variantName = 1;
        for (String[] line : lines) {
            String eccoConfig = "variant"+variantName;
            commit=line[1];
            System.out.println("commit: "+commit);
            int count = 0;
            for (String column : line) {
                if(count>=2) {
                    features.add(column);
                }
                count++;
            }
            Map<Feature, Integer> test = new HashMap<>();
            String config = "";
            for (String mapconfig : features) {
                String f = mapconfig.substring(0,mapconfig.indexOf("."));
                Feature feat = new Feature(f);
                test.put(feat, 1);
                config+=","+mapconfig;
            }
            config=config.replaceFirst(",","");
            System.out.println("------ Variant to generate with config: " + eccoConfig);
            gitHelper.checkOutCommit(commit);
            pph.generateVariants(test, gitFolder, randomVariantFolder, gitHelper.getDirFiles(), eccoConfig);
            File configfile = new File(randomVariantFolder, eccoConfig + ".config");
            BufferedWriter writer = new BufferedWriter(new FileWriter(configfile));
            writer.write(config);
            writer.close();
            System.out.println("Variant generated with config: " + config);
            features = new ArrayList<>();
            variantName++;
        }

        System.out.println("finished analyzing repo");
    }


}

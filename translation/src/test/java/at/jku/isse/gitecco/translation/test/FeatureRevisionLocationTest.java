package at.jku.isse.gitecco.translation.test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import at.jku.isse.gitecco.translation.variantscomparison.util.CompareVariants;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

public class FeatureRevisionLocationTest {
    //directory where you have the folder with the artifacts of the target systyem
    public final String resultsCSVs_path = "C:\\Users\\gabil\\Downloads\\SPLC2020-FeatureRevisionLocation-master\\LibSSH";
    //directory with the folder "variant_results" inside the folder with the artifacts of the target systyem
    public final String resultMetrics_path = "C:\\Users\\gabil\\Downloads\\SPLC2020-FeatureRevisionLocation-master\\LibSSH\\variant_results";
    //directory with the file "configurations.csv" inside the folder with the artifacts of the target systyem
    public final String configuration_path = "C:\\Users\\gabil\\Downloads\\SPLC2020-FeatureRevisionLocation-master\\LibSSH\\configurations.csv";
    //directory where you have the folder with the artifacts of Marlin target systyem
    public final String marlinFolder = "C:\\Users\\gabil\\Downloads\\SPLC2020-FeatureRevisionLocation-master\\Marlin";
    //directory where you have the folder with the artifacts of LibSSH target systyem
    public final String libsshFolder = "C:\\Users\\gabil\\Downloads\\SPLC2020-FeatureRevisionLocation-master\\LibSSH";
    //directory where you have the folder with the artifacts of SQLite target systyem
    public final String sqliteFolder = "C:\\Users\\gabil\\Downloads\\SPLC2020-FeatureRevisionLocation-master\\SQLite";
    //directory where you want to store the result file containing the metrics computed for all target systems
    public final String metricsResultFolder = "C:\\Users\\gabil\\Downloads\\SPLC2020-FeatureRevisionLocation-master";


    //feature revision location where the traces are computed by the input containing a set of feature revisions (configuration) and its artifacts (variant source code)
    @org.testng.annotations.Test
    public void TestEccoCommit() throws IOException {
        CompareVariants cV = new CompareVariants();
        ArrayList<String> configsToCommit = new ArrayList<>();
        File configuration = new File(configuration_path);
        Path OUTPUT_DIR = Paths.get(resultMetrics_path);
        File eccoFolder = new File(resultsCSVs_path+File.separator+"Input_variants"+File.separator);
        BufferedReader csvReader = null;
        try {
            csvReader = new BufferedReader(new FileReader(configuration));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String row = null;
        while ((row = csvReader.readLine()) != null) {
            if (!(row.equals("")) && !(row.contains("CommitNumber"))) {
                String[] data = row.split(",");
                String conf = "";
                for (int i = 2; i < data.length; i++) {
                    if (i < data.length - 1)
                        conf += data[i] + ",";
                    else
                        conf += data[i];
                }
                configsToCommit.add(conf);
            }
        }

        csvReader.close();

        cV.eccoCommit(eccoFolder, OUTPUT_DIR, configsToCommit);
    }


    //checkout each set of feature revisions (configuration) and its artifacts (variant source code) by the traces located before by the feature revision location (TestEccoCommit)
    @org.testng.annotations.Test
    public void TestEccoCheckout() throws IOException {
        CompareVariants cV = new CompareVariants();
        ArrayList<String> configsToCheckout = new ArrayList<>();
        File configuration = new File(configuration_path);
        Path OUTPUT_DIR = Paths.get(resultMetrics_path+File.separator);
        File eccoFolder = new File(resultsCSVs_path+File.separator+"Input_variants"+File.separator);
        File checkoutFolder = new File(resultMetrics_path+File.separator+"checkout"+File.separator);
        BufferedReader csvReader = null;
        try {
            csvReader = new BufferedReader(new FileReader(configuration));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String row = null;
        while ((row = csvReader.readLine()) != null) {
            if (!(row.equals("")) && !(row.contains("CommitNumber"))) {
                String[] data = row.split(",");
                String conf = "";
                for (int i = 2; i < data.length; i++) {
                    if (i < data.length - 1)
                        conf += data[i] + ",";
                    else
                        conf += data[i];
                }
                configsToCheckout.add(conf);
            }
        }

        csvReader.close();

        cV.eccoCheckout(configsToCheckout, OUTPUT_DIR, eccoFolder, checkoutFolder);
    }


    //compare the ground truth variants with the composed variants (containing the artifacts mapped according to the feature revision location)
    @org.testng.annotations.Test
    public void TestCompareVariants() {
        CompareVariants cV = new CompareVariants();
        //"input_variants" folder contains the ground truth variants and "checkout" folder contains the composed variants
        File variantsrc = new File(resultsCSVs_path, "Input_variants");
        File checkoutfile = new File(resultMetrics_path, "checkout");
        try {
            for (File path : variantsrc.listFiles()) {
                    cV.compareVariant(path, new File(checkoutfile + File.separator + path.getName()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //get the metrics of each and for all the target projects together.
    //To compute the metrics of variants this is considering all the files match and to compute files metrics this is considering all the lines match
    @org.testng.annotations.Test
    public void GetCSVInformationTotalTest() throws IOException {
        //set into this list of File the folders with csv files resulted from the comparison of variants of each target project
        File[] folder = {new File(marlinFolder),
                new File(libsshFolder),
                new File(sqliteFolder)};
        //write metrics in a csv file
        String filemetrics = "RandomMetricsEachAndTogether-no-inserted-lines.csv";
        FileWriter csvWriter = new FileWriter( metricsResultFolder + File.separator + filemetrics);

        Float totalmeanRunEccoCommit = Float.valueOf(0), totalmeanRunEccoCheckout = Float.valueOf(0), totalmeanRunPPCheckoutCleanVersion = Float.valueOf(0), totalmeanRunPPCheckoutGenerateVariant = Float.valueOf(0), totalmeanRunGitCommit = Float.valueOf(0), totalmeanRunGitCheckout = Float.valueOf(0);
        Float totaltotalnumberFiles = Float.valueOf(0), totalmatchesFiles = Float.valueOf(0), totaleccototalLines = Float.valueOf(0), totaloriginaltotalLines = Float.valueOf(0), totalmissingFiles = Float.valueOf(0), totalremainingFiles = Float.valueOf(0), totaltotalVariantsMatch = Float.valueOf(0), totaltruepositiveLines = Float.valueOf(0), totalfalsepositiveLines = Float.valueOf(0), totalfalsenegativeLines = Float.valueOf(0),
                totaltruepositiveLinesEachFile = Float.valueOf(0), totalfalsepositiveLinesEachFile = Float.valueOf(0), totalfalsenegativeLinesEachFile = Float.valueOf(0), totalnumberTotalFilesEachVariant = Float.valueOf(0), totalmatchFilesEachVariant = Float.valueOf(0), totaleccototalLinesEachFile = Float.valueOf(0), totaloriginaltotalLinesEachFile = Float.valueOf(0),  totalnumberCSV = Float.valueOf(0);
        for (int j = 0; j < folder.length; j++) {
            File[] lista = folder[j].listFiles();
            Float meanRunEccoCommit = Float.valueOf(0), meanRunEccoCheckout = Float.valueOf(0), meanRunPPCheckoutCleanVersion = Float.valueOf(0), meanRunPPCheckoutGenerateVariant = Float.valueOf(0), meanRunGitCommit = Float.valueOf(0), meanRunGitCheckout = Float.valueOf(0);
            Float totalnumberFiles = Float.valueOf(0), matchesFiles = Float.valueOf(0), eccototalLines = Float.valueOf(0), originaltotalLines = Float.valueOf(0), missingFiles = Float.valueOf(0), remainingFiles = Float.valueOf(0), totalVariantsMatch = Float.valueOf(0), truepositiveLines = Float.valueOf(0), falsepositiveLines = Float.valueOf(0), falsenegativeLines = Float.valueOf(0),
                    truepositiveLinesEachFile = Float.valueOf(0), falsepositiveLinesEachFile = Float.valueOf(0), falsenegativeLinesEachFile = Float.valueOf(0), numberTotalFilesEachVariant = Float.valueOf(0), matchFilesEachVariant = Float.valueOf(0), eccototalLinesEachFile = Float.valueOf(0), originaltotalLinesEachFile = Float.valueOf(0);
            Boolean variantMatch = true;
            Float numberCSV = Float.valueOf(0);
            for (File file : lista) {
                if ((file.getName().indexOf(".csv") != -1) && !(file.getName().contains("features_report_each_project_commit")) && !(file.getName().contains("configurations"))) {
                    Reader reader = Files.newBufferedReader(Paths.get(file.getAbsolutePath()));
                    CSVReader csvReader = new CSVReaderBuilder(reader).build();
                    List<String[]> matchesVariants = csvReader.readAll();

                    if (file.getName().contains("runtime")) {
                        for (int i = 0; i < matchesVariants.size(); i++) {
                            if (i != 0) {
                                String[] runtimes = matchesVariants.get(i);
                                meanRunEccoCommit += Float.valueOf(runtimes[2]);
                            }
                        }
                        totalmeanRunEccoCommit+=meanRunEccoCommit; totalmeanRunEccoCheckout+=meanRunEccoCheckout; totalmeanRunGitCommit+=meanRunGitCommit; totalmeanRunGitCheckout+=meanRunGitCheckout;
                        totalmeanRunPPCheckoutCleanVersion+=meanRunPPCheckoutCleanVersion; totalmeanRunPPCheckoutGenerateVariant+=meanRunPPCheckoutGenerateVariant;
                    } else {
                        for (int i = 1; i < matchesVariants.size(); i++) {

                            String[] line = matchesVariants.get(i);
                            truepositiveLines += Integer.valueOf(line[2]);
                            falsepositiveLines += Integer.valueOf(line[3]);
                            falsenegativeLines += Integer.valueOf(line[4]);
                            truepositiveLinesEachFile = Float.valueOf(Integer.valueOf(line[2]));
                            falsepositiveLinesEachFile = Float.valueOf(Integer.valueOf(line[3]));
                            falsenegativeLinesEachFile = Float.valueOf(Integer.valueOf(line[4]));
                            originaltotalLines += Integer.valueOf(line[5]);
                            eccototalLines += Integer.valueOf(line[6]);
                            originaltotalLinesEachFile = Float.valueOf(Integer.valueOf(line[5]));
                            eccototalLinesEachFile = Float.valueOf(Integer.valueOf(line[6]));

                            totaltruepositiveLines+=truepositiveLines;
                            totalfalsepositiveLines+=falsepositiveLines;
                            totalfalsenegativeLines+=falsenegativeLines;
                            totaltruepositiveLinesEachFile+=totaltruepositiveLinesEachFile;
                            totalfalsepositiveLinesEachFile+=falsepositiveLinesEachFile;
                            totalfalsenegativeLinesEachFile+=falsenegativeLinesEachFile;
                            totaloriginaltotalLines+=originaltotalLines;
                            totaleccototalLines+=eccototalLines;
                            totaloriginaltotalLinesEachFile+=originaltotalLinesEachFile;
                            totaleccototalLinesEachFile+=eccototalLinesEachFile;

                            if (line[1].equals("true")) {
                                if (Float.compare(originaltotalLinesEachFile, eccototalLinesEachFile) == 0 && Float.compare(truepositiveLinesEachFile, originaltotalLinesEachFile) == 0) {
                                    matchFilesEachVariant++;
                                    totalmatchFilesEachVariant++;
                                    matchesFiles++;
                                    totalmatchesFiles++;
                                } else {
                                    missingFiles++;
                                    totalmissingFiles++;
                                    variantMatch = false;
                                }
                                numberTotalFilesEachVariant += 1;
                                totalnumberTotalFilesEachVariant++;
                            } else if (line[1].equals("not")) {
                                variantMatch = false;
                                missingFiles++;
                                totalmissingFiles++;
                                numberTotalFilesEachVariant += 1;
                                totalnumberTotalFilesEachVariant++;
                            } else if (line[1].equals("justOnRetrieved")) {
                                variantMatch = false;
                                remainingFiles++;
                                totalremainingFiles++;
                            } else {
                                variantMatch = false;
                                missingFiles++;
                                totalmissingFiles++;
                            }
                        }
                    }
                    numberCSV++;
                    totalnumberCSV++;
                    if (variantMatch && Float.compare(matchFilesEachVariant, numberTotalFilesEachVariant) == 0) {
                        totalVariantsMatch++;
                        totaltotalVariantsMatch++;
                    }
                }
                numberTotalFilesEachVariant = Float.valueOf(0);
                matchFilesEachVariant = Float.valueOf(0);
                variantMatch = true;
            }
            meanRunEccoCommit = (meanRunEccoCommit / numberCSV) / 1000;
            meanRunEccoCheckout = (meanRunEccoCheckout / numberCSV) / 1000;
            meanRunGitCommit = (meanRunGitCommit / numberCSV) / 1000;
            meanRunGitCheckout = (meanRunGitCheckout / numberCSV) / 1000;
            Float totalVariantsNotMatch = (numberCSV - totalVariantsMatch);
            Float precisionVariants = totalVariantsMatch / (totalVariantsMatch + totalVariantsNotMatch);
            Float recallVariants = totalVariantsMatch / (numberCSV);
            Float f1scoreVariants = 2 * ((precisionVariants * recallVariants) / (precisionVariants + recallVariants));
            Float precisionLines = Float.valueOf(truepositiveLines / (truepositiveLines + falsepositiveLines));
            Float recallLines = Float.valueOf(truepositiveLines / (truepositiveLines + falsenegativeLines));
            Float f1scorelines = 2 * ((precisionLines * recallLines) / (precisionLines + recallLines));
            Float precisionFiles = Float.valueOf(matchesFiles / (matchesFiles + remainingFiles));
            Float recallFiles = Float.valueOf(matchesFiles / (matchesFiles + missingFiles));
            Float f1scoreFiles = 2 * ((precisionFiles * recallFiles) / (precisionFiles + recallFiles));


            //csv to report new features and features changed per git commit of the project
            try {
                List<List<String>> headerRows = Arrays.asList(Arrays.asList(folder[j].getName()),
                        Arrays.asList("PrecisionFiles", "RecallFiles", "F1ScoreFiles", "PrecisionLines", "RecalLines", "F1ScoreLines"),
                        Arrays.asList(precisionFiles.toString(), recallFiles.toString(), f1scoreFiles.toString(), precisionLines.toString(), recallLines.toString(), f1scorelines.toString()),
                        Arrays.asList("MeanRuntime"),
                        Arrays.asList(meanRunEccoCommit.toString(), meanRunEccoCheckout.toString(), meanRunGitCommit.toString(), meanRunGitCheckout.toString())
                );
                for (List<String> rowData : headerRows) {
                    csvWriter.append(String.join(",", rowData));
                    csvWriter.append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        totalmeanRunEccoCommit = (totalmeanRunEccoCommit / totalnumberCSV) / 1000;
        Float totalVariantsNotMatch = (totalnumberCSV - totaltotalVariantsMatch);
        Float precisionVariants = totaltotalVariantsMatch / (totaltotalVariantsMatch + totalVariantsNotMatch);
        Float recallVariants = totaltotalVariantsMatch / (totalnumberCSV);
        Float precisionLines = Float.valueOf(totaltruepositiveLines / (totaltruepositiveLines + totalfalsepositiveLines));
        Float recallLines = Float.valueOf(totaltruepositiveLines / (totaltruepositiveLines + totalfalsenegativeLines));
        Float f1scorelines = 2 * ((precisionLines * recallLines) / (precisionLines + recallLines));
        Float precisionFiles = Float.valueOf(totalmatchesFiles / (totalmatchesFiles + totalremainingFiles));
        Float recallFiles = Float.valueOf(totalmatchesFiles / (totalmatchesFiles + totalmissingFiles));
        Float f1scoreFiles = 2 * ((precisionFiles * recallFiles) / (precisionFiles + recallFiles));
        List<List<String>> totalRows = Arrays.asList(Arrays.asList("Three projects together"),
                Arrays.asList("TotalPrecisionFiles", "TotalRecallFiles", "TotalF1ScoreFiles", "TotalPrecisionLines", "TotalRecalLines", "TotalF1ScoreLines"),
                Arrays.asList(precisionFiles.toString(), recallFiles.toString(), f1scoreFiles.toString(), precisionLines.toString(), recallLines.toString(), f1scorelines.toString()),
                Arrays.asList("MeanRuntime"),
                Arrays.asList(totalmeanRunEccoCommit.toString())
        );
        for (List<String> rowData : totalRows) {
            csvWriter.append(String.join(",", rowData));
            csvWriter.append("\n");
        }
        csvWriter.flush();
        csvWriter.close();
    }

}

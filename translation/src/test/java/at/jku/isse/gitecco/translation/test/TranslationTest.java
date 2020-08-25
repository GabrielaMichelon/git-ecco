package at.jku.isse.gitecco.translation.test;


import at.jku.isse.gitecco.core.git.GitCommitList;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.solver.ExpressionSolver;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.core.type.FeatureImplication;
import at.jku.isse.gitecco.translation.variantscomparison.util.CompareVariants;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.anarres.cpp.featureExpr.FeatureExpressionParser;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.chocosolver.solver.constraints.nary.cnf.LogOp.*;

public class TranslationTest {

    public final String repo_path = "C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\tests-11-02-2020\\SQLite\\sqlite";
    public final String resultsCSVs_path = "C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\tests-11-02-2020\\RandomMarlin";
    public final String resultMetrics_path = "C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\tests-11-02-2020\\RandomMarlin\\variant_results";
    public final String configuration_path = "C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\tests-11-02-2020\\Marlin\\configurations.csv";
    public final String configurationRandomVariants_path = "C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\tests-11-02-2020\\Marlin\\randomconfigurations.csv";
    //git checkout $(git log --branches -1 --pretty=format:"%H")


    //get the metrics of each and for all the target projects together.
    //To compute the metrics of variants this is considering all the files match and to compute files metrics this is considering all the lines match
    @Test
    public void getCSVInformationTotal() throws IOException {
        //set into this list of File the folders with csv files resulted from the comparison of variants of each target project
        /*File[] folder = {new File("C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\bug-fixed\\Marlin50"),
                new File("C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\bug-fixed\\LibSSH50"),
                new File("C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\tests-new-code\\SQLite\\SQLite")};*/
        File[] folder = {new File("C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\tests-11-02-2020\\RandomMarlin"),
                new File("C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\tests-11-02-2020\\RandomLibssh"),
                new File("C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\tests-11-02-2020\\RandomSQLite")};
        //write metrics in a csv file
        String filemetrics = "RandomMetricsEachAndTogether-no-inserted-lines.csv";
        //FileWriter csvWriter = new FileWriter("C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\tests-new-code\\Libssh-deletedfiles-corrected\\results_metrics" + File.separator + filemetrics);
        FileWriter csvWriter = new FileWriter("C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\tests-11-02-2020\\results_metrics" + File.separator + filemetrics);

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
                                meanRunEccoCheckout += Float.valueOf(runtimes[3]);
                                meanRunPPCheckoutCleanVersion += Float.valueOf(runtimes[4]);
                                meanRunPPCheckoutGenerateVariant += Float.valueOf(runtimes[5]);
                                meanRunGitCommit += Float.valueOf(runtimes[6]);
                                meanRunGitCheckout += Float.valueOf(runtimes[7]);
                            }
                        }
                        totalmeanRunEccoCommit+=meanRunEccoCommit; totalmeanRunEccoCheckout+=meanRunEccoCheckout; totalmeanRunGitCommit+=meanRunGitCommit; totalmeanRunGitCheckout+=meanRunGitCheckout;
                        totalmeanRunPPCheckoutCleanVersion+=meanRunPPCheckoutCleanVersion; totalmeanRunPPCheckoutGenerateVariant+=meanRunPPCheckoutGenerateVariant;
                    } else {
                        Float qtdLines = Float.valueOf(matchesVariants.size() - 4);
                        //totalnumberFiles += qtdLines;
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
            meanRunPPCheckoutCleanVersion = (meanRunPPCheckoutCleanVersion / numberCSV) / 1000;
            meanRunPPCheckoutGenerateVariant = (meanRunPPCheckoutGenerateVariant / numberCSV) / 1000;
            meanRunGitCommit = (meanRunGitCommit / numberCSV) / 1000;
            meanRunGitCheckout = (meanRunGitCheckout / numberCSV) / 1000;
            Float totalVariantsNotMatch = (numberCSV - totalVariantsMatch);
            Float precisionVariants = totalVariantsMatch / (totalVariantsMatch + totalVariantsNotMatch);
            Float recallVariants = totalVariantsMatch / (numberCSV);
            Float f1scoreVariants = 2 * ((precisionVariants * recallVariants) / (precisionVariants + recallVariants));
            if (f1scoreVariants.toString().equals("NaN")) {
                f1scoreVariants = Float.valueOf(0);
            }
            Float precisionLines = Float.valueOf(truepositiveLines / (truepositiveLines + falsepositiveLines));
            Float recallLines = Float.valueOf(truepositiveLines / (truepositiveLines + falsenegativeLines));
            Float f1scorelines = 2 * ((precisionLines * recallLines) / (precisionLines + recallLines));
            Float precisionFiles = Float.valueOf(matchesFiles / (matchesFiles + remainingFiles));
            Float recallFiles = Float.valueOf(matchesFiles / (matchesFiles + missingFiles));
            Float f1scoreFiles = 2 * ((precisionFiles * recallFiles) / (precisionFiles + recallFiles));


            //csv to report new features and features changed per git commit of the project
            try {
                 List<List<String>> headerRows = Arrays.asList(Arrays.asList(folder[j].getName()),
                        Arrays.asList("PrecisionVariant", "RecallVariant", "F1ScoreVariant", "PrecisionFiles", "RecallFiles", "F1ScoreFiles", "PrecisionLines", "RecalLines", "F1ScoreLines"),
                        Arrays.asList(precisionVariants.toString(), recallVariants.toString(), f1scoreVariants.toString(), precisionFiles.toString(), recallFiles.toString(), f1scoreFiles.toString(), precisionLines.toString(), recallLines.toString(), f1scorelines.toString()),
                        Arrays.asList("MeanRuntimeEccoCommit", "MeanRuntimeEccoCheckout", "MeanRuntimeGitCommit", "MeanRuntimeGitCheckout"),
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
        totalmeanRunEccoCheckout = (totalmeanRunEccoCheckout / totalnumberCSV) / 1000;
        totalmeanRunPPCheckoutCleanVersion = (totalmeanRunPPCheckoutCleanVersion / totalnumberCSV) / 1000;
        totalmeanRunPPCheckoutGenerateVariant = (totalmeanRunPPCheckoutGenerateVariant / totalnumberCSV) / 1000;
        totalmeanRunGitCommit = (totalmeanRunGitCommit / totalnumberCSV) / 1000;
        totalmeanRunGitCheckout = (totalmeanRunGitCheckout / totalnumberCSV) / 1000;
        Float totalVariantsNotMatch = (totalnumberCSV - totaltotalVariantsMatch);
        Float precisionVariants = totaltotalVariantsMatch / (totaltotalVariantsMatch + totalVariantsNotMatch);
        Float recallVariants = totaltotalVariantsMatch / (totalnumberCSV);
        Float f1scoreVariants = 2 * ((precisionVariants * recallVariants) / (precisionVariants + recallVariants));
        if (f1scoreVariants.toString().equals("NaN")) {
            f1scoreVariants = Float.valueOf(0);
        }
        Float precisionLines = Float.valueOf(totaltruepositiveLines / (totaltruepositiveLines + totalfalsepositiveLines));
        Float recallLines = Float.valueOf(totaltruepositiveLines / (totaltruepositiveLines + totalfalsenegativeLines));
        Float f1scorelines = 2 * ((precisionLines * recallLines) / (precisionLines + recallLines));
        Float precisionFiles = Float.valueOf(totalmatchesFiles / (totalmatchesFiles + totalremainingFiles));
        Float recallFiles = Float.valueOf(totalmatchesFiles / (totalmatchesFiles + totalmissingFiles));
        Float f1scoreFiles = 2 * ((precisionFiles * recallFiles) / (precisionFiles + recallFiles));
        List<List<String>> totalRows = Arrays.asList(Arrays.asList("Three projects together"),
                Arrays.asList("TotalPrecisionVariant", "TotalRecallVariant", "TotalF1ScoreVariant", "TotalPrecisionFiles", "TotalRecallFiles", "TotalF1ScoreFiles", "TotalPrecisionLines", "TotalRecalLines", "TotalF1ScoreLines"),
                Arrays.asList(precisionVariants.toString(), recallVariants.toString(), f1scoreVariants.toString(), precisionFiles.toString(), recallFiles.toString(), f1scoreFiles.toString(), precisionLines.toString(), recallLines.toString(), f1scorelines.toString()),
                Arrays.asList("TotalMeanRuntimeEccoCommit", "TotalMeanRuntimeEccoCheckout", "TotalMeanRuntimeGitCommit", "TotalMeanRuntimeGitCheckout"),
                Arrays.asList(totalmeanRunEccoCommit.toString(), totalmeanRunEccoCheckout.toString(), totalmeanRunGitCommit.toString(), totalmeanRunGitCheckout.toString())
        );
        for (List<String> rowData : totalRows) {
            csvWriter.append(String.join(",", rowData));
            csvWriter.append("\n");
        }
        csvWriter.flush();
        csvWriter.close();
    }


    //to compute the metrics of variants this is considering all the files match and to compute files metrics this is considering all the lines match
    @Test
    public void getCSVInformation2() throws IOException {
        File folder = new File(resultsCSVs_path);
        File[] lista = folder.listFiles();
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
                            meanRunEccoCheckout += Float.valueOf(runtimes[3]);
                            meanRunPPCheckoutCleanVersion += Float.valueOf(runtimes[4]);
                            meanRunPPCheckoutGenerateVariant += Float.valueOf(runtimes[5]);
                            meanRunGitCommit += Float.valueOf(runtimes[6]);
                            meanRunGitCheckout += Float.valueOf(runtimes[7]);
                        }
                    }
                } else {
                    Float qtdLines = Float.valueOf(matchesVariants.size() - 4);
                    //totalnumberFiles += qtdLines;
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
                        if (line[1].equals("true")) {
                            if (Float.compare(originaltotalLinesEachFile, eccototalLinesEachFile) == 0 && Float.compare(truepositiveLinesEachFile, originaltotalLinesEachFile) == 0) {
                                matchFilesEachVariant++;
                                matchesFiles++;
                            } else {
                                missingFiles++;
                                variantMatch = false;
                            }
                            numberTotalFilesEachVariant += 1;
                        } else if (line[1].equals("not")) {
                            variantMatch = false;
                            missingFiles++;
                            numberTotalFilesEachVariant += 1;
                        } else if (line[1].equals("justOnRetrieved")) {
                            variantMatch = false;
                            remainingFiles++;
                        } else {
                            variantMatch = false;
                            missingFiles++;
                        }
                    }
                }
                numberCSV++;
                if (variantMatch && Float.compare(matchFilesEachVariant, numberTotalFilesEachVariant) == 0)
                    totalVariantsMatch++;
            }
            numberTotalFilesEachVariant = Float.valueOf(0);
            matchFilesEachVariant = Float.valueOf(0);
            variantMatch = true;
        }
        meanRunEccoCommit = (meanRunEccoCommit / numberCSV) / 1000;
        meanRunEccoCheckout = (meanRunEccoCheckout / numberCSV) / 1000;
        meanRunPPCheckoutCleanVersion = (meanRunPPCheckoutCleanVersion / numberCSV) / 1000;
        meanRunPPCheckoutGenerateVariant = (meanRunPPCheckoutGenerateVariant / numberCSV) / 1000;
        meanRunGitCommit = (meanRunGitCommit / numberCSV) / 1000;
        meanRunGitCheckout = (meanRunGitCheckout / numberCSV) / 1000;
        Float totalVariantsNotMatch = (numberCSV - totalVariantsMatch);
        Float precisionVariants = totalVariantsMatch / (totalVariantsMatch + totalVariantsNotMatch);
        Float recallVariants = totalVariantsMatch / (numberCSV);
        Float f1scoreVariants = 2 * ((precisionVariants * recallVariants) / (precisionVariants + recallVariants));
        if (f1scoreVariants.toString().equals("NaN")) {
            f1scoreVariants = Float.valueOf(0);
        }
        Float precisionLines = Float.valueOf(truepositiveLines / (truepositiveLines + falsepositiveLines));
        Float recallLines = Float.valueOf(truepositiveLines / (truepositiveLines + falsenegativeLines));
        Float f1scorelines = 2 * ((precisionLines * recallLines) / (precisionLines + recallLines));
        Float precisionFiles = Float.valueOf(matchesFiles / (matchesFiles + remainingFiles));
        Float recallFiles = Float.valueOf(matchesFiles / (matchesFiles + missingFiles));
        Float f1scoreFiles = 2 * ((precisionFiles * recallFiles) / (precisionFiles + recallFiles));

        //write metrics in a csv file
        String filemetrics = "metrics_justdeleted.csv";
        //csv to report new features and features changed per git commit of the project
        try {
            FileWriter csvWriter = new FileWriter(resultMetrics_path + File.separator + filemetrics);
            List<List<String>> headerRows = Arrays.asList(
                    Arrays.asList("PrecisionVariant", "RecallVariant", "F1ScoreVariant", "PrecisionFiles", "RecallFiles", "F1ScoreFiles", "PrecisionLines", "RecalLines", "F1ScoreLines"),
                    Arrays.asList(precisionVariants.toString(), recallVariants.toString(), f1scoreVariants.toString(), precisionFiles.toString(), recallFiles.toString(), f1scoreFiles.toString(), precisionLines.toString(), recallLines.toString(), f1scorelines.toString()),
                    Arrays.asList("MeanRuntimeEccoCommit", "MeanRuntimeEccoCheckout", "MeanRuntimeGitCommit", "MeanRuntimeGitCheckout"),
                    Arrays.asList(meanRunEccoCommit.toString(), meanRunEccoCheckout.toString(), meanRunGitCommit.toString(), meanRunGitCheckout.toString())
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
        System.out.println(eccototalLines);
        System.out.println("Total lines inserted: "+falsepositiveLines+ "\nTotal lines deleted: "+falsenegativeLines);
    }

    //to compute the metrics of variants this is not considering all the files match (just if the file exists) and to compute files metrics this is not considering all the lines match (just if the file exists)
    @Test
    public void getCSVInformation() throws IOException {
        File folder = new File(resultsCSVs_path);
        File[] lista = folder.listFiles();
        Float meanRunEccoCommit = Float.valueOf(0), meanRunEccoCheckout = Float.valueOf(0), meanRunPPCheckoutCleanVersion = Float.valueOf(0), meanRunPPCheckoutGenerateVariant = Float.valueOf(0), meanRunGitCommit = Float.valueOf(0), meanRunGitCheckout = Float.valueOf(0);
        Float totalnumberFiles = Float.valueOf(0), matchesFiles = Float.valueOf(0), eccototalLines = Float.valueOf(0), originaltotalLines = Float.valueOf(0), missingFiles = Float.valueOf(0), remainingFiles = Float.valueOf(0), totalVariantsMatch = Float.valueOf(0), truepositiveLines = Float.valueOf(0), falsepositiveLines = Float.valueOf(0), falsenegativeLines = Float.valueOf(0);
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
                            meanRunEccoCheckout += Float.valueOf(runtimes[3]);
                            meanRunPPCheckoutCleanVersion += Float.valueOf(runtimes[4]);
                            meanRunPPCheckoutGenerateVariant += Float.valueOf(runtimes[5]);
                            meanRunGitCommit += Float.valueOf(runtimes[6]);
                            meanRunGitCheckout += Float.valueOf(runtimes[7]);
                        }
                    }
                } else {
                    Float qtdLines = Float.valueOf(matchesVariants.size() - 4);
                    //totalnumberFiles += qtdLines;
                    for (int i = 1; i < matchesVariants.size(); i++) {
                        String[] line = matchesVariants.get(i);
                        truepositiveLines += Integer.valueOf(line[2]);
                        falsepositiveLines += Integer.valueOf(line[3]);
                        falsenegativeLines += Integer.valueOf(line[4]);
                        originaltotalLines += Integer.valueOf(line[5]);
                        eccototalLines += Integer.valueOf(line[6]);
                        if (line[1].equals("true")) {
                            variantMatch = true;
                            matchesFiles++;
                        } else if (line[1].equals("not")) {
                            variantMatch = false;
                            missingFiles++;
                        } else if (line[1].equals("justOnRetrieved")) {
                            variantMatch = false;
                            remainingFiles++;
                        } else {
                            variantMatch = false;
                            missingFiles++;
                        }
                    }
                }
                numberCSV++;
                if (variantMatch)
                    totalVariantsMatch++;
            }
        }
        meanRunEccoCommit = (meanRunEccoCommit / numberCSV) / 1000;
        meanRunEccoCheckout = (meanRunEccoCheckout / numberCSV) / 1000;
        meanRunPPCheckoutCleanVersion = (meanRunPPCheckoutCleanVersion / numberCSV) / 1000;
        meanRunPPCheckoutGenerateVariant = (meanRunPPCheckoutGenerateVariant / numberCSV) / 1000;
        meanRunGitCommit = (meanRunGitCommit / numberCSV) / 1000;
        meanRunGitCheckout = (meanRunGitCheckout / numberCSV) / 1000;
        Float totalVariantsNotMatch = (numberCSV - totalVariantsMatch);
        Float precisionVariants = totalVariantsMatch / (totalVariantsMatch + totalVariantsNotMatch);
        Float recallVariants = totalVariantsMatch / (numberCSV);
        Float f1scoreVariants = 2 * ((precisionVariants * recallVariants) / (precisionVariants + recallVariants));
        if (f1scoreVariants.toString().equals("NaN")) {
            f1scoreVariants = Float.valueOf(0);
        }
        Float precisionLines = Float.valueOf(truepositiveLines / (truepositiveLines + falsepositiveLines));
        Float recallLines = Float.valueOf(truepositiveLines / (truepositiveLines + falsenegativeLines));
        Float f1scorelines = 2 * ((precisionLines * recallLines) / (precisionLines + recallLines));
        Float precisionFiles = Float.valueOf(matchesFiles / (matchesFiles + remainingFiles));
        Float recallFiles = Float.valueOf(matchesFiles / (matchesFiles + missingFiles));
        Float f1scoreFiles = 2 * ((precisionFiles * recallFiles) / (precisionFiles + recallFiles));

        //write metrics in a csv file
        String filemetrics = "metrics.csv";
        //csv to report new features and features changed per git commit of the project
        try {
            FileWriter csvWriter = new FileWriter(resultMetrics_path + File.separator + filemetrics);
            List<List<String>> headerRows = Arrays.asList(
                    Arrays.asList("PrecisionVariant", "RecallVariant", "F1ScoreVariant", "PrecisionFiles", "RecallFiles", "F1ScoreFiles", "PrecisionLines", "RecalLines", "F1ScoreLines"),
                    Arrays.asList(precisionVariants.toString(), recallVariants.toString(), f1scoreVariants.toString(), precisionFiles.toString(), recallFiles.toString(), f1scoreFiles.toString(), precisionLines.toString(), recallLines.toString(), f1scorelines.toString()),
                    Arrays.asList("MeanRuntimeEccoCommit", "MeanRuntimeEccoCheckout", "MeanRuntimeGitCommit", "MeanRuntimeGitCheckout"),
                    Arrays.asList(meanRunEccoCommit.toString(), meanRunEccoCheckout.toString(), meanRunGitCommit.toString(), meanRunGitCheckout.toString())
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

    //compare the variant from PP (original) with the variant from Ecco (retrieved by feature revision location)
    @Test
    public void testeCompareVariants() {
        CompareVariants cV = new CompareVariants();
        //"ecco" if the folder is from the PP generated variants or "randomVariants" if the folder is from the PP generated random variants
        File variantsrc = new File("C:\\Users\\gabil\\Desktop\\test", "ecco");
        File checkoutfile = new File("C:\\Users\\gabil\\Desktop\\test", "checkout");
        try {
            for (File path : variantsrc.listFiles()) {
                cV.compareVariant(path, new File(checkoutfile + File.separator + path.getName()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //checkout each combination of feature revisions (configuration) and its artifacts (variant code) from ecco - generation of variant by the stored traces made before (ecco commit)
    @Test
    public void testCheckoutEcco() throws IOException {
        CompareVariants cV = new CompareVariants();
        ArrayList<String> configsToCheckout = new ArrayList<>();
        //File configuration = new File(configuration_path);
        File configuration = new File(configurationRandomVariants_path);
        Path OUTPUT_DIR = Paths.get(resultMetrics_path+File.separator);
        //File eccoFolder = new File(resultsCSVs_path+File.separator+"ecco"+File.separator);
        File eccoFolder = new File(resultsCSVs_path+File.separator+"randomVariants"+File.separator);
        File checkoutFolder = new File(resultMetrics_path+File.separator+"checkout"+File.separator);
        BufferedReader csvReader = null;
        try {
            csvReader = new BufferedReader(new FileReader(configuration));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String row = null;
        ArrayList<String> listHeader = new ArrayList<>();
        ArrayList<String> listRuntimeData = new ArrayList<>();
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

    //commit the traces of each combination of feature revisions (configuration) and its artifacts (variant code) in ecco - feature revision location
    @Test
    public void testEccoCommit() throws IOException {
        CompareVariants cV = new CompareVariants();
        ArrayList<String> configsToCommit = new ArrayList<>();
        File configuration = new File(configurationRandomVariants_path);
        Path OUTPUT_DIR = Paths.get(resultMetrics_path);
        File eccoFolder = new File(resultsCSVs_path+File.separator+"ecco"+File.separator);
        BufferedReader csvReader = null;
        try {
            csvReader = new BufferedReader(new FileReader(configuration));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String row = null;
        ArrayList<String> listHeader = new ArrayList<>();
        ArrayList<String> listRuntimeData = new ArrayList<>();
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


    //to have the estimated runtime of Git to commit a variant (what in this case is the time to commit the changes from one Git commit to another)
    @Test
    public void testGitCommit() throws IOException {
        CompareVariants cV = new CompareVariants();
        Map<String, String> configsToCommit = new HashMap<>();
        File configuration = new File(configuration_path);
        Path OUTPUT_DIR = Paths.get(resultMetrics_path+File.separator+"gitCommit");
        File srcFolder = new File(repo_path);
        BufferedReader csvReader = null;
        try {
            csvReader = new BufferedReader(new FileReader(configuration));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String row = null;
        ArrayList<String> listHeader = new ArrayList<>();
        ArrayList<String> listRuntimeData = new ArrayList<>();
        while ((row = csvReader.readLine()) != null) {
            if (!(row.equals("")) && !(row.contains("CommitNumber"))) {
                String[] data = row.split(",");
                String conf = "";
                String commitName = "";
                for (int i = 2; i < data.length; i++) {
                    if (i < data.length - 1)
                        conf += data[i] + ",";
                    else
                        conf += data[i];
                    commitName = data[1];
                }
                configsToCommit.put(conf, commitName);
            }
        }

        csvReader.close();
        //cV.gitCommit(srcFolder, OUTPUT_DIR, configsToCommit);
    }


    @Test
    public void JGitCommitAndCheckout() throws IOException {
        GitHelper gh = new GitHelper();
        try {
            String commitMessage = "test";
            gh.gitCommitAndCheckout("C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\tests-new-code\\SQLite\\SQLite\\sqlite", "C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\sqllite\\testeCommitJGit", "87feadeefec31ee4946663697432661cbc9fd186", commitMessage);
            System.out.println("time git commit" + gh.getRuntimeGitCommit());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void readCSV() throws IOException {
        String outputCSV = "C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\sqllite";
        String fileStr = outputCSV + File.separator + "BASE.1.csv";
        BufferedReader csvReader = null;
        try {
            csvReader = new BufferedReader(new FileReader(fileStr));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String row = null;
        ArrayList<String> listHeader = new ArrayList<>();
        ArrayList<String> listRuntimeData = new ArrayList<>();
        Boolean header = true;
        while ((row = csvReader.readLine()) != null) {
            String[] data = row.split(",");
            if (header) {
                for (int i = 0; i < data.length; i++) {
                    listHeader.add(data[i]);
                }
                header = false;
            } else {
                for (int i = 0; i < data.length; i++) {
                    if (i == 1) {
                        listRuntimeData.add(String.valueOf(10));
                    } else {
                        listRuntimeData.add(data[i]);
                    }
                }
            }
        }
        csvReader.close();
        File fwriter = new File(fileStr);
        FileWriter csvWriter = new FileWriter(fwriter);
        listRuntimeData.get(1).valueOf(10);
        csvWriter.append(String.join(",", listHeader));
        csvWriter.append("\n");
        csvWriter.append(String.join(",", listRuntimeData));
        csvWriter.flush();
        csvWriter.close();
    }


    @Test
    public void testgetEveryNth() throws Exception {

        GitHelper gh = new GitHelper("C:\\obermanndavid\\git-ecco-test\\2_second_run\\Marlin", null);
        final GitCommitList commitList = new GitCommitList(gh);

        commitList.addGitCommitListener((gc, gcl) -> {
            System.out.println(gc.getNumber() + " -> " + gc.getCommitName() + " diff to: " + gc.getDiffCommitName());
        });

        gh.getEveryNthCommit(commitList, null, 36, 50, 1);
    }

    @Test
    public void testNumberFormat() {
        String s = "F_CPU >= 16000000L";
        ExpressionSolver es = new ExpressionSolver();
        es.traverse(new FeatureExpressionParser(s).parse());
    }

    @Test
    public void testCorrectedClauseAdding() {
        String condition = "TESTVAR";
        ExpressionSolver es = new ExpressionSolver(condition);

        LinkedList<FeatureImplication> impls = new LinkedList<>();
        impls.add(new FeatureImplication("A", "FILE_H == 0"));
        impls.add(new FeatureImplication("!(FILE_H)", "FILE_H == 1"));
        es.addClause(new Feature("FILE_H"), impls);

        es.solve().entrySet().forEach(x -> System.out.println(x.getKey().getName() + " = " + x.getValue()));
    }

    @Test
    public void testunsatproblem() {
        Model model = new Model("test1");

        BoolVar y = model.boolVar("y");
        BoolVar x = model.boolVar("x");
        BoolVar b = model.boolVar("b");

        model.addClauses(ifThenElse(x.not(), x, x.not()));
        //model.addClauses(implies(x.not(),x));
        model.post(b.extension());

        Solution s = model.getSolver().findSolution();
        System.out.println(s);
    }

    @Test
    public void testSolver() {
        String condition = "C>5";

        ExpressionSolver es = new ExpressionSolver(condition);

        es.solve().entrySet().forEach(x -> System.out.println(x.getKey() + " = " + x.getValue()));
    }

    @Test
    public void testAddClause() {
        String condition = "C>5";
        ExpressionSolver es = new ExpressionSolver(condition);
        Queue<FeatureImplication> impls = new LinkedList<>();
        impls.add(new FeatureImplication("X", "C == 4"));
        impls.add(new FeatureImplication("Y", "C == 8"));
        es.addClause(new Feature("C"), impls);
        es.solve().entrySet().forEach(x -> System.out.println(x.getKey() + " = " + x.getValue()));
    }

    @Test
    public void testNegativeNumber() {
        String condition = "PIDTEMP > -1";
        ExpressionSolver es = new ExpressionSolver(condition);
        es.solve().entrySet().forEach(x -> System.out.println(x.getKey() + " = " + x.getValue()));
    }

    @Test
    public void failingIfThenChain() {
        Model model = new Model("test1");

        BoolVar y = model.boolVar("y");
        BoolVar x = model.boolVar("x");
        BoolVar b = model.boolVar("b");

        //model.post(LogOp.and(x,b));

        model.addClauses(ifThenElse(y, b, ifThenElse(x, b.not(), b.not())));
        model.post(b.extension());

        Solution s = model.getSolver().findSolution();
        System.out.println(s);

        Assert.assertNotNull(s);
    }

    @Test
    public void successfullIfThenChain() {
        Model model = new Model("test2");

        BoolVar x = model.boolVar("x");
        BoolVar y = model.boolVar("y");
        BoolVar b = model.boolVar("b");

        //solution here is comprehensible
        model.post(y.ift(b.not(), x.ift(b, b.not())).intVar().asBoolVar().extension());
        model.post(b.extension());

        Solution s = model.getSolver().findSolution();
        System.out.println(s);

        Assert.assertNotNull(s);
    }

    @Test
    public void chocoExperiment2() {
        Model model = new Model("test3");

        BoolVar x = model.boolVar("x");
        BoolVar y = model.boolVar("y");
        IntVar c = model.intVar("c", Short.MIN_VALUE, Short.MAX_VALUE);

        //why does this not have a solution?!?!?
        model.addClauses(ifThenElse(y, c.eq(4).boolVar(), ifThenElse(x, c.eq(9).boolVar(), c.eq(0).boolVar())));
        //model.post(y.ift(c.eq(4), x.ift(c.eq(9),model.boolVar(false))).intVar().asBoolVar().extension());
        model.post(c.gt(7).boolVar().extension());

        model.getSolver().findAllSolutions().forEach(System.out::println);
    }

    @Test
    public void chocoExperiment3() {
        Model model = new Model("test3");

        BoolVar x = model.boolVar("x");
        BoolVar y = model.boolVar("y");
        IntVar c = model.intVar("c", Short.MIN_VALUE, Short.MAX_VALUE);

        //why does this not have a solution?!?!?
        model.addClauses(ifThenElse(y, c.eq(4).boolVar(), implies(x, c.eq(9).boolVar())));
        //model.post(y.ift(c.eq(4), x.ift(c.eq(9),model.boolVar(false))).intVar().asBoolVar().extension());
        model.addClauses(and(c.gt(7).boolVar()));
        //model.addClauses(and(c.add(c).intVar().asBoolVar()));

        model.getSolver().findAllSolutions().forEach(System.out::println);
    }

    @Test
    public void chocoExperimentGabi() {
        Model model = new Model("test3");

        BoolVar a = model.boolVar("A");
        BoolVar b = model.boolVar("B");
        BoolVar c = model.boolVar("C");
        BoolVar y = model.boolVar("Y");
        model.addClauses(ifThenElse(a, b, implies(c.not(), b.not())));
        model.addClauses(implies(b, y));
        System.out.println(model.getSolver().findSolution());
    }


    @Test
    public void pptest() {
        String f2 = "C:\\obermanndavid\\git-ecco\\ppfiles\\test\\git2_sub\\clean";
        PreprocessorHelper pph = new PreprocessorHelper();
        String f1 = "C:\\obermanndavid\\git-ecco\\ppfiles\\test\\git2";
        File file = new File("C:\\obermanndavid\\git-ecco\\ppfiles\\test\\git2");
        File file2 = new File("C:\\obermanndavid\\git-ecco\\ppfiles\\test\\git2_sub\\clean");
        pph.generateCleanVersion(file, file2, null);
    }

    @Test
    public void chocoSolverExperiment() {
        Model model = new Model("test4");
        IntVar c = model.intVar("C", Short.MIN_VALUE, Short.MAX_VALUE);
        //IntVar c = checkVars(model, "C").asIntVar();
        model.post(c.gt(4).boolVar().and(model.boolVar("A")).extension());
        Variable var = checkVars(model, "C");
        model.ifOnlyIf(model.boolVar("X").extension(), model.arithm(var.asIntVar(), "=", 6));
        Solution solution = model.getSolver().findSolution();
        System.out.println(solution);
    }

    //helper method also in ExpressionSolver class
    private Variable checkVars(Model model, String name) {
        for (Variable var : model.getVars()) {
            if (var.getName().equals(name)) return var;
        }
        return null;
    }

    @Test
    public void random(){
        Random random = new Random();
        int x = random.nextInt(100);
        System.out.println(x);
    }
}

package at.jku.isse.gitecco.translation.test;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class MetricsTest {

    public static String resultsCSVs_path = "C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\releases-propagation\\dataset\\LibSSH4";

    public static void main(String[] args) throws Exception {
        File folder = new File(resultsCSVs_path);
        File[] lista = folder.listFiles();
        Float totalnumberFiles = Float.valueOf(0), matchesFiles = Float.valueOf(0), eccototalLines = Float.valueOf(0), originaltotalLines = Float.valueOf(0), missingFiles = Float.valueOf(0), remainingFiles = Float.valueOf(0), totalVariantsMatch = Float.valueOf(0), truepositiveLines = Float.valueOf(0), falsepositiveLines = Float.valueOf(0), falsenegativeLines = Float.valueOf(0),
                truepositiveLinesEachFile = Float.valueOf(0), falsepositiveLinesEachFile = Float.valueOf(0), falsenegativeLinesEachFile = Float.valueOf(0), numberTotalFilesEachVariant = Float.valueOf(0), matchFilesEachVariant = Float.valueOf(0), eccototalLinesEachFile = Float.valueOf(0), originaltotalLinesEachFile = Float.valueOf(0);
        Boolean variantMatch = true;
        Float numberCSV = Float.valueOf(0);
        for (File file : lista) {
            if ((file.getName().indexOf(".csv") != -1) && !(file.getName().contains("report_each_commit"))) {
                Reader reader = Files.newBufferedReader(Paths.get(file.getAbsolutePath()));
                CSVReader csvReader = new CSVReaderBuilder(reader).build();
                List<String[]> matchesVariants = csvReader.readAll();
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
                    if (line[1].toUpperCase().equals("TRUE")) {
                        matchFilesEachVariant++;
                        matchesFiles++;
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

                numberCSV++;
                if (variantMatch && Float.compare(matchFilesEachVariant, numberTotalFilesEachVariant) == 0)
                    totalVariantsMatch++;
            }
            numberTotalFilesEachVariant = Float.valueOf(0);
            matchFilesEachVariant = Float.valueOf(0);
            variantMatch = true;
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
            FileWriter csvWriter = new FileWriter(resultsCSVs_path + File.separator + filemetrics);
            List<List<String>> headerRows = Arrays.asList(
                    Arrays.asList("PrecisionFiles", "RecallFiles", "F1ScoreFiles", "PrecisionLines", "RecalLines", "F1ScoreLines"),
                    Arrays.asList(precisionFiles.toString(), recallFiles.toString(), f1scoreFiles.toString(), precisionLines.toString(), recallLines.toString(), f1scorelines.toString())
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
        System.out.println("Total lines inserted: " + falsepositiveLines + "\nTotal lines deleted: " + falsenegativeLines);
    }
}

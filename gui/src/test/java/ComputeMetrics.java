import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.*;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ComputeMetrics {
    //directory where you have the folder with the artifacts of the target systyem
    public final String resultsCSVs_path = "C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\ChangePropagation\\results\\sqlite\\c4c1a518e5676f3401e7fec3a727d10a66fdc364";
    //directory where you have the folder with the artifacts of Marlin target systyem
    public final String systemFolder = "C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\ChangePropagation\\results\\sqlite";
    public final String libsshFolder = "C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\ChangePropagation\\results\\libssh";
    public final String metricsResultFolder = "C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\ChangePropagation\\results";
    private List<String> fileTypes = new LinkedList<String>();

    //compare the ground truth variants with the composed variants
    @org.testng.annotations.Test
    public void TestCompareVariants() {
        //"input_variants" folder contains the ground truth variants and "checkout" folder contains the composed variants
        File variantsrc = new File(resultsCSVs_path, "original");
        File checkoutfile = new File(resultsCSVs_path, "checkout");
        try {
            for (File path : variantsrc.listFiles()) {
                compareVariant(path, new File(checkoutfile + File.separator + path.getName()));
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
        File[] folder = {new File(systemFolder),
                new File(libsshFolder),
                //new File(sqliteFolder)
        };
        //write metrics in a csv file
        String filemetrics = "MetricsEachAndAllSystemsTogether.csv";
        FileWriter csvWriter = new FileWriter(metricsResultFolder + File.separator + filemetrics);

        Float totalmeanRuntime = Float.valueOf(0);
        Float totaltotalnumberFiles = Float.valueOf(0), totalmatchesFiles = Float.valueOf(0), totalcheckouttotalLines = Float.valueOf(0), totaloriginaltotalLines = Float.valueOf(0), totalmissingFiles = Float.valueOf(0), totalremainingFiles = Float.valueOf(0), totaltotalVariantsMatch = Float.valueOf(0), totaltruepositiveLines = Float.valueOf(0), totalfalsepositiveLines = Float.valueOf(0), totalfalsenegativeLines = Float.valueOf(0),
                totaltruepositiveLinesEachFile = Float.valueOf(0), totalfalsepositiveLinesEachFile = Float.valueOf(0), totalfalsenegativeLinesEachFile = Float.valueOf(0), totalnumberTotalFilesEachVariant = Float.valueOf(0), totalmatchFilesEachVariant = Float.valueOf(0), totalcheckouttotalLinesEachFile = Float.valueOf(0), totaloriginaltotalLinesEachFile = Float.valueOf(0), totalnumberCSV = Float.valueOf(0);
        for (int j = 0; j < folder.length; j++) {
            File[] lista = folder[j].listFiles();
            Float meanRuntime = Float.valueOf(0);
            Float totalnumberFiles = Float.valueOf(0), matchesFiles = Float.valueOf(0), checkouttotalLines = Float.valueOf(0), originaltotalLines = Float.valueOf(0), missingFiles = Float.valueOf(0), remainingFiles = Float.valueOf(0), totalVariantsMatch = Float.valueOf(0), truepositiveLines = Float.valueOf(0), falsepositiveLines = Float.valueOf(0), falsenegativeLines = Float.valueOf(0),
                    truepositiveLinesEachFile = Float.valueOf(0), falsepositiveLinesEachFile = Float.valueOf(0), falsenegativeLinesEachFile = Float.valueOf(0), numberTotalFilesEachVariant = Float.valueOf(0), matchFilesEachVariant = Float.valueOf(0), checkouttotalLinesEachFile = Float.valueOf(0), originaltotalLinesEachFile = Float.valueOf(0);
            Boolean variantMatch = true;
            Float numberCSV = Float.valueOf(0);
            for (File file : lista) {
                for(File variant : file.listFiles()) {
                    if ((variant.getName().indexOf(".csv") != -1)) {
                        //runtime in ms
                        String path = variant.getName().substring(0, variant.getName().indexOf(".csv"));
                        File f = new File(file + File.separator + "checkout" + File.separator + path + File.separator + "time.txt");
                        BufferedReader brTest = new BufferedReader(new FileReader(f));
                        String runtimes = brTest.readLine();
                        meanRuntime += Float.valueOf(runtimes);
                        totalmeanRuntime += Float.valueOf(runtimes);
                        //precision and recall
                        Reader reader = Files.newBufferedReader(Paths.get(variant.getAbsolutePath()));
                        CSVReader csvReader = new CSVReaderBuilder(reader).build();
                        List<String[]> matchesVariants = csvReader.readAll();


                        for (int i = 1; i < matchesVariants.size(); i++) {

                            String[] line = matchesVariants.get(i);
                            truepositiveLines += Integer.valueOf(line[2]);
                            falsepositiveLines += Integer.valueOf(line[3]);
                            falsenegativeLines += Integer.valueOf(line[4]);
                            truepositiveLinesEachFile = Float.valueOf(Integer.valueOf(line[2]));
                            falsepositiveLinesEachFile = Float.valueOf(Integer.valueOf(line[3]));
                            falsenegativeLinesEachFile = Float.valueOf(Integer.valueOf(line[4]));
                            originaltotalLines += Integer.valueOf(line[5]);
                            checkouttotalLines += Integer.valueOf(line[6]);
                            originaltotalLinesEachFile = Float.valueOf(Integer.valueOf(line[5]));
                            checkouttotalLinesEachFile = Float.valueOf(Integer.valueOf(line[6]));

                            totaltruepositiveLines += truepositiveLines;
                            totalfalsepositiveLines += falsepositiveLines;
                            totalfalsenegativeLines += falsenegativeLines;
                            totaltruepositiveLinesEachFile += totaltruepositiveLinesEachFile;
                            totalfalsepositiveLinesEachFile += falsepositiveLinesEachFile;
                            totalfalsenegativeLinesEachFile += falsenegativeLinesEachFile;
                            totaloriginaltotalLines += originaltotalLines;
                            totalcheckouttotalLines += checkouttotalLines;
                            totaloriginaltotalLinesEachFile += originaltotalLinesEachFile;
                            totalcheckouttotalLinesEachFile += checkouttotalLinesEachFile;

                            if (line[1].equals("true")) {
                                if (Float.compare(originaltotalLinesEachFile, checkouttotalLinesEachFile) == 0 && Float.compare(truepositiveLinesEachFile, originaltotalLinesEachFile) == 0) {
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
                        numberCSV++;
                        totalnumberCSV++;
                    }

                    if (variantMatch && Float.compare(matchFilesEachVariant, numberTotalFilesEachVariant) == 0) {
                        totalVariantsMatch++;
                        totaltotalVariantsMatch++;
                    }

                    numberTotalFilesEachVariant = Float.valueOf(0);
                    matchFilesEachVariant = Float.valueOf(0);
                    variantMatch = true;
                }
            }

            meanRuntime = (meanRuntime / numberCSV) / 1000;
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
                        Arrays.asList(meanRuntime.toString())
                );
                for (List<String> rowData : headerRows) {
                    csvWriter.append(String.join(",", rowData));
                    csvWriter.append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        totalmeanRuntime = (totalmeanRuntime / totalnumberCSV) / 1000;
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
                Arrays.asList(totalmeanRuntime.toString())
        );
        for (List<String> rowData : totalRows) {
            csvWriter.append(String.join(",", rowData));
            csvWriter.append("\n");
        }
        csvWriter.flush();
        csvWriter.close();
    }


    public void compareVariant(File srcOriginal, File srcCheckout) throws IOException {
        fileTypes.add("c");
        fileTypes.add("cpp");
        fileTypes.add("h");
        fileTypes.add("hpp");
        fileTypes.add("txt");
        fileTypes.add("md");
        fileTypes.add("xml");
        fileTypes.add("html");
        fileTypes.add("css");
        fileTypes.add("js");
        fileTypes.add("java");

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
            } else {
                //compare other type files
                for (File fcheckout : filesCheckout) {
                    if (f.toPath().toString().substring(f.toPath().toString().indexOf("original\\") + 9).equals(fcheckout.toPath().toString().substring(fcheckout.toPath().toString().indexOf("checkout\\") + 9))) {
                        if (!f.isDirectory()) {
                            int byte_f1;
                            int byte_f2;
                            if (f.length() == fcheckout.length()) {
                                try {
                                    InputStream isf1 = new FileInputStream(f);
                                    InputStream isf2 = new FileInputStream(fcheckout);
                                    matchFiles = true;
                                    for (long i = 0; i <= f.length(); i++) {
                                        try {
                                            byte_f1 = isf1.read();
                                            byte_f2 = isf2.read();
                                            if (byte_f1 != byte_f2) {
                                                isf1.close();
                                                isf2.close();
                                                // tamanhos iguais e conteudos diferentes
                                                matchFiles = false;
                                            }
                                        } catch (IOException ex) {
                                        }
                                    }
                                } catch (FileNotFoundException ex) {
                                }
                                if (!matchFiles) {
                                    falsenegativeLines = 1;
                                    truepositiveLines = 0;
                                    falsepositiveLines = 0;
                                } else {
                                    // arquivos iguais
                                    truepositiveLines = 1;
                                    falsenegativeLines = 0;
                                    falsepositiveLines = 0;
                                }
                            } else {
                                // tamanho e conteudo diferente
                                matchFiles = false;
                                falsenegativeLines = 1;
                                truepositiveLines = 0;
                                falsepositiveLines = 0;
                            }


                        } else {
                            truepositiveLines = 1;
                            falsenegativeLines = 0;
                            falsepositiveLines = 0;
                            existJustCheckoutVariant = true;
                            matchFiles = true;
                        }
                        checkouttotalLines = 1;
                        originaltotalLines = 1;

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
                    falsenegativeLines = 1;
                    falsepositiveLines = 0;
                    truepositiveLines = 0;
                    List<List<String>> resultRows = Arrays.asList(
                            Arrays.asList(f.toPath().toString().substring(f.toPath().toString().indexOf("original\\") + 9).replace(",", "and"), "not", truepositiveLines.toString(), falsepositiveLines.toString(), falsenegativeLines.toString(), originaltotalLines.toString(), checkouttotalLines.toString())
                    );
                    for (List<String> rowData : resultRows) {
                        csvWriter.append(String.join(",", rowData));
                        csvWriter.append("\n");
                    }
                }

            }

        }


        //files that are just in checked out variant and not in original variant
        for (File fcheckout : filesCheckout) {
            Integer truepositiveLines = 0, falsepositiveLines = 0, falsenegativeLines = 0, originaltotalLines = 0, checkouttotalLines = 0;
            Boolean matchFiles = false;

            String extension = fcheckout.getName().substring(fcheckout.getName().lastIndexOf('.') + 1);
            if (fileTypes.contains(extension)) {

                //compare text of files
                List<String> original = Files.readAllLines(fcheckout.toPath());
                Boolean existJustCheckoutVariant = true;
                for (File f : filesOriginal) {
                    if (fcheckout.toPath().toString().substring(fcheckout.toPath().toString().indexOf("checkout\\") + 9).equals(f.toPath().toString().substring(f.toPath().toString().indexOf("original\\") + 9))) {
                        existJustCheckoutVariant = false;
                    }
                }
                //file just exist in checked out variant
                if (existJustCheckoutVariant) {

                    matchFiles = false;
                    checkouttotalLines = original.size() - 1;
                    falsepositiveLines = checkouttotalLines;
                    falsenegativeLines = 0;
                    originaltotalLines = 0;
                    truepositiveLines = checkouttotalLines - (falsepositiveLines);

                    List<List<String>> resultRows = Arrays.asList(
                            Arrays.asList(fcheckout.toPath().toString().substring(fcheckout.toPath().toString().indexOf("checkout\\") + 9).replace(",", "and"), "justOnRetrieved", truepositiveLines.toString(), falsepositiveLines.toString(), falsenegativeLines.toString(), originaltotalLines.toString(), checkouttotalLines.toString())
                    );
                    for (List<String> rowData : resultRows) {
                        csvWriter.append(String.join(",", rowData));
                        csvWriter.append("\n");
                    }
                }
            } else {
                //compare other type files
                Boolean existJustCheckoutVariant = true;
                for (File f : filesOriginal) {
                    if (fcheckout.toPath().toString().substring(fcheckout.toPath().toString().indexOf("checkout\\") + 9).equals(f.toPath().toString().substring(f.toPath().toString().indexOf("original\\") + 9))) {
                        existJustCheckoutVariant = false;
                    }
                }
                if (existJustCheckoutVariant) {
                    matchFiles = false;
                    checkouttotalLines = 1;
                    falsepositiveLines = checkouttotalLines;
                    falsenegativeLines = 0;
                    originaltotalLines = 0;
                    truepositiveLines = checkouttotalLines - (falsepositiveLines);

                    List<List<String>> resultRows = Arrays.asList(
                            Arrays.asList(fcheckout.toPath().toString().substring(fcheckout.toPath().toString().indexOf("checkout\\") + 9).replace(",", "and"), "justOnRetrieved", truepositiveLines.toString(), falsepositiveLines.toString(), falsenegativeLines.toString(), originaltotalLines.toString(), checkouttotalLines.toString())
                    );
                    for (List<String> rowData : resultRows) {
                        csvWriter.append(String.join(",", rowData));
                        csvWriter.append("\n");
                    }
                }

            }
        }
        csvWriter.close();
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

}

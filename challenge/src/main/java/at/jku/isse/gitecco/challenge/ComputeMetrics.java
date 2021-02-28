package at.jku.isse.gitecco.challenge;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

import java.io.*;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ComputeMetrics {

    private static String INPUT_VARIATNS = "";//"C:\\Users\\gabil\\Desktop\\PHD\\ChallengePaper\\TestScript\\LibSSH";
    private static String OUTPUT_VARIANTS = "";//"C:\\Users\\gabil\\Desktop\\PHD\\ChallengePaper\\TestScript\\LibSSH";
    private static List<String> fileTypes = new LinkedList<String>();
    private static File folderResultsCSVs = new File("");

    public static void main(String[] args) throws Exception {
        if (args.length > 1) {
            INPUT_VARIATNS = args[0];
            OUTPUT_VARIANTS = args[1];
            //"input_variants" folder contains the ground truth variants and "checkout" folder contains the composed variants
            File variantsrc = new File(INPUT_VARIATNS, "input");
            File outputfile = new File(OUTPUT_VARIANTS, "output");
            try {
                System.out.println("Started comparison of variants");
                for (File path : outputfile.listFiles()) {
                    compareVariant(new File(variantsrc + File.separator + path.getName()), path);
                }
                getCSVInformation2();
                System.out.println("Finished comparison of variants");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Inform the correct parameters!");
        }
    }


    public static void compareVariant(File srcOriginal, File srcEcco) throws IOException {
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

        LinkedList<File> filesVariant = new LinkedList<>();
        LinkedList<File> filesEcco = new LinkedList<>();
        getFilesToProcess(srcEcco, filesEcco);
        filesEcco.remove(srcEcco);
        getFilesToProcess(srcOriginal, filesVariant);
        filesVariant.remove(srcOriginal);
        String outputCSV = srcOriginal.getParentFile().getParentFile().getAbsolutePath();
        folderResultsCSVs = new File(outputCSV, "resultsCSV");
        if (!folderResultsCSVs.exists())
            folderResultsCSVs.mkdir();
        String fileStr = folderResultsCSVs + File.separator + srcOriginal.getName() + ".csv";
        File fWriter = new File(fileStr);
        FileWriter csvWriter = new FileWriter(fWriter);

        List<List<String>> headerRows = Arrays.asList(
                Arrays.asList("fileName", "matchFile", "truepositiveLines", "falsepositiveLines", "falsenegativeLines", "inputtotalLines", "outputtotalLines")
        );
        for (List<String> rowData : headerRows) {
            csvWriter.write(String.join(",", rowData));
            csvWriter.write("\n");
        }

        //files that are in ecco and variant
        for (File f : filesVariant) {
            Boolean fileExistsInEcco = false;
            Integer truepositiveLines = 0, falsepositiveLines = 0, falsenegativeLines = 0, originaltotalLines = 0, eccototalLines = 0;
            Boolean matchFiles = false;
            List<String> original = new ArrayList<>();
            List<String> revised = new ArrayList<>();

            String extension = f.getName().substring(f.getName().lastIndexOf('.') + 1);
            if (fileTypes.contains(extension)) {
                //compare text of files
                for (File fEcco : filesEcco) {
                    if (f.toPath().toString().substring(f.toPath().toString().indexOf("input\\") + 6).equals(fEcco.toPath().toString().substring(fEcco.toPath().toString().indexOf("output\\") + 7))) {
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
                            revised = Files.readAllLines(fEcco.toPath());
                        } catch (MalformedInputException e) {
                            StringBuilder contentBuilder = new StringBuilder();
                            File filenew = new File(String.valueOf(fEcco.toPath()));
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
                                //List<String> unifiedDiff = DiffUtils.generateUnifiedDiff(f.getName(), fEcco.getName(), original, patch, original.size());
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
                                            //System.out.println("file: " + fEcco.getAbsolutePath()  +" TYPE: " +delta.getType().toString() +"delta: " + deltaaux);
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
                                            //System.out.println("file: " + fEcco.getAbsolutePath() +" TYPE: " +delta.getType().toString() +"delta: " + deltaaux);
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
                                    System.out.println("file: " + fEcco.getAbsolutePath() + " TYPE: DELETE delta: " + line);
                                }
                            }
                            if (insertedLines.size() > 0) {
                                for (String line : insertedLines) {
                                    System.out.println("file: " + fEcco.getAbsolutePath() + " TYPE: INSERT delta: " + line);
                                }
                            }
                        }

                        eccototalLines = (revised.size() - 1);
                        originaltotalLines = original.size() - 1;
                        truepositiveLines = eccototalLines - (falsepositiveLines);

                        List<List<String>> resultRows = Arrays.asList(
                                Arrays.asList(f.toPath().toString().substring(f.toPath().toString().indexOf("input\\") + 6).replace(",", "and"), matchFiles.toString(), truepositiveLines.toString(), falsepositiveLines.toString(), falsenegativeLines.toString(), originaltotalLines.toString(), eccototalLines.toString())
                        );
                        for (List<String> rowData : resultRows) {
                            csvWriter.append(String.join(",", rowData));
                            csvWriter.append("\n");
                        }
                        fileExistsInEcco = true;
                    }

                }
                if (!fileExistsInEcco) {
                    List<List<String>> resultRows = Arrays.asList(
                            Arrays.asList(f.toPath().toString().substring(f.toPath().toString().indexOf("input\\") + 6).replace(",", "and"), "not", "0", "0", originaltotalLines.toString(), originaltotalLines.toString(), eccototalLines.toString())
                    );
                    for (List<String> rowData : resultRows) {
                        csvWriter.append(String.join(",", rowData));
                        csvWriter.append("\n");
                    }
                }
            } else {
                //compare other type files
                for (File fEcco : filesEcco) {
                    if (f.toPath().toString().substring(f.toPath().toString().indexOf("input\\") + 6).equals(fEcco.toPath().toString().substring(fEcco.toPath().toString().indexOf("output\\") + 7))) {
                        if (!f.isDirectory()) {
                            int byte_f1;
                            int byte_f2;
                            if (f.length() == fEcco.length()) {
                                try {
                                    InputStream isf1 = new FileInputStream(f);
                                    InputStream isf2 = new FileInputStream(fEcco);
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
                            fileExistsInEcco = true;
                            matchFiles = true;
                        }
                        eccototalLines = 1;
                        originaltotalLines = 1;

                        List<List<String>> resultRows = Arrays.asList(
                                Arrays.asList(f.toPath().toString().substring(f.toPath().toString().indexOf("input\\") + 6).replace(",", "and"), matchFiles.toString(), truepositiveLines.toString(), falsepositiveLines.toString(), falsenegativeLines.toString(), originaltotalLines.toString(), eccototalLines.toString())
                        );
                        for (List<String> rowData : resultRows) {
                            csvWriter.append(String.join(",", rowData));
                            csvWriter.append("\n");
                        }
                        fileExistsInEcco = true;
                    }
                }
                if (!fileExistsInEcco) {
                    falsenegativeLines = 1;
                    falsepositiveLines = 0;
                    truepositiveLines = 0;
                    List<List<String>> resultRows = Arrays.asList(
                            Arrays.asList(f.toPath().toString().substring(f.toPath().toString().indexOf("input\\") + 6).replace(",", "and"), "not", truepositiveLines.toString(), falsepositiveLines.toString(), falsenegativeLines.toString(), originaltotalLines.toString(), eccototalLines.toString())
                    );
                    for (List<String> rowData : resultRows) {
                        csvWriter.append(String.join(",", rowData));
                        csvWriter.append("\n");
                    }
                }

            }

        }


        //files that are just in ecco and not in variant
        for (File fEcco : filesEcco) {
            Boolean fileExistsInEcco = false;
            Integer truepositiveLines = 0, falsepositiveLines = 0, falsenegativeLines = 0, originaltotalLines = 0, eccototalLines = 0;
            Boolean matchFiles = false;

            String extension = fEcco.getName().substring(fEcco.getName().lastIndexOf('.') + 1);
            if (fileTypes.contains(extension)) {

                //compare text of files
                List<String> original = Files.readAllLines(fEcco.toPath());
                Boolean existJustEcco = true;
                for (File f : filesVariant) {
                    if (fEcco.toPath().toString().substring(fEcco.toPath().toString().indexOf("output\\") + 7).equals(f.toPath().toString().substring(f.toPath().toString().indexOf("input\\") + 6))) {
                        existJustEcco = false;
                    }
                }
                //file just exist in ecco
                if (existJustEcco) {

                    matchFiles = false;
                    eccototalLines = original.size() - 1;
                    falsepositiveLines = eccototalLines;
                    falsenegativeLines = 0;
                    originaltotalLines = 0;
                    truepositiveLines = eccototalLines - (falsepositiveLines);

                    List<List<String>> resultRows = Arrays.asList(
                            Arrays.asList(fEcco.toPath().toString().substring(fEcco.toPath().toString().indexOf("output\\") + 7).replace(",", "and"), "justOnRetrieved", truepositiveLines.toString(), falsepositiveLines.toString(), falsenegativeLines.toString(), originaltotalLines.toString(), eccototalLines.toString())
                    );
                    for (List<String> rowData : resultRows) {
                        csvWriter.append(String.join(",", rowData));
                        csvWriter.append("\n");
                    }
                }
            } else {
                //compare other type files
                Boolean existJustEcco = true;
                for (File f : filesVariant) {
                    if (fEcco.toPath().toString().substring(fEcco.toPath().toString().indexOf("output\\") + 7).equals(f.toPath().toString().substring(f.toPath().toString().indexOf("input\\") + 6))) {
                        existJustEcco = false;
                    }
                }
                if (existJustEcco) {
                    matchFiles = false;
                    eccototalLines = 1;
                    falsepositiveLines = eccototalLines;
                    falsenegativeLines = 0;
                    originaltotalLines = 0;
                    truepositiveLines = eccototalLines - (falsepositiveLines);

                    List<List<String>> resultRows = Arrays.asList(
                            Arrays.asList(fEcco.toPath().toString().substring(fEcco.toPath().toString().indexOf("output\\") + 7).replace(",", "and"), "justOnRetrieved", truepositiveLines.toString(), falsepositiveLines.toString(), falsenegativeLines.toString(), originaltotalLines.toString(), eccototalLines.toString())
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


    private static void getFilesToProcess(File f, List<File> files) {
        if (f.isDirectory()) {
            for (File file : f.listFiles()) {
                if (!files.contains(f) && !file.getName().equals(f.getName()))
                    files.add(f);
                getFilesToProcess(file, files);
            }
        } else if (f.isFile()) {
            //for (String ext : this.fileTypes) {
            //    if (f.getName().endsWith("." + ext)) {
            if (!f.getName().equals(".config") && !f.getName().equals(".hashes") && !f.getName().equals(".warnings"))
                files.add(f);
            //    }
            //}
        }
    }

    public static void getCSVInformation2() throws IOException {
        File folder = folderResultsCSVs;
        File[] lista = folder.listFiles();
        Float totalnumberFiles = Float.valueOf(0), matchesFiles = Float.valueOf(0), eccototalLines = Float.valueOf(0), originaltotalLines = Float.valueOf(0), missingFiles = Float.valueOf(0), remainingFiles = Float.valueOf(0), totalVariantsMatch = Float.valueOf(0), truepositiveLines = Float.valueOf(0), falsepositiveLines = Float.valueOf(0), falsenegativeLines = Float.valueOf(0),
                truepositiveLinesEachFile = Float.valueOf(0), falsepositiveLinesEachFile = Float.valueOf(0), falsenegativeLinesEachFile = Float.valueOf(0), numberTotalFilesEachVariant = Float.valueOf(0), matchFilesEachVariant = Float.valueOf(0), eccototalLinesEachFile = Float.valueOf(0), originaltotalLinesEachFile = Float.valueOf(0);
        Boolean variantMatch = true;
        Float numberCSV = Float.valueOf(0);
        for (File file : lista) {
            if ((file.getName().indexOf(".csv") != -1) && !(file.getName().contains("features_report_each_project_commit")) && !(file.getName().contains("configurations"))) {
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
            FileWriter csvWriter = new FileWriter(folderResultsCSVs + File.separator + filemetrics);
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

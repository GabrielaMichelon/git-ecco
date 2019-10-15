package at.jku.isse.gitecco.translation.variantscomparison.util;


import at.jku.isse.ecco.service.EccoService;
import at.jku.isse.gitecco.core.git.GitCommitList;
import at.jku.isse.gitecco.core.git.GitHelper;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CompareVariants {
    private List<String> fileTypes = new LinkedList<String>();


    public void compareVariant(File srcOriginal, File srcEcco) throws IOException {
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
        String fileStr = outputCSV + File.separator + srcOriginal.getName() + ".csv";
        File fWriter = new File(fileStr);
        FileWriter csvWriter = new FileWriter(fWriter);

        List<List<String>> headerRows = Arrays.asList(
                Arrays.asList("fileName", "matchFile", "truepositiveLines", "falsepositiveLines", "falsenegativeLines", "originaltotalLines", "eccototalLines")
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
            List<String> original = null;
            List<String> revised = null;

            String extension = f.getName().substring(f.getName().lastIndexOf('.') + 1);
            if (fileTypes.contains(extension)) {
                //compare text of files
                for (File fEcco : filesEcco) {
                    if (f.getName().equals(fEcco.getName())) {
                        try {
                            original = Files.readAllLines(f.toPath());
                            revised = Files.readAllLines(fEcco.toPath());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        // Compute diff. Get the Patch object. Patch is the container for computed deltas.
                        Patch<String> patch = null;
                        patch = DiffUtils.diff(original, revised);

                        if (patch.getDeltas().size() == 0) {
                            //files match
                            matchFiles = true;
                        } else {
                            matchFiles = false;
                            for (Delta delta : patch.getDeltas()) {
                                Integer difLines = Math.abs(delta.getOriginal().getLines().size() - delta.getRevised().getLines().size());
                                //List<String> unifiedDiff = DiffUtils.generateUnifiedDiff(f.getName(), fEcco.getName(), original, patch, original.size());
                                System.out.println(delta.getType().toString());
                                if (delta.getType().toString().equals("INSERT"))
                                    falsepositiveLines += difLines;
                                else
                                    falsenegativeLines += difLines;
                            }
                        }
                        eccototalLines = (revised.size() - 1);
                        originaltotalLines = original.size() - 1;
                        truepositiveLines = eccototalLines - (falsepositiveLines);

                        List<List<String>> resultRows = Arrays.asList(
                                Arrays.asList(f.getName(), matchFiles.toString(), truepositiveLines.toString(), falsepositiveLines.toString(), falsenegativeLines.toString(), originaltotalLines.toString(), eccototalLines.toString())
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
                            Arrays.asList(f.getName(), "not", "0", "0", originaltotalLines.toString(), originaltotalLines.toString(), eccototalLines.toString())
                    );
                    for (List<String> rowData : resultRows) {
                        csvWriter.append(String.join(",", rowData));
                        csvWriter.append("\n");
                    }
                }
            } else {
                //compare other type files
                for (File fEcco : filesEcco) {
                    if (f.getName().equals(fEcco.getName())) {
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
                                Arrays.asList(f.getName(), matchFiles.toString(), truepositiveLines.toString(), falsepositiveLines.toString(), falsenegativeLines.toString(), originaltotalLines.toString(), eccototalLines.toString())
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
                            Arrays.asList(f.getName(), "not", truepositiveLines.toString(), falsepositiveLines.toString(), falsenegativeLines.toString(), originaltotalLines.toString(), eccototalLines.toString())
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
                    if (fEcco.getName().equals(f.getName())) {
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
                            Arrays.asList(fEcco.getName(), "justEcco", truepositiveLines.toString(), falsepositiveLines.toString(), falsenegativeLines.toString(), originaltotalLines.toString(), eccototalLines.toString())
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
                    if (fEcco.getName().equals(f.getName())) {
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
                            Arrays.asList(fEcco.getName(), "justEcco", truepositiveLines.toString(), falsepositiveLines.toString(), falsenegativeLines.toString(), originaltotalLines.toString(), eccototalLines.toString())
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


    public void eccoCheckout(ArrayList<String> configsToCheckout, Path OUTPUT_DIR, File eccoFolder, File
            checkoutFolder) throws IOException {

        EccoService service = new EccoService();
        service.setRepositoryDir(OUTPUT_DIR.resolve("repo"));
        service.open();
        //checkout
        Long runtimeEccoCheckout, timeBefore, timeAfter;
        for (String config : configsToCheckout) {
            Path pathcheckout = Paths.get(OUTPUT_DIR.resolve("checkout") + File.separator + config);
            File checkoutfile = new File(String.valueOf(pathcheckout));
            if (checkoutfile.exists()) GitCommitList.recursiveDelete(checkoutfile.toPath());
            checkoutfile.mkdir();
            service.setBaseDir(pathcheckout);
            timeBefore = System.currentTimeMillis();
            service.checkout(config);
            timeAfter = System.currentTimeMillis();
            runtimeEccoCheckout = timeAfter - timeBefore;
            String outputCSV = eccoFolder.getParentFile().getAbsolutePath();
            String fileStr = outputCSV + File.separator + "runtime.csv";
            BufferedReader csvReader = null;
            try {
                csvReader = new BufferedReader(new FileReader(fileStr));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            String row = null;
            ArrayList<String> listHeader = new ArrayList<>();
            ArrayList<String> listRuntimeData = new ArrayList<>();
            List<List<String>> rows = new ArrayList<>();

            Boolean header = true;
            while ((row = csvReader.readLine()) != null) {
                String[] data = row.split(",");
                ArrayList<String> dataAux = new ArrayList<>();
                if (header) {
                    for (int i = 0; i < data.length; i++) {
                        listHeader.add(data[i]);
                    }
                    header = false;
                } else {
                    for (int i = 0; i < data.length; i++) {
                        if ((data[1].equals(config.replace(",", "AND"))) && (i == 3)) {
                            data[i] = (String.valueOf(runtimeEccoCheckout));
                        }
                        dataAux.add(data[i]);
                    }
                    rows.add(dataAux);
                }

            }
            csvReader.close();
            File fwriter = new File(fileStr);
            FileWriter csvWriter = new FileWriter(fwriter);

            csvWriter.write(String.join(",", listHeader));
            csvWriter.write("\n");
            for (List<String> line : rows) {
                csvWriter.write(String.join(",", line));
                csvWriter.write("\n");
            }
            csvWriter.flush();
            csvWriter.close();
        }
        //end checkout

        //close ecco repository
        service.close();


    }

    public void eccoCommit(File eccoFolder, Path OUTPUT_DIR, ArrayList<String> configsToCommit) throws IOException {
        EccoService service = new EccoService();
        if (OUTPUT_DIR.resolve("repo").toFile().exists()) GitCommitList.recursiveDelete(OUTPUT_DIR.resolve("repo"));
        service.setRepositoryDir(OUTPUT_DIR.resolve("repo"));
        //initializing repo
        service.init();
        System.out.println("Repository initialized.");
        //commit
        Long runtimeEccoCommit, timeBefore, timeAfter;
        for (String config : configsToCommit) {
            //ecco commit
            System.out.println("CONFIG: " + config);
            File variantsrc = new File(eccoFolder, config);
            Path variant_dir = Paths.get(String.valueOf(variantsrc));
            service.setBaseDir(variant_dir);
            timeBefore = System.currentTimeMillis();
            service.commit(config);
            System.out.println("Committed: " + variant_dir);
            timeAfter = System.currentTimeMillis();
            runtimeEccoCommit = timeAfter - timeBefore;
            //end ecco commit
            String outputCSV = eccoFolder.getParentFile().getAbsolutePath();
            String fileStr = outputCSV + File.separator + "runtime.csv";
            BufferedReader csvReader = null;
            try {
                csvReader = new BufferedReader(new FileReader(fileStr));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            String row = null;
            ArrayList<String> listHeader = new ArrayList<>();
            ArrayList<String> listRuntimeData = new ArrayList<>();
            List<List<String>> rows = new ArrayList<>();

            Boolean header = true;
            while ((row = csvReader.readLine()) != null) {
                String[] data = row.split(",");
                ArrayList<String> dataAux = new ArrayList<>();
                if (header) {
                    for (int i = 0; i < data.length; i++) {
                        listHeader.add(data[i]);
                    }
                    header = false;
                } else {
                    for (int i = 0; i < data.length; i++) {
                        if ((data[1].equals(config.replace(",", "AND"))) && (i == 2)) {
                            data[i] = (String.valueOf(runtimeEccoCommit));
                        }
                        dataAux.add(data[i]);
                    }
                    rows.add(dataAux);
                }

            }
            csvReader.close();
            File fwriter = new File(fileStr);
            FileWriter csvWriter = new FileWriter(fwriter);

            csvWriter.write(String.join(",", listHeader));
            csvWriter.write("\n");
            for (List<String> line : rows) {
                csvWriter.write(String.join(",", line));
                csvWriter.write("\n");
            }
            csvWriter.flush();
            csvWriter.close();
        }
        //end commit

        //close ecco repository
        service.close();


    }


    public void gitCommit(File srcFolder, Path OUTPUT_DIR, Map<String,String> configsToCommit) throws IOException {

        for (Map.Entry<String, String> config : configsToCommit.entrySet()) {
            Path variant_dir = Paths.get(String.valueOf(srcFolder));
            Path folderGit = Paths.get(String.valueOf(OUTPUT_DIR));
            //computing the git commit runtime of the variant
            Long runtimeGitCommit;
            GitHelper gh = new GitHelper();
            File dstGitProject = new File(String.valueOf(folderGit));
            try {
                gh.gitCommitAndCheckout(srcFolder.getAbsolutePath(),dstGitProject.getAbsolutePath(), config.getValue(), config.getKey());
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
            runtimeGitCommit = gh.getRuntimeGitCommit();
            String outputCSV = srcFolder.getParentFile().getAbsolutePath();
            String fileStr = outputCSV + File.separator + "runtime.csv";
            BufferedReader csvReader = null;
            try {
                csvReader = new BufferedReader(new FileReader(fileStr));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            String row = null;
            ArrayList<String> listHeader = new ArrayList<>();
            ArrayList<String> listRuntimeData = new ArrayList<>();
            List<List<String>> rows = new ArrayList<>();

            Boolean header = true;
            while ((row = csvReader.readLine()) != null) {
                String[] data = row.split(",");
                ArrayList<String> dataAux = new ArrayList<>();
                if (header) {
                    for (int i = 0; i < data.length; i++) {
                        listHeader.add(data[i]);
                    }
                    header = false;
                } else {
                    for (int i = 0; i < data.length; i++) {
                        if ((data[1].equals(config.getKey().replace(",", "AND"))) && (i == 6)) {
                            data[i] = (String.valueOf(runtimeGitCommit));
                        }
                        dataAux.add(data[i]);
                    }
                    rows.add(dataAux);
                }

            }
            csvReader.close();
            File fwriter = new File(fileStr);
            FileWriter csvWriter = new FileWriter(fwriter);

            csvWriter.write(String.join(",", listHeader));
            csvWriter.write("\n");
            for (List<String> line : rows) {
                csvWriter.write(String.join(",", line));
                csvWriter.write("\n");
            }
            csvWriter.flush();
            csvWriter.close();

            //end computing the git commit and checkout
        }
    }

}

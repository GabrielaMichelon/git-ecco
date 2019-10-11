package at.jku.isse.gitecco.translation.variantscomparison.util;



import at.jku.isse.ecco.service.EccoService;
import at.jku.isse.gitecco.core.git.GitCommitList;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import org.glassfish.grizzly.http.server.accesslog.FileAppender;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class CompareVariants{
    private List<String> fileTypes = new LinkedList<String>();


    public void compareVariant(File srcOriginal, File srcEcco) throws IOException {
        fileTypes.add("c");
        fileTypes.add("cpp");
        fileTypes.add("h");
        fileTypes.add("hpp");

        List<File> filesVariant = new LinkedList<File>();
        List<File> filesEcco = new LinkedList<File>();
        getFilesToProcess(srcOriginal, filesVariant);
        getFilesToProcess(srcEcco, filesEcco);
        String outputCSV = srcOriginal.getParentFile().getParentFile().getAbsolutePath();
        String fileStr = outputCSV+File.separator+srcOriginal.getName()+".csv";
               File fAppend = new File(fileStr);
        FileAppender  csvWriter = new  FileAppender(fAppend);

        List<List<String>> headerRows = Arrays.asList(
                Arrays.asList("fileName","matchFile","truepositiveLines", "falsepositiveLines","falsenegativeLines", "originaltotalLines", "eccototalLines")
        );
        for (List<String> rowData : headerRows) {
            csvWriter.append("\n");
            csvWriter.append(String.join(",", rowData));
        }


        for (File f : filesVariant) {

            Boolean fileExistsInEcco = false;
            Integer truepositiveLines=0, falsepositiveLines=0,falsenegativeLines=0, originaltotalLines=0, eccototalLines=0;
            Boolean matchFiles = false;
            List<String> original =  original = Files.readAllLines(f.toPath());
            List<String> revised = null;
            //compare text files
            for (File fEcco : filesEcco) {
                if(f.getName().equals(fEcco.getName())){
                    try {
                        revised = Files.readAllLines(fEcco.toPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Compute diff. Get the Patch object. Patch is the container for computed deltas.
                    Patch<String> patch = null;
                    patch = DiffUtils.diff(original, revised);

                    if(patch.getDeltas().size()==0){
                        //files match
                        matchFiles = true;
                    }else {
                        matchFiles = false;
                        for (Delta delta : patch.getDeltas()) {
                            Integer difLines = Math.abs(delta.getOriginal().getLines().size() - delta.getRevised().getLines().size());
                            //List<String> unifiedDiff = DiffUtils.generateUnifiedDiff(f.getName(), fEcco.getName(), original, patch, original.size());
                            System.out.println(delta.getType().toString());
                            if(delta.getType().toString().equals("INSERT"))
                               falsepositiveLines += difLines;
                            else
                                falsenegativeLines += difLines;
                        }
                    }
                    eccototalLines = (revised.size()-1);
                    originaltotalLines = original.size()-1;
                    truepositiveLines = eccototalLines - (falsenegativeLines);

                    List<List<String>> resultRows = Arrays.asList(
                            Arrays.asList( f.getName(),matchFiles.toString(),truepositiveLines.toString(), falsepositiveLines.toString(), falsenegativeLines.toString(), originaltotalLines.toString(), eccototalLines.toString())
                    );
                    for (List<String> rowData : resultRows) {
                        csvWriter.append(String.join(",", rowData));
                    }
                    fileExistsInEcco = true;
                }
            }
            if(!fileExistsInEcco){
                List<List<String>> resultRows = Arrays.asList(
                        Arrays.asList(f.getName(),"not",  "0", "0", originaltotalLines.toString(), originaltotalLines.toString(), eccototalLines.toString())
                );
                for (List<String> rowData : resultRows) {
                    csvWriter.append(String.join(",", rowData));
                }
            }
        }

        //csvWriter.flush();
        csvWriter.close();
    }


    private void getFilesToProcess(File f, List<File> files) {
        if (f.isDirectory()) {
            for (File file : f.listFiles()) {
                getFilesToProcess(file, files);
            }
        } else if (f.isFile()) {
            //for (String ext : this.fileTypes) {
            //    if (f.getName().endsWith("." + ext)) {
                    files.add(f);
            //    }
            //}
        }
    }


    public void eccoCheckout(ArrayList<String> configsToCheckout, Path OUTPUT_DIR, File eccoFolder, File checkoutFolder) throws IOException {

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
            String fileStr = outputCSV + File.separator + config + ".csv";
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
                            listRuntimeData.add(String.valueOf(runtimeEccoCheckout));
                        } else {
                            listRuntimeData.add(String.valueOf(data[i]));
                        }
                    }
                }
            }
            csvReader.close();
            File fwriter = new File(fileStr);
            FileWriter csvWriter = new FileWriter(fwriter);
            csvWriter.append(String.join(",", listHeader));
            csvWriter.append("\n");
            csvWriter.append(String.join(",", listRuntimeData));
            csvWriter.flush();
            csvWriter.close();
        }
        //end checkout

        //close ecco repository
        service.close();

        //compare variant with ecco checkout
        for (File path : eccoFolder.listFiles()) {
            CompareVariants cV = new CompareVariants();
            cV.compareVariant(path, new File(checkoutFolder + File.separator + path.getName()));
        }

    }

}

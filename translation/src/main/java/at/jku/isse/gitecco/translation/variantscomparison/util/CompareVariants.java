package at.jku.isse.gitecco.translation.variantscomparison.util;



import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import org.glassfish.grizzly.http.server.accesslog.FileAppender;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
                Arrays.asList("fileName","matchFile","difLines", "totalLines")
        );
        for (List<String> rowData : headerRows) {
            csvWriter.append("\n");
            csvWriter.append(String.join(",", rowData));
        }


        for (File f : filesVariant) {
            Boolean fileExistsInEcco = false;
            Integer accLineDiff = 0;
            Boolean matchFiles = false;
            //compare text files
            for (File fEcco : filesEcco) {
                if(f.getName().equals(fEcco.getName())){
                    List<String> original = null;
                    List<String> revised = null;
                    try {
                        original = Files.readAllLines(f.toPath());
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
                            accLineDiff += difLines;
                        }
                    }
                    List<List<String>> resultRows = Arrays.asList(
                            Arrays.asList( f.getName(),matchFiles.toString(),accLineDiff.toString(),Integer.toString(original.size()-1))
                    );
                    for (List<String> rowData : resultRows) {
                        csvWriter.append(String.join(",", rowData));
                    }
                    fileExistsInEcco = true;
                }
            }
            if(!fileExistsInEcco){
                List<List<String>> resultRows = Arrays.asList(
                        Arrays.asList(f.getName(),"not","not")
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
            for (String ext : this.fileTypes) {
                if (f.getName().endsWith("." + ext)) {
                    files.add(f);
                }
            }
        }
    }


}

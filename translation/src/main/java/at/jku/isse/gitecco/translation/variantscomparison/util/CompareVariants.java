package at.jku.isse.gitecco.translation.variantscomparison.util;


import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class CompareVariants{
    private List<String> fileTypes = new LinkedList<String>();


    public void compareVariant(File srcOriginal, File srcEcco) {
        fileTypes.add("c");
        fileTypes.add("cpp");
        fileTypes.add("h");
        fileTypes.add("hpp");
        List<File> filesVariant = new LinkedList<File>();
        List<File> filesEcco = new LinkedList<File>();
        getFilesToProcess(srcOriginal, filesVariant);
        getFilesToProcess(srcOriginal, filesEcco);


        for (File f : filesVariant) {
            System.out.println(f.getName());
        }
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

package at.jku.isse.gitecco.featureid.test;

import at.jku.isse.gitecco.core.git.GitCommitList;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.featureid.identification.ID;
import at.jku.isse.gitecco.featureid.type.TraceableFeature;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static java.lang.Thread.sleep;

public class FeatureIdTest {

    public final String resultsCSVs_path = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\systems\\feature_identification\\bison";
    String[] features = {"BASE", "TRACE", "DEBUG", "MSDOS", "eta10", "__GO32__", "DONTDEF", "VMS", "HAVE_ALLOCA_H", "__GNUC__", "_AIX", "__STDC__", "HAVE_STDLIB_H", "HAVE_MEMORY_H", "STDC_HEADERS"};
    private final static ArrayList<String> featureList = new ArrayList<>();

    public void addFeatures(){
        for (String feat : features) {
            featureList.add(feat);
        }
    }

    @Test
    public void getFeatureDeletedTimes() throws IOException {
        addFeatures();
        File folder = new File(resultsCSVs_path);
        File[] lista = folder.listFiles();
        int deletedTimes = 0;
        Map<Integer, Integer> deletePerGitCommit = new HashMap<>();
        for (File file : lista) {
            if (featureList.contains(file.getName().substring(0, file.getName().indexOf(".csv")))) {
                Reader reader = Files.newBufferedReader(Paths.get(file.getAbsolutePath()));
                CSVReader csvReader = new CSVReaderBuilder(reader).build();
                List<String[]> lines = csvReader.readAll();
                int count = 0;
                for (int i = 1; i < lines.size(); i++) {
                    String[] line = lines.get(i);
                    String[] split = line[0].split(";");
                    if (i == 1) {
                        int first = Integer.valueOf(split[0]);
                        count = first;
                    } else {
                        int commitNumber = Integer.valueOf(split[0]);
                        if (commitNumber != (count + 1)) {
                            deletedTimes++;
                            Integer alreadyExist = deletePerGitCommit.computeIfPresent(commitNumber, (k, v) -> v + 1);
                            if(alreadyExist!=1)
                                deletePerGitCommit.put(commitNumber,1);
                        }
                        count = commitNumber;
                    }

                }
                //RQ.2 How many times one feature were deleted along a number of Git commits?
                System.out.println("Feature: " + file.getName() + " deleted times: " + deletedTimes);
                deletedTimes = 0;
            }
        }
        //RQ.2 How many features were deleted per Git commit?
        for (Map.Entry<Integer, Integer> commit:deletePerGitCommit.entrySet() ) {
            System.out.println("Commit Number: "+commit.getKey()+" features deleted: "+commit.getValue());
        }
    }

}

package at.jku.isse.gitecco.translation.test;

import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.translation.mining.ChangeCharacteristic;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.junit.Test;
import scala.Int;
import scala.util.parsing.combinator.testing.Str;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MiningStatistics {

    private final String releasePath = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\Ready\\Bison\\Mining";
    //private final String releasePath = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\Ready\\SQLite";
    //private final String releasePath = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\Ready\\LibSSH\\Mining";
    //private final String releasePath = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\Ready\\Irssi\\Mining\\Mining";
    //private final String releasePath = "D:\\Mining\\Test_statistics";


    @Test
    public void featuresPerCommit() throws IOException {
        File[] releasesDirectories = new File(releasePath).listFiles(File::isDirectory);

        Map<Integer, Integer> featPerCommit = new HashMap<>();

        for (File directory : releasesDirectories) {
            if (directory == null) {
                System.out.println("Either dir does not exist or is not a directory");
            } else {
                File featuresFolder = new File(directory, "FeatureCharacteristic");
                File[] files = featuresFolder.listFiles();


                for (File f : files) {
                    //System.out.println(directory.getName() + " file " + f.getName());
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    String line = "";
                    Boolean firstLine = true;
                    while ((line = br.readLine()) != null) {
                        if (firstLine) {
                            firstLine = false;
                        } else {
                            String[] cols = line.split(",");
                            //Commit Nr,LOC,SD IF,SD NIF,SD File,TD IF,ND IFs,NOTLB,NONTLB
                            if (featPerCommit.get(Integer.valueOf(cols[0])) == null) {
                                featPerCommit.put(Integer.valueOf(cols[0]), 1);
                            } else {
                                featPerCommit.put(Integer.valueOf(cols[0]), featPerCommit.get(Integer.valueOf(cols[0])) + 1);
                            }
                        }
                    }
                }
            }
        }
        printMaps(featPerCommit);
    }


    @Test
    public void featuresPerRelease() throws IOException {
        File[] releasesDirectories = new File(releasePath).listFiles();
        //Label/FeatureName	_total	_external	_internal	_transient
        Map<String, Integer> nrFeaturePerRelease = new HashMap<>();

        for (File directory : releasesDirectories) {
            if (directory == null) {
                System.out.println("Either dir does not exist or is not a directory");
            } else if (directory.getName().contains(".csv")) {
                BufferedReader br = new BufferedReader(new FileReader(directory));
                String line = "";
                Boolean firstLine = true;
                int count = 0;
                while ((line = br.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;
                    } else {
                        String[] cols = line.split(",");
                        if (cols[1].equals(cols[2])) {
                            count++;
                        }
                    }
                }
                nrFeaturePerRelease.put(directory.getName(), count);
            }
        }
        for (Map.Entry<String, Integer> featPerRelease : nrFeaturePerRelease.entrySet()) {
            System.out.println(featPerRelease.getKey().substring(0, featPerRelease.getKey().indexOf(".csv")) + "," + featPerRelease.getValue());
        }
    }


    @Test
    public void kruskalwallisdata() throws IOException {
        File[] releasesDirectories = new File(releasePath).listFiles(File::isDirectory);
        ArrayList<String> firstRelease = new ArrayList<>();
        String l = "";
        File directory = releasesDirectories[0];
        if (directory == null) {
            System.out.println("Either dir does not exist or is not a directory");
        } else {
            File featuresFolder = new File(directory, "FeatureCharacteristic");
            File[] files = featuresFolder.listFiles();

            for (File f : files) {
                //System.out.println(directory.getName() + " file " + f.getName());
                BufferedReader br = new BufferedReader(new FileReader(f));
                String line = "";
                Boolean firstLine = true;
                while ((line = br.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;
                    } else {
                        l = line;
                        firstRelease.add(f.getName().substring(0, f.getName().indexOf(".csv")) + "," + l);
                    }
                }
            }
        }

        for (String features : firstRelease) {
            System.out.println(features);
        }

    }


    @Test
    public void mannWhitneydata() throws IOException {
        File[] releasesDirectories = new File(releasePath).listFiles(File::isDirectory);
        Map<String, String> firstRelease = new HashMap<>();
        Map<String, String> lastRelease = new HashMap<>();
        String l = "";
        for (File directory : releasesDirectories) {
            if (directory == null) {
                System.out.println("Either dir does not exist or is not a directory");
            } else {
                File featuresFolder = new File(directory, "FeatureCharacteristic");
                File[] files = featuresFolder.listFiles();

                for (File f : files) {
                    if (firstRelease.get(f.getName().substring(0, f.getName().indexOf(".csv"))) == null) {
                        //System.out.println(directory.getName() + " file " + f.getName());
                        BufferedReader br = new BufferedReader(new FileReader(f));
                        String line = "";
                        Boolean firstLine = true;
                        while ((line = br.readLine()) != null) {
                            if (firstLine) {
                                firstLine = false;
                            } else {
                                l = line;
                            }
                        }
                        firstRelease.put(f.getName().substring(0, f.getName().indexOf(".csv")), l);
                    } else {
                        BufferedReader br = new BufferedReader(new FileReader(f));
                        String line = "";
                        Boolean firstLine = true;
                        while ((line = br.readLine()) != null) {
                            if (firstLine) {
                                firstLine = false;
                            } else {
                                l = line;
                            }
                        }
                        lastRelease.put(f.getName().substring(0, f.getName().indexOf(".csv")), l);
                    }
                }
            }
        }
        String lines = "";
        for (Map.Entry<String, String> map : firstRelease.entrySet()) {
            if (lastRelease.containsKey(map.getKey())) {
                lines += map.getKey() + "," + map.getValue() + "," + lastRelease.get(map.getKey()) + "\n";
            }
        }
        System.out.println(lines);
    }


    @Test
    public void percentOfFeaturesOnChanges() throws IOException {
        File[] releasesDirectories = new File(releasePath).listFiles(File::isDirectory);
        int nrTotalCommits = 0;
        int nrCommitsFeaturesChanged = 0;
        int nrCommitsBaseChanged = 0;
        int nrCommitsFeaturesANDBaseChanged = 0;

        for (File directory : releasesDirectories) {
            if (directory == null) {
                System.out.println("Either dir does not exist or is not a directory");
            } else {
                File[] files = directory.listFiles(new FilenameFilter() {

                    @Override
                    public boolean accept(File dir, String name) {
                        if (name.toLowerCase().equals("configurations.csv")) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                });


                for (File f : files) {
                    //System.out.println(directory.getName() + " file" + f.getName());
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    String line = "";
                    Boolean first = true;
                    String lastcommit = "";
                    String baseVersion = "";
                    Boolean basec = false, baseandfc = false, fc = false;
                    while ((line = br.readLine()) != null) {
                        // use comma as separator
                        String[] cols = line.split(",");
                        if (first) {
                            lastcommit = cols[0];
                            first = false;
                            nrTotalCommits++;
                        } else if (!lastcommit.equals(cols[0])) {
                            lastcommit = cols[0];
                            nrTotalCommits++;
                            if (baseandfc && !basec && !fc) {
                                nrCommitsFeaturesANDBaseChanged++;
                            } else if (basec && !fc && !baseandfc) {
                                nrCommitsBaseChanged++;
                            } else if (fc && !basec && !baseandfc) {
                                nrCommitsFeaturesChanged++;
                            } else
                                nrCommitsBaseChanged++;
                            basec = false;
                            fc = false;
                            baseandfc = false;
                        }
                        if (cols.length > 3) {
                            for (String col : cols) {
                                if (col.contains("BASE.")) {
                                    String[] version = col.split("\\.");
                                    if (version.length > 1) {
                                        if (!baseVersion.equals(version[1])) {
                                            baseVersion = version[1];
                                            baseandfc = true;
                                        } else {
                                            fc = true;
                                        }
                                    } else {
                                        System.out.println(directory.getName());
                                        fc = true;
                                    }
                                }
                            }
                        } else {
                            String aux = cols[2];
                            String[] version = aux.split("\\.");
                            if (version.length == 1) {
                                basec = true;
                                //System.out.println(aux);
                            } else if (!baseVersion.equals(version[1])) {
                                baseVersion = version[1];
                                basec = true;
                            }
                        }
                    }
                    if (baseandfc && !basec && !fc) {
                        nrCommitsFeaturesANDBaseChanged++;
                    } else if (basec && !fc && !baseandfc) {
                        nrCommitsBaseChanged++;
                    } else if (fc && !basec && !baseandfc) {
                        nrCommitsFeaturesChanged++;
                    } else {
                        nrCommitsBaseChanged++;
                    }
                }

            }
        }
        System.out.println("TotalCommits," + nrTotalCommits + "\nOnlyBase," + nrCommitsBaseChanged + "\nOnlyFeatures," + nrCommitsFeaturesChanged + "\nBase e Features," + nrCommitsFeaturesANDBaseChanged);


    }


    @Test
    public void NewDeletedAndChangedFeatures() throws IOException {
        File[] releasesDirectories = new File(releasePath).listFiles(File::isDirectory);
        int nrTotalCommits = 0;
        int nrCommitsFeaturesChanged = 0;
        int nrCommitsFeaturesAdded = 0;
        int nrCommitsFeaturesDeleted = 0;

        for (File directory : releasesDirectories) {
            int nrTotalCommitsRelease = 0;
            int nrCommitsFeaturesChangedRelease = 0;
            int nrCommitsFeaturesAddedRelease = 0;
            int nrCommitsFeaturesDeletedRelease = 0;
            if (directory == null) {
                System.out.println("Either dir does not exist or is not a directory");
            } else {
                File[] files = directory.listFiles(new FilenameFilter() {

                    @Override
                    public boolean accept(File dir, String name) {
                        if (name.toLowerCase().equals("features_report_each_project_commit.csv")) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                });

                File[] deletedFeat = directory.listFiles(new FilenameFilter() {

                    @Override
                    public boolean accept(File dir, String name) {
                        if (name.equals("delFeatsGitCommit.csv")) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
                String line = "";
                for (File f : files) {
                    Boolean firstLine = true;
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    while ((line = br.readLine()) != null) {
                        if (firstLine) {
                            firstLine = false;
                        } else {
                            // use comma as separator
                            String[] cols = line.split(",");
                            nrTotalCommits++;
                            nrTotalCommitsRelease++;
                            nrCommitsFeaturesAdded += Integer.valueOf(cols[1]);
                            nrCommitsFeaturesAddedRelease += Integer.valueOf(cols[1]);
                            nrCommitsFeaturesChanged += Integer.valueOf(cols[2]);
                            nrCommitsFeaturesChangedRelease += Integer.valueOf(cols[2]);

                        }
                    }

                }
                for (File f : deletedFeat) {
                    //System.out.println(" file " + f.getName());
                    Boolean firstLine = true;
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    while ((line = br.readLine()) != null) {
                        if (firstLine) {
                            firstLine = false;
                        } else {
                            // use comma as separator
                            String[] cols = line.split(",");
                            nrCommitsFeaturesDeleted += Integer.valueOf(cols[1]);
                            nrCommitsFeaturesDeletedRelease += Integer.valueOf(cols[1]);
                        }
                    }
                }
            }
            System.out.println(directory.getName() + "," + nrTotalCommitsRelease + "," + nrCommitsFeaturesAddedRelease + "," + nrCommitsFeaturesChangedRelease + "," + nrCommitsFeaturesDeletedRelease);
        }
        System.out.println("TotalCommits," + nrTotalCommits + "\nNewFeatures," + nrCommitsFeaturesAdded + "\nChangedFeatures," + nrCommitsFeaturesChanged + "\nDeletedFeatures," + nrCommitsFeaturesDeleted);
    }

    @Test
    public void featuresCharacteristics() throws IOException {
        File[] releasesDirectories = new File(releasePath).listFiles(File::isDirectory);

        Map<Integer, Integer> sdif = new HashMap<>();
        Map<Integer, Integer> sdnif = new HashMap<>();
        Map<Integer, Integer> sdfile = new HashMap<>();
        Map<Integer, Integer> tdif = new HashMap<>();
        Map<Integer, Integer> ndif = new HashMap<>();
        Map<Integer, Integer> notlb = new HashMap<>();
        Map<Integer, Integer> nontlb = new HashMap<>();

        for (File directory : releasesDirectories) {
            if (directory == null) {
                System.out.println("Either dir does not exist or is not a directory");
            } else {
                File featuresFolder = new File(directory, "FeatureCharacteristic");
                File[] files = featuresFolder.listFiles();


                for (File f : files) {
                    if (!f.getName().equals("BASE.csv")) {
                        //System.out.println(directory.getName() + " file " + f.getName());
                        BufferedReader br = new BufferedReader(new FileReader(f));
                        String line = "";
                        Boolean firstLine = true;
                        while ((line = br.readLine()) != null) {
                            if (firstLine) {
                                firstLine = false;
                            } else {
                                String[] cols = line.split(",");
                                //Commit Nr,LOC,SD IF,SD NIF,SD File,TD IF,ND IFs,NOTLB,NONTLB
                                if (sdif.get(Integer.valueOf(cols[2])) == null) {
                                    sdif.put(Integer.valueOf(cols[2]), 1);
                                } else {
                                    //if (Integer.valueOf(cols[2]) ==0)
                                    //    System.out.println(cols[0]+f.getName());
                                    sdif.put(Integer.valueOf(cols[2]), sdif.get(Integer.valueOf(cols[2])) + 1);
                                }
                                if (sdnif.get(Integer.valueOf(cols[3])) == null) {
                                    sdnif.put(Integer.valueOf(cols[3]), 1);
                                } else {
                                    //if (Integer.valueOf(cols[2]) ==0)
                                    //    System.out.println(cols[0]+f.getName());
                                    sdnif.put(Integer.valueOf(cols[3]), sdnif.get(Integer.valueOf(cols[3])) + 1);
                                }
                                if (sdfile.get(Integer.valueOf(cols[4])) == null) {
                                    sdfile.put(Integer.valueOf(cols[4]), 1);
                                } else {
                                    //if (Integer.valueOf(cols[2]) ==0)
                                    //    System.out.println(cols[0]+f.getName());
                                    sdfile.put(Integer.valueOf(cols[4]), sdfile.get(Integer.valueOf(cols[4])) + 1);
                                }
                                if (tdif.get(Integer.valueOf(cols[5])) == null) {
                                    tdif.put(Integer.valueOf(cols[5]), 1);
                                } else {
                                    //if (Integer.valueOf(cols[2]) ==0)
                                    //    System.out.println(cols[0]+f.getName());
                                    tdif.put(Integer.valueOf(cols[5]), tdif.get(Integer.valueOf(cols[5])) + 1);
                                }
                                if (ndif.get(Integer.valueOf(cols[6])) == null) {
                                    ndif.put(Integer.valueOf(cols[6]), 1);
                                } else {
                                    //if (Integer.valueOf(cols[2]) ==0)
                                    //    System.out.println(cols[0]+f.getName());
                                    ndif.put(Integer.valueOf(cols[6]), ndif.get(Integer.valueOf(cols[6])) + 1);
                                }
                                if (notlb.get(Integer.valueOf(cols[7])) == null) {
                                    notlb.put(Integer.valueOf(cols[7]), 1);
                                } else {
                                    //if (Integer.valueOf(cols[2]) ==0)
                                    //    System.out.println(cols[0]+f.getName());
                                    notlb.put(Integer.valueOf(cols[7]), notlb.get(Integer.valueOf(cols[7])) + 1);
                                }
                                if (nontlb.get(Integer.valueOf(cols[8])) == null) {
                                    nontlb.put(Integer.valueOf(cols[8]), 1);
                                } else {
                                    //if (Integer.valueOf(cols[2]) ==0)
                                    //    System.out.println(cols[0]+f.getName());
                                    nontlb.put(Integer.valueOf(cols[8]), nontlb.get(Integer.valueOf(cols[8])) + 1);
                                }
                            }
                        }
                    }
                }
            }
        }
        printMaps(sdif);
        System.out.println("-----------");
        printMaps(sdnif);
        System.out.println("-----------");
        printMaps(sdfile);
        System.out.println("-----------");
        printMaps(tdif);
        System.out.println("-----------");
        printMaps(ndif);
        System.out.println("-----------");
        printMaps(notlb);
        System.out.println("-----------");
        printMaps(nontlb);
    }


    @Test
    public void hotSpotfeaturesCharacteristics() throws IOException {
        File[] releasesDirectories = new File(releasePath).listFiles(File::isDirectory);

        String l = "";
        for (File directory : releasesDirectories) {
            if (directory == null) {
                System.out.println("Either dir does not exist or is not a directory");
            } else {
                File featuresFolder = new File(directory, "FeatureCharacteristic");
                File[] files = featuresFolder.listFiles();

                for (File f : files) {
                    if (f.getName().equals("HAVE_OPENSSL.csv")) {
                        //System.out.println(directory.getName() + " file " + f.getName());
                        BufferedReader br = new BufferedReader(new FileReader(f));
                        String line = "";
                        Boolean firstLine = true;
                        while ((line = br.readLine()) != null) {
                            if (firstLine) {
                                firstLine = false;
                            } else {
                                l += line + "\n";
                            }
                        }
                    }
                }
            }
        }
        System.out.println(l);

    }

    @Test
    public void firstandLastRelease() throws IOException {
        File[] releasesDirectories = new File(releasePath).listFiles(File::isDirectory);
        Map<String, String> firstRelease = new HashMap<>();
        Map<String, String> lastRelease = new HashMap<>();
        String l = "";
        for (File directory : releasesDirectories) {
            if (directory == null) {
                System.out.println("Either dir does not exist or is not a directory");
            } else if (directory.getName().contains("0.7.16")) {
                File featuresFolder = new File(directory, "FeatureCharacteristic");
                File[] files = featuresFolder.listFiles();

                for (File f : files) {
                    //System.out.println(directory.getName() + " file " + f.getName());
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    String line = "";
                    Boolean firstLine = true;
                    while ((line = br.readLine()) != null) {
                        if (firstLine) {
                            firstLine = false;
                        } else {
                            l = line;
                        }
                    }
                    firstRelease.put(f.getName().substring(0, f.getName().indexOf(".csv")), l);
                }
            } else if (directory.getName().contains("1.2.2")) {
                File featuresFolder = new File(directory, "FeatureCharacteristic");
                File[] files = featuresFolder.listFiles();

                for (File f : files) {
                    //System.out.println(directory.getName() + " file " + f.getName());
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    String line = "";
                    Boolean firstLine = true;
                    while ((line = br.readLine()) != null) {
                        if (firstLine) {
                            firstLine = false;
                        } else {
                            l = line;
                        }
                    }
                    lastRelease.put(f.getName().substring(0, f.getName().indexOf(".csv")), l);
                }
            }
        }
        String first = "";
        String last = "";
        for (Map.Entry<String, String> map : firstRelease.entrySet()) {
            if (lastRelease.containsKey(map.getKey())) {
                first += map.getKey() + "," + map.getValue() + "\n";
                last += map.getKey() + "," + lastRelease.get(map.getKey()) + "\n";
            }
        }
        System.out.println(first);
        System.out.println("!!!!!!");
        System.out.println(last);
    }


    @Test
    public void linesOfCodefeaturesCharacteristics() throws IOException {
        File[] releasesDirectories = new File(releasePath).listFiles(File::isDirectory);

        Map<String, Integer> locBase = new HashMap<>();
        Map<String, Integer> locFeatures = new HashMap<>();

        for (File directory : releasesDirectories) {
            if (directory == null) {
                System.out.println("Either dir does not exist or is not a directory");
            } else {
                File featuresFolder = new File(directory, "FeatureCharacteristic");
                File[] files = featuresFolder.listFiles();


                for (File f : files) {
                    if (!f.getName().equals("BASE.csv")) {
                        //System.out.println(directory.getName() + " file " + f.getName());
                        BufferedReader br = new BufferedReader(new FileReader(f));
                        String line = "";
                        Boolean firstLine = true;
                        while ((line = br.readLine()) != null) {
                            if (firstLine) {
                                firstLine = false;
                            } else {
                                String[] cols = line.split(",");
                                //Commit Nr,LOC,SD IF,SD NIF,SD File,TD IF,ND IFs,NOTLB,NONTLB
                                if (locFeatures.get(cols[0]) == null) {
                                    locFeatures.put(cols[0], Integer.valueOf(cols[1]));
                                } else {
                                    locFeatures.put(cols[0], locFeatures.get(cols[0]) + Integer.valueOf(cols[1]));
                                }
                            }
                        }
                    } else {
                        BufferedReader br = new BufferedReader(new FileReader(f));
                        String line = "";
                        Boolean firstLine = true;
                        while ((line = br.readLine()) != null) {
                            if (firstLine) {
                                firstLine = false;
                            } else {
                                String[] cols = line.split(",");
                                //Commit Nr,LOC,SD IF,SD NIF,SD File,TD IF,ND IFs,NOTLB,NONTLB
                                if (locBase.get(cols[0]) == null) {
                                    locBase.put(cols[0], Integer.valueOf(cols[1]));
                                } else {
                                    locBase.put(cols[0], locBase.get(cols[0]) + Integer.valueOf(cols[1]));
                                }
                            }
                        }
                    }
                }
            }
        }
        for (Map.Entry<String, Integer> map : locFeatures.entrySet()) {
            System.out.println(map.getKey() + "," + map.getValue() + "," + locBase.get(map.getKey()));
        }
    }


    @Test
    public void featuresChanges() throws IOException {
        File[] releasesDirectories = new File(releasePath).listFiles(File::isDirectory);
        //Commit Nr,LOC A,LOC R,SD IF,SD File,TD IF
        Map<Integer, Integer> sdif = new HashMap<>();
        Map<Integer, Integer> sdfile = new HashMap<>();
        Map<Integer, Integer> tdif = new HashMap<>();
        Map<Integer, Integer> locADD = new HashMap<>();
        Map<Integer, Integer> lofDel = new HashMap<>();

        for (File directory : releasesDirectories) {
            if (directory == null) {
                System.out.println("Either dir does not exist or is not a directory");
            } else {
                File featuresFolder = new File(directory, "ChangeCharacteristic");
                File[] files = featuresFolder.listFiles();


                for (File f : files) {
                    if (!f.getName().equals("BASE.csv")) {
                        //System.out.println(directory.getName() + " file " + f.getName());
                        BufferedReader br = new BufferedReader(new FileReader(f));
                        String line = "";
                        Boolean firstLine = true;
                        while ((line = br.readLine()) != null) {
                            if (firstLine) {
                                firstLine = false;
                            } else {
                                String[] cols = line.split(",");
                                //Commit Nr,LOC A,LOC R,SD IF,SD File,TD IF
                                if (sdif.get(Integer.valueOf(cols[3])) == null) {
                                    sdif.put(Integer.valueOf(cols[3]), 1);
                                } else {
                                    //if (Integer.valueOf(cols[2]) ==0)
                                    //    System.out.println(cols[0]+f.getName());
                                    sdif.put(Integer.valueOf(cols[3]), sdif.get(Integer.valueOf(cols[3])) + 1);
                                }
                                if (sdfile.get(Integer.valueOf(cols[4])) == null) {
                                    sdfile.put(Integer.valueOf(cols[4]), 1);
                                } else {
                                    //if (Integer.valueOf(cols[2]) ==0)
                                    //    System.out.println(cols[0]+f.getName());
                                    sdfile.put(Integer.valueOf(cols[4]), sdfile.get(Integer.valueOf(cols[4])) + 1);
                                }
                                if (tdif.get(Integer.valueOf(cols[5])) == null) {
                                    tdif.put(Integer.valueOf(cols[5]), 1);
                                } else {
                                    //if (Integer.valueOf(cols[2]) ==0)
                                    //    System.out.println(cols[0]+f.getName());
                                    tdif.put(Integer.valueOf(cols[5]), tdif.get(Integer.valueOf(cols[5])) + 1);
                                }
                                if (locADD.get(Integer.valueOf(cols[1])) == null) {
                                    locADD.put(Integer.valueOf(cols[1]), 1);
                                } else {
                                    if (Integer.valueOf(cols[1]) < 0)
                                        System.out.println(cols[0] + " " + f.getName() + " " + directory.getName());
                                    locADD.put(Integer.valueOf(cols[1]), locADD.get(Integer.valueOf(cols[1])) + 1);
                                }
                                if (lofDel.get(Integer.valueOf(cols[2])) == null) {
                                    lofDel.put(Integer.valueOf(cols[2]), 1);
                                } else {
                                    //if (Integer.valueOf(cols[2]) ==0)
                                    //    System.out.println(cols[0]+f.getName());
                                    lofDel.put(Integer.valueOf(cols[2]), lofDel.get(Integer.valueOf(cols[2])) + 1);
                                }
                            }
                        }
                    }
                }
            }
        }
        printMaps(sdif);
        System.out.println("-----------");
        printMaps(sdfile);
        System.out.println("-----------");
        printMaps(tdif);
        System.out.println("-----------");
        printMaps(locADD);
        System.out.println("-----------");
        printMaps(lofDel);
    }


    @Test
    public void featureMostComplexChange() throws IOException {
        File[] releasesDirectories = new File(releasePath).listFiles(File::isDirectory);
        //Commit Nr,LOC A,LOC R,SD IF,SD File,TD IF
        Integer sdif = 0;
        Integer tdif = 0;
        Integer sdfile = 0;
        Integer loca = 0;
        Integer locr = 0;
        String featureName = "";
        Integer commitNumber = 0;
        String release = "";

        for (File directory : releasesDirectories) {
            if (directory == null) {
                System.out.println("Either dir does not exist or is not a directory");
            } else {
                File featuresFolder = new File(directory, "ChangeCharacteristic");
                File[] files = featuresFolder.listFiles();


                for (File f : files) {
                    if (!f.getName().equals("BASE.csv")) {
                        //System.out.println(directory.getName() + " file " + f.getName());
                        BufferedReader br = new BufferedReader(new FileReader(f));
                        String line = "";
                        Boolean firstLine = true;
                        while ((line = br.readLine()) != null) {
                            if (firstLine) {
                                firstLine = false;
                            } else {
                                String[] cols = line.split(",");
                                //Commit Nr,LOC A,LOC R,SD IF,SD File,TD IF
                                if (tdif < Integer.valueOf(cols[5])) {
                                    sdif = Integer.valueOf(cols[3]);
                                    tdif = Integer.valueOf(cols[5]);
                                    sdfile = Integer.valueOf(cols[4]);
                                    ;
                                    loca = Integer.valueOf(cols[1]);
                                    ;
                                    locr = Integer.valueOf(cols[2]);
                                    featureName = f.getName();
                                    commitNumber = Integer.valueOf(cols[0]);
                                    release = directory.getName();
                                }
                            }
                        }
                    }
                }
            }
        }

        System.out.println(release + "," + featureName + "," + commitNumber + "," + loca + "," + locr + "," + sdif + "," + sdfile + "," + tdif);
    }


    @Test
    public void featureHotspotChanges() throws IOException {
        File[] releasesDirectories = new File(releasePath).listFiles(File::isDirectory);
        Map<at.jku.isse.gitecco.core.type.Feature, Integer> changeEachFeature = new HashMap<>();
        for (File directory : releasesDirectories) {
            if (directory == null) {
                System.out.println("Either dir does not exist or is not a directory");
            } else {
                File[] files = directory.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        if (name.equals("TimesEachFeatureChanged.txt")) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
                for (File f : files) {
                    //System.out.println(directory.getName() + " file " + f.getName());
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    String line = "";
                    // Boolean firstLine = true;
                    while ((line = br.readLine()) != null) {
                        //if (firstLine) {
                        //    firstLine = false;
                        //} else {
                        String[] cols = line.split(" Changed");
                        at.jku.isse.gitecco.core.type.Feature feat = new Feature(cols[0]);
                        if (changeEachFeature.get(feat) == null) {
                            changeEachFeature.put(feat, Integer.valueOf(cols[1].substring(1, cols[1].indexOf(" times"))));
                        } else {
                            changeEachFeature.put(feat, changeEachFeature.get(feat) + Integer.valueOf(cols[1].substring(1, cols[1].indexOf(" times"))));
                        }
                        //}
                    }
                }
            }
        }
        for (Map.Entry<Feature, Integer> map : changeEachFeature.entrySet()) {
            System.out.println(map.getKey().getName() + "," + map.getValue());
        }
    }


    @Test
    public void correlationChanges() throws IOException {
        File[] releasesDirectories = new File(releasePath).listFiles(File::isDirectory);
        //Commit Nr,LOC A,LOC R,SD IF,SD File,TD IF
        int countreleases1 = 0;
        int countreleases2 = 0;
        int countreleases3 = 0;
        int countreleases4 = 0;
        int countreleases5 = 0;
        int countreleases6 = 0;
        int countreleases7 = 0;
        String var1 = "";
        String var2 = "";
        String var3 = "";
        String var4 = "";
        String var5 = "";
        String var6 = "";
        String var7 = "";
        for (File directory : releasesDirectories) {
            if (directory == null) {
                System.out.println("Either dir does not exist or is not a directory");
            } else {
                File featuresFolder = new File(directory, "ChangeCharacteristic");
                File[] files = featuresFolder.listFiles();

                for (File f : files) {

                    if (f.getName().equals("__SIZE_TYPE__.csv") || f.getName().equals("YYPRINT.csv") || f.getName().equals("__cplusplus.csv") ||
                            f.getName().equals("__GNUC__.csv") || f.getName().equals("YYERROR_VERBOSE.csv") || f.getName().equals("YYDEBUG.csv") || f.getName().equals("BASE.csv")) {
                        if (f.getName().equals("__SIZE_TYPE__.csv")) {
                            countreleases1++;
                        } else if (f.getName().equals("YYPRINT.csv")) {
                            countreleases2++;
                        } else if (f.getName().equals("__cplusplus.csv")) {
                            countreleases3++;
                        } else if (f.getName().equals("__GNUC__.csv")) {
                            countreleases4++;
                        } else if (f.getName().equals("YYERROR_VERBOSE.csv")) {
                            countreleases5++;
                        } else if (f.getName().equals("YYDEBUG.csv")) {
                            countreleases6++;
                        } else if (f.getName().equals("BASE.csv")) {
                            countreleases7++;
                        }

                        BufferedReader br = new BufferedReader(new FileReader(f));
                        String line = "";
                        Boolean firstLine = true;
                        Boolean secondLine = true;
                        String commitcomparewithlastrelease = "";
                        while ((line = br.readLine()) != null) {
                            if (firstLine) {
                                firstLine = false;
                            } else if (secondLine) {
                                String[] cols = line.split(",");
                                commitcomparewithlastrelease = cols[0];
                                secondLine = false;
                            } else {
                                String[] cols = line.split(",");
                                //Commit Nr,LOC A,LOC R,SD IF,SD File,TD IF
                                if (!commitcomparewithlastrelease.equals(cols[0]))
                                    if (f.getName().equals("__SIZE_TYPE__.csv")) {
                                        var1 = var1 + line + "\n";
                                    } else if (f.getName().equals("YYPRINT.csv")) {
                                        var2 = var2 + line + "\n";
                                    } else if (f.getName().equals("__cplusplus.csv")) {
                                        var3 = var3 + line + "\n";
                                    } else if (f.getName().equals("__GNUC__.csv")) {
                                        var4 = var4 + line + "\n";
                                    } else if (f.getName().equals("YYERROR_VERBOSE.csv")) {
                                        var5 = var5 + line + "\n";
                                    } else if (f.getName().equals("YYDEBUG.csv")) {
                                        var6 = var6 + line + "\n";
                                    } else if (f.getName().equals("BASE.csv")) {
                                        var7 = var7 + line + "\n";
                                    }
                                //System.out.println(cols[0]+","+cols[1]+","+cols[2]+","+cols[3]+","+cols[4]+","+cols[5]);
                            }
                        }
                    }
                }
            }
        }
        System.out.println("VAR 1: releases " + countreleases1 + "\n\n" + var1 +
                "VAR 2: releases " + countreleases2 + "\n\n" + var2 +
                "VAR 3: releases " + countreleases3 + "\n\n" + var3 +
                "VAR 4: releases " + countreleases4 + "\n\n" + var4 +
                "VAR 5: releases " + countreleases5 + "\n\n" + var5 +
                "VAR 6: releases " + countreleases6 + "\n\n" + var6 +
                "VAR 7: releases " + countreleases7 + "\n\n" + var7);
    }


    public void printMaps(Map<Integer, Integer> mapToPrint) {
        for (Map.Entry<Integer, Integer> map : mapToPrint.entrySet()) {
            System.out.println(map.getKey() + "," + map.getValue());
        }
    }

}
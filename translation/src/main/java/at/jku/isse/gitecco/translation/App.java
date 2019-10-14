package at.jku.isse.gitecco.translation;

import at.jku.isse.ecco.service.EccoService;
import at.jku.isse.gitecco.core.git.Change;
import at.jku.isse.gitecco.core.git.GitCommitList;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.tree.nodes.BaseNode;
import at.jku.isse.gitecco.core.tree.nodes.ConditionalNode;
import at.jku.isse.gitecco.core.tree.nodes.FileNode;
import at.jku.isse.gitecco.core.tree.nodes.SourceFileNode;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.translation.constraintcomputation.util.ConstraintComputer;
import at.jku.isse.gitecco.translation.visitor.GetNodesForChangeVisitor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.glassfish.grizzly.http.server.accesslog.FileAppender;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class App {


    public static void main(String... args) throws Exception {
        final int MAXCOMMITS = 200;
        //TODO: planned arguments: DEBUG, dispose tree, max commits, repo path, csv path(feature id), outpath for ecco
        //String repoPath = "C:\\Users\\gabil\\Desktop\\ECCO_Work\\test-featureid";
        //String repoPath = "C:\\Users\\gabil\\Desktop\\ECCO_Work\\TestMarlin\\Marlin\\Marlin\\Marlin";
        String repoPath = "C:\\obermanndavid\\git-ecco-test\\2_second_run\\Marlin";
        //String repoPath = "C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\sqllite\\sqlite";

        //optional features of the project obtained by the featureID (chosen that which is in almost cases external feature)
        //MARLIN
       String[] featuresToAdd = {"BASE", "__AVR_ATmega644P__", "F_FILE_DIR_DIRTY", "F_UNUSED", "F_FILE_UNBUFFERED_READ", "RAMPS_V_1_0", "__AVR_ATmega2560__", "F_CPU", "F_OFLAG", "WATCHPERIOD",
                "THERMISTORHEATER", "THERMISTORBED", "TSd2PinMap_hHERMISTORHEATER", "PID_DEBUG", "HEATER_USES_THERMISTOR", "__AVR_ATmega328P__", "__AVR_ATmega1280__", "__AVR_ATmega168__",
                "ADVANCE", "PID_OPENLOOP", "SDSUPPORT", "BED_USES_THERMISTOR", "SIMPLE_LCD", "NEWPANEL", "DEBUG_STEPS", "BED_USES_AD595", "ARDUINO",
                "HEATER_1_USES_THERMISTOR", "THERMISTORHEATER_1", "HEATER_USES_THERMISTOR_1", "HEATER_2_USES_AD595", "HEATER_1_MAXTEMP", "THERMISTORHEATER_0",
                "HEATER_1_MINTEMP", "HEATER_0_USES_THERMISTOR", "RESET_MANUAL", "PID_PID"};
       //LIBSSH
         /*String[] featuresToAdd = {"BASE", "WITH_SERVER", "HAVE_LIBZ", "WORDS_BIGENDIAN", "DEBUG_CRYPTO",
                "HAVE_OPENSSL_AES_H","HAVE_GETHOSTBYNAME", "OPENSSL_VERSION_NUMBER","HAVE_SYS_POLL_H",
                "HAVE_OPENSSL_BLOWFISH_H", "HAVE_SYS_TIME_H", "HAVE_POLL", "HAVE_SELECT", "HAVE_GETHOSTBYADDR",
                "__cplusplus", "HAVE_SSH1", "NO_SERVER", "HAVE_PTY_H", "HAVE_STDINT_H", "HAVE_MEMORY_H", "HAVE_LIBWSOCK32",
                "HAVE_GETPWUID", "DEBUG", "HAVE_ERRNO_H", "HAVE_CTYPE_H", "HAVE_NETINET_IN_H", "__CYGWIN_","HAVE_STRSEP",
                "HAVE_GETUID", "HAVE_STDIO_H", "HAVE_CONFIG_H","HAVE_STRING_H","HAVE_ARPA_INET_H","HAVE_STRINGS_H",
                "HAVE_SYS_SOCKET_H", "HAVE_SYS_TYPES_H","HAVE_STRTOLL","HAVE_PWD_H","HAVE_FCNTL_H","HAVE_OPENNET_H",
                "TIME_WITH_SYS_TIME","HAVE_DIRENT_H","HAVE_NETDB_H","__WIN32__","HAVE_INTTYPES_H","HAVE_LIBOPENNET",
                "HAVE_SYS_STAT_H","__MINGW32__", "GCRYPT", "HAVE_LIBCRYPTO", "HAVE_PAM_PAM_APPL_H", "HAVE_LIBCRYPT", "HAVE_OPENSSL_DES_H",
                "_WIN32", "_MSC_VER", "__GNUC__","EWOULDBLOCK","uid_t","gid_t", "WITH_LIBZ"};*/
        //SQLITE
        /*String[] featuresToAdd = {"BASE", "YYERRORSYMBOL", "TEST_COMPARE", "_WIN32", "WIN32", "TEST", "NDEBUG", "NO_READLINE", "TCLSH", "MEMORY_DEBUG", "HAVE_USLEEP", "HAVE_READLINE", "OS_WIN", "NO_TCL",
                "COMPATIBILITY", "etCOMPATIBILITY", "DEBUG", "__cplusplus", "__STDC__", "SIGINT", "BIG_ENDIAN", "DISABLE_GDBM", "SQLITE_TEST", "SQLITE_UTF8", "TCL_UTF_MAX", "USE_TCL_STUBS", "__CYGWIN__",
                "THREADSAFE", "__MINGW32__", "__BORLANDC__", "NDEEBUG", "NDEBUG2"};*/

        ArrayList<String> featureList = new ArrayList<>();

        for (String feat : featuresToAdd) {
            featureList.add(feat);
        }

        //add directories that we need to include manually to get all the files to create a clean version because "/usr/local/include"
        // and "/usr/include")does not includes files outside the root path
        final List<String> dirFiles = new ArrayList<>();
        //dirFiles.add("C:\\Users\\gabil\\Desktop\\ECCO_Work\\test-featureid\\Marlin");
        //dirFiles.add("C:\\Users\\gabil\\Desktop\\ECCO_Work\\TestMarlin\\Marlin\\Marlin\\Marlin");
        dirFiles.add("C:\\obermanndavid\\git-ecco-test\\2_second_run\\Marlin\\Marlin");
        /*dirFiles.add("C:\\obermanndavid\\git-ecco-test\\test_sqlite\\sqlite\\doc");
        dirFiles.add("C:\\obermanndavid\\git-ecco-test\\test_sqlite\\sqlite\\src");
        dirFiles.add("C:\\obermanndavid\\git-ecco-test\\test_sqlite\\sqlite\\test");
        dirFiles.add("C:\\obermanndavid\\git-ecco-test\\test_sqlite\\sqlite\\tool");
        dirFiles.add("C:\\obermanndavid\\git-ecco-test\\test_sqlite\\sqlite\\wwww");
        dirFiles.add("C:\\obermanndavid\\git-ecco-test\\test_sqlite\\sqlite\\art");
        dirFiles.add("C:\\obermanndavid\\git-ecco-test\\test_sqlite\\sqlite\\contrib");
        dirFiles.add("C:\\obermanndavid\\git-ecco-test\\test_sqlite\\sqlite\\ext");
        dirFiles.add("C:\\obermanndavid\\git-ecco-test\\test_sqlite\\sqlite");*/
        final GitHelper gitHelper = new GitHelper(repoPath, dirFiles);
        final GitCommitList commitList = new GitCommitList(gitHelper);

        //creating ecco repository for each commit
        //set this path to where the results should be stored
        final Path OUTPUT_DIR = Paths.get("C:\\obermanndavid\\git-ecco-test\\2_second_run\\variant_results");
        if (OUTPUT_DIR.resolve("repo").toFile().exists()) GitCommitList.recursiveDelete(OUTPUT_DIR.resolve("repo"));
        EccoService service = new EccoService();
        service.setRepositoryDir(OUTPUT_DIR.resolve("repo"));
        //initializing repo
        service.init();
        System.out.println("Repository initialized.");

        final File gitFolder = new File(gitHelper.getPath());
        final File eccoFolder = new File(gitFolder.getParent(), "ecco");
        final File checkoutFolder = new File("C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\libssh-mirror\\variant_results\\checkout\\");
        final File destGitCommitAndCheckout = new  File ("C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\libssh-mirror\\variant_results\\gitCommit");
        ArrayList<String> configsToCheckout = new ArrayList<>();

        Map<Feature, Integer> featureVersions = new HashMap<>();
        final Integer[] countFeaturesChanged = {0}; //COUNT PER GIT COMMIT
        final Integer[] newFeatures = {0}; //COUNT PER GIT COMMIT

        File gitRepositoryFolder = new File(gitHelper.getPath());
        File eccoVariantsFolder = new File(gitRepositoryFolder.getParent(), "ecco");
        if (eccoVariantsFolder.exists()) GitCommitList.recursiveDelete(eccoVariantsFolder.toPath());

        String fileReportFeature = "features_report_each_project_commit.csv";
        //csv to report new features and features changed per git commit of the project
        /*try {
            FileWriter csvWriter = new FileWriter(gitRepositoryFolder.getParent() + File.separator +fileReportFeature);
            List<List<String>> headerRows = Arrays.asList(
                    Arrays.asList( "CommitNumber", "NewFeatures", "ChangedFeatures")
            );
            for (List<String> rowData : headerRows) {
                csvWriter.append(String.join(",", rowData));
                csvWriter.append("\n");
            }
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        // end csv to report new features and features changed

        String fileStoreConfig = "configurations.csv";
        //csv to save the configurations to
        /*try {
            FileWriter csvWriter = new FileWriter(gitRepositoryFolder.getParent() + File.separator +fileStoreConfig);
            List<List<String>> headerRows = Arrays.asList(
                    Arrays.asList( "CommitNumber", "Config")
            );
            for (List<String> rowData : headerRows) {
                csvWriter.append(String.join(",", rowData));
                csvWriter.append("\n");
            }
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        //creating runtime csv
        /*try {
            String fileStr = gitRepositoryFolder.getParent() + File.separator +"runtime.csv";
            FileWriter csvWriter = new FileWriter(fileStr);
            List<List<String>> headerRows = Arrays.asList(
                    Arrays.asList("CommitNr","configuration","runtimeEccoCommit", "runtimeEccoCheckout", "runtimeCleanVersionPP", "runtimeGenerateVariantPP", "runtimeGitCommit", "runtimeGitCheckout")
            );
            for (List<String> rowData : headerRows) {
                csvWriter.append(String.join(",", rowData));
                csvWriter.append("\n");
            }
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        //end runtime csv

        commitList.addGitCommitListener((gc, gcl) -> {
            if(gcl.size() >= MAXCOMMITS) System.exit(0);
            List<String> configurations = new ArrayList<>();
            System.out.println(gc.getCommitName() + ":");

            GetNodesForChangeVisitor visitor = new GetNodesForChangeVisitor();
            Set<ConditionalNode> changedNodes = new HashSet<>();
            List<String> changedFiles = gitHelper.getChangedFiles(gc);

            //retrieve changed nodes
            for (FileNode child : gc.getTree().getChildren()) {
                if (child instanceof SourceFileNode) {
                    Change[] changes = null;
                    try {
                        changes = gitHelper.getFileDiffs(gc, child);
                    } catch (Exception e) {
                        System.err.println("error while executing the file diff: " + child.getFilePath());
                        e.printStackTrace();
                    }

                    for (Change change : changes) {
                        visitor.setChange(change);
                        child.accept(visitor);
                        changedNodes.addAll(visitor.getchangedNodes());
                    }
                }
            }


            final ConstraintComputer constraintComputer = new ConstraintComputer(featureList);
            final PreprocessorHelper pph = new PreprocessorHelper();
            Map<Feature, Integer> config;
            Set<Feature> changed;
            Set<Feature> alreadyComitted = new HashSet<>();

            //if there is no changed node then there must be a change in the binary files --> commit base.
            if(changedNodes.isEmpty() && gcl.size() > 1) changedNodes.add(new BaseNode(null, 0));

            //changedNodes = changedNodes.stream().filter(x -> x.getLocalCondition().equals("__AVR_ATmega644P__ || __AVR_ATmega644__")).collect(Collectors.toSet());
            for (ConditionalNode changedNode : changedNodes) {
                Long runtimeEccoCommit, runtimeEccoCheckout = Long.valueOf(0), runtimePPCheckoutGenerateVariant, timeBefore, timeAfter;
                Long runtimeGitCommit = Long.valueOf(0), runtimeGitCheckout = commitList.getRuntimePPCheckoutCleanVersion(), runtimePPCheckoutCleanVersion = commitList.getRuntimePPCheckoutCleanVersion();
                //compute the config for the var gen
                config = constraintComputer.computeConfig(changedNode, gc.getTree());
                if (config != null && !config.isEmpty()) {
                    //compute the marked as changed features.
                    changed = constraintComputer.computeChangedFeatures(changedNode, config);

                    //map with the name of the feature and version and a boolean to set true when it is already incremented in the analysed commit
                    String eccoConfig = "";
                    for (Map.Entry<Feature, Integer> configFeature : config.entrySet()) {
                        int version = 0;
                        if (configFeature.getValue() != 0) {
                            if (featureVersions.containsKey(configFeature.getKey())) {
                                version = featureVersions.get(configFeature.getKey());
                            }
                            if (!alreadyComitted.contains(configFeature.getKey())) {
                                alreadyComitted.add(configFeature.getKey());
                                //for marlin: first commit is empty, thus we need the < 2. otherwise <1 should be enough.
                                if (gcl.size() < 2 || changed.contains(configFeature.getKey())) {
                                    version++;
                                    if(version==1)
                                        newFeatures[0]++;
                                    else
                                        countFeaturesChanged[0]++;
                                }
                                featureVersions.put(configFeature.getKey(), version);
                            }
                            eccoConfig += "," + configFeature.getKey().toString() + "." + version;
                        }
                    }
                    eccoConfig = eccoConfig.replaceFirst(",", "");

                    if (configurations.contains(eccoConfig)) {
                        System.out.println("Config already commited on ecco: " + eccoConfig);
                        //don't need to generate variant and commit it again at the same commit of the project git repository
                    } else {
                        timeBefore = System.currentTimeMillis();
                        //generate the variant for this config
                        //pph.generateVariants(config, gitFolder, eccoFolder, gitHelper.getDirFiles(), eccoConfig);
                        timeAfter = System.currentTimeMillis();
                        runtimePPCheckoutGenerateVariant = timeAfter - timeBefore;
                        runtimeGitCheckout += runtimePPCheckoutGenerateVariant;

                        configurations.add(eccoConfig);
                        //folder where the variant is stored
                        File variantsrc = new File(eccoFolder, eccoConfig);
                        String outputCSV = variantsrc.getParentFile().getParentFile().getAbsolutePath();
                        final Path variant_dir = Paths.get(String.valueOf(variantsrc));

                        //ecco commit
                        System.out.println("Committing: " + variant_dir);
                        System.out.println("changed node: " + changedNode.getLocalCondition());
                        System.out.println("CONFIG: " + eccoConfig);
                        //service.setBaseDir(variant_dir);
                        timeBefore = System.currentTimeMillis();
                        //service.commit(eccoConfig);
                        System.out.println("Committed: " + variant_dir);
                        timeAfter = System.currentTimeMillis();
                        runtimeEccoCommit = timeAfter - timeBefore;
                        //end ecco commit

                        //appending to the config csv
                        /*try {
                            String fileStr = gitRepositoryFolder.getParent() + File.separator + fileStoreConfig;
                            FileAppender csvWriter = new FileAppender(new File(fileStr));
                            List<List<String>> headerRows = Arrays.asList(
                                    Arrays.asList(Long.toString(gc.getNumber()), eccoConfig)
                            );
                            for (List<String> rowData : headerRows) {
                                csvWriter.append(String.join(",", rowData));
                            }
                            csvWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }*/


                        //computing the git commit runtime of the variant
                        /*GitHelper gh = new GitHelper();
                        try {
                            File srcGitProject = new File(repoPath);
                            try {
                                gh.gitCommitAndCheckout(srcGitProject.getAbsolutePath(), destGitCommitAndCheckout.getAbsolutePath(), gc.getCommitName(), eccoConfig);
                            } catch (GitAPIException e) {
                                e.printStackTrace();
                            }
                            runtimeGitCommit = gh.getRuntimeGitCommit();
                        } catch (IOException e) {

                        }*/
                        //end computing the git commit and checkout

                        //add config to checkout after all project commits
                        configsToCheckout.add(eccoConfig);


                        //appending to the runtime csv
                        /*try {
                            File fileStr = new File(gitRepositoryFolder.getParent() + File.separator +"runtime.csv");
                            FileAppender csvAppender = new FileAppender(fileStr);
                            List<List<String>> headerRows = Arrays.asList(
                                    Arrays.asList(Long.toString(gc.getNumber()),eccoConfig.replace(",","AND"),runtimeEccoCommit.toString(), runtimeEccoCheckout.toString(), runtimePPCheckoutCleanVersion.toString(), runtimePPCheckoutGenerateVariant.toString(), runtimeGitCommit.toString(), runtimeGitCheckout.toString())
                            );
                            for (List<String> rowData : headerRows) {
                                csvAppender.append(String.join(",", rowData));
                            }
                            csvAppender.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }*/
                        //end appending to the runtime csv

                    }
                }
            }

            //append results to the feature report csv
            /*try {
                FileAppender csvAppender = new FileAppender(new File(gitRepositoryFolder.getParent() + File.separator + fileReportFeature));
                List<List<String>> contentRows = Arrays.asList(
                        Arrays.asList(Long.toString(gc.getNumber()),newFeatures[0].toString(),countFeaturesChanged[0].toString())
                );
                for (List<String> rowData : contentRows) {
                    csvAppender.append(String.join(",", rowData));
                }
                csvAppender.close();
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            //end append results to the feature report csv

            countFeaturesChanged[0] = 0;
            newFeatures[0] = 0;

        });

        //gitHelper.getEveryNthCommit(commitList, 11, 13, 1);
        gitHelper.getAllCommits(commitList);

        //close ecco repository
        service.close();
        System.out.println("Repository closed.");

    }

}

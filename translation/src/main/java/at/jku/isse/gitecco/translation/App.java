package at.jku.isse.gitecco.translation;

import at.jku.isse.ecco.service.EccoService;
import at.jku.isse.gitecco.core.git.Change;
import at.jku.isse.gitecco.core.git.GitCommitList;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.tree.nodes.ConditionalNode;
import at.jku.isse.gitecco.core.tree.nodes.FileNode;
import at.jku.isse.gitecco.core.tree.nodes.SourceFileNode;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.translation.constraintcomputation.util.ConstraintComputer;
import at.jku.isse.gitecco.translation.visitor.GetNodesForChangeVisitor;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class App {


    public static void main(String... args) throws Exception {
        final boolean debug = true;
        //TODO: planned arguments: DEBUG, dispose tree, max commits, repo path, csv path(feature id), outpath for ecco
        //maybe even start commit and/or end commit (hashes or numbers)
        //String repoPath = "C:\\Users\\gabil\\Desktop\\ECCO_Work\\testpidtemp";
        String repoPath = "C:\\Users\\gabil\\Desktop\\ECCO_Work\\TestMarlin\\Marlin\\Marlin\\Marlin";
        //String repoPath = "C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\libssh-mirror\\libssh-mirror";
        //String repoPath = "C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\sqllite\\sqlite";

        //optional features of the project obtained by the featureID (chosen that which is in almost cases external feature)
        String[] featuresToAdd = {"BASE", "F_FILE_DIR_DIRTY", "F_UNUSED", "F_FILE_UNBUFFERED_READ", "RAMPS_V_1_0", "__AVR_ATmega2560__", "F_CPU", "F_OFLAG", "WATCHPERIOD",
                "THERMISTORHEATER", "THERMISTORBED", "TSd2PinMap_hHERMISTORHEATER", "PID_DEBUG", "HEATER_USES_THERMISTOR", "__AVR_ATmega328P__", "__AVR_ATmega1280__", "__AVR_ATmega168__",
                "ADVANCE", "PID_OPENLOOP", "SDSUPPORT", "BED_USES_THERMISTOR", "SIMPLE_LCD", "NEWPANEL", "DEBUG_STEPS", "BED_USES_AD595", "ARDUINO",
                "HEATER_1_USES_THERMISTOR", "THERMISTORHEATER_1", "HEATER_USES_THERMISTOR_1", "HEATER_2_USES_AD595", "HEATER_1_MAXTEMP", "THERMISTORHEATER_0",
                "HEATER_1_MINTEMP", "HEATER_0_USES_THERMISTOR", "RESET_MANUAL", "PID_PID"};
        /*String[] featuresToAdd = {"BASE", "WITH_SERVER", "HAVE_LIBZ", "WORDS_BIGENDIAN", "DEBUG_CRYPTO",
                "HAVE_OPENSSL_AES_H","HAVE_GETHOSTBYNAME", "OPENSSL_VERSION_NUMBER","HAVE_SYS_POLL_H",
                "HAVE_OPENSSL_BLOWFISH_H", "HAVE_SYS_TIME_H", "HAVE_POLL", "HAVE_SELECT", "HAVE_GETHOSTBYADDR",
                "__cplusplus", "HAVE_SSH1", "NO_SERVER", "HAVE_PTY_H", "HAVE_STDINT_H", "HAVE_MEMORY_H", "HAVE_LIBWSOCK32",
                "HAVE_GETPWUID", "DEBUG", "HAVE_ERRNO_H", "HAVE_CTYPE_H", "HAVE_NETINET_IN_H", "__CYGWIN_","HAVE_STRSEP",
                "HAVE_GETUID", "HAVE_STDIO_H", "HAVE_CONFIG_H","HAVE_STRING_H","HAVE_ARPA_INET_H","HAVE_STRINGS_H",
                "HAVE_SYS_SOCKET_H", "HAVE_SYS_TYPES_H","HAVE_STRTOLL","HAVE_PWD_H","HAVE_FCNTL_H","HAVE_OPENNET_H",
                "TIME_WITH_SYS_TIME","HAVE_DIRENT_H","HAVE_NETDB_H","__WIN32__","HAVE_INTTYPES_H","HAVE_LIBOPENNET",
                "HAVE_SYS_STAT_H","__MINGW32__"};
        /*String[] featuresToAdd = {"BASE", "YYERRORSYMBOL","TEST_COMPARE","_WIN32","WIN32",
                "TEST","NDEBUG","NO_READLINE","TCLSH","MEMORY_DEBUG"};*/
        ArrayList<String> featureList = new ArrayList<>();
        for (String feat : featuresToAdd) {
            featureList.add(feat);
        }
        //add directories that we need to include manually to get all the files to create a clean version because "/usr/local/include"
        // and "/usr/include")does not includes files outside the root path
        final List<String> dirFiles = new ArrayList<>();
        //dirFiles.add("C:\\Users\\gabil\\Desktop\\ECCO_Work\\testpidtemp");
        dirFiles.add("C:\\Users\\gabil\\Desktop\\ECCO_Work\\TestMarlin\\Marlin\\Marlin\\Marlin");
        //dirFiles.add("C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\libssh-mirror\\libssh-mirror");
        //dirFiles.add("C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\sqllite\\sqlite");
        final GitHelper gitHelper = new GitHelper(repoPath, dirFiles);
        final GitCommitList commitList = new GitCommitList(gitHelper);

        //creating ecco repository for each commit
        //set this path to where the results should be stored
        final Path OUTPUT_DIR = Paths.get("C:\\Users\\gabil\\Desktop\\ECCO_Work\\variant_result");
        if (OUTPUT_DIR.resolve("repo").toFile().exists()) GitCommitList.recursiveDelete(OUTPUT_DIR.resolve("repo"));
        EccoService service = new EccoService();
        service.setRepositoryDir(OUTPUT_DIR.resolve("repo"));
        //initializing repo
        service.init();
        System.out.println("Repository initialized.");

        Map<Feature, Integer> featureVersions = new HashMap<>();
        final int[] countFeaturesChanged = {0};

        commitList.addGitCommitListener((gc, gcl) -> {
            System.out.println(gc.getCommitName() + ":");

            //TODO: do the git commit and measure time or whatever

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
                        System.err.println("error while executing the file diff: "+child.getFilePath());
                        e.printStackTrace();
                    }

                    for (Change change : changes) {
                        visitor.setChange(change);
                        child.accept(visitor);
                        changedNodes.addAll(visitor.getchangedNodes());
                    }
                }
            }


            final File gitFolder = new File(gitHelper.getPath());
            final File eccoFolder = new File(gitFolder.getParent(), "ecco");
            final ConstraintComputer constraintComputer = new ConstraintComputer(featureList);
            final PreprocessorHelper pph = new PreprocessorHelper();
            Map<Feature, Integer> config;
            Set<Feature> changed;
            Set<Feature> alreadyComitted = new HashSet<>();

            //changedNodes = changedNodes.stream().filter(x -> x.getLocalCondition().equals("Z_MIN_PIN > -1")).collect(Collectors.toSet());
            for (ConditionalNode changedNode : changedNodes) {
                //compute the config for the var gen
                //TODO: print every condition that should be solved.
                config = constraintComputer.computeConfig(changedNode, gc.getTree());
                if (config != null && !config.isEmpty()) {
                    //generate the variant for this config
                    //TODO: add binary files to the generated variant
                    //pph.generateVariants(config, gitFolder, eccoFolder, gitHelper.getDirFiles());
                    //compute the marked as changed features.
                    changed = constraintComputer.computeChangedFeatures(changedNode, config);


                    //map with the name of the feature and version and a boolean to set true when it is already incremented in the analysed commit
                    String eccoConfig = "";
                    for (Map.Entry<Feature, Integer> configFeature : config.entrySet()) {
                        int version = 0;
                        if (configFeature.getValue() != 0) {
                            if(featureVersions.containsKey(configFeature.getKey())) {
                                version = featureVersions.get(configFeature.getKey());
                            }
                            if(!alreadyComitted.contains(configFeature.getKey())) {
                                alreadyComitted.add(configFeature.getKey());
                                //for marlin: first commit is empty, thus we need the < 2. otherwise <1 should be enough.
                                if(gcl.size() < 2 || changed.contains(configFeature.getKey())) {
                                    version++;
                                    countFeaturesChanged[0]++;
                                }
                                featureVersions.put(configFeature.getKey(), version);
                            }
                            eccoConfig += ","+configFeature.getKey().toString()+"."+version;
                        }
                    }

                    eccoConfig = eccoConfig.replaceFirst(",","");

                    //folder where the variant is stored
                    final Path variant_dir = Paths.get(String.valueOf(eccoFolder));

                    //ecco commit
                    System.out.println("Committing: "+variant_dir);
                    System.out.println("changed node: "+changedNode.getLocalCondition());
                    System.out.println("CONFIG: "+eccoConfig);

                    service.setBaseDir(variant_dir);
                    //actual commit
                    service.commit(eccoConfig);
                    System.out.println("Committed: "+variant_dir);
                    //end ecco commit
                }
            }
            System.out.println("Feature changed per commit: "+countFeaturesChanged[0]);
            countFeaturesChanged[0]=0;
        });

        gitHelper.getAllCommits(commitList);

        //close ecco repository
        service.close();
        System.out.println("Repository closed.");

    }

}

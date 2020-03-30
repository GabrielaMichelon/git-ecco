package at.jku.isse.gitecco.translation.test;

import at.jku.isse.gitecco.core.git.GitCommitList;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.featureid.type.TraceableFeature;
import at.jku.isse.gitecco.translation.mining.ComputeRQMetrics;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.glassfish.grizzly.http.server.accesslog.FileAppender;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.lang.Thread.sleep;

public class MiningTest {

    private final static boolean DISPOSE = true;
    private final static int MAX_COMMITS = 500;
    private final static boolean EVERYCOMMIT = false;
    private final static int STARTCOMMIT = 0;
    private final static int ENDCOMMIT = 50;
    private final static int EVERY_NTH_COMMIT = 1;
    private final static boolean MAX_COMMITS_ENA = true;
    private final static boolean PARALLEL = false;
    private final static ArrayList<Feature> featureList = new ArrayList<>();
    private final static ArrayList<String> featureNamesList = new ArrayList<String>();
    private final static String REPO_PATH = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\systems\\curl\\curl";
    private final static String featureFolder = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\systems\\curl";
    public final String resultsCSVs_path = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\systems\\curl";
    //curl
    String[] features = {"HAVE_STDLIB_H","__sparc","HAVE_ALLOCA_H","__TURBOC__","HAVE_TCGETATTR","HAVE_OPENSSL_ERR_H","__BEOS__","HAVE_PEM_H","HAVE_STRCASECMP","MIME_SEPARATORS","HAVE_LIBDL","__STDC__","DPRINTF_DEBUG","CURL_SEPARATORS","__hpux","TELOPTS","HAVE_SYS_TIME_H","HAVE_ARPA_INET_H","_WIN32","HAVE_ISASCII","STDC_HEADERS","_MPRINTF_REPLACE","HAVE_UNAME","HAVE_SYS_PARAM_H","HAVE_SYS_SELECT_H","HAVE_FCNTL_H","HAVE_TCSETATTR","HAVE_ZLIB_H","TIME_WITH_SYS_TIME","fileno","HAVE_NETDB_H","__GNUC_MINOR__","YYLSP_NEEDED","_MSDOS","HAVE_NET_IF_H","HAVE_OPENSSL_X509_H","HAVE_SYS_STAT_H","HAVE_CRYPTO_H","HAVE_LIBCRYPTO","AUTH_NAMES","__MINGW32__","HAVE_UNISTD_H","sparc","HAVE_X509_H","USG","TELCMDS","HAVE_OPENSSL_CRYPTO_H","TEST","_WINSOCKAPI_","HAVE_ERR_H","_MSDOS_","DPRINTF_DEBUG2","HAVE_BCOPY","EINTR","HAVE_CONFIG_H","SIZEOF_LONG_LONG","YYERROR_VERBOSE","HAVE_OPENSSL_RSA_H","HAVE_SSL_H","__sgi","_NETRC_DEBUG","__sparc__","vms","SSL_VERIFY_CERT","HAVE_SYS_SOCKET_H","_AIX","__cplusplus","isascii","ENCRYPT_NAMES","HAVE_GETHOSTNAME","HAVE_SYS_TYPES_H","HAVE_TERMIO_H","emacs","YYPARSE_PARAM","HAVE_LIBZ","HAVE_STRFTIME","__GNUC__","PROTOTYPES","HAVE_INET_ADDR","HAVE_OPENSSL_SSL_H","OPENSSL_VERSION_NUMBER","HAVE_LIBSSL","_FORM_DEBUG","HAVE_IO_H","YYPURE","HAVE_DLOPEN","SIZEOF_LONG_DOUBLE","SSLEAY_VERSION_NUMBER","MULTIDOC","__i386","yyoverflow","HAVE_PERROR","ECONNREFUSED","HAVE_MEMCPY","__sun","HAVE_OPENSSL_PEM_H","BASE","HAVE_INET_NTOA","HAVE_RSA_H","GLOBURL","YYDEBUG","HAVE_SYS_SOCKIO_H","YYPRINT","HAVE_DLFCN_H","YYLEX_PARAM","__APPLE__"};
    //busybox
    //String[] features = {"PKZIP_BUG_WORKAROUND","__TURBOC__","BB_SWAPOFF","NO_STDLIB_H","isgraph","lint","S_IFMPB","NO_STRING_H","S_IFLNK","BB_SWAPON","__STDC__","BB_LOSETUP","MACOS","FEATURE_RECURSIVE","__alpha__","BB_BLOCK_DEVICE","S_IFIFO","MPW","LOCALTIME_CACHE","atarist","TOPS20","NDIR","RCSID","__OS2__","NO_ASM","FULL_SEARCH","S_IFCHR","SIGHUP","sparc","MEDIUM_MEM","S_IFNWK","S_IFDIR","__GLIBC__","SELECT","DEBUG","S_IFREG","__ZTC__","unix","SYSNDIR","UNALIGNED_OK","VMS","__GNU_LIBRARY__","pyr","__EMX__","__cplusplus","isblank","__GNUC__","_MSC_VER","S_IFBLK","DUMP_BL_TREE","__50SERIES","WIN32","__MSDOS__","VAXC","NTFAT","TOSFS","S_IFSOCK","BB_CHMOD","BASE","AMIGA","OS2FAT","NO_TIME_H","__BORLANDC__","FORCE_METHOD","ATARI","ERASE_STUFF","fooBar","foobar","PIO_FONTX","ENOIOCTLCMD","__sparc_v9__","__mips__","PAGE_SIZE","fooo","__sparc__","__GLIBC_MINOR__","MINIX2_SUPER_MAGIC2"};
    //BISON
    //String[] features = {"BASE", "TRACE", "DEBUG", "MSDOS", "eta10", "__GO32__", "DONTDEF", "VMS", "HAVE_ALLOCA_H", "__GNUC__", "_AIX", "__STDC__", "HAVE_STDLIB_H", "HAVE_MEMORY_H", "STDC_HEADERS"};


    @Test
    public void getFeatureDeletedTimes() throws IOException {
        addFeatures();
        File folder = new File(resultsCSVs_path);
        File[] lista = folder.listFiles();
        int deletedTimes = 0;
        Map<Integer, Integer> deletePerGitCommit = new HashMap<>();
        for (File file : lista) {
            if (featureNamesList.contains(file.getName().substring(0, file.getName().indexOf(".csv")))) {
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
                File featureCSV = new File(resultsCSVs_path,"deleteFeatures.csv");
                if(!featureCSV.exists()){
                    try {
                        FileWriter csvWriter = new FileWriter(featureCSV);
                        List<List<String>> headerRows = Arrays.asList(
                                Arrays.asList("Feature", "Deleted Times")
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
                }
                try {
                    FileAppender csvAppender = new FileAppender(featureCSV);
                    List<List<String>> contentRows = Arrays.asList(
                            Arrays.asList(file.getName().substring(0,file.getName().indexOf(".csv")),String.valueOf(deletedTimes)));
                    for (List<String> rowData : contentRows) {
                        csvAppender.append(String.join(",", rowData));
                    }
                    csvAppender.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //System.out.println("Feature: " + file.getName() + " deleted times: " + deletedTimes);
                deletedTimes = 0;
            }
        }

        //RQ.2 How many features were deleted per Git commit?
        for (Map.Entry<Integer, Integer> commit:deletePerGitCommit.entrySet() ) {
            System.out.println("Commit Number: "+commit.getKey()+" features deleted: "+commit.getValue());
        }
    }


    @Test
    public void FeatureRevision() throws Exception {
        ComputeRQMetrics.CharacteristicsChange(features, REPO_PATH);
    }

    @Test
    public void getFeatureCharacteristics() throws Exception {

        final GitHelper gitHelper = new GitHelper(REPO_PATH, null);
        final GitCommitList commitList = new GitCommitList(gitHelper);
        final List<Future<?>> tasks = new ArrayList<>();
        final ExecutorService executorService = Executors.newFixedThreadPool(30);
        addFeatures();

        commitList.addGitCommitListener(
                (gc, gcl) -> {
                    if(gcl.size() > MAX_COMMITS && MAX_COMMITS_ENA) {
                        // writeToCsv(evaluation, csvPath);
                        executorService.shutdownNow();
                        System.exit(0);
                    }

                    if(PARALLEL) {
                        tasks.add(
                                executorService.submit(() -> {
                                    ComputeRQMetrics.CharacteristicsFeature(featureFolder, gc.getNumber(), gc.getTree(), featureList,featureNamesList);
                                    //dispose tree if it is not needed -> for memory saving reasons.
                                    if (DISPOSE) gc.disposeTree();
                                })
                        );
                    } else {
                        ComputeRQMetrics.CharacteristicsFeature(featureFolder, gc.getNumber(), gc.getTree(), featureList,featureNamesList);
                        //dispose tree if it is not needed -> for memory saving reasons.
                        if (DISPOSE) gc.disposeTree();
                    }
                }
        );


        if(EVERYCOMMIT) {
            gitHelper.getAllCommits(commitList);
        } else {
            gitHelper.getEveryNthCommit(commitList, null,STARTCOMMIT, ENDCOMMIT, EVERY_NTH_COMMIT);
        }

        while(PARALLEL && !isDone(tasks)) sleep(100);
        executorService.shutdownNow();

        //print to CSV:
        //writeToCsv(evaluation, csvPath);

        System.out.println("finished analyzing repo");
    }


    public void addFeatures(){
        for (String feat : features) {
            Feature feature = new Feature(feat);
            featureNamesList.add(feat);
            featureList.add(feature);
        }
    }

    /**
     * Helper method to check if all tasks are done.
     * @param tasks
     * @return
     */
    private static boolean isDone(List<Future<?>> tasks) {
        for (Future task : tasks)
            if(!task.isDone()) return false;

        return true;
    }
}

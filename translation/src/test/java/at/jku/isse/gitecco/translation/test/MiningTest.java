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
    private final static int ENDCOMMIT = 967;
    private final static int EVERY_NTH_COMMIT = 1;
    private final static boolean MAX_COMMITS_ENA = false;
    private final static boolean PARALLEL = false;
    private final static ArrayList<Feature> featureList = new ArrayList<>();
    private final static ArrayList<String> featureNamesList = new ArrayList<String>();
    private final static String REPO_PATH = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\systems\\MPSolve2\\MPSolve";
    private final static String featureFolder = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\systems\\MPSolve2";
    public final String resultsCSVs_path = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\systems\\feature_identification\\mpsolve2";
    //String[] features = {"__TURBOC__","BB_MNC","NO_STDLIB_H","isgraph","lint","BB_FDFLUSH","BB_PRINTF","__STDC__","__alpha__","NFSERR_INVAL","atarist","PIO_FONTX","HAVE_SYS_SYSTEMINFO_H","FULL_SEARCH","S_IFNWK","__ZTC__","SYSNDIR","__linux__","VMS","BB_MATH","__GNU_LIBRARY__","ENOIOCTLCMD","isblank","S_IFBLK","DUMP_BL_TREE","__GLIBC_MINOR__","VAXC","NFS_MOUNT_DEBUG","TOSFS","BASE","AMIGA","OS2FAT","ATARI","PKZIP_BUG_WORKAROUND","EWFLUSH","S_IFMPB","NO_STRING_H","BB_CHOWN","S_IFLNK","BB_LENGTH","MACOS","FEATURE_RECURSIVE","BB_BLOCK_DEVICE","S_IFIFO","MPW","__sparc_v9__","__mips__","BB_MAKEDEVS","BB_DUTMP","TOPS20","PAGE_SIZE","NDIR","__OS2__","NO_ASM","S_IFCHR","SIGHUP","MEDIUM_MEM","S_IFDIR","__GLIBC__","NFS_MOUNT_VERSION","DEBUG","S_IFREG","unix","SI_ARCHITECTURE","UNALIGNED_OK","BB_MTAB","BB_HALT","pyr","__EMX__","__sparc__","__cplusplus","__GNUC__","HAVE_SYSINFO","_MSC_VER","BB_SFDISK","__50SERIES","WIN32","__MSDOS__","NTFAT","BB_MT","S_IFSOCK","NO_TIME_H","__BORLANDC__","FORCE_METHOD","MINIX2_SUPER_MAGIC2"};
    //LibSSH
    //String[] features = {"WITH_SERVER","HAVE_LIBZ","WORDS_BIGENDIAN","DEBUG_CRYPTO","HAVE_OPENSSL_AES_H","HAVE_GETHOSTBYNAME","OPENSSL_VERSION_NUMBER","HAVE_SYS_POLL_H","HAVE_OPENSSL_BLOWFISH_H","HAVE_SYS_TIME_H","BASE","HAVE_POLL","HAVE_SELECT","HAVE_GETHOSTBYADDR","__cplusplus","HAVE_SSH1","NO_SERVER","HAVE_PTY_H","HAVE_STDINT_H","HAVE_MEMORY_H","HAVE_LIBWSOCK32","HAVE_GETPWUID","DEBUG","HAVE_ERRNO_H","HAVE_CTYPE_H","HAVE_NETINET_IN_H","__CYGWIN__","HAVE_STRSEP","HAVE_GETUID","HAVE_STDIO_H","HAVE_CONFIG_H","HAVE_STRING_H","HAVE_ARPA_INET_H","HAVE_STRINGS_H","HAVE_SYS_SOCKET_H","HAVE_SYS_TYPES_H","HAVE_STRTOLL","HAVE_PWD_H","HAVE_FCNTL_H","HAVE_OPENNET_H","TIME_WITH_SYS_TIME","HAVE_DIRENT_H","HAVE_NETDB_H","__WIN32__","HAVE_INTTYPES_H","HAVE_LIBOPENNET","HAVE_SYS_STAT_H","__MINGW32__","HAVE_PAM_PAM_APPL_H","HAVE_SECURITY_PAM_APPL_H","HAVE_LIBGCRYPT","HAVE_OPENSSL_DES_H","HAVE_LIBCRYPTO","GCRYPT"};
    //Marlin
    //String[] features = {"F_FILE_DIR_DIRTY","F_UNUSED","F_FILE_UNBUFFERED_READ","__AVR_ATmega644P__","__AVR_ATmega644PA__","RAMPS_V_1_0","__AVR_ATmega2560__","F_CPU","F_OFLAG","__AVR_AT90USB1286__","WATCHPERIOD","THERMISTORBED","THERMISTORHEATER","PID_DEBUG","HEATER_USES_THERMISTOR","__AVR_ATmega328P__","__AVR_ATmega1280__","__AVR_ATmega168__","ADVANCE","PID_OPENLOOP","__AVR_ATmega32U4__","__AVR_ATmega328__","__AVR_ATmega644__","BASE","__AVR_AT90USB1287__","BED_USES_THERMISTOR","__AVR_AT90USB646__","SIMPLE_LCD","DEBUG_STEPS","BED_USES_AD595","ARDUINO","HEATER_1_USES_THERMISTOR","THERMISTORHEATER_2","THERMISTORHEATER_1","HEATER_2_USES_THERMISTOR","HEATER_USES_THERMISTOR_1","HEATER_2_USES_AD595","HEATER_1_MAXTEMP","THERMISTORHEATER_0","HEATER_1_MINTEMP","HEATER_0_USES_THERMISTOR","RESET_MANUAL","PID_PID","AUTOTEMP"};
    //sqlite
    //String[] features = {"ENCODER_TEST","HAVE_USLEEP","SQLITE_TEST","SQLITE_UTF8","TEST","__CYGWIN__","YYERRORSYMBOL","_WIN32","USE_TCL_STUBS","etCOMPATIBILITY","NDEBUG","NO_TCL","__MACOS__","SQLITE_PTR_SZ","TCLSH","_MSC_VER","__DJGPP__","HAVE_READLINE","SIGINT","WIN32","TCL_UTF_MAX","THREADSAFE","SQLITE_OMIT_AUTHORIZATION","COMPATIBILITY","VDBE_PROFILE","BASE","SQLITE_DISABLE_LFS","YYFALLBACK","__MINGW32__","__BORLANDC__","MEMORY_DEBUG","SQLITE_SOUNDEX"};//{"YYERRORSYMBOL","TEST_COMPARE","_WIN32","WIN32","TEST","NDEBUG","BASE","NO_READLINE","TCLSH"};
    //mpsolve
    //String[] features = {"mpz_swap","mpf_pow_ui","NOMPTEMP","mpz_tstbit","BASE","mpf_swap","RAND_VAL","boolean","mpq_swap","mpq_out_str"};
    //busybox
    //String[] features = {"PKZIP_BUG_WORKAROUND","__TURBOC__","BB_SWAPOFF","NO_STDLIB_H","isgraph","lint","S_IFMPB","NO_STRING_H","S_IFLNK","BB_SWAPON","__STDC__","BB_LOSETUP","MACOS","FEATURE_RECURSIVE","__alpha__","BB_BLOCK_DEVICE","S_IFIFO","MPW","LOCALTIME_CACHE","atarist","TOPS20","NDIR","RCSID","__OS2__","NO_ASM","FULL_SEARCH","S_IFCHR","SIGHUP","sparc","MEDIUM_MEM","S_IFNWK","S_IFDIR","__GLIBC__","SELECT","DEBUG","S_IFREG","__ZTC__","unix","SYSNDIR","UNALIGNED_OK","VMS","__GNU_LIBRARY__","pyr","__EMX__","__cplusplus","isblank","__GNUC__","_MSC_VER","S_IFBLK","DUMP_BL_TREE","__50SERIES","WIN32","__MSDOS__","VAXC","NTFAT","TOSFS","S_IFSOCK","BB_CHMOD","BASE","AMIGA","OS2FAT","NO_TIME_H","__BORLANDC__","FORCE_METHOD","ATARI","ERASE_STUFF","fooBar","foobar","PIO_FONTX","ENOIOCTLCMD","__sparc_v9__","__mips__","PAGE_SIZE","fooo","__sparc__","__GLIBC_MINOR__","MINIX2_SUPER_MAGIC2"};
    //BISON
    //String[] features = {"HAVE_STDLIB_H","UINT64_MAX","HAVE_ALLOCA_H","HAVE_DECL_STRNLEN","HAVE_MEMORY_H","__PTRDIFF_TYPE__","HAVE_DECL_MEMRCHR","TESTING","HAVE_STDDEF_H","WITH_DMALLOC","memcpy","CRAY","_GNU_GETOPT_INTERFACE_VERSION","__STDC__","_LIBC","HAVE___SECURE_GETENV","ENABLE_CHECKING","HAVE_SYS_TIME_H","HAVE_STRING_H","HAVE_DECL_STRERROR","HAVE_STRERROR_R","_WIN32","USE_NONOPTION_FLAGS","HAVE_DECL_STRERROR_R","HAVE_ISASCII","STDC_HEADERS","text_set_element","HAVE_DECL_MEMCHR","static","alloca","HAVE_VPRINTF","HAVE_BP_SYM_H","HAVE_WCTYPE_H","HAVE_FCNTL_H","STRERROR_R_CHAR_P","S_IWRITE","HAVE_LOCALE_H","__GNUC_MINOR__","USE_DIFF_HASH","S_IEXEC","ENABLE_NLS","HAVE_STDBOOL_H","HAVE_STDINT_H","HAVE_DECL_STRCHR","HAVE_UNISTD_H","HAVE_DECL_STRSPN","STAT_MACROS_BROKEN","S_IREAD","DEBUG_I00AFUNC","CRAY_STACKSEG_END","HAVE_ISWPRINT","MSDOS","S_IFDIR","HAVE_DECL_STPCPY","__GLIBC__","_GNU_OBSTACK_INTERFACE_VERSION","HAVE_LIBINTL_H","HAVE_DECL_FREE","__STRICT_ANSI__","TEST","HAVE_CTYPE_H","USE_OBSTACK","VMS","CRAY2","HP_TIMING_AVAIL","__GNU_LIBRARY__","HAVE_WCHAR_H","HAVE_GETTIMEOFDAY","HAVE_CONFIG_H","HAVE_DECL_MALLOC","USE_IN_LIBIO","strlen","HAVE_DOPRNT","__cplusplus","_AIX","weak_alias","isascii","errno","TEST_DIRNAME","emacs","__GNUC__","PROTOTYPES","strerror_r","__NeXT__","HAVE_SETLOCALE","HAVE_STRERROR","HAVE_MBRTOWC","BASE","HAVE_C_BACKSLASH_A","C_ALLOCA","HAVE_LIMITS_H"};
    String[] features = {"mpz_swap","mpf_pow_ui","NOMPTEMP","mpz_tstbit","BASE","mpf_swap","RAND_VAL","boolean","mpq_swap","mpq_out_str","mps_boolean","__MATLAB_MEX","__USE_BOOL_AS_BOOLEAN","__UNDEF_CPLUSPLUS",
            "DISABLE_DEBUG","__STDC_VERSION__","__WINDOWS","NICE_DEBUG","THREAD_SAFE","WIN32","MPS_CATCH_FPE","__MPS_MATLAB_MODE","HAVE_FUNLOCKFILE","HAVE_FLOCKFILE","_MPS_PRIVATE","HAVE_CONFIG_H","__GCC__","HAVE_GETLINE","MPS_USE_BUILTIN_COMPLEX"}; //{"mpz_swap","__STDC_VERSION__","mpf_pow_ui","mpz_tstbit","__WINDOWS","__GCC__","DISABLE_DEBUG","__MPS_MATLAB_MODE","__UNDEF_CPLUSPLUS","mpf_swap","RAND_VAL","mpq_out_str","HAVE_FLOCKFILE","MPS_USE_BUILTIN_COMPLEX","_MPS_PRIVATE","HAVE_GETLINE","HAVE_CONFIG_H","NICE_DEBUG","MPS_CATCH_FPE","BASE","HAVE_FUNLOCKFILE","__cplusplus","mpq_swap"};
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
                            if(alreadyExist==null)
                                deletePerGitCommit.put(commitNumber,1);
                        }
                        count = commitNumber;
                    }

                }
                //RQ.2 How many times one feature were deleted along a number of Git commits?
                File featureCSV = new File(featureFolder,"deleteFeatures.csv");
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

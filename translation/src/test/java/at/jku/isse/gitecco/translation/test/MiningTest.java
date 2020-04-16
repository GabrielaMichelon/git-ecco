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
    private final static int STARTCOMMIT = 12240;
    private final static int ENDCOMMIT = 12244;//967;
    private final static int EVERY_NTH_COMMIT = 1;
    private final static boolean MAX_COMMITS_ENA = false;
    private final static boolean PARALLEL = false;
    private final static ArrayList<Feature> featureList = new ArrayList<>();
    private final static ArrayList<String> featureNamesList = new ArrayList<String>();
    private final static String REPO_PATH = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\systems\\sqlite-versions\\sqlite-3.13.0\\sqlite";
    private final static File featureFolder = new File("C:\\Users\\gabil\\Desktop\\PHD\\Mining\\systems\\sqlite-versions\\sqlite-3.13.0");
    public final String resultsCSVs_path = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\systems\\feature_identification\\sqlite-versions\\3.13.0";
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
    String[] features = {"SQLITE_OMIT_BUILTIN_TEST","SQLITE_OMIT_CTE","SQLITE_OMIT_ATTACH","SQLITE_ENABLE_EXPENSIVE_ASSERT","__PTRDIFF_TYPE__","SQLITE_OMIT_TRUNCATE_OPTIMIZATION","SQLITE_WIN32_MALLOC","SQLITE_OMIT_REINDEX","SQLITE_OMIT_BETWEEN_OPTIMIZATION","SQLITE_OMIT_LIKE_OPTIMIZATION","SQLITE_EXTRA_SHUTDOWN","SQLITE_CHECK_PAGES","HAVE_UINT16_T","SQLITE_WIN32_MALLOC_VALIDATE","SQLITE_ENABLE_COLUMN_METADATA","__ppc__","TCL_MINOR_VERSION","SQLITE_OMIT_BTREECOUNT","SQLITE_SHM_DIRECTORY","YYERRORSYMBOL","SQLITE_OMIT_VIRTUALTABLE","SQLITE_ENABLE_MEMSYS5","SQLITE_ENABLE_MEMSYS3","SQLITE_OMIT_AUTOINCREMENT","__QNXNTO__","SQLITE_OMIT_INCRBLOB","SQLITE_OMIT_PROGRESS_CALLBACK","SQLITE_MEMORY_BARRIER","winIoerrCanRetry2","SQLITE_OMIT_FLAG_PRAGMAS","SQLITE_ENABLE_ASYNCIO","SQLITE_SUBSTR_COMPATIBILITY","SIGINT","SQLITE_OMIT_XFER_OPT","_WRS_KERNEL","SQLITE_OMIT_AUTHORIZATION","SQLITE_SHELL_DBNAME_PROC","YY_SHIFT_MIN","AMATCH_TRACE_1","_M_IX86","SQLITE_TCL","HAVE_STDINT_H","__MINGW32__","sparc","SQLITE_DIRECT_OVERFLOW_READ","SQLITE_HAS_CODEC","_CRT_INSECURE_DEPRECATE","_HAVE_MINGW_H","__unix__","HAVE_MALLOC_USABLE_SIZE","SQLITE_ENABLE_HIDDEN_COLUMNS","SQLITE_ENABLE_FTS3_TOKENIZER","SQLITE_RUNTIME_BYTEORDER","SQLITE_TRACE_SIZE_LIMIT","SQLITE_NO_SYNC","SQLITE_IGNORE_AFP_LOCK_ERRORS","SQLITE_OMIT_QUICKBALANCE","SQLITE_DISABLE_DIRSYNC","SQLITE_SECURE_DELETE","__MAC_OS_X_VERSION_MIN_REQUIRED","__linux__","SQLITE_ENABLE_CURSOR_HINTS","TEST","SQLITE_OMIT_SHARED_CACHE","SQLITE_PERFORMANCE_TRACE","__HP_cc","SQLITE_OMIT_UTF16","_DEBUG","SQLITE_MULTIPLEX_EXT_OVWR","SQLITE_OMIT_CHECK","SQLITE_WIN32_NO_WIDE","SQLITE_OMIT_DEPRECATED","SQLITE_LOCK_TRACE","SQLITE_WITHOUT_ZONEMALLOC","SQLITE_OMIT_WAL","HAVE_LOCALTIME_R","SQLITE_REVERSE_UNORDERED_SELECTS","SQLITE_DISABLE_FTS3_UNICODE","TCL_MAJOR_VERSION","FOSSIL_OMIT_DELTA_CKSUM_TEST","__x86","_HAVE__MINGW_H","SQLITE_ENABLE_FTS1","SQLITE_ENABLE_FTS2","SQLITE_ENABLE_DBSTAT_VTAB","HAVE_INTTYPES_H","SQLITE_FTS5_ENABLE_TEST_MI","SQLITE_ENABLE_FTS5","HAVE_PREAD64","_M_ARM","SQLITE_WIN32_NO_OVERLAPPED","SQLITE_OMIT_INTEGRITY_CHECK","MEMORY_DEBUG","SQLITE_OMIT_FLOATING_POINT","FD_CLOEXEC","__minux","SQLITE_OMIT_WSD","SQLITE_TCL_DEFAULT_FULLMUTEX","SQLITE_OMIT_TCL_VARIABLE","SQLITE_ENABLE_COLUMN_USED_MASK","SQLITE_OMIT_COMPILEOPTION_DIAGS","SQLITE_OMIT_SUBQUERY","SQLITE_PROXY_DEBUG","SQLITE_WIN32_NO_ANSI","_WIN32","__SANITIZE_ADDRESS__","__FAST_MATH__","SQLITE_ENABLE_CEROD","SQLITE_ENABLE_WHERETRACE","__DragonFly__","SQLITE_ENABLE_SNAPSHOT","SQLITE_64BIT_STATS","STRERROR_R_CHAR_P","SQLITE_OMIT_TRIGGER","SQLITE_OMIT_AUTOVACUUM","SQLITE_UNLINK_AFTER_CLOSE","SQLITE_BITMASK_TYPE","_M_AMD64","SQLITE_DISABLE_LFS","SQLITE_SOUNDEX","SQLITE_ENABLE_STAT4","__DARWIN__","__GLIBC__","MemoryBarrier","SQLITE_OMIT_OR_OPTIMIZATION","FTS3_LOG_MERGES","SQLITE_ENABLE_SQLLOG","HAVE_UINT8_T","unix","_WIN64","__SIZEOF_POINTER__","SQLITE_OMIT_CONFLICT_CLAUSE","HAVE_LINENOISE","SQLITE_CASE_SENSITIVE_LIKE","SQLITE_POW2_MEMORY_SIZE","__APPLE__","InterlockedCompareExchange","__has_feature","SQLITE_ENABLE_RBU","SQLITE_ENABLE_LOAD_EXTENSION","SQLITE_TEST_REALLOC_STRESS","__USE_GNU","SQLITE_OMIT_EXPLAIN","i386","SQLITE_EXTRA_DURABLE","SQLITE_OMIT_DISKIO","SQLITE_OMIT_MEMORYDB","_MSC_VER","HAVE_UINT32_T","HAVE_FDATASYNC","SQLITE_PCACHE_SEPARATE_HEADER","SQLITE_FCNTL_VFSNAME","SQLITE_SMALL_STACK","_M_IA64","SQLITE_ENABLE_STMT_SCANSTATUS","__BORLANDC__","SQLITE_OMIT_FOREIGN_KEY","SQLITE_OMIT_VIEW","SQLITE_SSE","_CS_DARWIN_USER_TEMP_DIR","__CYGWIN__","__MINGW_H","HAVE_ISNAN","SQLITE_DEFAULT_FOREIGN_KEYS","SQLITE_USER_AUTHENTICATION","HAVE_PWRITE64","SQLITE_DISABLE_FTS4_DEFERRED","SQLITE_DISABLE_PAGECACHE_OVERFLOW_STATS","SQLITE_OMIT_COMPOUND_SELECT","HAVE_STRERROR_R","SQLITE_VERSION_NUMBER","TARGET_IPHONE_SIMULATOR","SQLITE_WIN32_MUTEX_TRACE_STATIC","SQLITE_ENABLE_SELECTTRACE","SQLITE_ENABLE_ATOMIC_WRITE","SQLITE_N_KEYWORD","SQLITE_EXPLAIN_ESTIMATED_ROWS","SQLITE_ENABLE_8_3_NAMES","SQLITE_DEFAULT_AUTOMATIC_INDEX","sqlite3Isdigit","SQLITE_NOINLINE","TCLSH","__x86_64__","HAVE_READLINE","F_FULLFSYNC","SQLITE_WIN32_FILEMAPPING_API","SQLITE_VDBE_COVERAGE","__arm__","TRANSLATE_TRACE","__MSVCRT__","SQLITE_DEBUG","VDBE_PROFILE","__FreeBSD__","SQLITE_ENABLE_UNLOCK_NOTIFY","YYNOERRORRECOVERY","HAVE_INT16_T","SQLITE_AMALGAMATION","SQLITE_OMIT_TEMPDB","WIN_SHM_BASE","SQLITE_ENABLE_API_ARMOR","SQLITE_OMIT_PRAGMA","SQLITE_COVERAGE_TEST","HAVE_USLEEP","NTDDI_VERSION","SQLITE_TEST","SQLITE_OMIT_PAGER_PRAGMAS","SQLITE_USE_ALLOCA","SQLITE_WITHOUT_MSIZE","YYSTACKDEPTH","SQLITE_OMIT_CAST","HAVE_INT8_T","SQLITE_MEMDEBUG","SQLITE_OMIT_GET_TABLE","SQLITE_MSVC_LOCALTIME_API","SQLITE_WIN32_MUTEX_TRACE_DYNAMIC","SQLITE_OMIT_SCHEMA_PRAGMAS","SQLITE_ENABLE_BROKEN_FTS1","SQLITE_ENABLE_BROKEN_FTS2","SQLITE_ENABLE_COSTMULT","INCLUDE_MSVC_H","__IPHONE_OS_VERSION_MIN_REQUIRED","__RTP__","SQLITE_DISABLE_INTRINSIC","SQLITE_ZERO_MALLOC","O_CLOEXEC","THREADSAFE","SQLITE_LOG_CACHE_SPILL","BASE","TRACE_CRASHTEST","_WIN32_WINNT","SQLITE_PREFER_PROXY_LOCKING","HAVE_POSIX_FALLOCATE","SQLITE_LIKE_DOESNT_MATCH_BLOBS","__ANDROID__","SQLITE_USE_FCNTL_TRACE","SQLITE_4_BYTE_ALIGNED_MALLOC","SQLITE_OMIT_RANDOMNESS","SQLITE_OMIT_AUTOINIT","USE_SYSTEM_SQLITE","SQLITE_TCLMD5","SQLITE_ENABLE_JSON1","YYWILDCARD","_HAVE_SQLITE_CONFIG_H","SQLITE_INT64_TYPE","SQLITE_OMIT_PARSER_TRACE","LOCKPROXYDIR","SQLITE_ENABLE_MULTIPLEX","__MINGW_MAJOR_VERSION","HAVE_EDITLINE","SQLITE_OMIT_AUTORESET","SQLITE_ENABLE_FTS3_PARENTHESIS","SQLITE_OMIT_ANALYZE","SQLITE_SERVER","__DJGPP__","__i386__","FILENAME_MAX","SQLITE_ENABLE_OVERSIZE_CELL_CHECK","SQLITE_PRINTF_PRECISION_LIMIT","YY_SHIFT_MAX","SQLITE_MMAP_READWRITE","SQLITE_IGNORE_FLOCK_LOCK_ERRORS","__OpenBSD__","SQLITE_OMIT_AUTOMATIC_INDEX","HAVE_STRCHRNUL","SQLITE_32BIT_ROWID","SQLITE_RTREE_INT_ONLY","NO_GETTOD","SQLITE_OMIT_ALTERTABLE","SQLITE_OMIT_VACUUM","_PCACHE_H_","YYFALLBACK","SQLITE_ENABLE_ZIPVFS","TARGET_OS_EMBEDDED","SQLITE_MEMORY_SIZE","SQLITE_ENABLE_VFSTRACE","__MACH__","SQLITE_OMIT_LOOKASIDE","SQLITE_OMIT_LOCALTIME","SQLITE_OMIT_DECLTYPE","SQLITE_ALLOW_URI_AUTHORITY","YYTRACKMAXSTACKDEPTH","HAVE_OSINST","SQLITE_OMIT_CODEC_FROM_TCL","SQLITE_OMIT_BLOB_LITERAL","SQLITE_EXTRA_INIT","_WIN32_WCE","EINTR","SQLITE_OMIT_SCHEMA_VERSION_PRAGMAS","YY_ACTTAB_COUNT","SQLITE_OMIT_COMPLETE","__cplusplus","SQLITE_DEFAULT_LOCKING_MODE","_USE_64BIT_TIME_T","SQLITE_ENABLE_ICU","SQLITE_WIN32_USE_UUID","_M_X64","__GNUC__","EOVERFLOW","SQLITE_FORCE_OS_TRACE","__x86_64","SQLITE_DEFAULT_CKPTFULLFSYNC","WIN32","SQLITE_ENABLE_MEMORY_MANAGEMENT","BUILD_sqlite","SQLITE_ENABLE_UPDATE_DELETE_LIMIT","HAVE_MALLOC_H","__sun","SQLITE_ENABLE_MODULE_COMMENTS","HAVE_GMTIME_R","SQLITE_ENABLE_IOTRACE","UNIX_SHM_BASE","SQLITE_ENABLE_RTREE","SQLITE_ENABLE_PREUPDATE_HOOK","SQLITE_ENABLE_SESSION","SQLITE_WRITE_WALFRAME_PREBUFFERED","ENABLE_FORCE_WAL","SQLITE_ENABLE_SQLRR","SQLITE_ENABLE_APPLE_SPI","OSLOCKING_CHECK_BUSY_IOERR","OSCLOSE_CHECK_CLOSE_IOERR","SQLITE_ENABLE_DATA_PROTECTION","SQLITE_USE_REQUEST_FULLFSYNC","SQLITE_ENABLE_PERSIST_WAL","TH3_COMPATIBILITY","SQLITE_ENABLE_PURGEABLE_PCACHE","SQLITE_ENABLE_AUTO_PROFILE","SCRUB_STANDALONE","SQLITE_OMIT_CONCURRENT","SQLITE_MUTATION_TEST"};

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
        ComputeRQMetrics.characteristicsChange(features, REPO_PATH);
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
                                    ComputeRQMetrics.characteristicsFeature(featureFolder, gc.getNumber(), gc.getTree(),featureNamesList);
                                    //dispose tree if it is not needed -> for memory saving reasons.
                                    if (DISPOSE) gc.disposeTree();
                                })
                        );
                    } else {
                        ComputeRQMetrics.characteristicsFeature(featureFolder, gc.getNumber(), gc.getTree(),featureNamesList);
                        //dispose tree if it is not needed -> for memory saving reasons.
                        if (DISPOSE) gc.disposeTree();
                    }
                }
        );


        if(EVERYCOMMIT) {
            gitHelper.getAllCommits(commitList);
        } else {
            //gitHelper.getEveryNthCommit(commitList, null,STARTCOMMIT, ENDCOMMIT, EVERY_NTH_COMMIT);
            //gitHelper.getEveryNthCommit2(commitList, null, "f044b7153a46d7b2f3de4730c042c780a400b748", "55bcaf6829131233488f57035bc8c2dc6bbdaed1", EVERY_NTH_COMMIT);
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

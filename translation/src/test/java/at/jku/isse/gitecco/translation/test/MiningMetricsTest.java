package at.jku.isse.gitecco.translation.test;

import at.jku.isse.gitecco.core.git.Change;
import at.jku.isse.gitecco.core.git.GitCommit;
import at.jku.isse.gitecco.core.git.GitCommitList;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.tree.nodes.BaseNode;
import at.jku.isse.gitecco.core.tree.nodes.ConditionalNode;
import at.jku.isse.gitecco.core.tree.nodes.FileNode;
import at.jku.isse.gitecco.core.tree.nodes.SourceFileNode;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.featureid.identification.ID;
import at.jku.isse.gitecco.featureid.type.TraceableFeature;
import at.jku.isse.gitecco.translation.constraintcomputation.util.ConstraintComputer;
import at.jku.isse.gitecco.translation.mining.ChangeCharacteristic;
import at.jku.isse.gitecco.translation.mining.ComputeRQMetrics;
import at.jku.isse.gitecco.translation.visitor.GetNodesForChangeVisitor;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import org.glassfish.grizzly.http.server.accesslog.FileAppender;
import org.jfree.data.io.CSV;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;


public class MiningMetricsTest {

    private final static boolean EVERYCOMMIT = false;
    private final static int EVERY_NTH_COMMIT = 1;
    private final static ArrayList<Feature> featureList = new ArrayList<>();
    private final static ArrayList<String> featureNamesList = new ArrayList<String>();
    private final static String REPO_PATH = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\systems\\Sqlite - Copy\\sqlite";
    //private final static String featureFolder = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\systems\\sqlite-versions\\sqlite-3.13.0";
    private final static String FEATURES_PATH = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\systems\\feature_identification\\sqlite-versions2";
    //String[] features = {"SQLITE_OMIT_BUILTIN_TEST", "SQLITE_OMIT_CTE", "SQLITE_OMIT_ATTACH", "SQLITE_ENABLE_EXPENSIVE_ASSERT", "__PTRDIFF_TYPE__", "SQLITE_OMIT_TRUNCATE_OPTIMIZATION", "SQLITE_WIN32_MALLOC", "SQLITE_OMIT_REINDEX", "SQLITE_OMIT_BETWEEN_OPTIMIZATION", "SQLITE_OMIT_LIKE_OPTIMIZATION", "SQLITE_EXTRA_SHUTDOWN", "SQLITE_CHECK_PAGES", "HAVE_UINT16_T", "SQLITE_WIN32_MALLOC_VALIDATE", "SQLITE_ENABLE_COLUMN_METADATA", "__ppc__", "TCL_MINOR_VERSION", "SQLITE_OMIT_BTREECOUNT", "SQLITE_SHM_DIRECTORY", "YYERRORSYMBOL", "SQLITE_OMIT_VIRTUALTABLE", "SQLITE_ENABLE_MEMSYS5", "SQLITE_ENABLE_MEMSYS3", "SQLITE_OMIT_AUTOINCREMENT", "__QNXNTO__", "SQLITE_OMIT_INCRBLOB", "SQLITE_OMIT_PROGRESS_CALLBACK", "SQLITE_MEMORY_BARRIER", "winIoerrCanRetry2", "SQLITE_OMIT_FLAG_PRAGMAS", "SQLITE_ENABLE_ASYNCIO", "SQLITE_SUBSTR_COMPATIBILITY", "SIGINT", "SQLITE_OMIT_XFER_OPT", "_WRS_KERNEL", "SQLITE_OMIT_AUTHORIZATION", "SQLITE_SHELL_DBNAME_PROC", "YY_SHIFT_MIN", "AMATCH_TRACE_1", "_M_IX86", "SQLITE_TCL", "HAVE_STDINT_H", "__MINGW32__", "sparc", "SQLITE_DIRECT_OVERFLOW_READ", "SQLITE_HAS_CODEC", "_CRT_INSECURE_DEPRECATE", "_HAVE_MINGW_H", "__unix__", "HAVE_MALLOC_USABLE_SIZE", "SQLITE_ENABLE_HIDDEN_COLUMNS", "SQLITE_ENABLE_FTS3_TOKENIZER", "SQLITE_RUNTIME_BYTEORDER", "SQLITE_TRACE_SIZE_LIMIT", "SQLITE_NO_SYNC", "SQLITE_IGNORE_AFP_LOCK_ERRORS", "SQLITE_OMIT_QUICKBALANCE", "SQLITE_DISABLE_DIRSYNC", "SQLITE_SECURE_DELETE", "__MAC_OS_X_VERSION_MIN_REQUIRED", "__linux__", "SQLITE_ENABLE_CURSOR_HINTS", "TEST", "SQLITE_OMIT_SHARED_CACHE", "SQLITE_PERFORMANCE_TRACE", "__HP_cc", "SQLITE_OMIT_UTF16", "_DEBUG", "SQLITE_MULTIPLEX_EXT_OVWR", "SQLITE_OMIT_CHECK", "SQLITE_WIN32_NO_WIDE", "SQLITE_OMIT_DEPRECATED", "SQLITE_LOCK_TRACE", "SQLITE_WITHOUT_ZONEMALLOC", "SQLITE_OMIT_WAL", "HAVE_LOCALTIME_R", "SQLITE_REVERSE_UNORDERED_SELECTS", "SQLITE_DISABLE_FTS3_UNICODE", "TCL_MAJOR_VERSION", "FOSSIL_OMIT_DELTA_CKSUM_TEST", "__x86", "_HAVE__MINGW_H", "SQLITE_ENABLE_FTS1", "SQLITE_ENABLE_FTS2", "SQLITE_ENABLE_DBSTAT_VTAB", "HAVE_INTTYPES_H", "SQLITE_FTS5_ENABLE_TEST_MI", "SQLITE_ENABLE_FTS5", "HAVE_PREAD64", "_M_ARM", "SQLITE_WIN32_NO_OVERLAPPED", "SQLITE_OMIT_INTEGRITY_CHECK", "MEMORY_DEBUG", "SQLITE_OMIT_FLOATING_POINT", "FD_CLOEXEC", "__minux", "SQLITE_OMIT_WSD", "SQLITE_TCL_DEFAULT_FULLMUTEX", "SQLITE_OMIT_TCL_VARIABLE", "SQLITE_ENABLE_COLUMN_USED_MASK", "SQLITE_OMIT_COMPILEOPTION_DIAGS", "SQLITE_OMIT_SUBQUERY", "SQLITE_PROXY_DEBUG", "SQLITE_WIN32_NO_ANSI", "_WIN32", "__SANITIZE_ADDRESS__", "__FAST_MATH__", "SQLITE_ENABLE_CEROD", "SQLITE_ENABLE_WHERETRACE", "__DragonFly__", "SQLITE_ENABLE_SNAPSHOT", "SQLITE_64BIT_STATS", "STRERROR_R_CHAR_P", "SQLITE_OMIT_TRIGGER", "SQLITE_OMIT_AUTOVACUUM", "SQLITE_UNLINK_AFTER_CLOSE", "SQLITE_BITMASK_TYPE", "_M_AMD64", "SQLITE_DISABLE_LFS", "SQLITE_SOUNDEX", "SQLITE_ENABLE_STAT4", "__DARWIN__", "__GLIBC__", "MemoryBarrier", "SQLITE_OMIT_OR_OPTIMIZATION", "FTS3_LOG_MERGES", "SQLITE_ENABLE_SQLLOG", "HAVE_UINT8_T", "unix", "_WIN64", "__SIZEOF_POINTER__", "SQLITE_OMIT_CONFLICT_CLAUSE", "HAVE_LINENOISE", "SQLITE_CASE_SENSITIVE_LIKE", "SQLITE_POW2_MEMORY_SIZE", "__APPLE__", "InterlockedCompareExchange", "__has_feature", "SQLITE_ENABLE_RBU", "SQLITE_ENABLE_LOAD_EXTENSION", "SQLITE_TEST_REALLOC_STRESS", "__USE_GNU", "SQLITE_OMIT_EXPLAIN", "i386", "SQLITE_EXTRA_DURABLE", "SQLITE_OMIT_DISKIO", "SQLITE_OMIT_MEMORYDB", "_MSC_VER", "HAVE_UINT32_T", "HAVE_FDATASYNC", "SQLITE_PCACHE_SEPARATE_HEADER", "SQLITE_FCNTL_VFSNAME", "SQLITE_SMALL_STACK", "_M_IA64", "SQLITE_ENABLE_STMT_SCANSTATUS", "__BORLANDC__", "SQLITE_OMIT_FOREIGN_KEY", "SQLITE_OMIT_VIEW", "SQLITE_SSE", "_CS_DARWIN_USER_TEMP_DIR", "__CYGWIN__", "__MINGW_H", "HAVE_ISNAN", "SQLITE_DEFAULT_FOREIGN_KEYS", "SQLITE_USER_AUTHENTICATION", "HAVE_PWRITE64", "SQLITE_DISABLE_FTS4_DEFERRED", "SQLITE_DISABLE_PAGECACHE_OVERFLOW_STATS", "SQLITE_OMIT_COMPOUND_SELECT", "HAVE_STRERROR_R", "SQLITE_VERSION_NUMBER", "TARGET_IPHONE_SIMULATOR", "SQLITE_WIN32_MUTEX_TRACE_STATIC", "SQLITE_ENABLE_SELECTTRACE", "SQLITE_ENABLE_ATOMIC_WRITE", "SQLITE_N_KEYWORD", "SQLITE_EXPLAIN_ESTIMATED_ROWS", "SQLITE_ENABLE_8_3_NAMES", "SQLITE_DEFAULT_AUTOMATIC_INDEX", "sqlite3Isdigit", "SQLITE_NOINLINE", "TCLSH", "__x86_64__", "HAVE_READLINE", "F_FULLFSYNC", "SQLITE_WIN32_FILEMAPPING_API", "SQLITE_VDBE_COVERAGE", "__arm__", "TRANSLATE_TRACE", "__MSVCRT__", "SQLITE_DEBUG", "VDBE_PROFILE", "__FreeBSD__", "SQLITE_ENABLE_UNLOCK_NOTIFY", "YYNOERRORRECOVERY", "HAVE_INT16_T", "SQLITE_AMALGAMATION", "SQLITE_OMIT_TEMPDB", "WIN_SHM_BASE", "SQLITE_ENABLE_API_ARMOR", "SQLITE_OMIT_PRAGMA", "SQLITE_COVERAGE_TEST", "HAVE_USLEEP", "NTDDI_VERSION", "SQLITE_TEST", "SQLITE_OMIT_PAGER_PRAGMAS", "SQLITE_USE_ALLOCA", "SQLITE_WITHOUT_MSIZE", "YYSTACKDEPTH", "SQLITE_OMIT_CAST", "HAVE_INT8_T", "SQLITE_MEMDEBUG", "SQLITE_OMIT_GET_TABLE", "SQLITE_MSVC_LOCALTIME_API", "SQLITE_WIN32_MUTEX_TRACE_DYNAMIC", "SQLITE_OMIT_SCHEMA_PRAGMAS", "SQLITE_ENABLE_BROKEN_FTS1", "SQLITE_ENABLE_BROKEN_FTS2", "SQLITE_ENABLE_COSTMULT", "INCLUDE_MSVC_H", "__IPHONE_OS_VERSION_MIN_REQUIRED", "__RTP__", "SQLITE_DISABLE_INTRINSIC", "SQLITE_ZERO_MALLOC", "O_CLOEXEC", "THREADSAFE", "SQLITE_LOG_CACHE_SPILL", "BASE", "TRACE_CRASHTEST", "_WIN32_WINNT", "SQLITE_PREFER_PROXY_LOCKING", "HAVE_POSIX_FALLOCATE", "SQLITE_LIKE_DOESNT_MATCH_BLOBS", "__ANDROID__", "SQLITE_USE_FCNTL_TRACE", "SQLITE_4_BYTE_ALIGNED_MALLOC", "SQLITE_OMIT_RANDOMNESS", "SQLITE_OMIT_AUTOINIT", "USE_SYSTEM_SQLITE", "SQLITE_TCLMD5", "SQLITE_ENABLE_JSON1", "YYWILDCARD", "_HAVE_SQLITE_CONFIG_H", "SQLITE_INT64_TYPE", "SQLITE_OMIT_PARSER_TRACE", "LOCKPROXYDIR", "SQLITE_ENABLE_MULTIPLEX", "__MINGW_MAJOR_VERSION", "HAVE_EDITLINE", "SQLITE_OMIT_AUTORESET", "SQLITE_ENABLE_FTS3_PARENTHESIS", "SQLITE_OMIT_ANALYZE", "SQLITE_SERVER", "__DJGPP__", "__i386__", "FILENAME_MAX", "SQLITE_ENABLE_OVERSIZE_CELL_CHECK", "SQLITE_PRINTF_PRECISION_LIMIT", "YY_SHIFT_MAX", "SQLITE_MMAP_READWRITE", "SQLITE_IGNORE_FLOCK_LOCK_ERRORS", "__OpenBSD__", "SQLITE_OMIT_AUTOMATIC_INDEX", "HAVE_STRCHRNUL", "SQLITE_32BIT_ROWID", "SQLITE_RTREE_INT_ONLY", "NO_GETTOD", "SQLITE_OMIT_ALTERTABLE", "SQLITE_OMIT_VACUUM", "_PCACHE_H_", "YYFALLBACK", "SQLITE_ENABLE_ZIPVFS", "TARGET_OS_EMBEDDED", "SQLITE_MEMORY_SIZE", "SQLITE_ENABLE_VFSTRACE", "__MACH__", "SQLITE_OMIT_LOOKASIDE", "SQLITE_OMIT_LOCALTIME", "SQLITE_OMIT_DECLTYPE", "SQLITE_ALLOW_URI_AUTHORITY", "YYTRACKMAXSTACKDEPTH", "HAVE_OSINST", "SQLITE_OMIT_CODEC_FROM_TCL", "SQLITE_OMIT_BLOB_LITERAL", "SQLITE_EXTRA_INIT", "_WIN32_WCE", "EINTR", "SQLITE_OMIT_SCHEMA_VERSION_PRAGMAS", "YY_ACTTAB_COUNT", "SQLITE_OMIT_COMPLETE", "__cplusplus", "SQLITE_DEFAULT_LOCKING_MODE", "_USE_64BIT_TIME_T", "SQLITE_ENABLE_ICU", "SQLITE_WIN32_USE_UUID", "_M_X64", "__GNUC__", "EOVERFLOW", "SQLITE_FORCE_OS_TRACE", "__x86_64", "SQLITE_DEFAULT_CKPTFULLFSYNC", "WIN32", "SQLITE_ENABLE_MEMORY_MANAGEMENT", "BUILD_sqlite", "SQLITE_ENABLE_UPDATE_DELETE_LIMIT", "HAVE_MALLOC_H", "__sun", "SQLITE_ENABLE_MODULE_COMMENTS", "HAVE_GMTIME_R", "SQLITE_ENABLE_IOTRACE", "UNIX_SHM_BASE", "SQLITE_ENABLE_RTREE", "SQLITE_ENABLE_PREUPDATE_HOOK", "SQLITE_ENABLE_SESSION", "SQLITE_WRITE_WALFRAME_PREBUFFERED", "ENABLE_FORCE_WAL", "SQLITE_ENABLE_SQLRR", "SQLITE_ENABLE_APPLE_SPI", "OSLOCKING_CHECK_BUSY_IOERR", "OSCLOSE_CHECK_CLOSE_IOERR", "SQLITE_ENABLE_DATA_PROTECTION", "SQLITE_USE_REQUEST_FULLFSYNC", "SQLITE_ENABLE_PERSIST_WAL", "TH3_COMPATIBILITY", "SQLITE_ENABLE_PURGEABLE_PCACHE", "SQLITE_ENABLE_AUTO_PROFILE", "SCRUB_STANDALONE", "SQLITE_OMIT_CONCURRENT", "SQLITE_MUTATION_TEST"};
    private static String feats = "{";
    String fileReportFeature = "features_report_each_project_commit.csv";
    String fileStoreConfig = "configurations.csv";
    List<String> changedFiles = new ArrayList<>();
    List<String> changedFilesNext = new ArrayList<>();
    GitCommit[] gcPrevious = {null};
    Boolean[] previous = {true};
    List<String> configurations = new ArrayList<>();
    Map<Feature, Integer> featureVersions = new HashMap<>();

    @Test
    public void identification() throws Exception {
        long measure = System.currentTimeMillis();
        String repoPath;
        repoPath = REPO_PATH;

        final GitHelper gitHelper = new GitHelper(repoPath, null);
        GitCommitList commitList = new GitCommitList(gitHelper);

        if (EVERYCOMMIT) {
            gitHelper.getAllCommits(commitList);
        } else {
            //gitHelper.getEveryNthCommit(commitList, null,STARTCOMMIT, ENDCOMMIT, EVERY_NTH_COMMIT);
            Map<Long, String> mapTags = gitHelper.getCommitNumberTag();
            LinkedHashMap<Long, String> orderedMap = mapTags.entrySet() //
                    .stream() //
                    .sorted(Map.Entry.comparingByKey()) //
                    .collect(Collectors.toMap(Map.Entry::getKey, //
                            Map.Entry::getValue, //
                            (key, content) -> content, //
                            LinkedHashMap::new)); //

            int i = 0;
            for (Map.Entry<Long, String> releases : orderedMap.entrySet()) {
                System.out.println("TAG: " + releases.getValue());
                gitHelper.getEveryNthCommit2(commitList, releases.getValue(), null, i, Math.toIntExact(releases.getKey()), EVERY_NTH_COMMIT);
                i = Math.toIntExact(releases.getKey()) + 1;
                File file = new File(FEATURES_PATH,releases.getValue().substring(releases.getValue().lastIndexOf("/") + 1));
                if (!file.exists())
                    file.mkdir();
                String folderRelease =  file.getAbsolutePath();
                final File changeFolder = new File(file, "ChangeCharacteristic");
                final File folder = new File(file, "FeatureCharacteristic");
                final File idFeatsfolder = new File(file, "IdentifiedFeatures");
                // if the directory does not exist, create it
                if (!changeFolder.exists())
                    changeFolder.mkdir();
                if (!folder.exists())
                    folder.mkdir();
                if (!idFeatsfolder.exists())
                    idFeatsfolder.mkdir();
                //feature identification
                identifyfeatures(commitList, releases.getValue(),idFeatsfolder);
                initVars(folderRelease);
                for (GitCommit commits : commitList) {
                    ComputeRQMetrics.characteristicsFeature(folder, commits.getNumber(), commits.getTree(), featureNamesList);
                    configurations = new ArrayList<>();
                    characteristicsChange2(gitHelper, changeFolder, commits, featureNamesList);
                    //dispose tree if it is not needed -> for memory saving reasons.
                    commits.disposeTree();
                }
                //RQ.2 How many times one feature changed along a number of Git commits?
                File filetxt = new File(folder.getParent(),"TimesEachFeatureChanged.txt");
                PrintWriter writerTXT = new PrintWriter( filetxt.getAbsolutePath(), "UTF-8");
                for (Map.Entry<Feature, Integer> featureRevision : featureVersions.entrySet()) {
                    if (featureRevision.getValue() > 1)
                        writerTXT.println(featureRevision.getKey() + " Changed " + (featureRevision.getValue() - 1) + " times.");
                        //System.out.println(featureRevision.getKey() + " Changed " + (featureRevision.getValue() - 1) + " times.");
                }
                writerTXT.close();
                commitList = new GitCommitList(gitHelper);
            }
        }

        //print to CSV:
        //writeToCsv(evaluation, csvPath);
        //getFeatureDeletedTimes();
        System.out.println("finished analyzing repo");
    }


    public void initVars(String folderRelease){

        changedFiles = new ArrayList<>();
        changedFilesNext = new ArrayList<>();
        gcPrevious = new GitCommit[]{null};
        previous = new Boolean[]{true};
        configurations = new ArrayList<>();
        featureVersions = new HashMap<>();

        //csv to report new features and features changed per git commit of the project
        //RQ.2 How many features changed per Git commit?
        try {
            FileWriter csvWriter = new FileWriter(folderRelease + File.separator + fileReportFeature);
            List<List<String>> headerRows = Arrays.asList(
                    Arrays.asList("CommitNumber", "NewFeatures", "ChangedFeatures")
            );
            for (List<String> rowData : headerRows) {
                csvWriter.append(String.join(",", rowData));
                csvWriter.append("\n");
            }
            csvWriter.flush();
            csvWriter.close();
        } catch (
                IOException e) {
            e.printStackTrace();
        }
        // end csv to report new features and features changed
    }

    //RQ1
    public void characteristicsChange2(GitHelper gitHelper, File changeFolder, GitCommit gc, ArrayList<String> featureNamesList) throws Exception {

        final File gitFolder = new File(gitHelper.getPath());
        final File eccoFolder = new File(gitFolder.getParent(), "ecco");

        final Integer[] countFeaturesChanged = {0}; //COUNT PER GIT COMMIT
        final Integer[] newFeatures = {0}; //COUNT PER GIT COMMIT

        GetNodesForChangeVisitor visitor = new GetNodesForChangeVisitor();
        ArrayList<ConditionalNode> changedNodes = new ArrayList<>();
        ArrayList<ConditionalNode> deletedNodes = new ArrayList<>();
        Map<Feature, ChangeCharacteristic> featureMap = new HashMap<>();

        if (gc.getNumber() == 0) {
            gcPrevious[0] = new GitCommit(gc.getCommitName(), gc.getNumber(), gc.getDiffCommitName(), gc.getBranch(), gc.getRevCommit());
            gcPrevious[0].setTree(gc.getTree());
        } else if (previous[0] || previous[0] != null) {
            if (gc.getNumber() - 1 < 1 || gc.getNumber() == 15704) {
                System.out.println("---- commit name "+ gc.getCommitName());
                previous[0] = false;
            } else {
                gcPrevious[0] = new GitCommit(gc.getRevCommit().getParent(0).getName(), gc.getNumber() - 1, gc.getRevCommit().getParent(0).getParent(0).getName(), gc.getBranch(), gc.getRevCommit().getParent(0));
                GitCommitList gcl =  new GitCommitList(gitHelper);
                gcl.addTreeParent(gcPrevious[0], gc.getCommitName());
            }
        }

        Map<Change, FileNode> changesDelete = new HashMap<>();
        //retrieve changed nodes
        for (FileNode child : gc.getTree().getChildren()) {
            if (child instanceof SourceFileNode) {

                Change[] changes = null;
                try {
                    changes = gitHelper.getFileDiffs(gc, child, false);
                } catch (Exception e) {
                    System.err.println("error while executing the file diff: " + child.getFilePath());
                    e.printStackTrace();
                }

                for (Change change : changes) {
                    if (change.getChangeType().equals("INSERT")) {
                        visitor.setChange(change);
                        child.accept(visitor);
                        changedNodes.addAll(visitor.getchangedNodes());
                    } else if (change.getChangeType().equals("DELETE")) {
                        changesDelete.put(change, child);
                    } else if (change.getChangeType().equals("CHANGE")) {
                        visitor.setChange(change);
                        child.accept(visitor);
                        changedNodes.addAll(visitor.getchangedNodes());
                    }

                }
            }
            if (gc.getNumber() == 0) {
                changedFiles.add(child.getFilePath());
                previous[0] = false;
            } else {
                changedFilesNext.add(child.getFilePath());
            }
        }

        if (previous[0]) {
            for (FileNode child : gcPrevious[0].getTree().getChildren()) {
                String start = child.getFilePath().replace("arent" + File.separator, "");
                changedFiles.add(start);
                previous[0] = false;
            }
        }

        if (gc.getNumber() == 0 || previous[0]) {
            previous[0] = false;
        } else {
            //to retrieve changed nodes of deleted files
            if (gcPrevious[0] != null) {
                for (String file : changedFiles) {
                    if (!changedFilesNext.contains(file)) {
                        FileNode child = gcPrevious[0].getTree().getChild(file);
                        if (child instanceof SourceFileNode) {
                            Change[] changes = null;
                            try {
                                changes = gitHelper.getFileDiffs(gc, child, true);
                            } catch (Exception e) {
                                System.err.println("error while executing the file diff: " + child.getFilePath());
                                e.printStackTrace();
                            }

                            for (Change change : changes) {
                                visitor.setChange(change);
                                child.accept(visitor);
                                deletedNodes.addAll(visitor.getchangedNodes());
                            }
                        }
                    }
                }
                for (Map.Entry<Change, FileNode> changeInsert : changesDelete.entrySet()) {
                    Change change = changeInsert.getKey();
                    FileNode childAux = changeInsert.getValue();
                    FileNode child = gcPrevious[0].getTree().getChild(childAux.getFilePath());
                    visitor.setChange(change);
                    child.accept(visitor);
                    deletedNodes.addAll(visitor.getchangedNodes());
                }
            }
            //next is changedFiles for the next commit
            changedFiles.removeAll(changedFiles);
            changedFiles.addAll(changedFilesNext);
            changedFilesNext.removeAll(changedFilesNext);
        }


        final ConstraintComputer constraintComputer = new ConstraintComputer(featureNamesList);
        final PreprocessorHelper pph = new PreprocessorHelper();
        Map<Feature, Integer> config;
        Set<Feature> changed;
        Set<Feature> alreadyComitted = new HashSet<>();
        Integer count = 0;

        //if there is no changed node then there must be a change in the binary files --> commit base.
        if ((changedNodes.size() == 0) && (deletedNodes.size() == 0) && (gc.getTree().getChildren().size() > 0))
            changedNodes.add(new BaseNode(null, 0));

        Boolean baseChanged = false;
        for (ConditionalNode nods : changedNodes) {
            if (nods instanceof BaseNode) {
                baseChanged = true;
            }
        }
        if (!baseChanged) {
            for (ConditionalNode nods : deletedNodes) {
                if (nods instanceof BaseNode) {
                    baseChanged = true;
                }
            }
        }

        ArrayList<String> configsToCommit = new ArrayList<>();
        Map<Map<Feature, Integer>, String> configsToGenerateVariant = new HashMap<>();
        Feature base = new Feature("BASE");
        //changedNodes = changedNodes.stream().filter(x -> x.getLocalCondition().equals("__AVR_ATmega644P__ || __AVR_ATmega644__")).collect(Collectors.toSet());
        for (ConditionalNode changedNode : changedNodes) {
            //compute the config for the var gen
            config = constraintComputer.computeConfig(changedNode, gc.getTree());
            if (config != null && !config.isEmpty()) {
                //compute the marked as changed features.
                changed = constraintComputer.computeChangedFeatures(changedNode, config);
                int tanglingDegree = 0;
                for (Map.Entry<Feature, Integer> feat : config.entrySet()) {
                    if (featureNamesList.contains(feat.getKey().getName()))
                        tanglingDegree++;
                }
                if (!changed.contains(base))
                    config.remove(base);
                //map with the name of the feature and version and a boolean to set true when it is already incremented in the analysed commit
                String eccoConfig = "";
                String file = "";
                if (changedNode.getContainingFile() != null)
                    file = changedNode.getContainingFile().getFilePath();
                for (Map.Entry<Feature, Integer> configFeature : config.entrySet()) {
                    int version = 0;
                    if (featureNamesList.contains(configFeature.getKey().getName()) && configFeature.getValue() != 0) {
                        if (featureVersions.containsKey(configFeature.getKey())) {
                            version = featureVersions.get(configFeature.getKey());
                        }
                        if (!alreadyComitted.contains(configFeature.getKey())) {
                            alreadyComitted.add(configFeature.getKey());
                            //for marlin: first commit is empty, thus we need the < 2. otherwise <1 should be enough.
                            if (changed.contains(configFeature.getKey())){ //|| gcl.size() < 2) {
                                version++;
                                if (version == 1)
                                    newFeatures[0]++;
                                else
                                    countFeaturesChanged[0]++;
                            }
                            featureVersions.put(configFeature.getKey(), version);
                        }
                        if (!configFeature.getKey().toString().equals("BASE"))
                            eccoConfig += "," + configFeature.getKey().toString() + "." + version;
                        else
                            eccoConfig += "," + configFeature.getKey().toString() + ".$$";

                        //RQ1.
                        ChangeCharacteristic changeCharacteristic;
                        if (featureMap.get(configFeature.getKey()) == null) {
                            changeCharacteristic = new ChangeCharacteristic();
                            featureMap.put(configFeature.getKey(), new ChangeCharacteristic());
                        } else {
                            changeCharacteristic = featureMap.get(configFeature.getKey());
                        }
                        int aux = 0;
                        if (changedNode.getLineNumberInserts().size() > 0) {
                            for (int i = 2; i < changedNode.getLineNumberInserts().size(); i += 3) {
                                aux += changedNode.getLineNumberInserts().get(i);
                            }
                            changeCharacteristic.setLinesOfCodeAdded(changeCharacteristic.getLinesOfCodeAdded() + aux);
                            changedNode.getLineNumberInserts().removeAll(changedNode.getLineNumberInserts());
                        }

                        changeCharacteristic.addTanglingDegree(tanglingDegree);
                        if (!changeCharacteristic.getScatteringDegreeFiles().contains(file)) {
                            changeCharacteristic.addScatteringDegreeFiles(file);
                        }
                        if (!(changedNode instanceof BaseNode))
                            changeCharacteristic.setScatteringDegreeIfs(changeCharacteristic.getScatteringDegreeIfs() + 1);
                        ChangeCharacteristic finalChangeCharacteristic = changeCharacteristic;
                        featureMap.computeIfAbsent(configFeature.getKey(), v -> finalChangeCharacteristic);
                        featureMap.computeIfPresent(configFeature.getKey(), (k, v) -> finalChangeCharacteristic);
                    }
                }
                if (!eccoConfig.contains("BASE")) {
                    eccoConfig += "," + "BASE.$$";
                }
                eccoConfig = eccoConfig.replaceFirst(",", "");

                if (configurations.contains(eccoConfig)) {
                    System.out.println("Config already used to generate a variant: " + eccoConfig);
                    //don't need to generate variant and commit it again at the same commit of the project git repository
                } else {
                    count++;
                    //configuration that will be used to generate the variant of this changed node
                    configsToGenerateVariant.put(config, eccoConfig);

                    configurations.add(eccoConfig);
                    //folder where the variant is stored
                    File variantsrc = new File(eccoFolder, eccoConfig);
                    String outputCSV = variantsrc.getParentFile().getParentFile().getAbsolutePath();
                    final Path variant_dir = Paths.get(String.valueOf(variantsrc));

                }
            }
        }

        if (deletedNodes.size() != 0) {
            String file = "";
            for (ConditionalNode deletedNode : deletedNodes) {
                file = deletedNode.getContainingFile().getFilePath();
                //compute the config for the var gen
                config = constraintComputer.computeConfig(deletedNode, gcPrevious[0].getTree());
                if (config != null && !config.isEmpty()) {
                    //compute the marked as changed features.
                    changed = constraintComputer.computeChangedFeatures(deletedNode, config);
                    int tanglingDegree = 0;
                    for (Map.Entry<Feature, Integer> feat : config.entrySet()) {
                        if (featureNamesList.contains(feat.getKey().getName()))
                            tanglingDegree++;
                    }
                    if (!changed.contains(base))
                        config.remove(base);
                    //map with the name of the feature and version and a boolean to set true when it is already incremented in the analysed commit
                    String eccoConfig = "";
                    for (Map.Entry<Feature, Integer> configFeature : config.entrySet()) {
                        int version = 0;
                        if (featureNamesList.contains(configFeature.getKey().getName()) && configFeature.getValue() != 0) {
                            if (featureVersions.containsKey(configFeature.getKey())) {
                                version = featureVersions.get(configFeature.getKey());
                            }
                            if (!alreadyComitted.contains(configFeature.getKey())) {
                                alreadyComitted.add(configFeature.getKey());
                                //for marlin: first commit is empty, thus we need the < 2. otherwise <1 should be enough.
                                if (changed.contains(configFeature.getKey())) { //gcl.size() < 2 ||
                                    version++;
                                    if (version == 1)
                                        newFeatures[0]++;
                                    else
                                        countFeaturesChanged[0]++;
                                }
                                featureVersions.put(configFeature.getKey(), version);
                            }
                            if (!configFeature.getKey().toString().equals("BASE"))
                                eccoConfig += "," + configFeature.getKey().toString() + "." + version;
                            else
                                eccoConfig += "," + configFeature.getKey().toString() + ".$$";


                            //RQ1. deleted lines
                            ChangeCharacteristic changeCharacteristic = featureMap.get(configFeature.getKey());
                            if (changeCharacteristic == null)
                                changeCharacteristic = new ChangeCharacteristic();
                            int aux = 0;
                            if (deletedNode.getLineNumberDeleted().size() > 0) {
                                for (int i = 2; i < deletedNode.getLineNumberDeleted().size(); i += 3) {
                                    aux += deletedNode.getLineNumberDeleted().get(i);
                                }
                                changeCharacteristic.setLinesOfCodeRemoved(changeCharacteristic.getLinesOfCodeRemoved() + aux);
                                deletedNode.getLineNumberDeleted().removeAll(deletedNode.getLineNumberDeleted());
                            }
                            changeCharacteristic.addTanglingDegree(tanglingDegree);
                            if (!changeCharacteristic.getScatteringDegreeFiles().contains(file)) {
                                changeCharacteristic.addScatteringDegreeFiles(file);
                            }
                            if (!(deletedNode instanceof BaseNode))
                                changeCharacteristic.setScatteringDegreeIfs(changeCharacteristic.getScatteringDegreeIfs() + 1);
                            ChangeCharacteristic finalChangeCharacteristic = changeCharacteristic;
                            featureMap.computeIfAbsent(configFeature.getKey(), v -> finalChangeCharacteristic);
                            featureMap.computeIfPresent(configFeature.getKey(), (k, v) -> finalChangeCharacteristic);
                        }
                    }
                    if (!eccoConfig.contains("BASE")) {
                        eccoConfig += "," + "BASE.$$";
                    }
                    eccoConfig = eccoConfig.replaceFirst(",", "");

                    if (configurations.contains(eccoConfig)) {
                        System.out.println("Config already used to generate a variant: " + eccoConfig);
                        //don't need to generate variant and commit it again at the same commit of the project git repository
                    } else {
                        count++;
                        //configuration that will be used to generate the variant of this changed node
                        configsToGenerateVariant.put(config, eccoConfig);

                        configurations.add(eccoConfig);


                    }
                }
            }
        }

        String baseVersion = "";
        for (Map.Entry<Feature, Integer> configFeature : featureVersions.entrySet()) {
            if (configFeature.getKey().equals(base))
                baseVersion = configFeature.getValue().toString();
        }

        //generate the variant for this config
        for (Map.Entry<Map<Feature, Integer>, String> variant : configsToGenerateVariant.entrySet()) {
            String eccoConfig = variant.getValue().replace("$$", baseVersion);
            //config that will be used to commit the variant generated with this changed node in ecco
            configsToCommit.add(eccoConfig);
        }


        //appending to the config csv
        try {

            FileAppender csvWriter = new FileAppender(new File(changeFolder.getParent(), fileStoreConfig));

            for (String configs : configsToCommit) {
                List<List<String>> headerRows = Arrays.asList(
                        Arrays.asList(Long.toString(gc.getNumber()), gc.getCommitName(), configs)
                );
                for (List<String> rowData : headerRows) {
                    csvWriter.append(String.join(",", rowData));
                }
            }
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        //append results to the feature report csv
        try {
            FileAppender csvAppender = new FileAppender(new File(changeFolder.getParent(), fileReportFeature));
            List<List<String>> contentRows = Arrays.asList(
                    Arrays.asList(Long.toString(gc.getNumber()), newFeatures[0].toString(), countFeaturesChanged[0].toString())
            );
            for (List<String> rowData : contentRows) {
                csvAppender.append(String.join(",", rowData));
            }
            csvAppender.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //end append results to the feature report csv


        countFeaturesChanged[0] = 0;
        newFeatures[0] = 0;
        for (Map.Entry<Feature, Integer> featureRevision : featureVersions.entrySet()) {
            System.out.println(featureRevision.getKey() + "." + featureRevision.getValue());
        }


        for (Map.Entry<Feature, ChangeCharacteristic> changes : featureMap.entrySet()) {
            ChangeCharacteristic changeCharacteristic = changes.getValue();
            Collections.sort(changeCharacteristic.getTanglingDegree());
            File featureCSV = new File(changeFolder, changes.getKey().getName() + ".csv");
            if (!featureCSV.exists()) {
                try {
                    FileWriter csvWriter = new FileWriter(featureCSV);
                    List<List<String>> headerRows = Arrays.asList(
                            Arrays.asList("Commit Nr", "LOC A", "LOC R", "SD IF", "SD File", "TD IF")
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
                int tangling = 0;
                if (changeCharacteristic.getTanglingDegree().size() > 0)
                    tangling = changeCharacteristic.getTanglingDegree().get(changeCharacteristic.getTanglingDegree().size() - 1);
                List<List<String>> contentRows = Arrays.asList(
                        Arrays.asList(Long.toString(gc.getNumber()), String.valueOf(changeCharacteristic.getLinesOfCodeAdded()), String.valueOf(changeCharacteristic.getLinesOfCodeRemoved()), String.valueOf(changeCharacteristic.getScatteringDegreeIfs()), String.valueOf(changeCharacteristic.getScatteringDegreeFiles().size()),
                                String.valueOf(tangling)));
                for (List<String> rowData : contentRows) {
                    csvAppender.append(String.join(",", rowData));
                }
                csvAppender.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }



    public void identifyfeatures(GitCommitList commitList, String release, File idFeatFolder) throws IOException {
        final List<TraceableFeature> evaluation = Collections.synchronizedList(new ArrayList<>());
        String csvFile = release.substring(release.lastIndexOf("/") + 1);
        for (GitCommit commits : commitList) {
            ID.evaluateFeatureMap(evaluation, ID.id(commits.getTree()), commits.getNumber());
            //dispose tree if it is not needed -> for memory saving reasons.
            if (commits.getNumber() == commitList.size() - 1) {
                //commits.disposeTree();
                writeToCsv(evaluation, csvFile, idFeatFolder);
                getFeatureDeletedTimes(idFeatFolder);
            } //else
                //commits.disposeTree();
        }


    }

    private void writeToCsv(List<TraceableFeature> features, String fileName, File idFeatsFolder) throws IOException {

        System.out.println("writing to CSV");
        FileWriter outputfile = null;
        File csvFile = new File(FEATURES_PATH, "features-" + fileName + ".csv");
        //second parameter is boolean for appending --> never append
        outputfile = new FileWriter(csvFile, false);

        // create CSVWriter object file writer object as parameter
        //deprecated but no other way available --> it still works anyways
        @SuppressWarnings("deprecation") CSVWriter writer = new CSVWriter(outputfile, ',', CSVWriter.NO_QUOTE_CHARACTER);

        //adding header to csv
        writer.writeNext(new String[]{"Label/FeatureName", "#total", "#external", "#internal", "#transient"});

        PrintWriter writerTXT = new PrintWriter(FEATURES_PATH + File.separator + "features-" + fileName + ".txt", "UTF-8");

        //write each feature/label with: Name, totalOcc, InternalOcc, externalOcc, transientOcc.
        for (TraceableFeature feature : features) {
            if (feature.getExternalOcc() == feature.getTotalOcc() || feature.getExternalOcc() == feature.getCommitList().size()) {
                feats += "\"" + feature.getName() + "\",";
                Feature feat = new Feature(feature.getName());
                featureList.add(feat);
                featureNamesList.add(feature.getName());
            }
            writer.writeNext(
                    new String[]{
                            feature.getName(),
                            feature.getTotalOcc().toString(),
                            feature.getExternalOcc().toString(),
                            feature.getInternalOcc().toString(),
                            feature.getTransientOcc().toString()
                    });
            FileWriter commitList = null;

            final File featureFile = new File(idFeatsFolder, feature.getName() + ".csv");
            commitList = new FileWriter(featureFile, false);
            CSVWriter writerFeature = new CSVWriter(commitList, ',', CSVWriter.NO_QUOTE_CHARACTER);
            writerFeature.writeNext(new String[]{"commitNumber", "Present"});
            for (Map.Entry<Long, Boolean> commit : feature.getCommitList().entrySet()) {
                writerFeature.writeNext(new String[]{String.valueOf(commit.getKey()), String.valueOf(commit.getValue())});
            }
            writerFeature.close();

        }

        writerTXT.println(feats.substring(0, feats.length() - 1) + "}");
        writerTXT.close();
        System.out.println(feats.substring(0, feats.length() - 1) + "}");
        // closing writer connection
        writer.close();
    }


    public void getFeatureDeletedTimes(File featureFolder) throws IOException {
        File[] lista = featureFolder.listFiles();
        int deletedTimes = 0;
        Map<Integer, Integer> deletePerGitCommit = new HashMap<>();
        if (lista.length > 0) {
            for (File file : lista) {
                if (featureNamesList.contains(file.getName().substring(0, file.getName().indexOf(".csv")))) {
                    Reader reader = Files.newBufferedReader(Paths.get(file.getAbsolutePath()));
                    CSVReader csvReader = new CSVReaderBuilder(reader).build();
                    List<String[]> lines = csvReader.readAll();
                    int count = 0;
                    for (int i = 1; i < lines.size(); i++) {
                        String[] line = lines.get(i);
                        String[] split = line[0].split(",");
                        if (i == 1) {
                            int first = Integer.valueOf(split[0]);
                            count = first;
                        } else {
                            int commitNumber = Integer.valueOf(split[0]);
                            if (commitNumber != (count + 1)) {
                                deletedTimes++;
                                Integer alreadyExist = deletePerGitCommit.computeIfPresent(commitNumber, (k, v) -> v + 1);
                                if (alreadyExist == null)
                                    deletePerGitCommit.put(commitNumber, 1);
                            }
                            count = commitNumber;
                        }

                    }
                    //RQ.2 How many times one feature were deleted along a number of Git commits?
                    File featureCSV = new File(featureFolder.getParent(), "deleteFeatures.csv");
                    if (!featureCSV.exists()) {
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
                                Arrays.asList(file.getName().substring(0, file.getName().indexOf(".csv")), String.valueOf(deletedTimes)));
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

            File featureCSV = new File(featureFolder.getParent(), "delFeatsGitCommit.csv");
            if (!featureCSV.exists()) {
                try {
                    FileWriter csvWriter = new FileWriter(featureCSV);
                    List<List<String>> headerRows = Arrays.asList(
                            Arrays.asList("Commit Nr.", "Nr. Feat Del")
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

            //RQ.2 How many features were deleted per Git commit?
            for (Map.Entry<Integer, Integer> commit : deletePerGitCommit.entrySet()) {
                try {
                    FileAppender csvAppender = new FileAppender(featureCSV);
                    List<List<String>> contentRows = Arrays.asList(
                            Arrays.asList(String.valueOf(commit.getKey()), String.valueOf(commit.getValue())));
                    for (List<String> rowData : contentRows) {
                        csvAppender.append(String.join(",", rowData));
                    }
                    csvAppender.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //System.out.println("Commit Number: " + commit.getKey() + " features deleted: " + commit.getValue());
            }
        }
    }

}

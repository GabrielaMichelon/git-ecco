package at.jku.isse.gitecco.core.git;

import at.jku.isse.gitecco.core.tree.nodes.BinaryFileNode;
import at.jku.isse.gitecco.core.tree.nodes.FileNode;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import org.chocosolver.solver.constraints.nary.nvalue.amnv.mis.F;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import scala.util.parsing.combinator.testing.Str;


import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static java.nio.file.Files.copy;

/**
 * HelperClass for working with JGit.
 */
public class GitHelper {

    private static Git git;
    private final String pathUrl;
    private final List<String> dirFiles;
    private Long runtimeGitCommit;

    public Long getRuntimeGitCommit() {
        return runtimeGitCommit;
    }

    public void setRuntimeGitCommit(Long runtimeGitCommit) {
        this.runtimeGitCommit = runtimeGitCommit;
    }

    //when having problems with repo --> following line checks out the latest commit
    //git checkout $(git log --branches -1 --pretty=format:"%H")

    /**
     * Creates a new instance of GitHelper and clones the specified
     * repo form the url String to the path String.
     *
     * @param url  The URL of the git repository to be cloned
     * @param path The path string, where the repository should be cloned to.
     * @throws Exception
     */
    public GitHelper(String url, String path, List<String> dirFiles) throws Exception {
        git = cloneRepo(url, path);
        pathUrl = path;
        this.dirFiles = dirFiles;
    }

    public GitHelper() {
        git = null;
        pathUrl = null;
        this.dirFiles = null;
    }

    public void gitCommitAndCheckout(String srcpathFiles, String destpathFiles, String commitName, String commitMessage) throws IOException, GitAPIException {
        Path sourcepath = Paths.get(srcpathFiles);
        Path destpath = Paths.get(destpathFiles);
        Files.setAttribute(destpath, "dos:readonly", false);
        File dest = new File(String.valueOf(destpath));
        Boolean initializeGit = true;
        if (destpath.toFile().exists()) {
            for (File path : dest.listFiles()) {
                if (path.isDirectory() && path.getName().equals(".git")) {
                    initializeGit = false;
                } else {
                    GitCommitList.recursiveDelete(path.toPath());
                }
            }
        } else {
            dest.mkdir();
            initializeGit = false;
        }

        if (initializeGit) {
            Git git = Git.init().setDirectory(dest).call();
        }

        //checkout of the specif commit of the original git project to copy the files that we need to simulate the GIT commit with the variant
        File srcPath = new File(srcpathFiles);
        Ref git = Git.open(srcPath)
                .checkout()
                .setName(commitName)
                .call();

        //end checkout of the specif commit of the original git project to copy the files that we need to simulate the GIT commit with the variant
        Path pathGit = Paths.get(sourcepath + File.separator + ".git");

        Files.walk(sourcepath)
                .forEach(source -> {
                    try {
                        if (!source.equals(pathGit))
                            copy(source, destpath.resolve(sourcepath.relativize(source)));
                    } catch (IOException e) {

                    }
                });

        File localPath = new File(destpathFiles);

        // Create the git repository with init


        // run the add-call

        Git gitOpen = Git.open(dest);
        gitOpen.add().addFilepattern(localPath.toString()).call();
        //git commit
        Long timeBefore = System.currentTimeMillis();
        gitOpen.commit().setMessage(commitMessage).call();
        Long timeAfter = System.currentTimeMillis();
        setRuntimeGitCommit(timeAfter - timeBefore);
        gitOpen.close();
        //end git commit


    }

    /**
     * Creates a new instance of GitHelper by opening an existing repository.
     * at the given path.
     * Note that the repository needs to be existing already.
     *
     * @param path The path String to the existing repository.
     * @throws IOException
     */
    public GitHelper(String path, List<String> dirFiles) throws IOException {
        git = openRepo(path);
        pathUrl = path;
        this.dirFiles = dirFiles;
    }

    public List<String> getDirFiles() {
        return dirFiles;
    }

    /**
     * Gets the path in which the repository is stored
     *
     * @return
     */
    public String getPath() {
        return this.pathUrl;
    }


    /**
     * Gets the Diff between two Commits that are not neighbors specified by their commit names.
     * The Diff is stored as a <code>Change</code>.
     * All the changes will be returned as an Array.
     *
     * @param newCommit The commit which should be diffed --> also contains the parent to diff with
     * @param sfn       the source file node which should be examined
     * @return An Array of Changes which contains all the changes between the commits.
     * @throws Exception
     */
    public Change[] getFileDiffsTwoCommits(GitCommit newCommit, FileNode sfn, Boolean deletedFile) throws Exception {
        if (sfn instanceof BinaryFileNode) throw new IllegalArgumentException("cannot diff a binary file node");
        return getFileDiffsTwoCommitsMethod(newCommit, sfn, deletedFile);
    }


    /**
     * Gets the Diff between two Commits that are not neighbors specified by their commit names.
     * The Diff is stored as a <code>Change</code>.
     * All the changes will be returned as an Array.
     *
     * @param newCommit The commit which should be diffed --> also contains the parent to diff with
     * @param filePath  The FilePath for which the Diff should be applied.
     * @return An Array of Changes which contains all the changes between the commits.
     * @throws Exception
     */
    public Change[] getFileDiffsTwoCommitsMethod(GitCommit newCommit, FileNode filePath, Boolean deletedFile) throws Exception {

        List<Change> changes = new ArrayList<Change>();

        if (deletedFile) {
            if (newCommit.getNumber() != Long.valueOf(0))
                git.stashCreate().setRef("HEAD").call();
            git.checkout().setName(newCommit.getDiffCommitName()).call();
            String filePathRenamed = "";
            if (filePath.getFilePath().contains("arent"))
                filePathRenamed = filePath.getFilePath().replace("arent" + File.separator, "");
            else
                filePathRenamed = filePath.getFilePath();
            String newPath = pathUrl + File.separator + filePathRenamed;
            ArrayList<Integer> lines = new ArrayList<>();
            lines.add(0);
            lines.add(Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1).size() - 1);
            if (filePath.getFileContent().size() == 0) {
                List<String> linesFile = Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1);
                filePath.setFileContent(linesFile);
            }
            changes.add(new Change(0, Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1).size(), lines, "DELETE"));
            git.stashCreate().setRef("HEAD").call();
            git.checkout().setName(newCommit.getCommitName()).call();
        } else {
            //prepare for file path filter.
            //String filterPath = filePath.substring(pathUrl.length()+1).replace("\\", "/");
            try {
                git.stashCreate().setRef("HEAD").call();
                git.checkout().setName(newCommit.getCommitName()).call();
            } catch (CheckoutConflictException ex) {
                System.out.println("aaaaaaaException checkout ----- " + ex);
            }
            //System.out.println("Checked out: "+newCommit.getCommitName());
            List<DiffEntry> diff;
            diff = git.diff().
                    setOldTree(prepareTreeParser(git.getRepository(), newCommit.getDiffCommitName())).
                    setNewTree(prepareTreeParser(git.getRepository(), newCommit.getCommitName())).
                    //setPathFilter(PathFilter.create(filePath)).
                            call();
            //to filter on Suffix use the following instead
            //setPathFilter(PathSuffixFilter.create(".cpp"))


            ByteArrayOutputStream diffStream = new ByteArrayOutputStream();
            DiffParser fileDiffParser = new DiffParser();


            for (DiffEntry entry : diff) {
                String newPath = entry.getNewPath().replace("/", "\\");
                String oldPath = entry.getOldPath().replace("/", "\\");
                if (filePath.getFilePath().equals(newPath)) {
                    //System.out.println("file diff: " + newPath);
                    if (entry.getChangeType().toString().equals("ADD")) {
                        newPath = pathUrl + File.separator + newPath;
                        ArrayList<Integer> lines = new ArrayList<>();
                        lines.add(0);
                        lines.add(Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1).size() - 1);
                        if (filePath.getFileContent().size() == 0) {
                            List<String> linesFile = Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1);
                            filePath.setFileContent(linesFile);
                        }
                        changes.add(new Change(0, Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1).size(), lines, "INSERT"));
                    } else if (entry.getChangeType().toString().equals("MODIFY")) {
                        List<String> actual = new ArrayList<>();
                        List<String> old = new ArrayList<>();
                        File filenew = new File(pathUrl + File.separator + newPath);
                        if (filePath.getFileContent().size() == 0) {
                            List<String> linesFile = Files.readAllLines(Paths.get(filenew.getAbsolutePath()), StandardCharsets.ISO_8859_1);
                            filePath.setFileContent(linesFile);
                            git.stashCreate().setRef("HEAD").call();
                            git.checkout().setName(newCommit.getDiffCommitName()).call();
                            if (filePath.getPreviousFileContent().size() == 0) {
                                linesFile = Files.readAllLines(Paths.get(filenew.getAbsolutePath()), StandardCharsets.ISO_8859_1);
                                filePath.setPreviousFileContent(linesFile);
                            }
                            git.stashCreate().setRef("HEAD").call();
                            git.checkout().setName(newCommit.getCommitName()).call();
                        }
                        try {
                            actual = Files.readAllLines(filenew.toPath());
                        } catch (MalformedInputException e) {
                            //StringBuilder contentBuilder = new StringBuilder();
                            BufferedReader br = new BufferedReader(new FileReader(filenew.getAbsoluteFile()));
                            String sCurrentLine;
                            while ((sCurrentLine = br.readLine()) != null) {
                                actual.add(sCurrentLine);
                            }
                            br.close();
                        }
                        try {
                            git.stashCreate().setRef("HEAD").call();
                            git.checkout().setName(newCommit.getDiffCommitName()).call();
                        } catch (CheckoutConflictException ex) {
                            System.out.println("Exception checkout ----- " + ex);
                        }
                        File fileold = new File(pathUrl + File.separator + oldPath);
                        try {
                            old = Files.readAllLines(fileold.toPath());
                        } catch (MalformedInputException e) {
                            StringBuilder contentBuilder = new StringBuilder();
                            BufferedReader br = new BufferedReader(new FileReader(fileold.getAbsoluteFile()));
                            String sCurrentLine;
                            while ((sCurrentLine = br.readLine()) != null) {
                                old.add(sCurrentLine);
                            }
                            br.close();
                        }
                        git.stashCreate().setRef("HEAD").call();
                        git.checkout().setName(newCommit.getCommitName()).call();
                        // Compute diff. Get the Patch object. Patch is the container for computed deltas.
                        Patch<String> patch = null;
                        patch = DiffUtils.diff(old, actual);
                        for (Delta delta : patch.getDeltas()) {
                            String changeType = null;
                            Integer first = 0, last = 0;
                            if (delta.getType().toString().equals("DELETE")) {
                                changeType = delta.getType().toString();
                                first = delta.getOriginal().getPosition();
                                last = delta.getOriginal().getPosition() + delta.getOriginal().getLines().size();
                                ArrayList<Integer> lines = new ArrayList<>();
                                lines.add(first);
                                lines.add(last - 1);
                                changes.add(new Change(first, last, lines, changeType));
                            } else if (delta.getType().toString().equals("INSERT")) {
                                changeType = delta.getType().toString();
                                first = delta.getRevised().getPosition();
                                last = delta.getRevised().getPosition() + delta.getRevised().getLines().size();
                                ArrayList<Integer> lines = new ArrayList<>();
                                lines.add(first);
                                lines.add(last - 1);
                                changes.add(new Change(first, last, lines, changeType));
                            } else if (delta.getType().toString().equals("CHANGE")) {
                                first = delta.getRevised().getPosition();
                                ArrayList<Integer> lines = new ArrayList<>();
                                last = delta.getRevised().getPosition() + delta.getRevised().getLines().size();
                                lines.add(first);
                                lines.add(last - 1);
                                //changes.add(new Change(first, last, lines, "CHANGE"));
                                changes.add(new Change(first, last, lines, "INSERT"));
                                last = delta.getRevised().getPosition() + delta.getOriginal().getLines().size();
                                lines = new ArrayList<>();
                                lines.add(first);
                                lines.add(last - 1);
                                changes.add(new Change(first, last, lines, "DELETE"));
                            }
                        }

                    }
                }

            }


       /* for (DiffEntry entry : diff) {
            diffStream.reset();
            try (DiffFormatter formatter = new DiffFormatter(diffStream)) {
                formatter.setRepository(git.getRepository());
                formatter.setContext(0);
                formatter.format(entry);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (diffStream.size() > 0) {
                if (fileDiffParser.parse(diffStream.toString())) {
                    for (Change r : fileDiffParser.getplusRanges()) {
                        changes.add(r);
                    }
                }
            }
            fileDiffParser.reset();
        }

        filePath = pathUrl + "\\" + filePath;

        if (newCommit.getDiffCommitName().equals("NULLCOMMIT"))
            changes.add(new Change(0, Files.readAllLines(Paths.get(filePath), StandardCharsets.ISO_8859_1).size()));
        */
        }
        return changes.toArray(new Change[changes.size()]);
    }


    /**
     * Gets the Diff between two Commits specified by their commit names.
     * The Diff is stored as a <code>Change</code>.
     * All the changes will be returned as an Array.
     *
     * @param newCommit The commit which should be diffed --> also contains the parent to diff with
     * @param sfn       the source file node which should be examined
     * @return An Array of Changes which contains all the changes between the commits.
     * @throws Exception
     */
    public Change[] getFileDiffs(GitCommit newCommit, FileNode sfn, Boolean deletedFile) throws Exception {
        if (sfn instanceof BinaryFileNode) throw new IllegalArgumentException("cannot diff a binary file node");
        return getFileDiffsMethod(newCommit, sfn, deletedFile);
    }


    /**
     * Gets the Diff between two Commits specified by their commit names.
     * The Diff is stored as a <code>Change</code>.
     * All the changes will be returned as an Array.
     *
     * @param newCommit The commit which should be diffed --> also contains the parent to diff with
     * @param filePath  The FilePath for which the Diff should be applied.
     * @return An Array of Changes which contains all the changes between the commits.
     * @throws Exception
     */
    public Change[] getFileDiffsMethod(GitCommit newCommit, FileNode filePath, Boolean deletedFile) throws Exception {

        List<Change> changes = new ArrayList<Change>();

        if (deletedFile) {
            if (newCommit.getNumber() != Long.valueOf(0))
                git.stashCreate().setRef("HEAD").call();
            git.checkout().setName(newCommit.getDiffCommitName()).call();
            String filepathRenamed = "";
            if (filePath.getFilePath().contains("arent"))
                filepathRenamed = filePath.getFilePath().replace("arent" + File.separator, "");
            else
                filepathRenamed = filePath.getFilePath();
            String newPath = pathUrl + File.separator + filepathRenamed;
            if (filePath.getFileContent().size() == 0) {
                List<String> linesFile = Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1);
                filePath.setFileContent(linesFile);
            }
            ArrayList<Integer> lines = new ArrayList<>();
            lines.add(0);
            lines.add(Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1).size() - 1);
            changes.add(new Change(0, Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1).size(), lines, "DELETE"));
            git.stashCreate().setRef("HEAD").call();
            git.checkout().setName(newCommit.getCommitName()).call();
        } else {
            //prepare for file path filter.
            //String filterPath = filePath.substring(pathUrl.length()+1).replace("\\", "/");
            try {
                git.stashCreate().setRef("HEAD").call();
                git.checkout().setName(newCommit.getCommitName()).call();
            } catch (CheckoutConflictException ex) {
                System.out.println("aaaaaaaException checkout ----- " + ex);
            }
            //System.out.println("Checked out: "+newCommit.getCommitName());
            List<DiffEntry> diff;
            diff = git.diff().
                    setOldTree(prepareTreeParser(git.getRepository(), newCommit.getDiffCommitName())).
                    setNewTree(prepareTreeParser(git.getRepository(), newCommit.getCommitName())).
                    //setPathFilter(PathFilter.create(filePath)).
                            call();
            //to filter on Suffix use the following instead
            //setPathFilter(PathSuffixFilter.create(".cpp"))


            ByteArrayOutputStream diffStream = new ByteArrayOutputStream();
            DiffParser fileDiffParser = new DiffParser();


            for (DiffEntry entry : diff) {
                String newPath = entry.getNewPath().replace("/", "\\");
                String oldPath = entry.getOldPath().replace("/", "\\");
                if (filePath.getFilePath().equals(newPath)) {
                    //System.out.println("file diff: " + newPath);
                    if (filePath.getFileContent().size() == 0) {
                        List<String> linesFile = Files.readAllLines(Paths.get(pathUrl + File.separator + newPath), StandardCharsets.ISO_8859_1);
                        filePath.setFileContent(linesFile);
                    }
                    if (entry.getChangeType().toString().equals("ADD")) {
                        newPath = pathUrl + File.separator + newPath;
                        ArrayList<Integer> lines = new ArrayList<>();
                        lines.add(0);
                        lines.add(Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1).size() - 1);
                        changes.add(new Change(0, Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1).size(), lines, "INSERT"));
                    } else if (entry.getChangeType().toString().equals("MODIFY")) {
                        List<String> actual = new ArrayList<>();
                        List<String> old = new ArrayList<>();
                        File filenew = new File(pathUrl + File.separator + newPath);
                        try {
                            actual = Files.readAllLines(filenew.toPath());
                        } catch (MalformedInputException e) {
                            StringBuilder contentBuilder = new StringBuilder();
                            BufferedReader br = new BufferedReader(new FileReader(filenew.getAbsoluteFile()));
                            String sCurrentLine;
                            while ((sCurrentLine = br.readLine()) != null) {
                                actual.add(sCurrentLine);
                            }
                            br.close();
                        }
                        try {
                            git.stashCreate().setRef("HEAD").call();
                            git.checkout().setName(newCommit.getDiffCommitName()).call();
                        } catch (CheckoutConflictException ex) {
                            System.out.println("Exception checkout ----- " + ex);
                        }
                        File fileold = new File(pathUrl + File.separator + oldPath);
                        try {
                            old = Files.readAllLines(fileold.toPath());
                        } catch (MalformedInputException e) {
                            StringBuilder contentBuilder = new StringBuilder();
                            BufferedReader br = new BufferedReader(new FileReader(fileold.getAbsoluteFile()));
                            String sCurrentLine;
                            while ((sCurrentLine = br.readLine()) != null) {
                                old.add(sCurrentLine);
                            }
                            br.close();
                        }
                        git.stashCreate().setRef("HEAD").call();
                        git.checkout().setName(newCommit.getCommitName()).call();
                        // Compute diff. Get the Patch object. Patch is the container for computed deltas.
                        Patch<String> patch = null;
                        patch = DiffUtils.diff(old, actual);
                        for (Delta delta : patch.getDeltas()) {
                            String changeType = null;
                            Integer first = 0, last = 0;
                            if (delta.getType().toString().equals("DELETE")) {
                                changeType = delta.getType().toString();
                                first = delta.getOriginal().getPosition();
                                last = delta.getOriginal().getPosition() + delta.getOriginal().getLines().size();
                                ArrayList<Integer> lines = new ArrayList<>();
                                lines.add(first);
                                lines.add(last - 1);
                                changes.add(new Change(first, last, lines, changeType));
                            } else if (delta.getType().toString().equals("INSERT")) {
                                changeType = delta.getType().toString();
                                first = delta.getRevised().getPosition();
                                last = delta.getRevised().getPosition() + delta.getRevised().getLines().size();
                                ArrayList<Integer> lines = new ArrayList<>();
                                lines.add(first);
                                lines.add(last - 1);
                                changes.add(new Change(first, last, lines, changeType));
                            } else if (delta.getType().toString().equals("CHANGE")) {
                                first = delta.getRevised().getPosition();
                                ArrayList<Integer> lines = new ArrayList<>();
                                last = delta.getRevised().getPosition() + delta.getRevised().getLines().size();
                                lines.add(first);
                                lines.add(last - 1);
                                changes.add(new Change(first, last, lines, "CHANGE"));
                                //changes.add(new Change(first, last, lines, "INSERT"));
                                last = delta.getRevised().getPosition() + delta.getOriginal().getLines().size();
                                lines = new ArrayList<>();
                                lines.add(first);
                                lines.add(last - 1);
                                //changes.add(new Change(first, last, lines, "DELETE"));

                            }
                        }

                    }
                }

            }


       /* for (DiffEntry entry : diff) {
            diffStream.reset();
            try (DiffFormatter formatter = new DiffFormatter(diffStream)) {
                formatter.setRepository(git.getRepository());
                formatter.setContext(0);
                formatter.format(entry);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (diffStream.size() > 0) {
                if (fileDiffParser.parse(diffStream.toString())) {
                    for (Change r : fileDiffParser.getplusRanges()) {
                        changes.add(r);
                    }
                }
            }
            fileDiffParser.reset();
        }

        filePath = pathUrl + "\\" + filePath;

        if (newCommit.getDiffCommitName().equals("NULLCOMMIT"))
            changes.add(new Change(0, Files.readAllLines(Paths.get(filePath), StandardCharsets.ISO_8859_1).size()));
        */
        }
        return changes.toArray(new Change[changes.size()]);
    }

    /**
     * Returns all paths to files that have changed in the repo between 2 commits.
     *
     * @param newCommit commit to diff with --> also contains the parent for diffing
     * @return List of Strings of paths.
     */
    public List<String> getChangedFiles(GitCommit newCommit) {
        final List<String> paths = new ArrayList<>();
        try {
            List<DiffEntry> diffs = git.diff().
                    setOldTree(prepareTreeParser(git.getRepository(), newCommit.getDiffCommitName())).
                    setNewTree(prepareTreeParser(git.getRepository(), newCommit.getCommitName())).
                    call();
            for (DiffEntry entry : diffs) {
                if (entry.getChangeType() != DiffEntry.ChangeType.DELETE) {
                    //if needed prepend pathURL + "\\" to get the absolute path
                    paths.add(entry.getNewPath().replace('/', '\\'));
                }
            }
        } catch (GitAPIException | IOException e) {
            e.printStackTrace();
        }

        return Collections.unmodifiableList(paths);
    }


    private Git openRepo(String dirPath) throws IOException {
        File dir = new File(dirPath);
        Git git = Git.open(dir);

        return git;
    }

    /**
     * Checks out a commit by the given name.
     * Does this by using the runtime execution since JGit is buggy
     * when it comes to checkouts and cleans, etc.
     *
     * @param name The name of the commit, which should be checked out.
     */
    public void checkOutCommit(String name) {
        /*System.out.println("Checking out commit: "+name
                +"\n at "+pathUrl);*/
        try {
            git.clean().setForce(true).call();
            git.reset().setMode(ResetCommand.ResetType.HARD).call();
            try {
                git.stashCreate().setRef("HEAD").call();
                git.checkout().setName(name).call();
            } catch (CheckoutConflictException e) {
                System.out.println(" EXCECPTION CHECKOUT. " + e);
            }

        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks out a commit by the given name.
     * Does this by using the runtime execution since JGit is buggy
     * when it comes to checkouts and cleans, etc.
     *
     * @param gc The commit, which should be checked out.
     */
    public void checkOutCommit(GitCommit gc) {
        this.checkOutCommit(gc.getCommitName());
    }


    public Git cloneRepo(String url, String dirPath) {
        File dir = new File(dirPath);
        //System.out.println("Cloning from " + url + " to " + dir);

        Git git = null;
        try {
            git = Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(dir)
                    .call();
        } catch (GitAPIException e) {
            System.err.println("problems checking out the given repo to the given destination path");
            e.printStackTrace();
        }

        //System.out.println("Having repository: " + git.getRepository().getDirectory() + "\n");

        return git;
    }

    /**
     * Lists all the Diffs between two Commits.
     *
     * @param newCommit The new commit which should be diffed --> also contains the parent to diff with.
     * @throws GitAPIException
     * @throws IOException
     */
    public void listDiff(GitCommit newCommit) throws GitAPIException, IOException {
        final List<DiffEntry> diffs = git.diff()
                .setOldTree(prepareTreeParser(git.getRepository(), newCommit.getDiffCommitName()))
                .setNewTree(prepareTreeParser(git.getRepository(), newCommit.getCommitName()))
                .call();

        //System.out.println("Found: " + diffs.size() + " differences");
        for (DiffEntry diff : diffs) {
            //System.out.println("Diff: " + diff.getChangeType() + ": " +
            //       (diff.getOldPath().equals(diff.getNewPath()) ? diff.getNewPath() : diff.getOldPath() + " -> " + diff.getNewPath()));
        }
    }


    /**
     * Gets all commit names of all the commits of the opened repository.
     *
     * @return String[] of all the commit names.
     * @throws GitAPIException
     */
    public String[] getAllCommitNames() throws GitAPIException, IOException {
        ArrayList<String> commitNames = new ArrayList();
        Iterable<RevCommit> log = git.log().call(); //extend with .all() before .call() to get ALL commits

        for (RevCommit rc : log) {
            commitNames.add(rc.getName());
        }

        Collections.reverse(commitNames);
        return commitNames.toArray(new String[commitNames.size()]);
    }

    /**
     * Method to retrieve all commits form a repository and put it to a GitCommitList.
     *
     * @param commits the GitCommitList to which the commits a re saved to.
     * @return The GitCommitList which was passed to the method.
     * @throws GitAPIException
     * @throws IOException
     */
    public GitCommitList getAllCommits(GitCommitList commits) throws Exception {
        final boolean FASTMODE = false;
        final Repository repository = git.getRepository();
        final Collection<Ref> allRefs = repository.getRefDatabase().getRefs();

        RevWalk revWalk = new RevWalk(repository);
        revWalk.sort(RevSort.TOPO, true);
        revWalk.sort(RevSort.REVERSE, true);
        revWalk.setRevFilter(RevFilter.NO_MERGES);

        //revWalk.markStart(revWalk.parseCommit(((List<Ref>) allRefs).get(0).getObjectId()));
        for (Ref ref : allRefs) revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));

        long number = 0;
        for (RevCommit rc : revWalk) {
            String branch = FASTMODE ? null : getBranchOfCommit(rc.getName());
            String parent;

            try {
                parent = rc.getParent(0).getName();
            } catch (ArrayIndexOutOfBoundsException e) {
                parent = "NULLCOMMIT";
            }
            commits.add(new GitCommit(rc.getName(), number, parent, branch, rc));
            number++;
        }

        return commits;
    }


    /**
     * Method to retrieve every nth commit from a repository and put it to a GitCommitList.
     * starts with a certain commit number and ends with a certain commit number
     *
     * @param commits the GitCommitList to which the commits are saved to.
     * @return The GitCommitList which was passed to the method.
     * @throws GitAPIException
     * @throws IOException
     */
    public GitCommitList getEveryNthCommit(GitCommitList commits, String firstDiff, int startcommit, int endcommit, int n) throws Exception {
        final Repository repository = git.getRepository();
        final Collection<Ref> allRefs = repository.getRefDatabase().getRefs();

        RevWalk revWalk = new RevWalk(repository);
        revWalk.sort(RevSort.TOPO, true);
        revWalk.sort(RevSort.REVERSE, true);


        for (Ref ref : allRefs) revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
        long number = 0;
        String parent = firstDiff;
        boolean enter = false;
        for (RevCommit rc : revWalk) {
            if (number > endcommit) break;

            if (number >= startcommit) {
                String branch = getBranchOfCommit(rc.getName());

                if (parent == null || enter) {
                    try {
                        parent = rc.getParent(0).getName();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        parent = "NULLCOMMIT";
                    }
                } else {
                    enter = true;
                }

                commits.add(new GitCommit(rc.getName(), number, parent, branch, rc));
            }

            number += number < startcommit ? 1 : n;
        }

        return commits;
    }


    /**
     * Method to retrieve the diff of two commits from a repository and put it to a GitCommitList.
     * starts with a specific commit and ends with another specific commit hash
     *
     * @param commits the GitCommitList to which the commits are saved to.
     * @return The GitCommitList which was passed to the method.
     * @throws GitAPIException
     * @throws IOException
     */
    public GitCommitList getTwoCommits(GitCommitList commits, String firstcommit, String secondcommit) throws Exception {
        final Repository repository = git.getRepository();
        List<Ref> tags = new ArrayList<Ref>(repository.getTags().values());
        RevWalk revWalk = new RevWalk(repository);
        revWalk.setRetainBody(false);
        revWalk.sort(RevSort.REVERSE, true);
        revWalk.setRevFilter(RevFilter.NO_MERGES);
        revWalk.setRetainBody(false);
        ObjectId id = repository.resolve(firstcommit);
        RevCommit rc = revWalk.parseCommit(id);
        String parent;

        //System.out.println(rc.getName());
        String branch = getBranchOfCommit(rc.getName());

        try {
            parent = rc.getParent(0).getName();
        } catch (ArrayIndexOutOfBoundsException e) {
            parent = "NULLCOMMIT";
        }
        commits.add(new GitCommit(rc.getName(), 1, parent, branch, rc));

        id = repository.resolve(secondcommit);
        rc = revWalk.parseCommit(id);
        try {
            parent = rc.getParent(0).getName();
        } catch (ArrayIndexOutOfBoundsException e) {
            parent = "NULLCOMMIT";
        }
        commits.add(new GitCommit(rc.getName(), 2, parent, branch, rc));

        return commits;
    }

    /**
     * Method to retrieve every nth commit from a repository and put it to a GitCommitList.
     * starts with a certain commit number and ends with a certain commit number
     *
     * @param commits the GitCommitList to which the commits are saved to.
     * @return The GitCommitList which was passed to the method.
     * @throws GitAPIException
     * @throws IOException
     */
    public GitCommitList getEveryNthCommit3(GitCommitList commits, String firstDiff, int startcommit, int endcommit, int n) throws Exception {
        final Repository repository = git.getRepository();
        final Collection<Ref> allRefs = repository.getRefDatabase().getRefs();

        RevWalk revWalk = new RevWalk(repository);
        revWalk.setRetainBody(false);
        revWalk.sort(RevSort.REVERSE, true);
        revWalk.setRevFilter(RevFilter.NO_MERGES);
        revWalk.setRetainBody(false);

        for (Ref ref : allRefs) revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
        long number = 0;
        String parent = firstDiff;
        boolean enter = false;

        if (startcommit != 0)
            startcommit -= 1;
        for (RevCommit rc : revWalk) {
            if (number > endcommit) break;

            if (number >= startcommit) {
                String branch = getBranchOfCommit(rc.getName());

                if (parent == null || enter) {
                    try {
                        parent = rc.getParent(0).getName();
                        enter = true;
                    } catch (ArrayIndexOutOfBoundsException e) {
                        parent = "NULLCOMMIT";
                        enter = true;
                    }
                } else {
                    enter = true;
                }

                commits.add(new GitCommit(rc.getName(), number, parent, branch, rc));
            }

            number += number < startcommit ? 1 : n;
        }

        return commits;
    }


    /**
     * Method to retrieve every nth commit from a repository and put it to a GitCommitList.
     * starts with a certain commit number and ends with a certain commit number
     *
     * @param commits the GitCommitList to which the commits are saved to.
     * @return The GitCommitList which was passed to the method.
     * @throws GitAPIException
     * @throws IOException
     */
    public GitCommitList getEveryNthCommit2(GitCommitList commits, String release, String firstDiff, int startcommit, int endcommit, int n) throws Exception {
        final Repository repository = git.getRepository();
        List<Ref> tags = new ArrayList<Ref>(repository.getTags().values());
        RevWalk revWalk = new RevWalk(repository);
        revWalk.setRetainBody(false);
        revWalk.sort(RevSort.REVERSE, true);
        revWalk.setRevFilter(RevFilter.NO_MERGES);
        revWalk.setRetainBody(false);

        for (Ref ref : tags) {
            if (ref.getName().equals(release)) {
                revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
                break;
            }
        }
        long number = 0;
        String parent = firstDiff;
        boolean enter = false;
        if (startcommit != 0)
            startcommit -= 1;
        for (RevCommit rc : revWalk) {
            if (number > endcommit) break;

            if (number >= startcommit) {
                String branch = getBranchOfCommit(rc.getName());

                if (parent == null || enter) {
                    try {
                        parent = rc.getParent(0).getName();
                        enter = true;
                    } catch (ArrayIndexOutOfBoundsException e) {
                        parent = "NULLCOMMIT";
                        enter = true;
                    }
                } else {
                    enter = true;
                }

                commits.add(new GitCommit(rc.getName(), number, parent, branch, rc));
            }

            number += number < startcommit ? 1 : n;
        }

        return commits;
    }

    /**
     * Method to retrieve every tags and each commit number where each release is finished
     *
     * @return The a map with commit number of each release/tag of the repository.
     * @throws GitAPIException
     * @throws IOException
     */
    public Map<Long, String> getCommitNumberTag() throws Exception {
        final Repository repository = git.getRepository();
        List<Ref> tags = new ArrayList<Ref>(repository.getTags().values());
        RevWalk revWalk = new RevWalk(repository);
        revWalk.setRetainBody(false);
        revWalk.sort(RevSort.REVERSE, true);
        revWalk.setRevFilter(RevFilter.NO_MERGES);
        revWalk.sort(RevSort.COMMIT_TIME_DESC);
        revWalk.setRetainBody(false);

        Map<Long, String> tagsCommits = new HashMap<>();
        ArrayList<Long> tagsNumberCommits = new ArrayList<>();

        for (Ref ref : tags) {
            revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
            long number = 0;
            for (RevCommit rc : revWalk) {
                number++;
            }
            tagsCommits.put(number, ref.getName());
            tagsNumberCommits.add(number);
            revWalk.dispose();
        }

        Collections.sort(tagsNumberCommits);
        for (Long nr : tagsNumberCommits) {
            System.out.println("Commit number: " + nr + " tag: " + tagsCommits.get(nr));

        }
        tags.clear();
        tagsNumberCommits.clear();

        /*revWalk.markStart( revWalk.parseCommit( git.getRepository().resolve( "version-3.10.0" ) ) );
        RevCommit next = revWalk.next();
        while( next != null ) {
            // ::pseudo code::
            // in real, iterate over allTags and compare each tag.getObjectId() with next
            System.out.println(next);
            if(tags.contains( next ) ) {

                // remember next and exit while loop
            }
            next = revWalk.next();
        }
        revWalk.close();*/
        return tagsCommits;

    }


    /**
     * Retrieves the file paths of the files in the repository for a given commit
     *
     * @param commit
     * @return List of paths as Strings
     * @throws IOException
     */
    public List<String> getRepositoryContents(GitCommit commit) {
        final List<String> files = new ArrayList<>();

        try {
            RevWalk walk = new RevWalk(git.getRepository());

            RevCommit revCommit = walk.parseCommit(git.getRepository().resolve(commit.getCommitName()));
            RevTree tree = revCommit.getTree();

            TreeWalk treeWalk = new TreeWalk(git.getRepository());
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);

            while (treeWalk.next()) {
                files.add(treeWalk.getPathString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Collections.unmodifiableList(files);
    }

    /**
     * Retrieves the first branch of a commit.
     * Actually not very useful. --> disabled through the fastmode in getAllCommits.
     *
     * @param commit
     * @return
     * @throws MissingObjectException
     * @throws GitAPIException
     */
    private String getBranchOfCommit(String commit) throws MissingObjectException, GitAPIException {
        Map<ObjectId, String> map = git
                .nameRev()
                .addPrefix("refs/heads")
                .add(ObjectId.fromString(commit))
                .call();

        //TODO: probably better solution: one commit may have multiple branches
        return map.isEmpty() ? "" : map.get(ObjectId.fromString(commit)).split("~")[0];
    }


    private AbstractTreeIterator prepareTreeParser(Repository repository, String commitName) throws IOException {
        if (commitName == null || commitName.equals("NULLCOMMIT")) return new EmptyTreeIterator();

        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(repository.resolve(commitName));
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }
            walk.dispose();

            return treeParser;
        }
    }

    public static void copyFolder(File source, File destination, ArrayList<String> filesRemoved, Map<String, FileChange> filesChanged) {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }

            String files[] = source.list();

            for (String file : files) {
                File srcFile = new File(source, file);
                if (!srcFile.getAbsolutePath().contains(".git")) {
                    File destFile = new File(destination, file);
                    copyFolder(srcFile, destFile, filesRemoved, filesChanged);
                }
            }
        } else if (!filesRemoved.contains(source.getName()) && !filesChanged.keySet().contains(source.getName())) {
            InputStream in = null;
            OutputStream out = null;

            try {
                in = new FileInputStream(source);
                out = new FileOutputStream(destination);

                byte[] buffer = new byte[1024];

                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
                in.close();
                out.close();
            } catch (Exception e) {
                try {
                    in.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    out.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public static void copyNewFiles(File source, File destination, ArrayList<String> filesAdded, Map<String, FileChange> filesChanged) {
        if (source.isDirectory()) {
            String files[] = source.list();

            for (String file : files) {
                File srcFile = new File(source, file);
                if (!srcFile.getAbsolutePath().contains(".git")) {
                    File destFile = new File(destination, file);
                    copyNewFiles(srcFile, destFile, filesAdded, filesChanged);
                }
            }
        } else if (filesAdded.contains(source.getName()) && !filesChanged.keySet().contains(source.getName())) {
            InputStream in = null;
            OutputStream out = null;

            try {
                in = new FileInputStream(source);
                out = new FileOutputStream(destination);

                byte[] buffer = new byte[1024];

                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
                in.close();
                out.close();
            } catch (Exception e) {
                try {
                    in.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    out.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * Propagate the changes of a feature between two Commits chosen by the user.
     * All the changes will be in a folder with the files resulted from the merge of changes.
     *
     * @param firstCommit  The commit which should be applied the changes
     * @param secondCommit The commit which changes are chosen to be propagated
     * @return true
     * @throws Exception
     */
    public static Boolean changePropagation(File mergeDir, String gitDir, String firstCommit, String secondCommit, ArrayList<String> filesAdded, ArrayList<String> filesRemoved, Map<String, FileChange> filesChanged) throws Exception {

        File gitDirectory = new File(gitDir);
        Map<String, ArrayList<String>> filesToWrite = new HashMap<>();
        List<String> fileLines = new ArrayList<>();
        List<String> previousLines = new ArrayList<>();

        git.stashCreate().setRef("HEAD").call();
        git.checkout().setName(secondCommit).call();
        copyNewFiles(gitDirectory, mergeDir, filesAdded, filesChanged);

        git.stashCreate().setRef("HEAD").call();
        git.checkout().setName(firstCommit).call();
        //Files.copy(gitDirectory, mergeDir, StandardCopyOption.REPLACE_EXISTING);
        copyFolder(gitDirectory, mergeDir, filesRemoved, filesChanged);

        String linesaddinit = "";
        String linesaddend = "";
        String linesremovedinit = "";
        String linesremovedend = "";
        for (Map.Entry<String, FileChange> change : filesChanged.entrySet()) {
            ArrayList<String> lines = change.getValue().getLines();
            fileLines = change.getValue().getFileLines();
            previousLines = change.getValue().getPreviousContent();
            ArrayList<Integer> linesaddinitArray = new ArrayList<>();
            ArrayList<Integer> linesaddendArray = new ArrayList<>();
            ArrayList<Integer> linesremovedinitArray = new ArrayList<>();
            ArrayList<Integer> linesremovedendArray = new ArrayList<>();
            List<String> linesToWrite = new ArrayList<>();

            int i = 0;
            for (String lineChanges : lines) {
                String[] index = lineChanges.split(",");
                if(!lineChanges.contains("added: []")) {
                    linesaddinit = index[0].substring(index[0].indexOf("added: [") + 8).replaceAll(" ", "");
                    linesaddend = index[1].replaceAll(" ", "");
                    linesaddinitArray.add(Integer.valueOf(linesaddinit));
                    linesaddendArray.add(Integer.valueOf(linesaddend));
                }
                if (!lineChanges.contains("removed: []")) {
                    linesremovedinit = index[2].substring(index[2].indexOf("removed: [") + 10).replaceAll(" ", "");
                    linesremovedend = index[3].replaceAll(" ", "");
                    linesremovedinitArray.add(Integer.valueOf(linesremovedinit));
                    linesremovedendArray.add(Integer.valueOf(linesremovedend));
                }
            }
            ArrayList<Integer> linesaddinitArrayAux = new ArrayList<>();
            ArrayList<Integer> linesaddendArrayAux = new ArrayList<>();
            ArrayList<Integer> linesremovedinitArrayAux = new ArrayList<>();
            ArrayList<Integer> linesremovedendArrayAux = new ArrayList<>();

            int index = 0;
            for (Integer value:linesaddinitArray) {
                if(!value.equals("")){
                    linesaddinitArrayAux.add(value);
                    linesaddendArrayAux.add(linesaddendArray.get(index));
                }
                index++;
            }
            index=0;
            for (Integer value:linesremovedinitArray) {
                if(!value.equals("")){
                    linesremovedinitArrayAux.add(value);
                    linesremovedendArrayAux.add(linesremovedendArray.get(index));
                }
                index++;
            }

            if(linesaddinitArrayAux.size() > 0){
                linesaddinit = String.valueOf(linesaddinitArrayAux.remove(0));
                linesaddend =  String.valueOf(linesaddendArrayAux.remove(0));
            }
            if(linesremovedinitArrayAux.size() > 0){
                linesremovedinit = String.valueOf(linesremovedinitArray.remove(0));
                linesremovedend =  String.valueOf(linesremovedendArray.remove(0));
            }
            int linesaddinitAux=Integer.valueOf(linesaddinit);
            int end = fileLines.size();
            if(previousLines != null && previousLines.size()>fileLines.size())
                end=previousLines.size();
            while(i<end) {
                if (!linesaddinit.equals("") && i >= (Integer.valueOf(linesaddinit)-1) && i <= (Integer.valueOf(linesaddend)-1)) {
                    linesToWrite.add(fileLines.get(i));
                    if(i==(Integer.valueOf(linesaddend)-1) && linesaddinitArrayAux.size() > 0){
                        i=Integer.valueOf(linesaddinit)-1;
                        linesaddinit = String.valueOf(linesaddinitArrayAux.remove(0));
                        linesaddend = String.valueOf(linesaddendArrayAux.remove(0));
                        i++;
                    }else if(i==(Integer.valueOf(linesaddend)-1)){
                        i=linesaddinitAux;
                        linesaddinit="";
                        i++;
                    }else if(!linesremovedend.equals("") && Integer.valueOf(linesremovedend)==i){

                    }else{
                        i++;
                    }
                } else if (!linesremovedend.equals("")  && (i > (Integer.valueOf(linesremovedend)-1) || i < (Integer.valueOf(linesremovedinit)-1))) {
                    linesToWrite.add(previousLines.get(i));
                    if(i > (Integer.valueOf(linesremovedend)-1) && linesremovedendArrayAux.size() > 0){
                        linesremovedend = String.valueOf(linesremovedendArrayAux.remove(0));
                        linesremovedinit = String.valueOf(linesremovedinitArrayAux.remove(0));
                    }
                    i++;
                }else{
                    if(previousLines!=null)
                        linesToWrite.add(previousLines.get(i));
                    else
                        linesToWrite.add(fileLines.get(i));
                    i++;
                }

            }
            if(previousLines != null) {
                linesToWrite.add(previousLines.get(i));
            }
            Files.write(Paths.get(mergeDir + File.separator + change.getKey()), linesToWrite, Charset.defaultCharset());
        }
        return true;
    }

}

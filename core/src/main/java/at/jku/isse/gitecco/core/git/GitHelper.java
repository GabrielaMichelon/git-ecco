package at.jku.isse.gitecco.core.git;

import at.jku.isse.gitecco.core.tree.nodes.BinaryFileNode;
import at.jku.isse.gitecco.core.tree.nodes.FileNode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * HelperClass for working with JGit.
 */
public class GitHelper {

    private final Git git;
    private final String pathUrl;

    /**
     * Creates a new instance of GitHelper and clones the specified
     * repo form the url String to the path String.
     *
     * @param url  The URL of the git repository to be cloned
     * @param path The path string, where the repository should be cloned to.
     * @throws Exception
     */
    public GitHelper(String url, String path) throws Exception {
        git = cloneRepo(url, path);
        pathUrl = path;
    }

    /**
     * Creates a new instance of GitHelper by opening an existing repository.
     * at the given path.
     * Note that the repository needs to be existing already.
     *
     * @param path The path String to the existing repository.
     * @throws IOException
     */
    public GitHelper(String path) throws IOException {
        git = openRepo(path);
        pathUrl = path;
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
     * Gets the Diff between two Commits specified by their commit names.
     * The Diff is stored as a <code>Change</code>.
     * All the changes will be returned as an Array.
     *
     * @param newCommit The commit which should be diffed --> also contains the parent to diff with
     * @param sfn the source file node which should be examined
     * @return An Array of Changes which contains all the changes between the commits.
     * @throws Exception
     */
    public Change[] getFileDiffs(GitCommit newCommit, FileNode sfn) throws Exception {
        if(sfn instanceof BinaryFileNode) throw new IllegalArgumentException("cannot diff a binary file node");
        return getFileDiffs(newCommit, sfn.getFilePath());
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
    public Change[] getFileDiffs(GitCommit newCommit, String filePath) throws Exception {

        //prepare for file path filter.
        //String filterPath = filePath.substring(pathUrl.length()+1).replace("\\", "/");
        List<DiffEntry> diff = git.diff().
                setOldTree(prepareTreeParser(git.getRepository(), newCommit.getDiffCommitName())).
                setNewTree(prepareTreeParser(git.getRepository(), newCommit.getCommitName())).
                setPathFilter(PathFilter.create(filePath)).
                call();

        //to filter on Suffix use the following instead
        //setPathFilter(PathSuffixFilter.create(".cpp"))

        List<Change> changes = new ArrayList<Change>();
        ByteArrayOutputStream diffStream = new ByteArrayOutputStream();
        DiffParser fileDiffParser = new DiffParser();

        for (DiffEntry entry : diff) {
            diffStream.reset();
            try (DiffFormatter formatter = new DiffFormatter(diffStream)) {
                formatter.setRepository(git.getRepository());
                formatter.setContext(0);
                formatter.format(entry);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (diffStream.size()>0) {
                if (fileDiffParser.parse(diffStream.toString())) {
                    for (Change r : fileDiffParser.getplusRanges()) {
                        changes.add(r);
                    }
                }
            }
            fileDiffParser.reset();
        }

        filePath = pathUrl + "\\" + filePath;

        if (changes.size() == 0) changes.add(new Change(0, Files.readAllLines(Paths.get(filePath), StandardCharsets.ISO_8859_1).size()));

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
                if(entry.getChangeType() != DiffEntry.ChangeType.DELETE) {
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
            git.checkout().setName(name).call();
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


    private Git cloneRepo(String url, String dirPath) {
        File dir = new File(dirPath);
        System.out.println("Cloning from "+url+" to "+dir);

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

        System.out.println("Having repository: "+git.getRepository().getDirectory()+"\n");

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

        System.out.println("Found: "+diffs.size()+" differences");
        for (DiffEntry diff : diffs) {
            System.out.println("Diff: "+diff.getChangeType()+": "+
                    (diff.getOldPath().equals(diff.getNewPath()) ? diff.getNewPath() : diff.getOldPath()+" -> "+diff.getNewPath()));
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
        final boolean FASTMODE = true;
        final List<GitCommitType> types = new ArrayList<>();
        final Repository repository = git.getRepository();
        final Collection<Ref> allRefs = repository.getRefDatabase().getRefs();

        RevWalk revWalk = new RevWalk(repository);
        revWalk.sort(RevSort.TOPO, true);
        revWalk.sort(RevSort.REVERSE, true);

        for(Ref ref : allRefs) revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));

        for(RevCommit rc : revWalk) {
            String branch = FASTMODE ? null : getBranchOfCommit(rc.getName());
            String parent;

            try {
                parent = rc.getParent(0).getName();
            }catch (ArrayIndexOutOfBoundsException e) {
                parent = "NULLCOMMIT";
            }
            commits.add(new GitCommit(rc.getName(), parent, null/*new ArrayList<GitCommitType>(types)*/, branch, rc));
        }

        return commits;
    }

    /**
     * Retrieves the file paths of the files in the repository for a given commit
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

}

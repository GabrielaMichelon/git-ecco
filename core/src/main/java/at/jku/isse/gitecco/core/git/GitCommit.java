package at.jku.isse.gitecco.core.git;

import at.jku.isse.gitecco.core.tree.nodes.RootNode;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.Collections;
import java.util.List;

/**
 * Class for handling commits and be able to
 * distinguish between normal commits, branch points and merges.
 */
public class GitCommit {
    private RootNode tree;
    private final String commitName;
    private final String diffCommit;
    private final List<GitCommitType> types;
    private final String branch;
    private final RevCommit revCommit;

    /**
     * Creates a new GitCommit
     *
     * @param commitName
     * @param types
     * @param branch
     */
    public GitCommit(String commitName, String diffCommit, List<GitCommitType> types, String branch, RevCommit rc) {
        this.commitName = commitName;
        this.types = types;
        this.branch = branch;
        this.diffCommit = diffCommit;
        this.revCommit = rc;
    }

    /**
     * Dispose the tree and invoke garbage collection.
     * For memory saving purposes.
     */
    public void disposeTree() {
        this.tree = null;
    }

    /**
     * Gets the tree of a commit
     * @return
     */
    public RootNode getTree(){
        return tree;
    }

    /**
     * sets the tree once, a second call is not effective
     * @param n
     */
    public void setTree(RootNode n) {
        if(tree == null) tree = n;
    }

    /**
     * Gets the branch of the commit
     *
     * @return
     */
    public String getBranch() {
        return branch;
    }

    /**
     * Gets the SHA1 ID aka the name of the commit to diff with
     *
     * @return the commit name / SHA1 ID
     */
    public String getDiffCommitName() {
        return diffCommit;
    }

    /**
     * Gets the SHA1 ID aka the name of the commit.
     *
     * @return the commit name / SHA1 ID
     */
    public String getCommitName() {
        return commitName;
    }

    /**
     * Gets the types of the commit: commit/branch/merge
     *
     * @return
     */
    public List<GitCommitType> getType() {
        return Collections.unmodifiableList(types);
    }
}

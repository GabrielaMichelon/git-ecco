package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.tree.visitor.Visitable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class for a root node of a commit tree.
 */
public final class RootNode extends Node implements Visitable {
    private final String path;
    private final List<FileNode> children;

    /**
     * Creates a new root node. The path is the path of the repository.
     * @param path
     */
    public RootNode(String path) {
        this.path = path;
        this.children = new ArrayList<>();
    }

    /**
     * Adds a new child to the root node.
     * These children can only be an instance of a FileNode.
     * @param n
     */
    public void addChild(FileNode n) {
        children.add(n);
    }

    @Override
    public void accept(TreeVisitor v) {
        for (FileNode child : children) {
            v.visit(this);
            child.accept(v);
        }
    }

    public List<FileNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Return null --> no parent.
     * @return
     */
    @Override
    public Node getParent() {
        return null;
    }
}

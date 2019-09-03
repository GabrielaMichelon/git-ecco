package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.tree.visitor.Visitable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Class for representing a source file of a repository in a commit tree.
 * A Source File has a BASE which will be the BASE feature.
 */
public final class SourceFileNode extends FileNode implements Visitable {
    /**The Base feature of this file.*/
    private ConditionBlockNode base;

    public SourceFileNode(RootNode parent, String filePath) {
        super(parent, filePath);
        base = null;
    }

    /**
     * Sets the given ConditionBlockNode as the BASE feature of the source file.
     * This can only be performed once. If this is called a second time an IllegalAccessException
     * will be raised.
     * @param n
     * @throws IllegalAccessException
     */
    public void setBase(ConditionBlockNode n) throws IllegalAccessException {
        if(base == null) this.base = n;
        else throw new IllegalAccessException("Cannot set base twice.");
    }

    public Collection<IncludeNode> getIncludesBase(){
        Collection<IncludeNode> includes = this.base.getIfBlock().getIncludeNodes();
        return includes;
    }

    @Override
    public void accept(TreeVisitor v) {
        if(base != null) base.accept(v);
        v.visit(this);
    }
}

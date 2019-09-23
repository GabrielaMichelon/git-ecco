package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.tree.visitor.Visitable;

/**
 * Class for representing a #include&lt;filename&gt; preprocessor statement
 */
public final class IncludeNode extends ConditionNode implements Visitable {
    private final String fileName;
    private final int lineInfo;
    private final ConditionalNode parent;

    public IncludeNode(String fileName, int lineInfo, ConditionalNode parent) {
        this.fileName = fileName;
        this.lineInfo = lineInfo;
        this.parent = parent;
    }

    /**
     * Retrieves the line info of this include node.
     * @return
     */
    public int getLineInfo() {
        return lineInfo;
    }

    /**
     * Retrieves the name of the file which is to be included into the source file.
     * @return
     */
    public String getFileName() {
        return this.fileName;
    }

    @Override
    public void accept(TreeVisitor v) {
        v.visit(this);
    }

    /**
     * Never used --> always accessed through the parent node.
     * @return
     */
    @Override
    public ConditionalNode getParent() {
        return this.parent;
    }
}

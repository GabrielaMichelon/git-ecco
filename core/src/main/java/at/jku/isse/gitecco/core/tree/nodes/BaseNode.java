package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.tree.visitor.Visitable;

public class BaseNode extends ConditionalNode implements Visitable {
    private final SourceFileNode file;

    public BaseNode(SourceFileNode file,int linecnt) {
        super(null);
        this.file = file;
        try {
            this.setLineFrom(0);
            this.setLineTo(linecnt);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public SourceFileNode getFileNode() {
        return this.file;
    }

    @Override
    public String getCondition() {
        return "BASE";
    }

    @Override
    public String getLocalCondition() {
        return "BASE";
    }

    @Override
    public void accept(TreeVisitor v) {
        for (ConditionBlockNode child : getChildren()) {
            child.accept(v);
        }
        for (DefineNode defineNode : getDefineNodes()) {
            defineNode.accept(v);
        }
        for (IncludeNode includeNode : getIncludeNodes()) {
            includeNode.accept(v);
        }
        v.visit(this);
    }
}

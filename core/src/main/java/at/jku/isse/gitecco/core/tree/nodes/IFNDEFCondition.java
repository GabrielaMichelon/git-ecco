package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.tree.visitor.Visitable;

/**
 * Class for representing an IFNDEF Condition.
 * Which means the condition of this Node must not be defined to be evaluated as true.
 */
public final class IFNDEFCondition extends ConditionalNode implements Visitable {
    private final String condition;

    public IFNDEFCondition(ConditionBlockNode parent, String condition) {
        super(parent);
        this.condition = "!(" + condition + ")";
    }

    @Override
    public String getCondition() {
            return this.condition;
    }

    @Override
    public String getLocalCondition() {
            return getCondition();
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

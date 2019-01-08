package at.jku.isse.gitecco.tree.nodes;

import at.jku.isse.gitecco.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.tree.visitor.Visitable;

/**
 * Class for representing an IFDEF Condition, which means the condition of this must
 * be defined for this to be evaluated as true.
 */
public final class IFDEFCondition extends ConditionalNode implements Visitable {
    private final String condition;

    public IFDEFCondition(Node parent, String condition) {
        super(parent);
        this.condition = condition;
    }


    @Override
    public String getCondition() {
        return this.condition;
    }

    @Override
    public void accept(TreeVisitor v) {
        for (ConditionBlockNode child : getChildren()) {
            child.accept(v);
        }
        v.visit(this);
    }
}
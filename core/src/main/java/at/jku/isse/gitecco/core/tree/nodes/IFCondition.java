package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.tree.visitor.Visitable;

/**
 * Class for representing an IFCondition.
 * Name speaks for itself.
 */
public final class IFCondition extends ConditionalNode implements Visitable {
    private final String condition;

    public IFCondition(ConditionBlockNode parent, String condition) {
        super(parent);
        this.condition = condition;
    }

    @Override
    public String getCondition() {
        //getting the parent blocks
        String expression = "(" + this.condition + ")";
        ConditionalNode conditionalNode = getParent().getParent();
        ConditionalNode changedNodeParent = getParent().getIfBlock().getParent().getParent();
        if (!changedNodeParent.getLocalCondition().contains("BASE")) {
            conditionalNode = changedNodeParent;
            while (!(conditionalNode.getParent().getParent().getLocalCondition().contains("BASE"))) {
                expression += " && ("+conditionalNode.getLocalCondition() + ")";
                conditionalNode = conditionalNode.getParent().getParent();
            }
            changedNodeParent = conditionalNode.getParent().getParent();
        }
        return  expression;
    }

    @Override
    public String getLocalCondition() {
        return this.condition;
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

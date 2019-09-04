package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.tree.visitor.Visitable;

/**
 * Class for representing an IFDEF Condition, which means the condition of this must
 * be defined for this to be evaluated as true.
 */
public final class IFDEFCondition extends ConditionalNode implements Visitable {
    private final String condition;

    public IFDEFCondition(ConditionBlockNode parent, String condition) {
        super(parent);
        this.condition = condition;
    }


    @Override
    public String getCondition() {
        //getting the parent blocks
        String expression = "(" + this.condition + ")";
        if (!getLocalCondition().contains("BASE")) {
            ConditionalNode conditionalNode = getParent().getParent();
            ConditionalNode changedNodeParent = getParent().getIfBlock().getParent().getParent();
            conditionalNode = changedNodeParent;
            while (!(conditionalNode.getParent().getParent().getLocalCondition().contains("BASE"))) {
                expression += " && (" + conditionalNode.getLocalCondition() + ")";
                conditionalNode = conditionalNode.getParent().getParent();
            }
            changedNodeParent = conditionalNode.getParent().getParent();
            expression += " && (" + conditionalNode.getParent().getParent().getLocalCondition() + ")";
            return expression;
        }
        return expression;
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

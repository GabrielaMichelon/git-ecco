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
        if(this.condition.contains("!"))
            return this.condition;//.replace("!", "==0");
        else
            return this.condition+"==1";
    }

    @Override
    public String getLocalCondition() {
        return  this.condition;
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

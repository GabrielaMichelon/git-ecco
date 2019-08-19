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
        String aux;
        if (this.condition.contains(">")) {
            aux=this.condition.substring(0, this.condition.indexOf(">"));
        }else if(this.condition.contains("<")){
            aux=this.condition.substring(0, this.condition.indexOf("<"));
        }else{
            aux=this.condition;
        }
            if (aux.contains("!"))
                return aux.replace("!", "") + "==0";
            else
                return aux + "==1";

    }

    @Override
    public String getLocalCondition() {
        return
                this.condition;
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

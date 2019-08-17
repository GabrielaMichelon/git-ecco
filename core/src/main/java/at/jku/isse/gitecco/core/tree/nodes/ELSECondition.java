package at.jku.isse.gitecco.core.tree.nodes;


import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.tree.visitor.Visitable;

/**
 * Class for representing an ELSECondition.
 * The name is slightly misleading since this has no condition on its own.
 * This feature/cond. depends on the corresponding IF/IFDEF/IFNDEF Condition.
 * It should represent an ELSE clause of an IF ELSE PPStatement.
 */
public final class ELSECondition extends ConditionalNode implements Visitable {

    public ELSECondition(ConditionBlockNode parent) {
        super(parent);
    }

    @Override
    public String getCondition() {
        StringBuilder ret = new StringBuilder();
        if(getParent().getIfBlock().getCondition().contains("!")){
            ret.append(getParent().getIfBlock().getLocalCondition().replace("!","==1"));
        }else{
            ret.append(getParent().getIfBlock().getLocalCondition()+"==0");
        }
        //ret.append("!" + getParent().getIfBlock().getCondition() );
        for (ELIFCondition elseIfBlock : getParent().getElseIfBlocks()) {
            if(elseIfBlock.getLocalCondition().contains("!")){
                ret.append(" && "+elseIfBlock.getLocalCondition().replace("!","==1"));
            }else{
                ret.append(" && "+elseIfBlock.getLocalCondition()+"==0");
            }
            //ret.append("!" + elseIfBlock.getCondition() + " && ");
        }
        return ret.toString();
    }

    @Override
    public String getLocalCondition() {
        return getCondition();
        //return "";
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

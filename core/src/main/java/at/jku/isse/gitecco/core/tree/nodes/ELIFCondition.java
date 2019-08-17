package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;

public final class ELIFCondition extends ConditionalNode {

    private final String condition;

    public ELIFCondition(ConditionBlockNode parent, String condition) {
        super(parent);
        this.condition = condition;
    }

    @Override
    public String getCondition() {
        /*StringBuilder ret = new StringBuilder();
        ret.append("!(" + getParent().getIfBlock().getCondition() + ")");
        for (ELIFCondition elseIfBlock : getParent().getElseIfBlocks()) {
            if(this.equals(elseIfBlock)) {
                break;
            }
            ret.append("!" + elseIfBlock.getCondition() + " && ");
        }
        return ret.toString();*/
        StringBuilder ret = new StringBuilder();
        if(getLocalCondition().contains("!")){
            ret.append(getLocalCondition().replace("!","==0"));
        }else{
            ret.append(getLocalCondition() + "==1");
        }
        if(getParent().getIfBlock().getCondition().contains("==0")){
            ret.append(" && "+getParent().getIfBlock().getCondition().replace("==0", "==1"));
        }else{
            ret.append(" && "+getParent().getIfBlock().getCondition().replace("==1", "==0"));
        }

        for (ELIFCondition elseIfBlock : getParent().getElseIfBlocks()) {
            if(this.equals(elseIfBlock)) {
                break;
            }
            if(elseIfBlock.getLocalCondition().contains("!")){
                ret.append(" && "+elseIfBlock.getLocalCondition().replace("!","==0") );
            }else{
                ret.append(" && "+elseIfBlock.getLocalCondition() + "==1");
            }
        }
        return ret.toString();
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

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
        StringBuilder ret = new StringBuilder();
        ret.append("!(" + getParent().getIfBlock().getCondition()+")");
        for (ELIFCondition elseIfBlock : getParent().getElseIfBlocks()) {
            if(this.equals(elseIfBlock)) {
                break;
            }
            ret.append(" && !(" + elseIfBlock.getLocalCondition()+")");
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
        for (NonConditionalNode node : getDefinesAndIncludes()) {
            node.accept(v);
        }
        v.visit(this);
    }
}

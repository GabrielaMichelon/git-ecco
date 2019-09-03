package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.Visitable;

import java.util.Objects;

/**
 * SuperClass for all define nodes (define, undef)
 */
public abstract class DefineNode extends ConditionNode implements Comparable<DefineNode>, Visitable {
    private final String macroName;
    private final int lineInfo;
    private final ConditionalNode parent;
    private final String condition;

    public DefineNode(String name, int lineInfo, ConditionalNode parent) {
        this.lineInfo = lineInfo;
        this.macroName = name;
        this.parent = parent;
        condition = parent.getCondition();
    }

    public DefineNode(String name, int lineInfo, ConditionalNode includeParent, Node defineNodeParent) {
        this.lineInfo = lineInfo;
        this.macroName = name;
        ConditionalNode condParent = (ConditionalNode) includeParent;
        this.parent = condParent;
        ConditionalNode cond = (ConditionalNode) defineNodeParent;
        if (condParent.getCondition() != null) {
            this.condition = "(" + condParent.getCondition() + ") && (" + ((ConditionalNode) defineNodeParent).getCondition() + ")";
        }else{
            this.condition =  ((ConditionalNode) defineNodeParent).getCondition();
        }

    }

    public String getCondition() {
        return condition;
    }

    public String getMacroName() {
        return macroName;
    }

    public int getLineInfo() {
        return this.lineInfo;
    }

    /**
     * Determines if the given node is identical to this one.
     * ATTENTION: This does also compare the line info of the given node.
     * use equals to check only for macro name.
     *
     * @param n
     * @return
     */
    public boolean isIdentical(DefineNode n) {
        return this.equals(n) && this.lineInfo == n.lineInfo;
    }


    @Override
    public int compareTo(DefineNode o) {
        if (Integer.compare(getLineInfo(), o.getLineInfo()) == 0) {
            return getMacroName().compareTo(o.getMacroName());
        }
        return Integer.compare(getLineInfo(), o.getLineInfo());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefineNode that = (DefineNode) o;
        return lineInfo == that.lineInfo &&
                macroName.equals(that.macroName) &&
                Objects.equals(parent, that.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(macroName, lineInfo);
    }

    /**
     * Never needed for define nodes. Will always be accessed through parent.
     *
     * @return null
     */
    @Override
    public Node getParent() {
        return this.parent;
    }
}

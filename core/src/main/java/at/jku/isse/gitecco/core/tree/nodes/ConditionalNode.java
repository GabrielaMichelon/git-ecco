package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.git.Change;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract class for representing a conditional expression.
 * Will be used for If, ifndef, ifdef, else conditions.
 */
public abstract class ConditionalNode extends ConditionNode {
    private int lineFrom = -1;
    private int lineTo = -1;
    private final List<ConditionBlockNode> children;
    private final ConditionBlockNode parent;
    private List<DefineNode> defineNodes = new ArrayList();
    private final List<IncludeNode> includeNodes = new ArrayList<>();

    public ConditionalNode(ConditionBlockNode parent) {
        children = new ArrayList<ConditionBlockNode>();
        this.parent = parent;
    }

    /**
     * Adds a new include as IncludeNode to the ConditionalNode.
     * @param n the IncludeNode
     */
    public void addInclude(IncludeNode n) {
        this.includeNodes.add(n);
    }

    /**
     * Returns a list of all the include nodes contained in this ConditionalNode
     * @return list of all includes in the conditional node.
     */
    public List<IncludeNode> getIncludeNodes() {
        return Collections.unmodifiableList(includeNodes);
    }

    /**
     * Adds a new define to the node.
     * @param d
     */
    public void addDefineNode(DefineNode d) {
        this.defineNodes.add(d);
    }

    public void deleteDefineNode(DefineNode d){
        this.defineNodes.remove(d);
    }

    /**
     * Returns all the define nodes parented by this node.
     * @return
     */
    public List<DefineNode> getDefineNodes() {
        return Collections.unmodifiableList(defineNodes);
    }

    /**
     * Evaluates if this Node contains a given change.
     * @param c
     * @return
     */
    public boolean containsChange(Change c) {
        if(c == null) return false;
        if(lineFrom == -1 || lineTo == -1)
            throw new IllegalStateException("line values have not been set correctly");
        return lineFrom <= c.getFrom() && lineTo >= c.getTo();
    }

    /**
     * Adds a child to this node.
     * Children are of types ConditionBlockNode. Which then will have ConditionalNodes as children.
     * @param n
     * @return
     */
    public ConditionBlockNode addChild(ConditionBlockNode n) {
        this.children.add(n);
        return n;
    }

    /**
     * Sets the LineFrom info. This can only be performed once.
     * On second call this will throw an IllegalAccessException.
     * @param lineFrom
     * @throws IllegalAccessException
     */
    public void setLineFrom(int lineFrom) throws IllegalAccessException {
        if(this.lineFrom == -1) this.lineFrom = lineFrom;
        else throw new IllegalAccessException("Cannot set the line more than once");
    }

    /**
     * Sets the LineTo info. This can only be performed once.
     * On second call this will thro an IllegalAccessException.
     * @param lineTo
     * @throws IllegalAccessException
     */
    public void setLineTo(int lineTo) throws IllegalAccessException {
        if(this.lineTo == -1) this.lineTo = lineTo;
        else throw new IllegalAccessException("Cannot set the line more than once");
    }

    /**
     * Retrieves the LineForm info.
     * --> start of this condition.
     * @return
     */
    public int getLineFrom() {
        return lineFrom;
    }

    @Override
    public ConditionBlockNode getParent() {
        return this.parent;
    }

    /**
     * Retrieves the LineTo info.
     * --> end of this condition.
     * @return
     */
    public int getLineTo() {
        return lineTo;
    }

    /**
     * Returns a list of all the children this node has.
     * @return
     */
    public List<ConditionBlockNode> getChildren() {
        return Collections.unmodifiableList(children);
    }


    /**
     * Returns the condition of this node.
     * @return
     */
    public abstract String getCondition();

    /**
     * Returns just the local (immediate) condition of this node.
     * @return
     */
    public abstract String getLocalCondition();
}

package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.tree.visitor.Visitable;

/**
 * Class to represent a #define preprocessor statement
 */
public final class Define extends DefineNode implements Visitable {
    private final String macroExpansion;

    public Define(String name, String exp, int lineInfo, ConditionalNode parent) {
        super(name, lineInfo, parent);
        this.macroExpansion = exp;
    }

    public Define(String name, String exp, int lineInfo, ConditionalNode parent, Node defineParent) {
        super(name, lineInfo, parent, defineParent);
        this.macroExpansion = exp;
    }

    public String getMacroExpansion() {
        return macroExpansion;

    }

    @Override
    public String toString() {
        return "#define " + this.getMacroName() + " " + this.getMacroExpansion();
    }

    @Override
    public void accept(TreeVisitor v) {
        v.visit(this);
    }
}

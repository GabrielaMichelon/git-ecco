package at.jku.isse.gitecco.featureid.featuretree.visitor;

import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GetAllDefinesVisitor implements TreeVisitor {
    private final List<DefineNode> defines;

    public GetAllDefinesVisitor() {
        defines = new ArrayList<>();
    }

    /**
     * Returns all the found defines and undefs.
     * @return all the found defines and undefs.
     */
    public List<DefineNode> getDefines() {
        return Collections.unmodifiableList(defines);
    }

    /**
     * Resets the visitor for a new round of traversing.
     */
    public void reset() {
        this.defines.clear();
    }

    @Override
    public void visit(RootNode n) {

    }

    @Override
    public void visit(BinaryFileNode n) {

    }

    @Override
    public void visit(SourceFileNode n) {

    }

    @Override
    public void visit(ConditionBlockNode n) {

    }

    @Override
    public void visit(IFCondition c) {

    }

    @Override
    public void visit(IFDEFCondition c) {

    }

    @Override
    public void visit(IFNDEFCondition c) {

    }

    @Override
    public void visit(ELSECondition c) {

    }

    @Override
    public void visit(ELIFCondition c) {

    }

    @Override
    public void visit(Define d) {
        defines.add(d);
    }

    @Override
    public void visit(Undef d) {
        defines.add(d);
    }

    @Override
    public void visit(IncludeNode n) {

    }

    @Override
    public void visit(BaseNode n) {

    }
}

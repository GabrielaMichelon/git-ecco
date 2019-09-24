package at.jku.isse.gitecco.translation.visitor;

import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;

import java.util.ArrayList;
import java.util.List;

public class GetAllIndirectIncludesVisitor implements TreeVisitor {

    private List<IncludeNode> includeNodes = new ArrayList<>();
    private Integer lineInformation;


    public List<IncludeNode> getIncludeNodes() {
        return includeNodes;
    }

    /**
     * New visitor with the linenumber that the found includes should have
     * @param lineNumber
     */
    public GetAllIndirectIncludesVisitor(Integer lineNumber) {
        this.lineInformation = lineNumber;
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

    }

    @Override
    public void visit(Undef d) {

    }

    @Override
    public void visit(IncludeNode n) {
        includeNodes.add(new IncludeNode(n.getFileName(), lineInformation, n.getParent(),n.getCondition()));
    }

    @Override
    public void visit(BaseNode n) {

    }
}

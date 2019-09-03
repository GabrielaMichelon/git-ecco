package at.jku.isse.gitecco.translation.constraintcomputation.util;

import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;

import java.util.ArrayList;
import java.util.List;

public class GetAllIncludesVisitor implements TreeVisitor {

    private List<IncludeNode> includeNodes = new ArrayList<>();
    private Integer lineInformation;


    public List<IncludeNode> getIncludeNodes() {
        return includeNodes;
    }

    public GetAllIncludesVisitor(Integer lineNumber) {
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
        if(this.lineInformation != null){
            if(this.lineInformation > n.getLineInfo()){
                includeNodes.add(n);
            }
        }else{
            includeNodes.add(n);
        }
    }
}

package at.jku.isse.gitecco.translation.visitor;

import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GetAllDefinesVisitor implements TreeVisitor {

    private List<DefineNode> defineNodes = new ArrayList<>();
    private Integer lineInformation;

    @Deprecated
    public List<DefineNode> getDefineNodes() {
        return defineNodes;
    }

    public GetAllDefinesVisitor(Integer lineNumber) {
        this.lineInformation = lineNumber;
    }


    public List<DefineNode> getDefines() {
        return Collections.unmodifiableList(defineNodes);
    }

    public List<DefineNode> getModifiedDefines(int newLineinfo) {
        List<DefineNode> ret = new ArrayList<>();
        defineNodes.forEach(node -> {
           if(node instanceof Define) {
               ret.add(new Define(node.getMacroName(),((Define) node).getMacroExpansion(),newLineinfo,node.getParent()));
           } else {
               //in case of undefine return a new define with macro expansion zero --> heuristic for solver
               ret.add(new Define(node.getMacroName(),"0",newLineinfo,node.getParent()));
           }
        });
        return Collections.unmodifiableList(ret);
    }

    public void reset(){
        this.defineNodes.clear();
        this.lineInformation = null;
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
        if (this.lineInformation != null) {
            if (this.lineInformation > d.getLineInfo()) {
                this.getDefineNodes().add(d);
            }
        } else {
            this.getDefineNodes().add(d);
        }
    }

    @Override
    public void visit(Undef d) {
        if (this.lineInformation != null) {
            if (this.lineInformation > d.getLineInfo()) {
                this.getDefineNodes().add(d);
            }
        } else {
            this.getDefineNodes().add(d);
        }
    }

    @Override
    public void visit(IncludeNode n) {

    }

    @Override
    public void visit(BaseNode n) {

    }
}

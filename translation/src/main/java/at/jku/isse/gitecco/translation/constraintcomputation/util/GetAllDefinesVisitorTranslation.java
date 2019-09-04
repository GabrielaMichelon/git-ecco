package at.jku.isse.gitecco.translation.constraintcomputation.util;

import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GetAllDefinesVisitorTranslation implements TreeVisitor {

    private List<DefineNode> defineNodes = new ArrayList<>();
    private Integer lineInformation;


    public List<DefineNode> getDefineNodes() {
        return defineNodes;
    }

    public GetAllDefinesVisitorTranslation(Integer lineNumber) {
        this.lineInformation = lineNumber;
    }


    public List<DefineNode> getDefines() {
        return Collections.unmodifiableList(defineNodes);
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

   /* public void insertAndSort(DefineNode defineNode){
        if(this.getDefineNodes().size() == 0){
            this.getDefineNodes().add(defineNode);
        }else {
            List<DefineNode> listSecond = new ArrayList<DefineNode>(this.getDefineNodes().size() + 1);
            int i = 0;
            while ((i < this.getDefineNodes().size()) && (this.getDefineNodes().get(i).getLineInfo() < defineNode.getLineInfo())) {
                listSecond.add(this.getDefineNodes().get(i));
                i++;
            }
            listSecond.add(defineNode);
            while (i < this.getDefineNodes().size()) {
                listSecond.add(this.getDefineNodes().get(i));
                i++;
            }
            this.getDefineNodes().clear();
            this.getDefineNodes().addAll(listSecond);
        }

    }*/

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
}

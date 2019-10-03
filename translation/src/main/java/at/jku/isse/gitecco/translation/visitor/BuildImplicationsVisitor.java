package at.jku.isse.gitecco.translation.visitor;

import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.core.type.FeatureImplication;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class BuildImplicationsVisitor implements TreeVisitor {

    private final Map<Feature, Queue<FeatureImplication>> implMap;
    private final String precondition;
    private final RootNode root;
    private final Integer line;


    public BuildImplicationsVisitor(Map<Feature, Queue<FeatureImplication>> implMap, RootNode root, int line) {
        this.implMap = implMap;
        this.precondition = null;
        this.root = root;
        this.line = line;
    }

    private BuildImplicationsVisitor(Map<Feature, Queue<FeatureImplication>> implMap, RootNode root, IncludeNode include) {
        this.implMap = implMap;
        this.precondition = include.getCondition();
        this.root = root;
        this.line = null;
    }

    public Map<Feature, Queue<FeatureImplication>> getImplMap() {
        return implMap;
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
        if( line != null && d.getLineInfo() > line) return;

        if((d.getParent() instanceof IFNDEFCondition) && (d.getLineInfo()-1 == d.getParent().getLineFrom())) {
            if(d.getParent().getLocalCondition().contains(d.getMacroName())) return;
        }

        String cond = precondition == null ? d.getCondition() : "(" + precondition + ") && (" + d.getCondition() + ")";
        FeatureImplication impl = new FeatureImplication(cond, d.getMacroName() + " == " + d.getMacroExpansion());

        Feature feature = new Feature(d.getMacroName());

        if(implMap.containsKey(feature)) {
            implMap.get(feature).add(impl);
        } else {
            implMap.put(feature, new LinkedList<FeatureImplication>());
        }
    }

    @Override
    public void visit(Undef d) {
        if( line != null && d.getLineInfo() > line) return;

        String cond = precondition == null ? d.getCondition() : "(" + precondition + ") && (" + d.getCondition() + ")";
        FeatureImplication impl = new FeatureImplication(cond, d.getMacroName() + " == 0");

        Feature feature = new Feature(d.getMacroName());

        if(implMap.containsKey(feature)) {
            implMap.get(feature).add(impl);
        } else {
            implMap.put(feature, new LinkedList<FeatureImplication>());
        }
    }

    @Override
    public void visit(IncludeNode n) {
        BuildImplicationsVisitor v = new BuildImplicationsVisitor(implMap, root, n);
        FileNode sfn = root.getChild(n.getFileName());
        if(sfn != null) sfn.accept(v);
        //else System.out.println(n.getFileName() + " cannot be found in the repository");
    }

    @Override
    public void visit(BaseNode n) {

    }
}

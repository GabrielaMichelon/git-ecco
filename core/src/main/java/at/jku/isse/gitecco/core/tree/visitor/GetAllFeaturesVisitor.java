package at.jku.isse.gitecco.core.tree.visitor;

import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.type.Feature;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GetAllFeaturesVisitor implements TreeVisitor {
    private final Set<Feature> features;

    public GetAllFeaturesVisitor() {
        this.features = new HashSet<>();
    }

    public Set<Feature> getAllFeatures() {
        return Collections.unmodifiableSet(features);
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
        features.addAll(Feature.parseCondition(c.getCondition()));
    }

    @Override
    public void visit(IFDEFCondition c) {
        features.addAll(Feature.parseCondition(c.getCondition()));
    }

    @Override
    public void visit(IFNDEFCondition c) {
        features.addAll(Feature.parseCondition(c.getCondition()));
    }

    @Override
    public void visit(ELSECondition c) {
    }

    @Override
    public void visit(ELIFCondition c) {
        features.addAll(Feature.parseCondition(c.getLocalCondition()));
    }

    @Override
    public void visit(Define d) {

    }

    @Override
    public void visit(Undef d) {

    }

    @Override
    public void visit(IncludeNode n) {

    }
}

package at.jku.isse.gitecco.featureid.featuretree.visitor;

import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.type.Feature;

import java.util.*;

public class GetAllFeaturesDefinesIncludesVisitor implements TreeVisitor {
    private final Map<Feature, Integer> featureMap;
    private final List<DefineNode> defines;
    private final List<IncludeNode> includes;

    public GetAllFeaturesDefinesIncludesVisitor() {
        featureMap = new HashMap<>();
        defines = new ArrayList<>();
        includes = new ArrayList<>();
    }

    public void reset() {
        featureMap.clear();
        defines.clear();
        includes.clear();
    }

    /**
     * Returns a set of all encountered features
     * @return a set of all encountered features
     */
    public Set<Feature> getFeatures() {
        return Collections.unmodifiableSet(featureMap.keySet());
    }

    /**
     * Returns all encountered features mapped to the line of their first occurrence.
     * @return all encountered features mapped to the line of their first occurrence.
     */
    public Map<Feature, Integer> getFeatureMap() {
        return Collections.unmodifiableMap(featureMap);
    }

    /**
     * Returns all the found defines and undefs.
     * @return all the found defines and undefs.
     */
    public List<DefineNode> getDefines() {
        return Collections.unmodifiableList(defines);
    }

    /**
     * Returns all the found includes.
     * @return all the found includes.
     */
    public List<IncludeNode> getIncludes() {
        return Collections.unmodifiableList(includes);
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
        for (Feature feature : Feature.parseCondition(c.getCondition())) {
            if(!featureMap.containsKey(feature)) featureMap.put(feature, c.getLineFrom());
        }
    }

    @Override
    public void visit(IFDEFCondition c) {
        for (Feature feature : Feature.parseCondition(c.getCondition())) {
            if(!featureMap.containsKey(feature)) featureMap.put(feature, c.getLineFrom());
        }
    }

    @Override
    public void visit(IFNDEFCondition c) {
        for (Feature feature : Feature.parseCondition(c.getCondition())) {
            if(!featureMap.containsKey(feature)) featureMap.put(feature, c.getLineFrom());
        }
    }

    @Override
    public void visit(ELIFCondition c) {
        for (Feature feature : Feature.parseCondition(c.getLocalCondition())) {
            if(!featureMap.containsKey(feature)) featureMap.put(feature, c.getLineFrom());
        }
    }

    @Override
    public void visit(ELSECondition c) {

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
        includes.add(n);
    }

    @Override
    public void visit(BaseNode n) {
        for (Feature feature : Feature.parseCondition(n.getLocalCondition())) {
            if(!featureMap.containsKey(feature)) featureMap.put(feature, n.getLineFrom());
        }
    }
}

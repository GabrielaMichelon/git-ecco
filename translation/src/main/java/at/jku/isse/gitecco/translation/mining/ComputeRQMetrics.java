package at.jku.isse.gitecco.translation.mining;

import at.jku.isse.gitecco.core.git.Change;
import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.tree.visitor.GetAllFeaturesVisitor;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.translation.constraintcomputation.util.ConstraintComputer;
import scala.Int;

import java.util.*;
import java.util.function.Function;

public class ComputeRQMetrics {


    /**
     * RQ.3 feature's characteristics in a tree of one commit.
     *
     * @param tree
     * @return
     */
    public static Map<Feature, FeatureCharacteristic> CharacteristicsFeature(RootNode tree, List<Feature> features, List<String> featureNamesList) {

        Map<Feature, FeatureCharacteristic> featureMap = new HashMap<>();
        GetAllConditionalStatementsVisitor visitor = new GetAllConditionalStatementsVisitor();
        Set<ConditionalNode> conditionalNodes = new HashSet<>();
        Set<ConditionalNode> negatedConditionalNodes = new HashSet<>();
        final ConstraintComputer constraintComputer = new ConstraintComputer(featureNamesList);
        Feature baseFeature = new Feature("BASE");
        for (FileNode child : tree.getChildren()) {
            if (child instanceof SourceFileNode) {
                int from = ((SourceFileNode) child).getBaseNode().getLineFrom();
                int to = ((SourceFileNode) child).getBaseNode().getLineTo();
                Change changes = new Change(from, to, null);
                visitor.setChange(changes);
                child.accept(visitor);
                conditionalNodes.addAll(visitor.getConditionalNodes());
                negatedConditionalNodes.addAll(visitor.getNegatedConditionalNodes());
                String file = child.getFilePath();
                Boolean first = true;
                int last = 0;
                //RQ3.LOC
                if (visitor.getLinesConditionalNodes().size() != 0) {
                    for (Map.Entry<Integer, Integer> linesBase : visitor.getLinesConditionalNodes().entrySet()) {
                        FeatureCharacteristic featureCharacteristic = featureMap.get(baseFeature);
                        if (first && from < linesBase.getKey()) {
                            int add = (linesBase.getKey() - from) - 1;
                            if (featureCharacteristic == null)
                                featureCharacteristic = new FeatureCharacteristic();
                            featureCharacteristic.setLinesOfCode(featureCharacteristic.getLinesOfCode() + (add));
                            last = linesBase.getValue();
                            first = false;
                        } else {
                            if (last == 0) {
                                last = linesBase.getValue();
                                first = false;
                                break;
                            } else {
                                if (last + 1 < linesBase.getKey()) {
                                    int add = (linesBase.getKey() - last) - 1;
                                    if (featureCharacteristic == null)
                                        featureCharacteristic = new FeatureCharacteristic();
                                    featureCharacteristic.setLinesOfCode(featureCharacteristic.getLinesOfCode() + (add));
                                }
                                last = linesBase.getValue();
                            }
                        }
                        //RQ3: SD files
                        if (!featureCharacteristic.getScatteringDegreeFiles().contains(file)) {
                            featureCharacteristic.addScatteringDegreeFiles(file);
                        }
                        FeatureCharacteristic finalFeatureCharacteristic = featureCharacteristic;
                        featureMap.computeIfAbsent(baseFeature, v -> finalFeatureCharacteristic);
                        featureMap.computeIfPresent(baseFeature, (k, v) -> finalFeatureCharacteristic);
                    }
                    if (last != to) {
                        int add = to - last;
                        FeatureCharacteristic featureCharacteristic = featureMap.get(baseFeature);
                        if (featureCharacteristic == null)
                            featureCharacteristic = new FeatureCharacteristic();
                        featureCharacteristic.setLinesOfCode(featureCharacteristic.getLinesOfCode() + (add));
                        FeatureCharacteristic finalFeatureCharacteristic = featureCharacteristic;
                        featureMap.computeIfAbsent(baseFeature, v -> finalFeatureCharacteristic);
                        featureMap.computeIfPresent(baseFeature, (k, v) -> finalFeatureCharacteristic);
                    }
                } else {
                    FeatureCharacteristic featureCharacteristic = featureMap.get(baseFeature);
                    if (featureCharacteristic == null)
                        featureCharacteristic = new FeatureCharacteristic();
                    //RQ3: SD files
                    if (!featureCharacteristic.getScatteringDegreeFiles().contains(file)) {
                        featureCharacteristic.addScatteringDegreeFiles(file);
                    }
                    featureCharacteristic.setLinesOfCode(featureCharacteristic.getLinesOfCode() + (to - from));
                    FeatureCharacteristic finalFeatureCharacteristic = featureCharacteristic;
                    featureMap.computeIfAbsent(baseFeature, v -> finalFeatureCharacteristic);
                    featureMap.computeIfPresent(baseFeature, (k, v) -> finalFeatureCharacteristic);
                }
                //}
            }
        }
        visitor.reset();
        Set<Feature> changed = new HashSet<>();

        for (ConditionalNode cNode : conditionalNodes) {
            GetAllFeaturesVisitor visitorFeatures = new GetAllFeaturesVisitor();
            cNode.accept(visitorFeatures);
            List<Feature> featureInteractions = new ArrayList<>();
            featureInteractions.addAll(visitorFeatures.getAllFeatures());
            featureInteractions.remove(baseFeature);
            int count=0;
            if(featureInteractions.size()>1){
                for (Feature featInteraction:featureInteractions) {
                    if(features.contains(featInteraction)){
                        count++;
                    }
                }
            }

            Map<Feature, Integer> config;
            config = constraintComputer.computeConfig(cNode, tree);
            changed = new HashSet<>();
            if (config != null && !config.isEmpty()) {
                //compute the marked as changed features.
                for (Map.Entry<Feature, Integer> feat : config.entrySet()) {
                    if (feat.getValue() != 0) {
                        changed.add(feat.getKey());
                    }
                }
                //changed = constraintComputer.computeChangedFeatures(cNode, config);
            }
            Map<Feature,List<String>> filesEachFeature = new HashMap<>();
            for (Feature featsConditionalStatement : changed) {
                FeatureCharacteristic featureCharacteristic = featureMap.get(featsConditionalStatement);
                if (featureCharacteristic == null)
                    featureCharacteristic = new FeatureCharacteristic();
                //RQ3.LOC
                featureCharacteristic.setLinesOfCode(featureCharacteristic.getLinesOfCode() - 1 + (cNode.getLineTo() - cNode.getLineFrom()));
                //RQ3: SD #ifdef
                featureCharacteristic.setScatteringDegreeIFs(featureCharacteristic.getScatteringDegreeIFs() + 1);
                String file = cNode.getParent().getParent().getContainingFile().getFilePath();
                //RQ3: SD files
                if(!featureCharacteristic.getScatteringDegreeFiles().contains(file)) {
                    featureCharacteristic.addScatteringDegreeFiles(file);
                }
                //RQ3: TD #ifdef
                if(count>1){
                    featureCharacteristic.addTanglingDegreeIFs(count,cNode.getLineTo()-cNode.getLineFrom());
                }
                //RQ3: ND #ifdef
                if(cNode.getChildren().size() > featureCharacteristic.getNestingDegree()){
                    featureCharacteristic.setNestingDegree(cNode.getChildren().size());
                }
                //RQ3: NOTLB OR NONTLB
                if(cNode.getParent().getParent().getLocalCondition().equals("BASE")){
                    featureCharacteristic.setNumberOfTopLevelBranches(featureCharacteristic.getNumberOfTopLevelBranches()+1);
                }else{
                    featureCharacteristic.setNumberOfNonTopLevelBranches(featureCharacteristic.getNumberOfNonTopLevelBranches()+1);
                }
                FeatureCharacteristic finalFeatureCharacteristic = featureCharacteristic;
                featureMap.computeIfAbsent(featsConditionalStatement, v -> finalFeatureCharacteristic);
                featureMap.computeIfPresent(featsConditionalStatement, (k, v) -> finalFeatureCharacteristic);
            }
        }
        for (ConditionalNode cNode : negatedConditionalNodes) {
            GetAllFeaturesVisitor visitorFeatures = new GetAllFeaturesVisitor();
            cNode.accept(visitorFeatures);
            List<Feature> featureInteractions = new ArrayList<>();
            featureInteractions.addAll(visitorFeatures.getAllFeatures());
            featureInteractions.remove(baseFeature);
            int count=0;
            if(featureInteractions.size()>1){
                for (Feature featInteraction:featureInteractions) {
                    if(features.contains(featInteraction)){
                        count++;
                    }
                }
            }
            Map<Feature, Integer> config;
            config = constraintComputer.computeConfig(cNode, tree);
            changed = new HashSet<>();
            if (config != null && !config.isEmpty()) {
                //compute the marked as changed features.
                for (Map.Entry<Feature, Integer> feat : config.entrySet()) {
                    if (feat.getValue() != 0) {
                        changed.add(feat.getKey());
                    }
                }
                //changed = constraintComputer.computeChangedFeatures(cNode, config);
            }
            for (Feature featsConditionalStatement : changed) {

                FeatureCharacteristic featureCharacteristic = featureMap.get(featsConditionalStatement);
                if (featureCharacteristic == null)
                    featureCharacteristic = new FeatureCharacteristic();
                //RQ3.LOC
                featureCharacteristic.setLinesOfCode(featureCharacteristic.getLinesOfCode() - 1 + (cNode.getLineTo() - cNode.getLineFrom()));
                //RQ3: SD #ifdef
                featureCharacteristic.setScatteringDegreeIFs(featureCharacteristic.getScatteringDegreeIFs() + 1);
                String file = cNode.getParent().getParent().getContainingFile().getFilePath();
                //RQ3: SD files
                if(!featureCharacteristic.getScatteringDegreeFiles().contains(file)) {
                    featureCharacteristic.addScatteringDegreeFiles(file);
                }
                //RQ3: TD #ifdef
                if(count>1){
                    featureCharacteristic.addTanglingDegreeIFs(count,cNode.getLineTo()-cNode.getLineFrom());
                }
                //RQ3: ND #ifdef
                if(cNode.getChildren().size() > featureCharacteristic.getNestingDegree()){
                    featureCharacteristic.setNestingDegree(cNode.getChildren().size());
                }
                //RQ3: NOTLB OR NONTLB
                if(cNode.getParent().getParent().getLocalCondition().equals("BASE")){
                    featureCharacteristic.setNumberOfTopLevelBranches(featureCharacteristic.getNumberOfTopLevelBranches()+1);
                }else{
                    if(cNode instanceof ELSECondition || cNode instanceof ELIFCondition){
                        ConditionalNode ifblock = cNode.getParent().getParent().getParent().getIfBlock();
                        if(ifblock.getParent().getParent().getLocalCondition().equals("BASE")){
                            featureCharacteristic.setNumberOfTopLevelBranches(featureCharacteristic.getNumberOfTopLevelBranches()+1);
                        }else{
                            featureCharacteristic.setNumberOfNonTopLevelBranches(featureCharacteristic.getNumberOfNonTopLevelBranches()+1);
                        }
                    }else {
                        featureCharacteristic.setNumberOfNonTopLevelBranches(featureCharacteristic.getNumberOfNonTopLevelBranches() + 1);
                    }
                }
                FeatureCharacteristic finalFeatureCharacteristic = featureCharacteristic;
                featureMap.computeIfAbsent(featsConditionalStatement, v -> finalFeatureCharacteristic);
                featureMap.computeIfPresent(featsConditionalStatement, (k, v) -> finalFeatureCharacteristic);
            }
        }
        //featureMap.put(feature, featureCharacteristic);
        return featureMap;
    }
}

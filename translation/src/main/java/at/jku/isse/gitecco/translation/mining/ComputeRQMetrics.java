package at.jku.isse.gitecco.translation.mining;

import at.jku.isse.gitecco.core.git.Change;
import at.jku.isse.gitecco.core.git.GitCommit;
import at.jku.isse.gitecco.core.git.GitCommitList;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.tree.visitor.GetAllFeaturesVisitor;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.translation.constraintcomputation.util.ConstraintComputer;
import at.jku.isse.gitecco.translation.visitor.GetNodesForChangeVisitor;
import org.glassfish.grizzly.http.server.accesslog.FileAppender;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ComputeRQMetrics {

    //RQ1
    public static void CharacteristicsChange () throws Exception {
        final int MAXCOMMITS = 200;
        //set as true to generate PP variants
        final boolean generateOriginalVariants = false;
        //TODO: planned arguments: DEBUG, dispose tree, max commits, repo path, csv path(feature id), outpath for ecco
        String repoPath = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\systems\\Bison\\bison";
        String[] featuresToAdd = {"BASE", "TRACE", "DEBUG", "MSDOS", "eta10", "__GO32__", "DONTDEF", "VMS", "HAVE_ALLOCA_H", "__GNUC__", "_AIX", "__STDC__", "HAVE_STDLIB_H", "HAVE_MEMORY_H", "STDC_HEADERS"};
        ArrayList<String> featureList = new ArrayList<>();

        for (String feat : featuresToAdd) {
            featureList.add(feat);
        }

        //add directories that we need to include manually to get all the files to create a clean version because "/usr/local/include"
        // and "/usr/include")does not includes files outside the root path
        final List<String> dirFiles = new ArrayList<>();
        final GitHelper gitHelper = new GitHelper(repoPath, dirFiles);
        final GitCommitList commitList = new GitCommitList(gitHelper);


        final File gitFolder = new File(gitHelper.getPath());
        final File eccoFolder = new File(gitFolder.getParent(), "ecco");

        Map<Feature, Integer> featureVersions = new HashMap<>();
        final Integer[] countFeaturesChanged = {0}; //COUNT PER GIT COMMIT
        final Integer[] newFeatures = {0}; //COUNT PER GIT COMMIT

        File gitRepositoryFolder = new File(gitHelper.getPath());
        File eccoVariantsFolder = new File(gitRepositoryFolder.getParent(), "ecco");
        if (eccoVariantsFolder.exists()) GitCommitList.recursiveDelete(eccoVariantsFolder.toPath());

        String fileReportFeature = "features_report_each_project_commit.csv";
        //csv to report new features and features changed per git commit of the project
        //RQ.2 How many features changed per Git commit?
        try {
            FileWriter csvWriter = new FileWriter(gitRepositoryFolder.getParent() + File.separator + fileReportFeature);
            List<List<String>> headerRows = Arrays.asList(
                    Arrays.asList("CommitNumber", "NewFeatures", "ChangedFeatures")
            );
            for (List<String> rowData : headerRows) {
                csvWriter.append(String.join(",", rowData));
                csvWriter.append("\n");
            }
            csvWriter.flush();
            csvWriter.close();
        } catch (
                IOException e) {
            e.printStackTrace();
        }
        // end csv to report new features and features changed

        String fileStoreConfig = "configurations.csv";


        List<String> changedFiles = new ArrayList<>();
        List<String> changedFilesNext = new ArrayList<>();
        final GitCommit[] gcPrevious = {null};
        final Boolean[] previous = {true};
        commitList.addGitCommitListener((gc, gcl) -> {
            if (gcl.size() >= MAXCOMMITS) System.exit(0);
            List<String> configurations = new ArrayList<>();
            System.out.println(gc.getCommitName() + ":");

            GetNodesForChangeVisitor visitor = new GetNodesForChangeVisitor();
            Set<ConditionalNode> changedNodes = new HashSet<>();
            Set<ConditionalNode> deletedNodes = new HashSet<>();
            Map<Feature, ChangeCharacteristic> featureMap = new HashMap<>();

            if (gc.getNumber() == 0) {
                gcPrevious[0] = new GitCommit(gc.getCommitName(), gc.getNumber(), gc.getDiffCommitName(), gc.getBranch(), gc.getRevCommit());
                gcPrevious[0].setTree(gc.getTree());
            } else if (previous[0] || previous[0] != null) {
                if (gc.getNumber() - 1 < 1) {
                    previous[0] = false;
                } else {
                    gcPrevious[0] = new GitCommit(gc.getRevCommit().getParent(0).getName(), gc.getNumber() - 1, gc.getRevCommit().getParent(0).getParent(0).getName(), gc.getBranch(), gc.getRevCommit().getParent(0));
                    gcl.addTreeParent(gcPrevious[0], gc.getCommitName());
                }
            }

            Map<Change, FileNode> changesInsert = new HashMap<>();
            //retrieve changed nodes
            for (FileNode child : gc.getTree().getChildren()) {
                if (child instanceof SourceFileNode) {
                    Change[] changes = null;
                    try {
                        changes = gitHelper.getFileDiffs(gc, child, false);
                    } catch (Exception e) {
                        System.err.println("error while executing the file diff: " + child.getFilePath());
                        e.printStackTrace();
                    }

                    for (Change change : changes) {
                        if (change.getChangeType() == null) {
                            visitor.setChange(change);
                            child.accept(visitor);
                            changedNodes.addAll(visitor.getchangedNodes());
                        } else {
                            changesInsert.put(change, child);
                        }
                    }
                }
                if (gc.getNumber() == 0) {
                    changedFiles.add(child.getFilePath());
                    previous[0] = false;
                } else {
                    changedFilesNext.add(child.getFilePath());
                }
            }

            if (previous[0]) {
                for (FileNode child : gcPrevious[0].getTree().getChildren()) {
                    String start = child.getFilePath().replace("arent" + File.separator, "");
                    changedFiles.add(start);
                    previous[0] = false;
                }
            }

            if (gc.getNumber() == 0 || previous[0]) {
                previous[0] = false;
            } else {
                //to retrieve changed nodes of deleted files
                if (gcPrevious[0] != null) {
                    for (String file : changedFiles) {
                        if (!changedFilesNext.contains(file)) {
                            FileNode child = gcPrevious[0].getTree().getChild(file);
                            if (child instanceof SourceFileNode) {
                                Change[] changes = null;
                                try {
                                    changes = gitHelper.getFileDiffs(gc, child, true);
                                } catch (Exception e) {
                                    System.err.println("error while executing the file diff: " + child.getFilePath());
                                    e.printStackTrace();
                                }

                                for (Change change : changes) {
                                    visitor.setChange(change);
                                    child.accept(visitor);
                                    deletedNodes.addAll(visitor.getchangedNodes());
                                }
                            }
                        }
                    }
                    for (Map.Entry<Change, FileNode> changeInsert : changesInsert.entrySet()) {
                        Change change = changeInsert.getKey();
                        FileNode childAux = changeInsert.getValue();
                        FileNode child = gcPrevious[0].getTree().getChild(childAux.getFilePath());
                        visitor.setChange(change);
                        child.accept(visitor);
                        deletedNodes.addAll(visitor.getchangedNodes());
                    }
                }
                //next is changedFiles for the next commit
                changedFiles.removeAll(changedFiles);
                changedFiles.addAll(changedFilesNext);
                changedFilesNext.removeAll(changedFilesNext);
            }


            final ConstraintComputer constraintComputer = new ConstraintComputer(featureList);
            final PreprocessorHelper pph = new PreprocessorHelper();
            Map<Feature, Integer> config;
            Set<Feature> changed;
            Set<Feature> alreadyComitted = new HashSet<>();
            Integer count = 0;

            //if there is no changed node then there must be a change in the binary files --> commit base.
            if (changedNodes.size() == 0 && gcl.size() > 1 && gc.getTree().getChildren().size() > 0)
                changedNodes.add(new BaseNode(null, 0));
            else if (changedNodes.size() == 0 && deletedNodes.size() == 0 && gc.getTree().getChildren().size() > 0) {
                changedNodes.add(new BaseNode(null, 0));
            }

            Boolean baseChanged = false;
            for (ConditionalNode nods : changedNodes) {
                if (nods instanceof BaseNode) {
                    baseChanged = true;
                }
            }
            if (!baseChanged) {
                for (ConditionalNode nods : deletedNodes) {
                    if (nods instanceof BaseNode) {
                        baseChanged = true;
                    }
                }
            }

            ArrayList<String> configsToCommit = new ArrayList<>();
            Map<Map<Feature, Integer>, String> configsToGenerateVariant = new HashMap<>();
            Feature base = new Feature("BASE");
            //changedNodes = changedNodes.stream().filter(x -> x.getLocalCondition().equals("__AVR_ATmega644P__ || __AVR_ATmega644__")).collect(Collectors.toSet());
            for (ConditionalNode changedNode : changedNodes) {
                //compute the config for the var gen
                config = constraintComputer.computeConfig(changedNode, gc.getTree());
                if (config != null && !config.isEmpty()) {
                    //compute the marked as changed features.
                    changed = constraintComputer.computeChangedFeatures(changedNode, config);

                    if (!changed.contains(base))
                        config.remove(base);
                    //map with the name of the feature and version and a boolean to set true when it is already incremented in the analysed commit
                    String eccoConfig = "";
                    String file = "";
                    if(changedNode.getContainingFile()!=null)
                         file = changedNode.getContainingFile().getFilePath();
                    for (Map.Entry<Feature, Integer> configFeature : config.entrySet()) {
                        int version = 0;
                        if (configFeature.getValue() != 0) {
                            if (featureVersions.containsKey(configFeature.getKey())) {
                                version = featureVersions.get(configFeature.getKey());
                            }
                            if (!alreadyComitted.contains(configFeature.getKey())) {
                                alreadyComitted.add(configFeature.getKey());
                                //for marlin: first commit is empty, thus we need the < 2. otherwise <1 should be enough.
                                if (gcl.size() < 2 || changed.contains(configFeature.getKey())) {
                                    version++;
                                    if (version == 1)
                                        newFeatures[0]++;
                                    else
                                        countFeaturesChanged[0]++;
                                }
                                featureVersions.put(configFeature.getKey(), version);
                            }
                            if (!configFeature.getKey().toString().equals("BASE"))
                                eccoConfig += "," + configFeature.getKey().toString() + "." + version;
                            else
                                eccoConfig += "," + configFeature.getKey().toString() + ".$$";
                        }
                        //RQ1.
                        ChangeCharacteristic changeCharacteristic;
                        if( featureMap.get(configFeature.getKey())== null) {
                            changeCharacteristic = new ChangeCharacteristic();
                            featureMap.put(configFeature.getKey(), new ChangeCharacteristic());
                        }else {
                            changeCharacteristic = featureMap.get(configFeature.getKey());
                        }
                        changeCharacteristic.setLinesOfCodeAdded(changeCharacteristic.getLinesOfCodeAdded() + (changedNode.getLineTo() - changedNode.getLineFrom()));
                        changeCharacteristic.addTanglingDegree(config.size());
                        if (!changeCharacteristic.getScatteringDegreeFiles().contains(file)) {
                            changeCharacteristic.addScatteringDegreeFiles(file);
                        }
                        changeCharacteristic.setScatteringDegreeIfs(changeCharacteristic.getScatteringDegreeIfs() + 1);
                        ChangeCharacteristic finalChangeCharacteristic = changeCharacteristic;
                        featureMap.computeIfAbsent(configFeature.getKey(), v -> finalChangeCharacteristic);
                        featureMap.computeIfPresent(configFeature.getKey(), (k, v) -> finalChangeCharacteristic);
                    }
                    if (!eccoConfig.contains("BASE")) {
                        eccoConfig += "," + "BASE.$$";
                    }
                    eccoConfig = eccoConfig.replaceFirst(",", "");

                    if (configurations.contains(eccoConfig)) {
                        System.out.println("Config already used to generate a variant: " + eccoConfig);
                        //don't need to generate variant and commit it again at the same commit of the project git repository
                    } else {
                        count++;
                        //configuration that will be used to generate the variant of this changed node
                        configsToGenerateVariant.put(config, eccoConfig);

                        configurations.add(eccoConfig);
                        //folder where the variant is stored
                        File variantsrc = new File(eccoFolder, eccoConfig);
                        String outputCSV = variantsrc.getParentFile().getParentFile().getAbsolutePath();
                        final Path variant_dir = Paths.get(String.valueOf(variantsrc));

                    }
                }
            }

            if (deletedNodes.size() != 0) {
                for (ConditionalNode deletedNode : deletedNodes) {
                    //compute the config for the var gen
                    config = constraintComputer.computeConfig(deletedNode, gcPrevious[0].getTree());
                    if (config != null && !config.isEmpty()) {
                        //compute the marked as changed features.
                        changed = constraintComputer.computeChangedFeatures(deletedNode, config);
                        if (!changed.contains(base))
                            config.remove(base);
                        //map with the name of the feature and version and a boolean to set true when it is already incremented in the analysed commit
                        String eccoConfig = "";
                        for (Map.Entry<Feature, Integer> configFeature : config.entrySet()) {
                            int version = 0;
                            if (configFeature.getValue() != 0) {
                                if (featureVersions.containsKey(configFeature.getKey())) {
                                    version = featureVersions.get(configFeature.getKey());
                                }
                                if (!alreadyComitted.contains(configFeature.getKey())) {
                                    alreadyComitted.add(configFeature.getKey());
                                    //for marlin: first commit is empty, thus we need the < 2. otherwise <1 should be enough.
                                    if (gcl.size() < 2 || changed.contains(configFeature.getKey())) {
                                        version++;
                                        if (version == 1)
                                            newFeatures[0]++;
                                        else
                                            countFeaturesChanged[0]++;
                                    }
                                    featureVersions.put(configFeature.getKey(), version);
                                }
                                if (!configFeature.getKey().toString().equals("BASE"))
                                    eccoConfig += "," + configFeature.getKey().toString() + "." + version;
                                else
                                    eccoConfig += "," + configFeature.getKey().toString() + ".$$";

                            }
                            //RQ1. deleted lines
                            ChangeCharacteristic changeCharacteristic = featureMap.get(configFeature.getKey());
                            changeCharacteristic.setScatteringDegreeIfs(changeCharacteristic.getScatteringDegreeIfs() + 1);
                            ChangeCharacteristic finalChangeCharacteristic = changeCharacteristic;
                            featureMap.computeIfAbsent(configFeature.getKey(), v -> finalChangeCharacteristic);
                            featureMap.computeIfPresent(configFeature.getKey(), (k, v) -> finalChangeCharacteristic);
                        }
                        if (!eccoConfig.contains("BASE")) {
                            eccoConfig += "," + "BASE.$$";
                        }
                        eccoConfig = eccoConfig.replaceFirst(",", "");

                        if (configurations.contains(eccoConfig)) {
                            System.out.println("Config already used to generate a variant: " + eccoConfig);
                            //don't need to generate variant and commit it again at the same commit of the project git repository
                        } else {
                            count++;
                            //configuration that will be used to generate the variant of this changed node
                            configsToGenerateVariant.put(config, eccoConfig);

                            configurations.add(eccoConfig);


                        }
                    }
                }
            }

            String baseVersion = "";
            for (Map.Entry<Feature, Integer> configFeature : featureVersions.entrySet()) {
                if (configFeature.getKey().equals(base))
                    baseVersion = configFeature.getValue().toString();
            }

            //generate the variant for this config
            for (Map.Entry<Map<Feature, Integer>, String> variant : configsToGenerateVariant.entrySet()) {
                String eccoConfig = variant.getValue().replace("$$", baseVersion);
                if (generateOriginalVariants) {
                    pph.generateVariants(variant.getKey(), gitFolder, eccoFolder, gitHelper.getDirFiles(), eccoConfig);
                    System.out.println("Variant generated with config: " + eccoConfig);
                }
                //config that will be used to commit the variant generated with this changed node in ecco
                configsToCommit.add(eccoConfig);
            }


            //appending to the config csv
            try {

                String fileStr = gitRepositoryFolder.getParent() + File.separator + fileStoreConfig;
                FileAppender csvWriter = new FileAppender(new File(fileStr));

                for (String configs : configsToCommit) {
                    List<List<String>> headerRows = Arrays.asList(
                            Arrays.asList(Long.toString(gc.getNumber()), gc.getCommitName(), configs)
                    );
                    for (List<String> rowData : headerRows) {
                        csvWriter.append(String.join(",", rowData));
                    }
                }
                csvWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


            //append results to the feature report csv
            try {
                FileAppender csvAppender = new FileAppender(new File(gitRepositoryFolder.getParent() + File.separator + fileReportFeature));
                List<List<String>> contentRows = Arrays.asList(
                        Arrays.asList(Long.toString(gc.getNumber()), newFeatures[0].toString(), countFeaturesChanged[0].toString())
                );
                for (List<String> rowData : contentRows) {
                    csvAppender.append(String.join(",", rowData));
                }
                csvAppender.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //end append results to the feature report csv


            countFeaturesChanged[0] = 0;
            newFeatures[0] = 0;
            for (Map.Entry<Feature, Integer> featureRevision : featureVersions.entrySet()) {
                System.out.println(featureRevision.getKey() + "." + featureRevision.getValue());
            }
            //RQ.2 How many times one feature changed along a number of Git commits?
            if (gc.getNumber() == commitList.size()) {
                for (Map.Entry<Feature, Integer> featureRevision : featureVersions.entrySet()) {
                    if (featureRevision.getValue() > 1)
                        System.out.println(featureRevision.getKey() + " Changed " + (featureRevision.getValue() - 1) + " times.");
                }
            }

            for (Map.Entry<Feature, ChangeCharacteristic> changes:featureMap.entrySet()) {
                ChangeCharacteristic changeCharacteristic = changes.getValue();
                System.out.println("Feature: "+changes.getKey());
                System.out.println("Lines of Code Added: "+changeCharacteristic.getLinesOfCodeAdded());
                System.out.println("Lines of Code Removed: "+changeCharacteristic.getLinesOfCodeRemoved());
                System.out.println("SD IFs: "+changeCharacteristic.getScatteringDegreeIfs());
                System.out.println("SD Files: "+changeCharacteristic.getScatteringDegreeFiles().size());
                Collections.sort(changeCharacteristic.getTanglingDegree());
                System.out.println("TD IFs: "+changeCharacteristic.getTanglingDegree().get(changeCharacteristic.getTanglingDegree().size()-1));
            }

        });

        //set second parameter as "NULLCOMMIT" when the first commit is 0, or null when the first commit is another (when startcommit is > 0)
        //gitHelper.getEveryNthCommit(commitList, "NULLCOMMIT", 0, 50, 1);
        gitHelper.getEveryNthCommit(commitList, "NULLCOMMIT", 0, 49, 1);
        //gitHelper.getAllCommits(commitList);


    }

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
            int count = 0;
            if (featureInteractions.size() > 1) {
                for (Feature featInteraction : featureInteractions) {
                    if (features.contains(featInteraction)) {
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
            Map<Feature, List<String>> filesEachFeature = new HashMap<>();
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
                if (!featureCharacteristic.getScatteringDegreeFiles().contains(file)) {
                    featureCharacteristic.addScatteringDegreeFiles(file);
                }
                //RQ3: TD #ifdef
                if (count > 1) {
                    featureCharacteristic.addTanglingDegreeIFs(count, cNode.getLineTo() - cNode.getLineFrom());
                }
                //RQ3: ND #ifdef
                if (cNode.getChildren().size() > featureCharacteristic.getNestingDegree()) {
                    featureCharacteristic.setNestingDegree(cNode.getChildren().size());
                }
                //RQ3: NOTLB OR NONTLB
                if (cNode.getParent().getParent().getLocalCondition().equals("BASE")) {
                    featureCharacteristic.setNumberOfTopLevelBranches(featureCharacteristic.getNumberOfTopLevelBranches() + 1);
                } else {
                    featureCharacteristic.setNumberOfNonTopLevelBranches(featureCharacteristic.getNumberOfNonTopLevelBranches() + 1);
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
            int count = 0;
            if (featureInteractions.size() > 1) {
                for (Feature featInteraction : featureInteractions) {
                    if (features.contains(featInteraction)) {
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
                if (!featureCharacteristic.getScatteringDegreeFiles().contains(file)) {
                    featureCharacteristic.addScatteringDegreeFiles(file);
                }
                //RQ3: TD #ifdef
                if (count > 1) {
                    featureCharacteristic.addTanglingDegreeIFs(count, cNode.getLineTo() - cNode.getLineFrom());
                }
                //RQ3: ND #ifdef
                if (cNode.getChildren().size() > featureCharacteristic.getNestingDegree()) {
                    featureCharacteristic.setNestingDegree(cNode.getChildren().size());
                }
                //RQ3: NOTLB OR NONTLB
                if (cNode.getParent().getParent().getLocalCondition().equals("BASE")) {
                    featureCharacteristic.setNumberOfTopLevelBranches(featureCharacteristic.getNumberOfTopLevelBranches() + 1);
                } else {
                    if (cNode instanceof ELSECondition || cNode instanceof ELIFCondition) {
                        ConditionalNode ifblock = cNode.getParent().getParent().getParent().getIfBlock();
                        if (ifblock.getParent().getParent().getLocalCondition().equals("BASE")) {
                            featureCharacteristic.setNumberOfTopLevelBranches(featureCharacteristic.getNumberOfTopLevelBranches() + 1);
                        } else {
                            featureCharacteristic.setNumberOfNonTopLevelBranches(featureCharacteristic.getNumberOfNonTopLevelBranches() + 1);
                        }
                    } else {
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

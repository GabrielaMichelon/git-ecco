package at.jku.isse.gitecco.translation.test;

import at.jku.isse.gitecco.core.git.Change;
import at.jku.isse.gitecco.core.git.GitCommit;
import at.jku.isse.gitecco.core.git.GitCommitList;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.tree.nodes.BaseNode;
import at.jku.isse.gitecco.core.tree.nodes.ConditionalNode;
import at.jku.isse.gitecco.core.tree.nodes.FileNode;
import at.jku.isse.gitecco.core.tree.nodes.SourceFileNode;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.translation.constraintcomputation.util.ConstraintComputer;
import at.jku.isse.gitecco.translation.mining.ChangeCharacteristic;
import at.jku.isse.gitecco.translation.mining.ComputeRQMetrics;
import at.jku.isse.gitecco.translation.mining.FeatureCharacteristic;
import at.jku.isse.gitecco.translation.visitor.GetNodesForChangeVisitor;
import org.glassfish.grizzly.http.server.accesslog.FileAppender;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class FeatureRevisionTest {

    @Test
    public void FeatureRevision() throws Exception {
        ComputeRQMetrics.CharacteristicsChange();
    }

}


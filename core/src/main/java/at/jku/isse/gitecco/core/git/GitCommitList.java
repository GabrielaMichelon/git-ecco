package at.jku.isse.gitecco.core.git;

import at.jku.isse.gitecco.core.cdt.CDTHelper;
import at.jku.isse.gitecco.core.cdt.FeatureParser;
import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.tree.nodes.BinaryFileNode;
import at.jku.isse.gitecco.core.tree.nodes.RootNode;
import at.jku.isse.gitecco.core.tree.nodes.SourceFileNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for storing a row of commits
 * With the option to react to added commits
 * depending on their types.
 */
public class GitCommitList extends ArrayList<GitCommit> {
    private final GitHelper gitHelper;
    private final List<GitCommitListener> observersC = new ArrayList();
    private final List<GitBranchListener> observersB = new ArrayList();
    private final List<GitMergeListener> observersM = new ArrayList();
    private Long runtimePPCheckoutCleanVersion;

    public Long getRuntimePPCheckoutCleanVersion() {
        return runtimePPCheckoutCleanVersion;
    }

    public void setRuntimePPCheckoutCleanVersion(Long runtimePPCheckoutCleanVersion) {
        this.runtimePPCheckoutCleanVersion = runtimePPCheckoutCleanVersion;
    }

    public GitCommitList(GitHelper gh) throws IOException {
        super();
        this.gitHelper = gh;
    }

    /**
     * Adds a GitCommitListener to the Object.
     *
     * @param gcl
     */
    public void addGitCommitListener(GitCommitListener gcl) {
        observersC.add(gcl);
    }

    /**
     * Adds a GitMergeListener to the Object.
     *
     * @param gml
     */
    public void addGitMergeListener(GitMergeListener gml) {
        observersM.add(gml);
    }

    /**
     * Adds a GitBranchListener to the Object.
     *
     * @param gbl
     */
    public void addGitBranchListener(GitBranchListener gbl) {
        observersB.add(gbl);
    }



    @Override
    public boolean add(GitCommit gitCommit) {
        final RootNode tree = new RootNode(gitHelper.getPath());
        gitHelper.checkOutCommit(gitCommit.getCommitName());

        final PreprocessorHelper pph = new PreprocessorHelper();
        final File gitFolder = new File(gitHelper.getPath());
        final File cleanFolder = new File(gitFolder.getParent(), "clean");
        final File outputDirectory = new File("C:\\obermanndavid\\git-ecco-test\\test_repo_gabi_log"+gitCommit.getBranch());

        //delete the clean directory if it exists:
        if (cleanFolder.exists()) recursiveDelete(cleanFolder.toPath());

        //generate clean version
        setRuntimePPCheckoutCleanVersion(pph.generateCleanVersion(gitFolder, cleanFolder, gitHelper.getDirFiles()));

        //creates a folder to each branch and if it is the first commit delete the folder in case of it already exists
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }else if(this.size()==0){
            GitCommitList.recursiveDelete(outputDirectory.toPath());
            outputDirectory.mkdirs();
        }
        //creates a file to commit
        try {
            File file = new File(outputDirectory+"\\"+this.size()+".txt");
            // If file doesn't exists, then create it
            if (!file.exists()) {
                file.createNewFile();
                String text = "commit nr.: "+gitCommit.getNumber() + "\n";
                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                // Write in file
                bw.write(text + "\n");
                bw.close();
            } else {
                String text = "commit nr.: "+gitCommit.getNumber() + "\n";
                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(text + "\n");
                bw.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Files.walk(cleanFolder.toPath())
                    .filter(path -> !path.toFile().isDirectory())
                    .forEach(filePath -> {

                        String file = filePath.toFile().getPath();
                        if (file.endsWith(".cpp") || file.endsWith(".hpp") || file.endsWith(".c") || file.endsWith(".h")) {

                            final SourceFileNode fn
                                    = new SourceFileNode(tree, file.substring((gitFolder.getParent()+"\\clean").length()+1));
                            List<String> codelist = null;

                            try {
                                codelist = Files.readAllLines(filePath, StandardCharsets.ISO_8859_1);
                            } catch (IOException e1) {
                                System.err.println("error reading file: "+file);
                                e1.printStackTrace();
                            }
                            final String code = codelist.stream().collect(Collectors.joining("\n"));

                            //file parsing
                            IASTTranslationUnit translationUnit = null;
                            try {
                                translationUnit = CDTHelper.parse(code.toCharArray());
                            } catch (CoreException e1) {
                                System.err.println("error parsing with CDT Core: "+file);
                                e1.printStackTrace();
                            }
                            final IASTPreprocessorStatement[] ppstatements = translationUnit.getAllPreprocessorStatements();
                            final FeatureParser featureParser = new FeatureParser();
                            //actual tree building
                            try {
                                featureParser.parseToTree(ppstatements, codelist.size(), fn);
                            } catch (Exception e) {
                                System.err.println("error parsing to tree: "+file);
                                e.printStackTrace();
                            }
                            tree.addChild(fn);
                        } else {
                            final BinaryFileNode fn
                                    = new BinaryFileNode(tree, file.substring((gitFolder.getParent()+"\\clean").length()+1));
                            tree.addChild(fn);
                        }

                    });
        } catch (IOException e) {
            if (this.size() != 0) e.printStackTrace();
        }

        gitCommit.setTree(tree);

        //trigger listeners, etc.
        notifyObservers(gitCommit);
        System.out.println("commit nr.:"+gitCommit.getNumber()+" branch: "+gitCommit.getBranch());
        return super.add(gitCommit);
    }


    /**
     * Helper method:
     * recursivley deletes a folder given by a path.
     * @param directory the path to the directory which should be deleted.
     */
    public static void recursiveDelete(Path directory) {
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.setAttribute(file, "dos:readonly", false);
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("problem deleting: " + directory);
            e.printStackTrace();
        }
    }

    private void notifyObservers(GitCommit gc) {
        for (GitCommitListener oc : observersC) {
            oc.onCommit(gc, this);
            /*for (GitCommitType gct : gc.getType()) {
                if (gct.equals(GitCommitType.BRANCH)) {
                    for (GitBranchListener ob : observersB) {
                        ob.onBranch(gc, this);
                    }
                }
                if (gct.equals(GitCommitType.MERGE)) {
                    for (GitMergeListener gm : observersM) {
                        gm.onMerge(gc, this);
                    }
                }
            }*/
        }
    }

    public String getBranch(GitCommit git, GitCommitList gcl) {
        return git.getBranch()+"\\"+gcl.size()+".txt";
    }
}

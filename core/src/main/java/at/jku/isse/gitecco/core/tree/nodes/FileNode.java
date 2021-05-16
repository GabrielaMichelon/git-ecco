package at.jku.isse.gitecco.core.tree.nodes;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class for FileNodes --> will be used for BinaryFileNodes and SourceFileNodes.
 */
public abstract class FileNode extends Node{
    private final String pathName;
    private final RootNode parent;
    private List<String> fileContent;

    public FileNode(RootNode parent, String name) {
        this.parent = parent;
        this.pathName= name;
        this.fileContent = new ArrayList<>();
    }

    /**
     * Returns the path of the file which is represented by this node.
     * @return
     */
    public String getFilePath() {
        return this.pathName;
    }

    public List<String> getFileContent() {
        return fileContent;
    }

    public void setFileContent(List<String> fileContent) {
        this.fileContent = fileContent;
    }

    @Override
    public RootNode getParent() {
        return this.parent;
    }
}


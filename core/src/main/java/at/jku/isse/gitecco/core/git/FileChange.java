package at.jku.isse.gitecco.core.git;

import java.util.ArrayList;
import java.util.List;

public class FileChange {
    private ArrayList<String> lines;
    private List<String> fileLines;
    private List<String> previousContent;

    public FileChange(ArrayList<String> lines, List<String> fileLines, List<String> previousContent) {
        this.lines = lines;
        this.fileLines = fileLines;
        this.previousContent = previousContent;
    }

    public ArrayList<String> getLines() {
        return lines;
    }

    public void setLines(ArrayList<String> lines) {
        this.lines = lines;
    }

    public List<String> getFileLines() {
        return fileLines;
    }

    public void setFileLines(List<String> fileLines) {
        this.fileLines = fileLines;
    }

    public List<String> getPreviousContent() {
        return previousContent;
    }

    public void setPreviousContent(List<String> previousContent) {
        this.previousContent = previousContent;
    }
}

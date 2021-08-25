package at.jku.isse.gitecco.core.git;

import java.util.ArrayList;
import java.util.List;

public class FileChange {
    private ArrayList<String> lines;
    private ArrayList<String> previousLines;
    private List<String> fileLines;
    private List<String> previousContent;

    public FileChange(ArrayList<String> lines, ArrayList<String> previousLines,List<String> fileLines, List<String> previousContent) {
        this.lines = lines;
        this.previousLines = previousLines;
        this.fileLines = fileLines;
        this.previousContent = previousContent;
    }

    public ArrayList<String> getLines() {
        return this.lines;
    }

    public void setLines(ArrayList<String> lines) {
        this.lines = lines;
    }

    public ArrayList<String> getPreviousLines() {
        return this.previousLines;
    }

    public void setPreviousLines(ArrayList<String> previousLines) {
        this.previousLines = previousLines;
    }

    public List<String> getFileLines() {
        return this.fileLines;
    }

    public void setFileLines(List<String> fileLines) {
        this.fileLines = fileLines;
    }

    public List<String> getPreviousContent() {
        return this.previousContent;
    }

    public void setPreviousContent(List<String> previousContent) {
        this.previousContent = previousContent;
    }
}

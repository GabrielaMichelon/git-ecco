package at.jku.isse.gitecco.translation.changepropagation;

import java.util.ArrayList;
import java.util.List;

public class ChangedFile {
    private ArrayList<Integer> linesInsert;
    private ArrayList<Integer> linesRemoved;
    private ArrayList<String> featureInteractions;
    private ArrayList<String> featureMightAffected;
    private List<String> currentLines;
    private List<String> previousLines;

    public ChangedFile(ArrayList<Integer> linesInsert, ArrayList<Integer> linesRemoved, ArrayList<String> featureInteractions, ArrayList<String> featureMightAffected, List<String> currentLines, List<String> previousLines) {
        this.linesInsert = linesInsert;
        this.linesRemoved = linesRemoved;
        this.featureInteractions = featureInteractions;
        this.featureMightAffected = featureMightAffected;
        this.currentLines = currentLines;
        this.previousLines = previousLines;
    }

    public ArrayList<Integer> getLinesInsert() {
        return linesInsert;
    }

    public void setLinesInsert(ArrayList<Integer> linesInsert) {
        this.linesInsert = linesInsert;
    }

    public ArrayList<Integer> getLinesRemoved() {
        return linesRemoved;
    }

    public void setLinesRemoved(ArrayList<Integer> linesRemoved) {
        this.linesRemoved = linesRemoved;
    }

    public ArrayList<String> getFeatureInteractions() {
        return featureInteractions;
    }

    public void setFeatureInteractions(ArrayList<String> featureInteractions) {
        this.featureInteractions = featureInteractions;
    }

    public ArrayList<String> getFeatureMightAffected() {
        return featureMightAffected;
    }

    public void setFeatureMightAffected(ArrayList<String> featureMightAffected) {
        this.featureMightAffected = featureMightAffected;
    }

    public List<String> getPreviousLines() {
        return previousLines;
    }

    public void setPreviousLines(List<String> previousLines) {
        this.previousLines = previousLines;
    }

    public List<String> getCurrentLines() {
        return currentLines;
    }

    public void setCurrentLines(List<String> currentLines) {
        this.currentLines = currentLines;
    }
}

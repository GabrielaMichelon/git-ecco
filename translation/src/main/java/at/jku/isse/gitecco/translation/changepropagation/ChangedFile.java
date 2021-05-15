package at.jku.isse.gitecco.translation.changepropagation;

import java.util.ArrayList;

public class ChangedFile {
    private ArrayList<Integer> linesInsert;
    private ArrayList<Integer> linesRemoved;
    private ArrayList<String> featureInteractions;
    private ArrayList<String> featureMightAffected;

    public ChangedFile(ArrayList<Integer> linesInsert, ArrayList<Integer> linesRemoved, ArrayList<String> featureInteractions, ArrayList<String> featureMightAffected) {
        this.linesInsert = linesInsert;
        this.linesRemoved = linesRemoved;
        this.featureInteractions = featureInteractions;
        this.featureMightAffected = featureMightAffected;
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
}

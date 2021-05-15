package at.jku.isse.gitecco.translation.changepropagation;

import java.util.ArrayList;

public class AddedFile {
    private ArrayList<Integer> linesInsert;
    private ArrayList<String> featureInteractions;
    private ArrayList<String> featureMightAffected;

    public AddedFile(ArrayList<Integer> linesInsert, ArrayList<String> featureInteractions, ArrayList<String> featureMightAffected) {
        this.linesInsert = linesInsert;
        this.featureInteractions = featureInteractions;
        this.featureMightAffected = featureMightAffected;
    }

    public ArrayList<Integer> getLinesInsert() {
        return linesInsert;
    }

    public void setLinesInsert(ArrayList<Integer> linesInsert) {
        this.linesInsert = linesInsert;
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

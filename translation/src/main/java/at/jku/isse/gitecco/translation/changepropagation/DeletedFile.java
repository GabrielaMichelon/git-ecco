package at.jku.isse.gitecco.translation.changepropagation;

import java.util.ArrayList;

public class DeletedFile {
    private ArrayList<Integer> linesRemoved;
    private ArrayList<String> featureInteractions;
    private ArrayList<String> featureMightAffected;

    public DeletedFile(ArrayList<Integer> linesRemoved, ArrayList<String> featureInteractions, ArrayList<String> featureMightAffected) {
        this.linesRemoved = linesRemoved;
        this.featureInteractions = featureInteractions;
        this.featureMightAffected = featureMightAffected;
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

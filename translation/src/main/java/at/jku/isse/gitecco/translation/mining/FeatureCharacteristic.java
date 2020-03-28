package at.jku.isse.gitecco.translation.mining;

import java.util.HashMap;
import java.util.Map;

public class FeatureCharacteristic {
    private Integer linesOfCode;
    private Integer scatteringDegreeIFs;
    private Integer scatteringDegreeFiles;
    //number of features with the analyzed #IFDEF of a feature revision and number of lines inside its block
    private Map<Integer,Integer> tanglingDegreeIFs;
    private Integer tanglingDegreeFiles;
    private Integer nestingDegree;
    private Integer numberOfTopLevelBranches;
    private Integer numberOfNonTopLevelBranches;

    public FeatureCharacteristic() {
        this.linesOfCode = 0;
        this.scatteringDegreeIFs = 0;
        this.scatteringDegreeFiles = 0;
        this.tanglingDegreeIFs = new HashMap<>();
        this.tanglingDegreeFiles = 0;
        this.nestingDegree = 0;
        this.numberOfTopLevelBranches = 0;
        this.numberOfNonTopLevelBranches = 0;
    }

    public Integer getLinesOfCode() {
        return linesOfCode;
    }

    public void setLinesOfCode(Integer linesOfCode) {
        this.linesOfCode = linesOfCode;
    }

    public Integer getScatteringDegreeIFs() {
        return scatteringDegreeIFs;
    }

    public void setScatteringDegreeIFs(Integer scatteringDegreeIFs) {
        this.scatteringDegreeIFs = scatteringDegreeIFs;
    }

    public Integer getScatteringDegreeFiles() {
        return scatteringDegreeFiles;
    }

    public void setScatteringDegreeFiles(Integer scatteringDegreeFiles) {
        this.scatteringDegreeFiles = scatteringDegreeFiles;
    }

    public Map<Integer, Integer> getTanglingDegreeIFs() {
        return tanglingDegreeIFs;
    }

    public void setTanglingDegreeIFs(Map<Integer, Integer> tanglingDegreeIFs) {
        this.tanglingDegreeIFs = tanglingDegreeIFs;
    }

    public Integer getTanglingDegreeFiles() {
        return tanglingDegreeFiles;
    }

    public void setTanglingDegreeFiles(Integer tanglingDegreeFiles) {
        this.tanglingDegreeFiles = tanglingDegreeFiles;
    }

    public Integer getNestingDegree() {
        return nestingDegree;
    }

    public void setNestingDegree(Integer nestingDegree) {
        this.nestingDegree = nestingDegree;
    }

    public Integer getNumberOfTopLevelBranches() {
        return numberOfTopLevelBranches;
    }

    public void setNumberOfTopLevelBranches(Integer numberOfTopLevelBranches) {
        this.numberOfTopLevelBranches = numberOfTopLevelBranches;
    }

    public Integer getNumberOfNonTopLevelBranches() {
        return numberOfNonTopLevelBranches;
    }

    public void setNumberOfNonTopLevelBranches(Integer numberOfNonTopLevelBranches) {
        this.numberOfNonTopLevelBranches = numberOfNonTopLevelBranches;
    }
}

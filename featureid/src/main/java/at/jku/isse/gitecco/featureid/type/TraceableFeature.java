package at.jku.isse.gitecco.featureid.type;

import at.jku.isse.gitecco.core.type.Feature;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class TraceableFeature extends Feature {
	private Integer externalOcc, internalOcc, transientOcc;
	private LinkedHashMap<Long, Boolean> commitList =  new LinkedHashMap<>();

	public TraceableFeature(String name) {
		super(name);
		externalOcc = 0;
		internalOcc = 0;
		transientOcc = 0;
	}

	public TraceableFeature(Feature f) {
		super(f.getName());
		externalOcc = 0;
		internalOcc = 0;
		transientOcc = 0;
	}

	public void setListcommit(Long commit, boolean present){
		this.commitList.put(commit,present);
	}

	/**
	 * Increments the counter corresponding to the types.
	 *
	 * @param t The types of the feature.
	 */
	public TraceableFeature inc(FeatureType t) {
		switch (t) {
			case EXTERNAL:
				externalOcc++;
				break;
			case INTERNAL:
				internalOcc++;
				break;
			case TRANSIENT:
				transientOcc++;
				break;
		}
		return this;
	}

	public Integer getExternalOcc() {
		return externalOcc;
	}

	public Integer getInternalOcc() {
		return internalOcc;
	}

	public Integer getTransientOcc() {
		return transientOcc;
	}

	public Integer getTotalOcc() {
		return externalOcc + internalOcc + transientOcc;
	}

	public Map<Long, Boolean> getCommitList() {
		return commitList;
	}
}
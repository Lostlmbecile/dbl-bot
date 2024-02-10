package com.github.egubot.objects;

import java.util.ArrayList;
import java.util.List;

public class SummonBanner {
	private String title;
	private List<SummonStep> onceOnlySteps = new ArrayList<>(2);
	private List<SummonStep> normalSteps = new ArrayList<>(3);
	private List<SummonCharacter> featuredUnits = new ArrayList<>(10);

	public List<SummonStep> getOnceOnlySteps() {
		return onceOnlySteps;
	}

	public void setOnceOnlySteps(List<SummonStep> onceOnlySteps) {
		this.onceOnlySteps = onceOnlySteps;
	}

	public List<SummonStep> getNormalSteps() {
		return normalSteps;
	}

	public void setNormalSteps(List<SummonStep> normalSteps) {
		this.normalSteps = normalSteps;
	}

	public List<SummonCharacter> getFeaturedUnits() {
		return featuredUnits;
	}

	public void setFeaturedUnits(List<SummonCharacter> featuredUnits) {
		this.featuredUnits = featuredUnits;
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String toString() {
		return "SummonBanner [\ntitle=" + title + "\nonceOnlySteps=" + onceOnlySteps + "\nnormalSteps=" + normalSteps
				+ "\nfeaturedUnits=" + featuredUnits + "\n]";
	}
}

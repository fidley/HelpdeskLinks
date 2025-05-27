package com.abapblog.helpdesklink.detector;

public enum ScopeType {
	INDEPENDENT, PROJECT, WORKINGSET;

	public static ScopeType fromString(String s) {
		if (s == null)
			return INDEPENDENT;
		switch (s.toLowerCase()) {
		case "project":
			return PROJECT;
		case "workingset":
			return WORKINGSET;
		default:
			return INDEPENDENT;
		}
	}

	@Override
	public String toString() {
		switch (this) {
		case PROJECT:
			return "project";
		case WORKINGSET:
			return "workingset";
		default:
			return "independent";
		}
	}
}

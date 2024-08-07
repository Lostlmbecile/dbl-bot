package com.github.egubot.shared.utils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ConvertObjects {
	private ConvertObjects() {
	}

	public static String listToText(List<String> data) {
		return data == null ? null : String.join(" ", data);
	}

	public static String listToText(List<String> data, String joinLinesWith) {
		return data == null ? null : String.join(joinLinesWith, data);
	}

	public static List<String> textToList(String jsonData) {
		return jsonData == null ? new ArrayList<>() : new ArrayList<>(List.of(jsonData.split("\n")));
	}

	public static String instantToString(Instant instant) {
		return instant == null ? null : DateUtils.getDateTime(instant);
	}

	public static String convertMilliSecondsToTime(long ms) {
		ms = ms / 1000;
		long hours = ms / 3600;
		long minutes = (ms % 3600) / 60;
		long remainingSeconds = ms % 60;
		if (hours > 0) {
			return String.format("%2d:%02d:%02d", hours, minutes, remainingSeconds);
		} else if (minutes > 0) {
			return String.format("%2d:%02d", minutes, remainingSeconds);
		} else {
			return String.format("%ds", remainingSeconds);
		}
	}
}

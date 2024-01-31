package com.github.egubot.shared;

import java.util.ArrayList;
import java.util.List;
import com.github.egubot.objects.CharacterHash;
import com.github.egubot.objects.Characters;

public class ConvertObjects {
	public static String listToText(List<String> data) {
		return String.join(" ", data);
	}

	public static List<String> textToList(String jsonData) {
		return new ArrayList<>(List.of(jsonData.split("\n")));
	}

	public static CharacterHash arraytoCharacterHash(List<Characters> arr) {
		CharacterHash hash = new CharacterHash();

		for (Characters element : arr) {
			if(hash.put(element))
				System.err.println("\nSite ID clash for: " + element.getSiteID());
		}
		return hash;
	}
}

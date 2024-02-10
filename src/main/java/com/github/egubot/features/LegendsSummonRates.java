package com.github.egubot.features;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.github.egubot.build.LegendsDatabase;
import com.github.egubot.objects.Characters;
import com.github.egubot.objects.SummonBanner;
import com.github.egubot.objects.SummonCharacter;
import com.github.egubot.objects.SummonStep;

public class LegendsSummonRates {
	private static final String FEATURED = "Featured";
	private static final String ULTRA = "Ultra";
	private static final String SPARKING = "Sparking";
	private static final String NEW = "New";
	private static final String LF = "LF";

	public static SummonBanner getBanner(String summonURL) throws IOException {
		Document document = Jsoup.connect(summonURL).get();
		SummonBanner banner = new SummonBanner();

		Element titleElement = document.selectFirst("h2.text-center");

		String title = titleElement.text();

		banner.setTitle(title);

		addSummonSteps(document, banner);

		addFeaturedCharacters(document, banner);

		return banner;
	}

	private static void addSummonSteps(Document document, SummonBanner banner) {
		Elements gashaDetails = document.select(".gasha-details");
		for (Element gashaDetail : gashaDetails) {
			SummonStep step = new SummonStep();

			Elements imageElements = gashaDetail.select("img");

			for (int i = 0; i < imageElements.size(); i++) {
				Element imageElement = imageElements.get(i);
				String imgTitle = imageElement.attr("title");
				String imageUrl = imageElement.absUrl("src");

				if (i == 0) {
					step.setCurrencyType(imgTitle);
				} else if (imgTitle.isBlank()) {
					step.setSpecialAttribute(imageUrl);
				} else {
					step.setRewardTitle(imgTitle);
					step.setRewardURL(imageUrl);
				}
			}

			Element crystalNumElement = gashaDetail.selectFirst(".crystal-num");
			String currencyNum;
			if (crystalNumElement != null)
				currencyNum = crystalNumElement.text();
			else {
				currencyNum = gashaDetail.selectFirst(".tix-num").text();
			}

			step.setCurrencyNeeded(currencyNum);

			try {
				Element rewardNumElement = gashaDetail.selectFirst(".reward-num");
				String rewardNum = rewardNumElement.text();
				step.setRewardNum(rewardNum);
			} catch (Exception e) {
				step.setRewardNum(0);
			}

			String text = gashaDetail.selectFirst("div[style]").text();
			step.setNumberOfPulls(text);

			if (step.getCurrencyNeeded() == 300) {
				banner.getOnceOnlySteps().add(step);
			} else {
				banner.getNormalSteps().add(step);
			}
		}
	}

	private static void addFeaturedCharacters(Document document, SummonBanner banner) {
		Element characterContainer = document.selectFirst(".character-container");

		Elements elements = characterContainer.select("a.chara-list");

		for (Element element : elements) {
			SummonCharacter character = new SummonCharacter();
			String href = element.attr("href");

			Characters temp = getCharacter(href);
			if (temp == null)
				continue;
			character.setCharacter(temp);

			Element leftElement = element.selectFirst("div.mx-2 > div:first-child");
			String leftValue = leftElement.text();
			character.setzPowerAmount(leftValue);

			Element rightElement = element.selectFirst("div.mx-2 > div:last-child");
			String rightValue = rightElement.text();
			character.setSummonRate(rightValue);

			Element isNewElement = element.selectFirst("div.isNew");
			character.setNew(isNewElement != null);
			banner.getFeaturedUnits().add(character);

			if (temp.isLF())
				banner.incrementLFCount();
		}
	}

	private static Map<String, Double> getTotalSummonsRates(SummonBanner banner, Map<Integer, Double> ratesMap) {
		Map<String, Double> result = new HashMap<>();

		result.put(LF, 0.0);
		result.put(NEW, 0.0);
		result.put(ULTRA, 0.0);
		result.put(SPARKING, 0.0);
		result.put(FEATURED, 0.0);

		double typeRate;
		double finalRate;
		for (SummonCharacter summonCharacter : banner.getFeaturedUnits()) {
			Characters character = summonCharacter.getCharacter();
			double rate = ratesMap.get(character.getSiteID());

			// New characters aren't part of the other total
			// LFs and sparkings are separated
			if (summonCharacter.isNew()) {
				typeRate = result.get(NEW);
				finalRate = (1 - typeRate) * (1 - rate);
				result.put(NEW, 1 - finalRate);
			} else if (character.isLF()) {
				typeRate = result.get(LF);
				finalRate = (1 - typeRate) * (1 - rate);
				result.put(LF, 1 - finalRate);
			} else if (character.isSparking()) {
				typeRate = result.get(SPARKING);
				finalRate = (1 - typeRate) * (1 - rate);
				result.put(SPARKING, 1 - finalRate);
			} else if (character.isUltra()) {
				typeRate = result.get(ULTRA);
				finalRate = (1 - typeRate) * (1 - rate);
				result.put(ULTRA, 1 - finalRate);
			}

			typeRate = result.get(FEATURED);
			finalRate = (1 - typeRate) * (1 - rate);
			result.put(FEATURED, 1 - finalRate);
		}

		for (SummonStep step : banner.getOnceOnlySteps()) {
			if (step.isOneLFGuaranteed() || step.isThreeLFGuaranteed()) {
				result.put(LF, 1.0);
				if (banner.getlFCount() > 0)
					result.put(FEATURED, 1.0);
			}

		}
		for (SummonStep step : banner.getNormalSteps()) {
			if (step.isOneLFGuaranteed() || step.isThreeLFGuaranteed()) {
				result.put(LF, 1.0);
				if (banner.getlFCount() > 0)
					result.put(FEATURED, 1.0);
			}
		}

		return result;
	}

	public static Map<String, Double> calculateChance(SummonBanner banner) {
		Map<Integer, Double> onceOnly = calculateChanceForOnceOnlySteps(banner);
		Map<Integer, Double> normal = calculateChanceForNormalSteps(banner);
		Map<Integer, Double> threeRotations = combineSteps(onceOnly,
				multiplyRotations(normal, 3, banner.getFeaturedUnits()), banner.getFeaturedUnits());
		System.out.println("3 rotations worth: " + threeRotations);
		Map<String, Double> result = null;

		System.out.println(getTotalSummonsRates(banner, threeRotations));

		return result;
	}

	private static Map<Integer, Double> multiplyRotations(Map<Integer, Double> normal, int i,
			List<SummonCharacter> featuredUnits) {
		Map<Integer, Double> result = new HashMap<>();
		for (SummonCharacter character : featuredUnits) {
			int id = character.getCharacter().getSiteID();
			double rate = normal.get(id);
			double newRate = Math.pow((1 - rate), i);
			result.put(id, 1 - newRate);
		}
		return result;
	}

	public static Map<Integer, Double> calculateChanceForOnceOnlySteps(SummonBanner banner) {
		List<Map<Integer, Double>> onceOnly = new ArrayList<>();

		for (SummonStep step : banner.getOnceOnlySteps()) {
			onceOnly.add(getStepChance(banner, step));
		}
		return combineSteps(onceOnly, banner.getFeaturedUnits());
	}

	public static Map<Integer, Double> calculateChanceForNormalSteps(SummonBanner banner) {
		List<Map<Integer, Double>> normal = new ArrayList<>();

		for (SummonStep step : banner.getNormalSteps()) {
			normal.add(getStepChance(banner, step));
		}

		return combineSteps(normal, banner.getFeaturedUnits());
	}

	private static Map<Integer, Double> combineSteps(Map<Integer, Double> results1, Map<Integer, Double> results2,
			List<SummonCharacter> featuredUnits) {
		Map<Integer, Double> result = new HashMap<>();
		for (SummonCharacter summonCharacter : featuredUnits) {
			int id = summonCharacter.getCharacter().getSiteID();
			double rate1 = results1.get(id);
			double rate2 = results2.get(id);
			double combinedRate = (1 - rate1) * (1 - rate2);
			result.put(id, 1 - combinedRate);
		}
		return result;
	}

	private static Map<Integer, Double> combineSteps(List<Map<Integer, Double>> stepResults,
			List<SummonCharacter> featuredUnits) {
		Map<Integer, Double> result = new HashMap<>();
		for (SummonCharacter summonCharacter : featuredUnits) {
			int id = summonCharacter.getCharacter().getSiteID();
			result.put(id, 0.0);
			for (Map<Integer, Double> map : stepResults) {
				double stepRate = map.get(id);
				double combinedRate = (1 - stepRate) * (1 - result.get(id));
				result.put(id, 1 - combinedRate);
			}
		}
		return result;
	}

	private static Map<Integer, Double> getStepChance(SummonBanner banner, SummonStep step) {
		Map<Integer, Double> map = new HashMap<>();

		double characterRate;
		double finalRate = 0;
		int initialPulls = step.getNumberOfPulls();
		double guaranteedSlotRate = 0;
		for (SummonCharacter summonCharacter : banner.getFeaturedUnits()) {
			Characters character = summonCharacter.getCharacter();
			characterRate = summonCharacter.getSummonRate();

			if (character.isLF() && step.isLFDouble())
				characterRate *= 2;
			if (character.isUltra() && step.isUltraDouble())
				characterRate *= 2;

			if (character.isLF() && step.isOneLFGuaranteed()) {
				initialPulls -= 1;
				guaranteedSlotRate = 1.0 / banner.getlFCount();
			} else if (character.isLF() && step.isThreeLFGuaranteed()) {
				initialPulls -= 3;
				guaranteedSlotRate = Math.pow(1.0 / banner.getlFCount(), 3);
				guaranteedSlotRate = 1 - guaranteedSlotRate;
			}

			finalRate = Math.pow(1 - characterRate, initialPulls);
			finalRate = 1 - finalRate;
			finalRate = (1 - finalRate) * (1 - guaranteedSlotRate);
			finalRate = 1 - finalRate;
			map.put(character.getSiteID(), finalRate);

		}
		return map;
	}

	private static Characters getCharacter(String href) {
		int id = LegendsDatabase.getSiteID(href);
		if (id == -1)
			return null;

		return LegendsDatabase.getCharacterHash().get(id);
	}

	public static void main(String[] args) {
		try {
			new LegendsDatabase();
			SummonBanner banner = getBanner("https://dblegends.net/banner/21400");
			System.out.println(banner.getTitle());
			calculateChance(banner);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

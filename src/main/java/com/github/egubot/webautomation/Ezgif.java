package com.github.egubot.webautomation;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Ezgif extends LocalWebDriver {
	private static final Pattern SIZE_PATTERN = Pattern.compile("(?i)(\\d+\\.?\\d*?)([KMG])?i?B");
	private static final Pattern LENGTH_PATTERN = Pattern.compile("length: (\\d{2}):(\\d{2}):(\\d{2})");
	private static final Pattern WIDTH_PATTERN = Pattern.compile("width: (\\d+)px");
	private static final Pattern HEIGHT_PATTERN = Pattern.compile("height: (\\d+)px");

	public Ezgif() {
		super(true, true, true);
	}

	public String videoToGIF(String url) {
		driver.get("https://ezgif.com/video-to-gif");

		uploadAndWait(url);

		WebElement end = driver.findElement(By.id("end"));
		end.clear();
		end.sendKeys("30");

		driver.findElement(By.name("video-to-gif")).click();

		String output = getImage();
		String size = getNewStats().getText();

		if (shouldCompress(size, "10MB")) {
			output = optimise(2, false);
		}

		return output;
	}

	public String gifToVideo(String url) {
		driver.get("https://ezgif.com/gif-to-mp4");

		uploadAndWait(url);

		driver.findElement(By.name("convert")).click();

		return getVideo();
	}

	private void uploadAndWait(String url) {
		driver.findElement(By.id("new-image-url")).sendKeys(url);
		driver.findElement(By.name("upload")).click();
		waitForPageChange();
	}

	private String getImage() {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofMinutes(2));
		WebElement outputElement = wait
				.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".outfile > img")));
		return outputElement.getAttribute("src");
	}

	private String getVideo() {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofMinutes(2));
		WebElement outputElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("video")));
		WebElement sourceElement = outputElement.findElement(By.tagName("source"));
		return sourceElement.getAttribute("src");	
	}

	private WebElement getNewStats() {
		return driver.findElement(By.cssSelector(".filestats:nth-child(2) > strong"));
	}

	private WebElement waitForPageChange() {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofMinutes(2));
		return wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".filestats")));
	}

	public String optimise(int attempts, boolean fromResize) {
		if (!fromResize)
			driver.findElement(By.cssSelector(".file-menu:nth-child(4) td:nth-child(4) img")).click();
		else
			driver.findElement(By.cssSelector(".file-menu:nth-child(3) td:nth-child(4) > a")).click();

		waitForPageChange();

		WebElement compress = driver.findElement(By.id("lossy"));
		compress.clear();
		compress.sendKeys("100");

		driver.findElement(By.name("optimize")).click();

		String output = getImage();
		String size = getNewStats().getText();

		if (shouldCompress(size, "10MB") && attempts > 1) {
			output = resize(--attempts);
		}

		return output;
	}

	public String resize(int attempts) {
		driver.findElement(By.cssSelector(".file-menu:nth-child(3) td:nth-child(2) > a")).click();

		waitForPageChange();

		WebElement a = driver.findElement(By.id("percentage"));
		a.clear();
		a.sendKeys("80");
		driver.findElement(By.name("resize-image")).click();

		getImage();

		return optimise(attempts, true);
	}

	public static int getLength(String input) {
		Matcher matcher = LENGTH_PATTERN.matcher(input);
		if (matcher.find()) {
			int hours = Integer.parseInt(matcher.group(1));
			int minutes = Integer.parseInt(matcher.group(2));
			int seconds = Integer.parseInt(matcher.group(3));
			int totalSeconds = hours * 3600 + minutes * 60 + seconds;
			return totalSeconds > 30 ? 30 : totalSeconds;
		}
		return -1;
	}

	public static int getWidth(String input) {
		Matcher matcher = WIDTH_PATTERN.matcher(input);
		if (matcher.find()) {
			return Integer.parseInt(matcher.group(1));
		}
		return -1;
	}

	public static int getHeight(String input) {
		Matcher matcher = HEIGHT_PATTERN.matcher(input);
		if (matcher.find()) {
			return Integer.parseInt(matcher.group(1));
		}
		return -1;
	}

	public static double getSizeInBytes(String input) {
		Matcher matcher = SIZE_PATTERN.matcher(input.toLowerCase());
		if (matcher.find()) {
			double size = Double.parseDouble(matcher.group(1));
			String unit = matcher.group(2);
			switch (unit) {
			case "k":
				return size * 1024;
			case "m":
				return size * 1024 * 1024;
			case "g":
				return size * 1024 * 1024 * 1024;
			default: // Assuming bytes
				return size;
			}
		}
		return -1;
	}

	public static boolean shouldCompress(String input, String target) {
		return getSizeInBytes(input) > getSizeInBytes(target);
	}

	public static void main(String[] args) {
		try (Ezgif a = new Ezgif()) {
			System.out.println(a.gifToVideo(""));
		}
	}
}
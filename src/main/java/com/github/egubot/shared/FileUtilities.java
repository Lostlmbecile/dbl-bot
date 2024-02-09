package com.github.egubot.shared;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class FileUtilities {
	protected static final Logger logger = LogManager.getLogger(FileUtilities.class.getName());

	public static void writeListToJson(List<?> list, String fileName) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			gson.toJson(list, writer);
		} catch (IOException e) {
			logger.error("Failed to write list to file.", e);
		}
	}

	public static OutputStream getFileOutputStream(String fileName) {
		try {
			return new FileOutputStream(fileName);
		} catch (IOException e) {
			logger.error("Failed to get output stream.", e);
			return null;
		}
	}

	public static InputStream getFileInputStream(String fileName, boolean createFileIfNotFound) {
		InputStream input;
		File file = new File(fileName);

		if (file.exists()) {
			input = getNewFile(file);

		} else {

			input = FileUtilities.class.getResourceAsStream("/" + fileName);

			if (input == null && createFileIfNotFound) {
				input = getNewFile(file);
			}
		}

		return input;
	}

	public static <T> List<T> readListFromJson(String fileName, Class<T[]> type) {
		try (InputStream input = getFileInputStream(fileName, true)) {
			Gson gson = new Gson();
			T[] array = gson.fromJson(new InputStreamReader(input), type);
			if (array != null) {
				return new ArrayList<>(List.of(array));
			}
		} catch (IOException e) {
			logger.error("Failed to read list from file.", e);
		}
		return new ArrayList<>();
	}

	private static InputStream getNewFile(File file) {
		try {
			if (!file.createNewFile()) {
				if (file.exists())
					return new FileInputStream(file);
				return null;
			} else {
				return new FileInputStream(file);
			}
		} catch (Exception e) {
			logger.error("Failed to create new file.", e);
			return null;
		}
	}

	public static String readInputStream(InputStream is) {
		StringBuilder result = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				result.append(line);
			}
		} catch (IOException e) {
			logger.error("Failed to read input stream.", e);
		}

		return result.toString();
	}

	public static String readInputStream(InputStream is, String joinLinesWith) {
		StringBuilder result = new StringBuilder();
		try (BufferedReader reader = getBufferedReader(is)) {
			String line;
			while ((line = reader.readLine()) != null) {
				result.append(line + joinLinesWith);
			}
		} catch (IOException e) {
			logger.error("Failed to read input stream.", e);
		}

		return result.toString();
	}

	public static String readURL(String link) {
		try {
			URL url = new URL(link);
			return FileUtilities.readInputStream(url.openStream(), "\n");
		} catch (IOException e) {
			return "failed";
		}
	}

	public static void writeToFile(String txt, String fileName) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
			writer.write(txt);
		} catch (IOException e) {
			logger.error("Failed to write string to file.", e);
		}
	}

	public static void writeToFile(List<String> list, String fileName) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
			writer.write(ConvertObjects.listToText(list, "\n"));
		} catch (IOException e) {
			logger.error("Failed to write string to file.", e);
		}
	}

	public static String getFileLastModified(String fileName) {
		File file = new File(fileName);
		if(!file.exists())
			return "null";
		
		long lastModifiedTime = file.lastModified();

		Instant instant = Instant.ofEpochMilli(lastModifiedTime);
		return ConvertObjects.instantToString(instant);
	}

	public static BufferedReader getBufferedReader(InputStream is) {
		return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
	}

	public static InputStream toInputStream(String st) {
		return IOUtils.toInputStream(st, StandardCharsets.UTF_8);
	}

	public static InputStream toInputStream(List<String> stringList) {
		String concatenatedString = String.join("\n", stringList);
		return toInputStream(concatenatedString);
	}

	public static BufferedInputStream toBufferedInputStream(List<String> list) {
		return new BufferedInputStream(toInputStream(list));
	}

	public static BufferedInputStream toBufferedInputStream(String st) {
		return new BufferedInputStream(toInputStream(st));
	}

	public static BufferedInputStream toBufferedInputStream(InputStream is) {
		return new BufferedInputStream(is);
	}

	public static void main(String[] args) {
		//FileUtilities.writeToFile(FileUtilities.readURL("https://dblegends.net/characters"), "legends.txt");
		System.out.println(FileUtilities.getFileLastModified("Autorespond.txt"));
	}
}
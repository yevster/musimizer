package com.musimizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class SettingsManager {
	// Application constants
	private static final String APP_NAME = "musimizer";
	private static final String SETTINGS_FILE = "settings.properties";
	
	// Property keys
	private static final String MUSIC_DIR_KEY = "music_dir";
	private static final String NUMBER_OF_PICKS_KEY = "number_of_picks";
	private static final String NUMBER_OF_SEARCH_RESULTS_KEY = "number_of_search_results";
	
	// Default values
	private static final int DEFAULT_NUMBER_OF_PICKS = 25;
	private static final int DEFAULT_NUMBER_OF_SEARCH_RESULTS = 25;
	private static final String SETTINGS_COMMENT = "Musimizer Settings";


	private static Path settingsDir() {
		String os = System.getProperty("os.name").toLowerCase();
		String userHome = System.getProperty("user.home");
		if (os.contains("win")) {
			String appData = System.getenv("APPDATA");
			return Paths.get(appData != null ? appData : userHome, APP_NAME);
		} else if (os.contains("mac")) {
			return Paths.get(userHome, "Library", "Application Support", APP_NAME);
		} else {
			return Paths.get(userHome, ".config", APP_NAME);
		}
	}


	private static Path settingsFile() {
		return settingsDir().resolve(SETTINGS_FILE);
	}


	public static Properties loadSettings() {
		Properties props = new Properties();
		Path file = settingsFile();
		if (Files.exists(file)) {
			try (var in = Files.newInputStream(file)) {
				props.load(in);
			} catch (IOException ignored) {}
		}
		return props;
	}


	public static void saveSettings(Properties props) {
		try {
			Files.createDirectories(settingsDir());
			try (var out = Files.newOutputStream(settingsFile())) {
				props.store(out, SETTINGS_COMMENT);
			}
		} catch (IOException ignored) {}
	}


	public static String getMusicDir() {
		return loadSettings().getProperty(MUSIC_DIR_KEY, "");
	}


	public static void setMusicDir(String dir) {
		Properties props = loadSettings();
		props.setProperty(MUSIC_DIR_KEY, dir);
		saveSettings(props);
	}


	public static int getNumberOfPicks() {
		String value = loadSettings().getProperty(NUMBER_OF_PICKS_KEY);
		try {
			return value != null ? Integer.parseInt(value) : DEFAULT_NUMBER_OF_PICKS;
		} catch (NumberFormatException e) {
			return DEFAULT_NUMBER_OF_PICKS;
		}
	}
	
	public static void setNumberOfPicks(int numberOfPicks) {
		if (numberOfPicks < 1) {
			numberOfPicks = 1; // Ensure at least 1 pick
		}
		Properties props = loadSettings();
		props.setProperty(NUMBER_OF_PICKS_KEY, String.valueOf(numberOfPicks));
		saveSettings(props);
	}

	public static int getNumberOfSearchResults() {
		String value = loadSettings().getProperty(NUMBER_OF_SEARCH_RESULTS_KEY);
		try {
			return value != null ? Integer.parseInt(value) : DEFAULT_NUMBER_OF_SEARCH_RESULTS;
		} catch (NumberFormatException e) {
			return DEFAULT_NUMBER_OF_SEARCH_RESULTS;
		}
	}
		
	public static void setNumberOfSearchResults(int count) {
		if (count < 1) {
			count = 1; // Ensure at least 1 search result
		}
		Properties props = loadSettings();
		props.setProperty(NUMBER_OF_SEARCH_RESULTS_KEY, String.valueOf(count));
		saveSettings(props);
	}
	

}

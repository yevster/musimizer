package com.musimizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class SettingsManager {
	private static final String APP_NAME = "musimizer";
	private static final String SETTINGS_FILE = "settings.properties";


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
				props.store(out, "Musimizer Settings");
			}
		} catch (IOException ignored) {}
	}


	public static String getMusicDir() {
		return loadSettings().getProperty("music_dir", "");
	}


	public static void setMusicDir(String dir) {
		Properties props = loadSettings();
		props.setProperty("music_dir", dir);
		saveSettings(props);
	}
}

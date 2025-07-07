package com.musimizer.settings;

/**
 * Interface for accessing application settings.
 * This allows for better testability by enabling dependency injection.
 */
public interface ApplicationSettings {
    /**
     * Checks if exclusions should be applied to search results.
     * @return true if exclusions should be applied, false otherwise
     */
    boolean isApplyExclusionsToSearch();
}

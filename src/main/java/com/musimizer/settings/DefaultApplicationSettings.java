package com.musimizer.settings;

import com.musimizer.util.SettingsManager;

/**
 * Default implementation of ApplicationSettings that delegates to SettingsManager.
 */
public class DefaultApplicationSettings implements ApplicationSettings {
    @Override
    public boolean isApplyExclusionsToSearch() {
        return SettingsManager.isApplyExclusionsToSearch();
    }
}

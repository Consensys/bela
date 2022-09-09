package org.hyperledger.bela.windows;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.prefs.Preferences;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.DirectoryDialogBuilder;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.components.ThemePicker;
import org.hyperledger.bela.components.settings.BelaSetting;
import org.hyperledger.bela.components.settings.CheckBoxSetting;
import org.hyperledger.bela.components.settings.PathSetting;
import org.hyperledger.bela.config.BelaConfigurationImpl;

import static org.hyperledger.bela.windows.Constants.DATA_PATH;
import static org.hyperledger.bela.windows.Constants.DATA_PATH_DEFAULT;
import static org.hyperledger.bela.windows.Constants.DEFAULT_THEME;
import static org.hyperledger.bela.windows.Constants.DETECT_COLUMNS;
import static org.hyperledger.bela.windows.Constants.FULL_SCREEN_WINDOWS;
import static org.hyperledger.bela.windows.Constants.GENESIS_PATH;
import static org.hyperledger.bela.windows.Constants.GENESIS_PATH_DEFAULT;
import static org.hyperledger.bela.windows.Constants.KEY_APPLY;
import static org.hyperledger.bela.windows.Constants.KEY_RESET;
import static org.hyperledger.bela.windows.Constants.OVERRIDE_STORAGE_PATH;
import static org.hyperledger.bela.windows.Constants.READ_ONLY_DB;
import static org.hyperledger.bela.windows.Constants.STORAGE_PATH;
import static org.hyperledger.bela.windows.Constants.STORAGE_PATH_DEFAULT;
import static org.hyperledger.bela.windows.Constants.THEME_KEY;

public class SettingsWindow extends AbstractBelaWindow {

    private final WindowBasedTextGUI gui;
    private final ThemePicker themePickerMenu;
    Map<String, BelaSetting<?>> settings = new LinkedHashMap<>();
    Preferences preferences;


    public SettingsWindow(final WindowBasedTextGUI gui, final Preferences preferences) {
        this.gui = gui;
        this.preferences = preferences;


        settings.put(DATA_PATH, new PathSetting(this.gui, "Data path", DATA_PATH, DATA_PATH_DEFAULT));
        settings.get(DATA_PATH).subscribe(value -> assumeStoragePath());

        settings.put(OVERRIDE_STORAGE_PATH, new CheckBoxSetting(this.gui, "Assume Storage path", OVERRIDE_STORAGE_PATH, true));
        settings.get(OVERRIDE_STORAGE_PATH).subscribe(value -> assumeStoragePath());

        settings.put(STORAGE_PATH, new PathSetting(this.gui, "Storage path", STORAGE_PATH, STORAGE_PATH_DEFAULT));
        settings.put(READ_ONLY_DB, new CheckBoxSetting(this.gui, "Read only database access", READ_ONLY_DB, true));
        settings.put(GENESIS_PATH, new PathSetting(this.gui, "Genesis path", GENESIS_PATH, GENESIS_PATH_DEFAULT));

        settings.put(DETECT_COLUMNS, new CheckBoxSetting(this.gui, "Auto detect columns in rocksdb", DETECT_COLUMNS, true));
        settings.put(FULL_SCREEN_WINDOWS, new CheckBoxSetting(this.gui, "open windows in full screen", FULL_SCREEN_WINDOWS, true));

        themePickerMenu = new ThemePicker(gui, preferences.get(THEME_KEY, DEFAULT_THEME));
    }


    @Override
    public String label() {
        return "Settings...";
    }

    @Override
    public MenuGroup group() {
        return MenuGroup.FILE;
    }

    @Override
    public KeyControls createControls() {
        return new KeyControls()
                .addControl("Reset", KEY_RESET, this::reset)
                .addControl("Apply", KEY_APPLY, this::apply);
    }


    @Override
    public Panel createMainPanel() {
        Panel panel = new Panel(new LinearLayout());

        for (BelaSetting<?> setting : settings.values()) {
            setting.load(preferences);
            panel.addComponent(setting.createComponent());
        }

        panel.addComponent(new Label("Theme"));
        panel.addComponent(themePickerMenu.createComponent()
                .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.BEGINNING, GridLayout.Alignment.BEGINNING, true, false, 2, 1)));

        return panel;
    }


    private void assumeStoragePath() {
        boolean assume = (boolean) settings.get(OVERRIDE_STORAGE_PATH).getValue();
        final BelaSetting<Path> storagePathSetting = (BelaSetting<Path>) settings.get(STORAGE_PATH);
        if (assume) {
            Path dataPath = (Path) settings.get(DATA_PATH).getValue();
            Path newPath = dataPath.resolve(STORAGE_PATH_DEFAULT);
            storagePathSetting.setValue(newPath);

        }
        storagePathSetting.setReadOnly(assume);
    }

    private void apply() {
        for (BelaSetting<?> belaSetting : settings.values()) {
            belaSetting.save(preferences);
        }
        preferences.put(THEME_KEY, themePickerMenu.getCurrentTheme());
        themePickerMenu.applyCurrent();
    }

    private void reset() {
        for (BelaSetting<?> belaSetting : settings.values()) {
            belaSetting.load(preferences);
        }
        themePickerMenu.resetToSavedTheme();
    }

    private void updateStoragePath(final TextBox storagePath, final boolean readOnly) {
        storagePath.setReadOnly(readOnly);
        if (readOnly) {
            final String defaultCalculatedPath = preferences.get(DATA_PATH, DATA_PATH_DEFAULT) + "/" + STORAGE_PATH_DEFAULT;
            storagePath.setText(defaultCalculatedPath);
        }
    }

    private Optional<String> askForPath(final String title, final String previousDirectory) {
        Path initialPath;
        try {
            initialPath = Path.of(previousDirectory);
        } catch (
                InvalidPathException e) {
            initialPath = Path.of(DATA_PATH_DEFAULT);
        }
        final File file = new DirectoryDialogBuilder()
                .setTitle(title)
                .setDescription("Choose a directory")
                .setActionLabel("Open")
                .setSelectedDirectory(initialPath.toFile())
                .build()
                .showDialog(gui);
        return Optional.ofNullable(file).map(File::toString);
    }

    public BelaConfigurationImpl createBelaConfiguration() {
        return new BelaConfigurationImpl(Path.of(preferences.get(DATA_PATH, DATA_PATH_DEFAULT)), Path.of(preferences.get(STORAGE_PATH, STORAGE_PATH_DEFAULT)));
    }
}

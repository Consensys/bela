package org.hyperledger.bela.windows;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.prefs.Preferences;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.CheckBox;
import com.googlecode.lanterna.gui2.CheckBoxList;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.DirectoryDialogBuilder;
import org.hyperledger.bela.components.ThemePicker;
import org.hyperledger.bela.config.BelaConfigurationImpl;

import static org.hyperledger.bela.windows.Constants.DATA_PATH;
import static org.hyperledger.bela.windows.Constants.DATA_PATH_DEFAULT;
import static org.hyperledger.bela.windows.Constants.DEFAULT_THEME;
import static org.hyperledger.bela.windows.Constants.DETECT_COLUMNS;
import static org.hyperledger.bela.windows.Constants.GENESIS_PATH;
import static org.hyperledger.bela.windows.Constants.GENESIS_PATH_DEFAULT;
import static org.hyperledger.bela.windows.Constants.OVERRIDE_STORAGE_PATH;
import static org.hyperledger.bela.windows.Constants.STORAGE_PATH;
import static org.hyperledger.bela.windows.Constants.STORAGE_PATH_DEFAULT;
import static org.hyperledger.bela.windows.Constants.THEME_KEY;

public class SettingsWindow implements LanternaWindow {

    private WindowBasedTextGUI gui;
    Preferences preferences;
    private ThemePicker themePickerMenu;
    private TextBox dataPath;
    private CheckBoxList<String> checkBoxList;
    private TextBox storagePath;
    private TextBox genesisPath;
    private Button storagePathButton;
    private CheckBox detectColumns = new CheckBox("Auto detect columns in rocksdb");

    public SettingsWindow(final WindowBasedTextGUI gui, final Preferences preferences) {
        this.gui = gui;
        this.preferences = preferences;
        themePickerMenu = new ThemePicker(gui, preferences.get(THEME_KEY, DEFAULT_THEME));
        detectColumns.setChecked(preferences.getBoolean(DETECT_COLUMNS, true));

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
    public Window createWindow() {
        Window window = new BasicWindow("Settings");
        Panel panel = new Panel(new GridLayout(3));
        GridLayout gridLayout = (GridLayout) panel.getLayoutManager();
        gridLayout.setHorizontalSpacing(3);


        panel.addComponent(new Label("Data Path"));
        dataPath = new TextBox(preferences.get(DATA_PATH, DATA_PATH_DEFAULT));
        dataPath.setTextChangeListener((newText, changedByUserInteraction) -> assumeStoragePath());
        panel.addComponent(dataPath);
        panel.addComponent(new Button("...", () -> {
            final Optional<String> path = askForPath("Data Path Directory");
            path.ifPresent(dataPath::setText);
        }));

        checkBoxList = new CheckBoxList<>();
        checkBoxList.addItem("Assume Storage path", preferences.getBoolean(OVERRIDE_STORAGE_PATH, true));
        panel.addComponent(new EmptySpace());
        panel.addComponent(checkBoxList.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.BEGINNING, GridLayout.Alignment.BEGINNING, true, false, 2, 1)));
        checkBoxList.addListener((itemIndex, checked) -> assumeStoragePath());

        panel.addComponent(new Label("Storage Path"));
        storagePath = new TextBox(preferences.get(STORAGE_PATH, STORAGE_PATH_DEFAULT));
        panel.addComponent(storagePath);
        storagePathButton = new Button("...", () -> {
            final Optional<String> path = askForPath("Storage Path Directory");
            path.ifPresent(storagePath::setText);
        });
        panel.addComponent(storagePathButton);
        assumeStoragePath();


        panel.addComponent(new Label("Genesis Path"));
        genesisPath = new TextBox(preferences.get(GENESIS_PATH, GENESIS_PATH_DEFAULT));
        panel.addComponent(genesisPath);
        panel.addComponent(new Button("...", () -> {
            final Optional<String> path = askForPath("Genesis Path Directory");
            path.ifPresent(genesisPath::setText);
        }));

        panel.addComponent(new EmptySpace());

        panel.addComponent(detectColumns);

        panel.addComponent(new EmptySpace());

        panel.addComponent(new Label("Theme"));
        panel.addComponent(themePickerMenu.createComponent()
                .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.BEGINNING, GridLayout.Alignment.BEGINNING, true, false, 2, 1)));

        panel.addComponent(new Button("Cancel", () -> {
            themePickerMenu.resetToSavedTheme();
            window.close();
        }));
        panel.addComponent(new Button("Apply", this::apply));

        panel.addComponent(new Button("Ok", () -> {
            apply();
            window.close();
        }));


        window.setComponent(panel);
        return window;
    }

    private void assumeStoragePath() {
        if (Boolean.TRUE.equals(checkBoxList.isChecked(0))) {
            storagePath.setEnabled(false);
            storagePathButton.setEnabled(false);
            storagePath.setText(dataPath.getText() + "/" + STORAGE_PATH_DEFAULT);
        } else {
            storagePath.setEnabled(true);
            storagePathButton.setEnabled(true);
        }
    }

    private void apply() {
        preferences.put(DATA_PATH, dataPath.getText());
        preferences.put(STORAGE_PATH, storagePath.getText());
        preferences.put(GENESIS_PATH, genesisPath.getText());
        preferences.putBoolean(OVERRIDE_STORAGE_PATH, checkBoxList.isChecked(0));
        preferences.put(THEME_KEY, themePickerMenu.getCurrentTheme());
        preferences.putBoolean(DETECT_COLUMNS, detectColumns.isChecked());
        themePickerMenu.applyCurrent();
    }

    private void updateStoragePath(final TextBox storagePath, final boolean readOnly) {
        storagePath.setReadOnly(readOnly);
        if (readOnly) {
            final String defaultCalculatedPath = preferences.get(DATA_PATH, DATA_PATH_DEFAULT) + "/" + STORAGE_PATH_DEFAULT;
            storagePath.setText(defaultCalculatedPath);
        }
    }

    private Optional<String> askForPath(final String title) {
        final File file = new DirectoryDialogBuilder()
                .setTitle(title)
                .setDescription("Choose a directory")
                .setActionLabel("Open")
                .build()
                .showDialog(gui);
        return Optional.ofNullable(file).map(File::toString);
    }

    public BelaConfigurationImpl createBelaConfiguration() {
        return new BelaConfigurationImpl(Path.of(preferences.get(DATA_PATH, DATA_PATH_DEFAULT)), Path.of(preferences.get(STORAGE_PATH, STORAGE_PATH_DEFAULT)));
    }
}

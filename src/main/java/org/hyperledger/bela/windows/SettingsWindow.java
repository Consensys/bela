package org.hyperledger.bela.windows;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.prefs.Preferences;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.CheckBoxList;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.DirectoryDialogBuilder;
import org.hyperledger.bela.config.BelaConfigurationImpl;

import static org.hyperledger.bela.windows.Constants.DATA_PATH;
import static org.hyperledger.bela.windows.Constants.DATA_PATH_DEFAULT;
import static org.hyperledger.bela.windows.Constants.GENESIS_PATH;
import static org.hyperledger.bela.windows.Constants.GENESIS_PATH_DEFAULT;
import static org.hyperledger.bela.windows.Constants.OVERRIDE_STORAGE_PATH;
import static org.hyperledger.bela.windows.Constants.STORAGE_PATH;
import static org.hyperledger.bela.windows.Constants.STORAGE_PATH_DEFAULT;

public class SettingsWindow implements LanternaWindow {

    private WindowBasedTextGUI gui;
    Preferences preferences;

    public SettingsWindow(final WindowBasedTextGUI gui, final Preferences preferences) {
        this.gui = gui;
        this.preferences = preferences;
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
        final TextBox dataPath = new TextBox(preferences.get(DATA_PATH, DATA_PATH_DEFAULT));
        panel.addComponent(dataPath);
        panel.addComponent(new Button("...", () -> {
            final Optional<String> path = askForPath("Data Path Directory");
            path.ifPresent(dataPath::setText);
        }));

        CheckBoxList<String> checkBoxList = new CheckBoxList<>();
        checkBoxList.addItem("Assume Storage path", preferences.getBoolean(OVERRIDE_STORAGE_PATH, true));
        panel.addComponent(new EmptySpace());
        panel.addComponent(checkBoxList);
        panel.addComponent(new EmptySpace());


        panel.addComponent(new Label("Storage Path"));
        final TextBox storagePath = new TextBox(preferences.get(STORAGE_PATH, STORAGE_PATH_DEFAULT));
        panel.addComponent(storagePath);
        panel.addComponent(new Button("...", () -> {
            final Optional<String> path = askForPath("Storage Path Directory");
            path.ifPresent(storagePath::setText);
        }));

        checkBoxList.addListener((itemIndex, checked) -> {
            if (itemIndex == 0) {
                updateStoragePath(storagePath, checked);
            }
        });
        updateStoragePath(storagePath, checkBoxList.isChecked(0));


        panel.addComponent(new Label("Genesis Path"));
        final TextBox genesisPath = new TextBox(preferences.get(GENESIS_PATH, GENESIS_PATH_DEFAULT));
        panel.addComponent(genesisPath);
        panel.addComponent(new Button("...", () -> {
            final Optional<String> path = askForPath("Genesis Path Directory");
            path.ifPresent(genesisPath::setText);
        }));

        panel.addComponent(new Button("Cancel", window::close));
        panel.addComponent(new Button("Apply", () -> {
            preferences.put(DATA_PATH, dataPath.getText());
            preferences.put(STORAGE_PATH, storagePath.getText());
            preferences.put(GENESIS_PATH, genesisPath.getText());
            preferences.putBoolean(OVERRIDE_STORAGE_PATH, checkBoxList.isChecked(0));
        }));

        panel.addComponent(new Button("Ok", () -> {
            preferences.put(DATA_PATH, dataPath.getText());
            preferences.put(STORAGE_PATH, storagePath.getText());
            preferences.put(GENESIS_PATH, genesisPath.getText());
            preferences.putBoolean(OVERRIDE_STORAGE_PATH, checkBoxList.isChecked(0));
            window.close();
        }));


        window.setComponent(panel);
        return window;
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

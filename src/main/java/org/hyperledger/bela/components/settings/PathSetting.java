package org.hyperledger.bela.components.settings;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.prefs.Preferences;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.DirectoryDialogBuilder;

import static org.hyperledger.bela.windows.Constants.DATA_PATH_DEFAULT;

public class PathSetting extends AbstractSetting<Path> {

    private final WindowBasedTextGUI gui;
    private final String label;
    private final String key;
    private final String defaultValue;
    private final TextBox pathTextBox;
    private final Button dataPathButton;

    public PathSetting(final WindowBasedTextGUI gui, final String label, final String key, final String defaultValue) {
        this.gui = gui;
        this.label = label;
        this.key = key;
        pathTextBox = new TextBox(defaultValue);
        this.defaultValue = defaultValue;
        pathTextBox.setTextChangeListener((tb, prev) -> {
            notifyListeners(Path.of(tb));
        });
        dataPathButton = new Button("...", () -> {
            final Optional<String> path = askForPath("Data Path Directory", pathTextBox.getText());
            path.ifPresent(pathTextBox::setText);
        });
    }

    @Override
    public Component createComponent() {
        Panel panel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        panel.addComponent(new Label(label));
        panel.addComponent(pathTextBox);
        panel.addComponent(dataPathButton);
        return panel;
    }

    @Override
    public Path getValue() {
        return Path.of(pathTextBox.getText());
    }

    @Override
    public void setValue(final Path value) {
        pathTextBox.setText(value.toString());
    }

    @Override
    public void load(final Preferences preferences) {
        final String path = preferences.get(key, defaultValue);
        pathTextBox.setText(path);
    }

    @Override
    public void save(final Preferences preferences) {
        preferences.put(key, pathTextBox.getText());
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

    @Override
    public void setReadOnly(final boolean readOnly) {
        pathTextBox.setReadOnly(readOnly);
        dataPathButton.setEnabled(!readOnly);
    }
}

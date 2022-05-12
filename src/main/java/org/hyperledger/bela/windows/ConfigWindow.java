package org.hyperledger.bela.windows;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.prefs.Preferences;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.DirectoryDialogBuilder;
import org.hyperledger.bela.config.BelaConfigurationImpl;
import org.jetbrains.annotations.NotNull;

public class ConfigWindow  implements LanternaWindow{

    private WindowBasedTextGUI gui;
    Preferences preferences ;

    public ConfigWindow(final WindowBasedTextGUI gui, final Preferences preferences) {
        this.gui = gui;
        this.preferences = preferences;
    }

    private static final  String DATA_PATH = "DATA_PATH";
    private static final  String DATA_PATH_DEFAULT = ".";
    private static final  String STORAGE_PATH = "STORAGE_PATH";
    private static final  String STORAGE_PATH_DEFAULT = "./database";


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
        Window window = new BasicWindow(label());
        Panel panel = new Panel(new GridLayout(3));
        GridLayout gridLayout = (GridLayout)panel.getLayoutManager();
        gridLayout.setHorizontalSpacing(3);


        panel.addComponent(new Label("Data Path"));
        final TextBox dataPath = new TextBox(preferences.get(DATA_PATH,DATA_PATH_DEFAULT));
        panel.addComponent(dataPath);
        panel.addComponent(new Button("...",() -> {
            final Optional<String> path = askForPath("Data Path Directory");
            path.ifPresent(dataPath::setText);
        }));

        panel.addComponent(new Label("Storage Path"));
        final TextBox storagePath = new TextBox(preferences.get(STORAGE_PATH,STORAGE_PATH_DEFAULT));
        panel.addComponent(storagePath);
        panel.addComponent(new Button("...",() -> {
            final Optional<String> path = askForPath("Storage Path Directory");
            path.ifPresent(storagePath::setText);
        }));

        panel.addComponent(new Button("Cancel", window::close));
        panel.addComponent(new Button("Apply",() ->{
            preferences.put(DATA_PATH,dataPath.getText());
            preferences.put(STORAGE_PATH, storagePath.getText());
        } ));

        panel.addComponent(new Button("Ok",() ->{
            preferences.put(DATA_PATH,dataPath.getText());
            preferences.put(STORAGE_PATH, storagePath.getText());
            window.close();
        } ));


        window.setComponent(panel);
        return window;
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

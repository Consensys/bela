package org.hyperledger.bela.windows;

import java.nio.file.Path;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import org.hyperledger.bela.config.BelaConfigurationImpl;

public class ConfigWindow  implements LanternaWindow{

    Preferences preferences ;

    public ConfigWindow(final Preferences preferences) {
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
        Panel panel = new Panel(new GridLayout(2));
        GridLayout gridLayout = (GridLayout)panel.getLayoutManager();
        gridLayout.setHorizontalSpacing(3);


        panel.addComponent(new Label("Data Path"));
        final TextBox dataPath = new TextBox(preferences.get(DATA_PATH,DATA_PATH_DEFAULT));
        panel.addComponent(dataPath);

        panel.addComponent(new Label("Storage Path"));
        final TextBox storagePath = new TextBox(preferences.get(STORAGE_PATH,STORAGE_PATH_DEFAULT));
        panel.addComponent(storagePath);

        panel.addComponent(new Button("Update...",() ->{
            preferences.put(DATA_PATH,dataPath.getText());
            preferences.put(STORAGE_PATH, storagePath.getText());
        } ));

        panel.addComponent(new Button("Cancel...", window::close));


        window.setComponent(panel);
        return window;
    }

    public BelaConfigurationImpl createBelaConfiguration() {
        return new BelaConfigurationImpl(Path.of(preferences.get(DATA_PATH,DATA_PATH_DEFAULT)),Path.of(STORAGE_PATH, STORAGE_PATH_DEFAULT));
    }
}

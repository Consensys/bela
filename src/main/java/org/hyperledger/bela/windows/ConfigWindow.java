package org.hyperledger.bela.windows;

import java.nio.file.Path;
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

    BelaConfigurationImpl configuration;

    public ConfigWindow(final BelaConfigurationImpl configuration) {
        this.configuration = configuration;
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
        Window window = new BasicWindow(label());
        Panel panel = new Panel(new GridLayout(2));
        GridLayout gridLayout = (GridLayout)panel.getLayoutManager();
        gridLayout.setHorizontalSpacing(3);


        panel.addComponent(new Label("Data Path"));
        final TextBox dataPath = new TextBox(configuration.getDataPath().toString());
        final Pattern pathPattern = Pattern.compile("^/|(/[a-zA-Z0-9_-]+)+$");
        dataPath.setValidationPattern(pathPattern);
        panel.addComponent(dataPath);

        panel.addComponent(new Label("Storage Path"));
        final TextBox storagePath = new TextBox(configuration.getStoragePath().toString());
        storagePath.setValidationPattern(pathPattern);
        panel.addComponent(storagePath);

        panel.addComponent(new Button("Update...",() ->{
            configuration.setDataPath(Path.of(dataPath.getText()));
            configuration.setStoragePath(Path.of(storagePath.getText()));
        } ));

        panel.addComponent(new Button("Cancel...", window::close));


        window.setComponent(panel);
        return window;
    }
}

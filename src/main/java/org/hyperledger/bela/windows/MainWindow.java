package org.hyperledger.bela.windows;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.menu.Menu;
import com.googlecode.lanterna.gui2.menu.MenuBar;
import com.googlecode.lanterna.gui2.menu.MenuItem;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.dialogs.BelaDialog;

import static kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory.getLogger;
import static org.hyperledger.bela.windows.Constants.FULL_SCREEN_WINDOWS;

public class MainWindow {
    private static final LambdaLogger log = getLogger(MainWindow.class);
    private final Window window = new BasicWindow("Main Window");
    private final Preferences preferences;
    private final Panel mainPanel = new Panel();
    private final List<BelaWindow> windows = new ArrayList<>();
    private final WindowBasedTextGUI gui;
    private KeyControls controls;

    public MainWindow(final WindowBasedTextGUI gui, final Preferences preferences) {
        this.gui = gui;
        this.preferences = preferences;
    }

    public void registerWindow(BelaWindow window) {
        this.windows.add(window);
    }

    public Window createWindow() {
        window.setHints(List.of(Window.Hint.FULL_SCREEN));
        window.setMenuBar(createMainMenu(window));
        window.setComponent(mainPanel);
        return window;
    }

    private MenuBar createMainMenu(final Window window) {
        final MenuBar bar = new MenuBar();

        Map<MenuGroup, Menu> groups = new EnumMap<>(MenuGroup.class);
        for (MenuGroup menuGroup : MenuGroup.values()) {
            groups.put(menuGroup, new Menu(menuGroup.name()));
            bar.add(groups.get(menuGroup));
        }
        for (BelaWindow belaWindow : windows) {
            groups.get(belaWindow.group())
                    .add(new MenuItem(belaWindow.label(), () -> launchWindow(belaWindow)));
        }

        groups.get(MenuGroup.FILE).add(new MenuItem("Exit", window::close));
        return bar;
    }

    private void launchWindow(final BelaWindow window) {
        if (preferences.getBoolean(FULL_SCREEN_WINDOWS, true)) {
            try {
                gui.addWindowAndWait(window.createWindow());
            } catch (Exception e) {
                log.error("There was an error when launching window {}", window.label(), e);
                BelaDialog.showException(gui, e);

            }
        } else {
            mainPanel.removeAllComponents();
            if (controls != null) {
                this.window.removeWindowListener(controls);
            }
            controls = window.createControls();
            mainPanel.addComponent(controls.createComponent());
            mainPanel.addComponent(window.createMainPanel());
            this.window.addWindowListener(controls);

        }

    }
}

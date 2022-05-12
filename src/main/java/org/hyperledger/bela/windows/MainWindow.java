package org.hyperledger.bela.windows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.menu.Menu;
import com.googlecode.lanterna.gui2.menu.MenuBar;
import com.googlecode.lanterna.gui2.menu.MenuItem;

public class MainWindow implements LanternaWindow {


    private List<LanternaWindow> windows = new ArrayList<>();
    private WindowBasedTextGUI gui;

    public MainWindow(final WindowBasedTextGUI gui) {

        this.gui = gui;
    }

    public void registerWindow(LanternaWindow window) {
        this.windows.add(window);
    }

    @Override
    public String label() {
        return "Main Window";
    }

    @Override
    public MenuGroup group() {
        return MenuGroup.FILE;
    }

    @Override
    public Window createWindow() {
        final Window window = new BasicWindow(label());
        final MenuBar bar = new MenuBar();

        Map<MenuGroup, Menu> groups = new HashMap<>();

        for (MenuGroup menuGroup : MenuGroup.values()) {
            groups.put(menuGroup, new Menu(menuGroup.name()));
            bar.add(groups.get(menuGroup));
        }
        for (LanternaWindow lanternaWindow : windows) {
            groups.get(lanternaWindow.group())
                    .add(new MenuItem(lanternaWindow.label(), () -> launchWindow(lanternaWindow)));
        }

        groups.get(MenuGroup.FILE).add(new MenuItem("Exit...", window::close));

        window.setMenuBar(bar);
        return window;
    }

    private void launchWindow(final LanternaWindow window) {
        try {
            gui.addWindowAndWait(window.createWindow());
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showMessageDialog(gui, "error", e.getMessage());

        }
    }
}

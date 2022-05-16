package org.hyperledger.bela.components;

import java.util.ArrayList;
import com.googlecode.lanterna.bundle.LanternaThemes;
import com.googlecode.lanterna.graphics.Theme;
import com.googlecode.lanterna.gui2.ComboBox;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;

public class ThemePicker implements LanternaComponent<Panel>, ComboBox.Listener {

    public static final ArrayList<String> REGISTERED_THEMES = new ArrayList<>(LanternaThemes.getRegisteredThemes());
    private final String savedTheme;
    private String currentTheme;
    private WindowBasedTextGUI gui;

    public ThemePicker(final WindowBasedTextGUI gui, final String savedTheme) {
        this.gui = gui;
        this.savedTheme = savedTheme;
    }

    @Override
    public Panel createComponent() {
        Panel panel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        panel.addComponent(new Label("Theme: "));


        final ComboBox<String> combo = new ComboBox<>(REGISTERED_THEMES);
        final Theme theme = gui.getTheme();
        for (int i = 0; i < REGISTERED_THEMES.size(); i++) {
            final Theme registeredTheme = LanternaThemes.getRegisteredTheme(REGISTERED_THEMES.get(i));
            if (registeredTheme.equals(theme)) {
                combo.setSelectedIndex(i);
            }
        }
        combo.addListener(this);
        panel.addComponent(combo);

        return panel;
    }

    @Override
    public void onSelectionChanged(final int selectedIndex, final int previousSelection, final boolean changedByUserInteraction) {
        currentTheme = REGISTERED_THEMES.get(selectedIndex);
        gui.setTheme(LanternaThemes.getRegisteredTheme(currentTheme));
    }

    public String getCurrentTheme() {
        return currentTheme;
    }

    public void resetToSavedTheme() {
        gui.setTheme(LanternaThemes.getRegisteredTheme(savedTheme));
    }
}

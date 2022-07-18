package org.hyperledger.bela.components;

import java.util.ArrayList;
import java.util.List;
import com.googlecode.lanterna.bundle.LanternaThemes;
import com.googlecode.lanterna.graphics.Theme;
import com.googlecode.lanterna.gui2.ComboBox;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;

public class ThemePicker implements BelaComponent<ComboBox<String>>, ComboBox.Listener {

    public static final List<String> REGISTERED_THEMES = new ArrayList<>(LanternaThemes.getRegisteredThemes());
    private String savedTheme;
    private String currentTheme;
    private WindowBasedTextGUI gui;

    public ThemePicker(final WindowBasedTextGUI gui, final String savedTheme) {
        this.gui = gui;
        this.savedTheme = savedTheme;
        this.currentTheme = savedTheme;
    }

    @Override
    public ComboBox<String> createComponent() {


        final ComboBox<String> combo = new ComboBox<>(REGISTERED_THEMES);
        final Theme theme = gui.getTheme();
        for (int i = 0; i < REGISTERED_THEMES.size(); i++) {
            final Theme registeredTheme = LanternaThemes.getRegisteredTheme(REGISTERED_THEMES.get(i));
            if (registeredTheme.equals(theme)) {
                combo.setSelectedIndex(i);
            }
        }
        combo.addListener(this);

        return combo;
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

    public void applyCurrent() {
        this.savedTheme = currentTheme;
    }
}

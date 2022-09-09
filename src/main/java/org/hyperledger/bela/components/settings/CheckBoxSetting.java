package org.hyperledger.bela.components.settings;

import java.util.prefs.Preferences;
import com.googlecode.lanterna.gui2.CheckBox;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;

public class CheckBoxSetting extends AbstractSetting<Boolean> {
    private final WindowBasedTextGUI gui;
    private final CheckBox checkbox;
    private final String key;
    private final boolean defaultValue;

    public CheckBoxSetting(final WindowBasedTextGUI gui, final String label, final String key, final boolean defaultValue) {
        this.gui = gui;
        this.checkbox = new CheckBox(label);
        this.key = key;
        this.defaultValue = defaultValue;
        this.checkbox.setChecked(defaultValue);
    }

    @Override
    public Boolean getValue() {
        return checkbox.isChecked();
    }

    @Override
    public void setValue(final Boolean value) {
        checkbox.setChecked(value);
    }

    @Override
    public void load(final Preferences preferences) {
        setValue(preferences.getBoolean(key, defaultValue));
    }

    @Override
    public void save(final Preferences preferences) {
        preferences.putBoolean(key, getValue());
    }

    @Override
    public void setReadOnly(final boolean readOnly) {
        checkbox.setEnabled(!readOnly);
    }

    public Component createComponent() {
        return checkbox;
    }
}

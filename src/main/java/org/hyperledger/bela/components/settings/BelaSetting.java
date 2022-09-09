package org.hyperledger.bela.components.settings;

import java.util.prefs.Preferences;
import org.hyperledger.bela.components.BelaComponent;

public interface BelaSetting<T> extends BelaComponent {
    T getValue();
    void setValue(T value);

    void load(Preferences preferences);
    void save(Preferences preferences);

    void subscribe(BelaSettingListener<T> listener);

    void setReadOnly(boolean readOnly);

    interface BelaSettingListener<T>{
        void onChange(T value);
    }
}

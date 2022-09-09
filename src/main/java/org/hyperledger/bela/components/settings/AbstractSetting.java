package org.hyperledger.bela.components.settings;

import java.util.ArrayList;
import java.util.prefs.Preferences;
import org.hyperledger.bela.components.Counter;

public abstract class AbstractSetting<T> implements BelaSetting<T>{

    private ArrayList<BelaSettingListener<T>> listeners = new ArrayList<>();

    @Override
    public void subscribe(final BelaSettingListener<T> listener) {
        this.listeners.add(listener);
    }

    protected void notifyListeners(final T value) {
        for (final BelaSettingListener<T> listener : listeners) {
            listener.onChange(value);
        }
    }
}

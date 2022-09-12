package org.hyperledger.bela.windows;

import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import org.hyperledger.bela.components.KeyControls;

public interface BelaWindow {
    String label();

    MenuGroup group();

    Window createWindow();

    KeyControls createControls();

    Panel createMainPanel();

    default void close(){};
}

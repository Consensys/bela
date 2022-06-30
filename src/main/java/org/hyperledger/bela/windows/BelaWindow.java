package org.hyperledger.bela.windows;

import com.googlecode.lanterna.gui2.Window;

public interface BelaWindow {
    String label();

    MenuGroup group();

    Window createWindow();
}

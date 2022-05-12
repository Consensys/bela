package org.hyperledger.bela.windows;

import com.googlecode.lanterna.gui2.Window;

public interface LanternaWindow {
    String label();

    MenuGroup group();

    Window createWindow();
}

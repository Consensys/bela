package org.hyperledger.bela.dialogs;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;

public interface BelaDialog {

    public void showAndWait(WindowBasedTextGUI gui);
}

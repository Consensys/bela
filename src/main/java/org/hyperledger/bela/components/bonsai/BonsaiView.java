package org.hyperledger.bela.components.bonsai;

import com.googlecode.lanterna.gui2.Panel;
import org.hyperledger.bela.components.BelaComponent;

public interface BonsaiView extends BelaComponent<Panel> {
    void focus();

    boolean isFocused();

    void takeFocus();

    void expand();
}

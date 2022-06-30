package org.hyperledger.bela.components.bonsai;

import com.googlecode.lanterna.gui2.Panel;
import org.hyperledger.bela.components.LanternaComponent;

public interface BonsaiView extends LanternaComponent<Panel> {
    void focus();

    boolean isFocused();

    void takeFocus();

    void expand();
}

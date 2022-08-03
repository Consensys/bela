package org.hyperledger.bela.components;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.AbstractListBox;

public class StringListBox extends AbstractListBox<String, StringListBox> {

    public StringListBox() {
        super();
    }

    public StringListBox(final TerminalSize size) {
        super(size);
    }
}

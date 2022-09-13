package org.hyperledger.bela.dialogs;

import java.io.IOException;
import java.util.List;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.ProgressBar;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;

public class ProgressBarPopup extends DialogWindow {

    private final ProgressBar progressBar;
    private final WindowBasedTextGUI gui;

    protected ProgressBarPopup(final WindowBasedTextGUI gui, final String title, final int maxValue) {
        super(title);
        this.gui = gui;
        Panel mainPanel = new Panel();
        mainPanel.setLayoutManager(
                new GridLayout(1)
                        .setLeftMarginSize(1)
                        .setRightMarginSize(1));
        progressBar = new ProgressBar(0, maxValue, 30);
        mainPanel.addComponent(progressBar);
        mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));
        setComponent(mainPanel);
        setHints(List.of(Hint.CENTERED));
    }

    public static ProgressBarPopup showPopup(final WindowBasedTextGUI gui, final String title, final int maxValue) {
        final ProgressBarPopup popup = new ProgressBarPopup(gui, title, maxValue);
        gui.addWindow(popup);
        try {
            gui.updateScreen();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return popup;
    }

    public void increment() {
        this.progressBar.setValue(this.progressBar.getValue() + 1);
        try {
            gui.updateScreen();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

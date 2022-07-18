package org.hyperledger.bela.dialogs;

import java.io.IOException;
import java.util.Arrays;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;

public class NonClosableMessage extends DialogWindow {
    /**
     * Default constructor, takes a title for the dialog and runs code shared for dialogs
     *
     * @param title Title of the window
     */
    public NonClosableMessage(final String title, final String text) {
        super(title);

        Panel mainPanel = new Panel();
        mainPanel.setLayoutManager(
                new GridLayout(1)
                        .setLeftMarginSize(1)
                        .setRightMarginSize(1));
        mainPanel.addComponent(new Label(text));
        mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));
        setComponent(mainPanel);
        setHints(Arrays.asList(Window.Hint.CENTERED));
    }

    public static NonClosableMessage showMessage(final WindowBasedTextGUI gui, final String text) {
        final NonClosableMessage message = new NonClosableMessage("Message",text);
        gui.addWindow(message);
        try {
            gui.updateScreen();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return message;
    }
}

package org.hyperledger.bela.dialogs;

import com.google.common.collect.ImmutableList;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;

public interface BelaDialog {


    static void showListDialog(WindowBasedTextGUI gui, String title, ImmutableList<String> list) {
        new ListDialog(title, list).showAndWait(gui);
    }

    static void showMessage(final WindowBasedTextGUI gui, String title, String message) {
        final MessageDialog dialog = new MessageDialogBuilder()
                .setTitle(title)
                .setText(message)
                .build();
        dialog.showDialog(gui);
    }

    public static void showException(final WindowBasedTextGUI gui, Exception e) {
        new BelaExceptionDialog(e).showAndWait(gui);
    }

    public void showAndWait(WindowBasedTextGUI gui);
}

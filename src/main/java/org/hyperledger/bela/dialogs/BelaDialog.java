package org.hyperledger.bela.dialogs;

import java.util.List;
import java.util.function.Function;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;

public interface BelaDialog {


    static void showListDialog(WindowBasedTextGUI gui, String title, List<String> list) {
        new ListDialog(title, list).showAndWait(gui);
    }

    static void showMessage(final WindowBasedTextGUI gui, String title, String message) {
        final MessageDialog dialog = new MessageDialogBuilder()
                .setTitle(title)
                .setText(message)
                .build();
        dialog.showDialog(gui);
    }

    static void showException(final WindowBasedTextGUI gui, Exception e) {
        new BelaExceptionDialog(e).showAndWait(gui);
    }

    static <T> void showDelegateListDialog(WindowBasedTextGUI gui, final String title, List<T> list, Function<T,String> nameGenerator, BelaActionListDialog.DialogSubscriber<T> subscriber) {
        new BelaActionListDialog<T>(title, list, nameGenerator, subscriber).showAndWait(gui);
    }

    void showAndWait(WindowBasedTextGUI gui);



}

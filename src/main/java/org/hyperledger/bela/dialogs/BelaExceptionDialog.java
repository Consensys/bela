package org.hyperledger.bela.dialogs;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import com.google.common.base.CharMatcher;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class BelaExceptionDialog implements BelaDialog {
    private static final Logger LOG = getLogger(BelaExceptionDialog.class);


    Exception exception;

    Mode mode = Mode.Message;
    Panel modePanel = new Panel();

    public BelaExceptionDialog(final Exception exception) {
        this.exception = exception;
        updateModePanel(mode, exception);
    }

    private void updateModePanel(final Mode mode, final Exception exception) {
        this.mode = mode;
        modePanel.removeAllComponents();
        modePanel.addComponent(mode.createComponent(exception));
    }

    @Override
    public void showAndWait(final WindowBasedTextGUI gui) {
        Window window = new BasicWindow("Exception");


        List<Button> buttons = new ArrayList<>();
        buttons.add(new Button("Close", window::close));
        buttons.add(new Button("Details", () -> updateModePanel(mode.other(), exception)));
        buttons.add(new Button("To Logs", () -> LOG.error("There was an exception", exception)));


        Panel buttonPanel = new Panel();
        buttonPanel.setLayoutManager(new GridLayout(buttons.size()).setHorizontalSpacing(1));
        for (final Button button : buttons) {
            buttonPanel.addComponent(button);
        }

        Panel mainPanel = new Panel();
        mainPanel.setLayoutManager(
                new GridLayout(1)
                        .setLeftMarginSize(1)
                        .setRightMarginSize(1));
        mainPanel.addComponent(modePanel);
        mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));
        buttonPanel.setLayoutData(
                        GridLayout.createLayoutData(
                                GridLayout.Alignment.END,
                                GridLayout.Alignment.CENTER,
                                false,
                                false))
                .addTo(mainPanel);

        window.setComponent(mainPanel);
        gui.addWindowAndWait(window);
    }

    private enum Mode {
        Message {
            @Override
            public Component createComponent(final Exception exception) {
                final String message = exception.getMessage();
                if (message != null) {
                    return new Label(CharMatcher.javaIsoControl().removeFrom(message));
                } else {
                    return new Label("With empty message");
                }
            }

            @Override
            public Mode other() {
                return StackTrace;
            }
        }, StackTrace {
            @Override
            public Component createComponent(final Exception exception) {
                StringWriter errors = new StringWriter();
                exception.printStackTrace(new PrintWriter(errors));
                final String initialContent = errors.toString();
                final TextBox textBox = new TextBox(new TerminalSize(80, 20));
                textBox.setText(initialContent);
                return textBox;
            }

            @Override
            public Mode other() {
                return Message;
            }
        };

        abstract public Component createComponent(final Exception exception);

        abstract public Mode other();
    }
}

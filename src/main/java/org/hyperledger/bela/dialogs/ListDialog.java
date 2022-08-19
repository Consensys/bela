package org.hyperledger.bela.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import org.hyperledger.bela.components.StringListBox;

public class ListDialog implements BelaDialog {

    List<String> items;
    String title;

    public ListDialog(String title, List<String> items) {
        this.title = title;
        this.items = items;
    }

    @Override
    public void showAndWait(final WindowBasedTextGUI gui) {
        Window window = new BasicWindow(title);


        List<Button> buttons = new ArrayList<>();
        buttons.add(new Button("Close", window::close));

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

        final StringListBox component = new StringListBox(new TerminalSize(calculateWidth(), Math.min(items.size(),30)));
        for (String element : items) {
            component.addItem(element);
        }

        mainPanel.addComponent(component);

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

    private int calculateWidth() {
        final Optional<Integer> max = this.items.stream().map(String::length).max(Integer::compareTo);
        return Math.min(max.orElse(20) + 2, 60);
    }
}

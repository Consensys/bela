package org.hyperledger.bela.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import org.hyperledger.bela.components.StringListBox;

public class BelaActionListDialog<T> implements BelaDialog{
    private final List<T> items;
    private final Function<T, String> nameGenerator;
    private final DialogSubscriber<T> subscriber;
    private final String title;

    public BelaActionListDialog(final String title, List<T> list, Function<T,String> nameGenerator, DialogSubscriber<T> subscriber) {
        this.title = title;
        this.items = list;
        this.nameGenerator = nameGenerator;
        this.subscriber = subscriber;
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

        final ActionListBox actionListBox = new ActionListBox(new TerminalSize(calculateWidth(), 10));
        for (T element : items) {
            actionListBox.addItem(nameGenerator.apply(element), () ->subscriber.onItemSelected(element));
        }

        mainPanel.addComponent(actionListBox);

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
        final Optional<Integer> max = this.items.stream().map(t -> nameGenerator.apply(t).length()).max(Integer::compareTo);
        return Math.min(max.orElse(20) + 2, 60);
    }

    public interface DialogSubscriber<T>{
        void onItemSelected(T item);
    }
}

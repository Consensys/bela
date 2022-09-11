package org.hyperledger.bela.components.bonsai;

import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import org.hyperledger.bela.components.BelaComponent;

public class LabelWithTextBox implements BelaComponent<Panel> {
    private final Label label;
    private final TextBox valueTextBox;

    public LabelWithTextBox(final String title, final String value) {
        label = new Label(title);
        valueTextBox = new TextBox(value);
        valueTextBox.setReadOnly(true);
    }

    public static LabelWithTextBox labelWithTextBox(final String title, final String value) {
        return new LabelWithTextBox(title, value);
    }

    @Override
    public Panel createComponent() {
        Panel panel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        panel.addComponent(label);
        panel.addComponent(valueTextBox);
        return panel;
    }
}

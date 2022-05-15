package org.hyperledger.bela.components;

import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;

public class Counter implements LanternaComponent<Panel> {
    private Label number = new Label("0");

    private final String name;

    public Counter(final String name) {
        this.name = name;
    }


    @Override
    public Panel createComponent() {
        Panel panel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        panel.addComponent(new Label(name));
        panel.addComponent(number);
        return panel;
    }

    public int add(int value) {
        int v = Integer.parseInt(number.getText()) + value;
        number.setText(String.valueOf(v));
        return v;
    }
}

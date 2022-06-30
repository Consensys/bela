package org.hyperledger.bela.components.bonsai;

import java.util.ArrayList;
import java.util.List;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;

public abstract class AbstractBonsaiNodeView implements BonsaiView{
    private final TextBox labelBox;
    protected final int depth;

    private List<BonsaiView> children = new ArrayList<>();


    protected Panel panel= new Panel();


    public AbstractBonsaiNodeView(final String label, final int depth) {
        this.depth = depth;
        String content = " ".repeat(depth * 2) + "└─" + label;
        labelBox = new TextBox(content, TextBox.Style.SINGLE_LINE);
        labelBox.setReadOnly(true);
    }

    @Override
    public void takeFocus() {
        labelBox.takeFocus();
    }

    public boolean isFocused() {
        return labelBox.isFocused();
    }

    @Override
    public Panel createComponent() {
        redraw();
        return panel;
    }

    protected void redraw() {
        panel.removeAllComponents();
        panel.addComponent(labelBox);
        children.forEach(c -> panel.addComponent(c.createComponent()));
    }

    public void focus() {
        if (isFocused()) {
            expand();
        } else {
            checkChildren();
        }
    }

    private void checkChildren() {
        for (BonsaiView child : children) {
            child.focus();
            if (child.isFocused()){
                this.children = new ArrayList<>();
                children.add(child);
                redraw();
                child.takeFocus();
                return;
            }
        }
    }

    protected void setChildren(List<BonsaiView> children) {
        this.children = children;
    }

    protected void addChild(final LabelNodeView child) {
        children.add(child);
    }
}

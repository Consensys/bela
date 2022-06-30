package org.hyperledger.bela.components;

import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;

public class TraceTransactionPanel implements BelaComponent<Panel> {
    private static final int MAX_VALUE_WIDTH = 60;

    private final Label trace = new Label("empty").setLabelWidth(MAX_VALUE_WIDTH);

    public TraceTransactionPanel(final String traceResult) {
        updateWithTrace(traceResult);
    }

    @Override
    public Panel createComponent() {
        Panel panel = new Panel();
        panel.setLayoutManager(new GridLayout(2));

        panel.addComponent(new Label("Trace:"));
        panel.addComponent(trace);

        return panel;
    }

    public void updateWithTrace(final String trace) {
        this.trace.setText(trace);
    }
}

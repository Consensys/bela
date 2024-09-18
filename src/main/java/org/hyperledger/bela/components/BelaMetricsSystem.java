package org.hyperledger.bela.components;

import java.util.function.DoubleSupplier;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.Counter;
import org.hyperledger.besu.plugin.services.metrics.LabelledGauge;
import org.hyperledger.besu.plugin.services.metrics.LabelledMetric;
import org.hyperledger.besu.plugin.services.metrics.MetricCategory;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer;

public class BelaMetricsSystem implements BelaComponent<Panel> {

    MetricsSystem delegate;

    public BelaMetricsSystem(final MetricsSystem delegate) {
        this.delegate = delegate;

    }

    @Override
    public Panel createComponent() {
        Panel panel = new Panel(new LinearLayout());
        panel.addComponent(new Label("Empty for now..."));
        return panel;
    }
}

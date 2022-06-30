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

public class BelaMetricsSystem implements MetricsSystem, BelaComponent<Panel> {

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

    @Override
    public LabelledMetric<Counter> createLabelledCounter(final MetricCategory category, final String name, final String help, final String... labelNames) {
        return delegate.createLabelledCounter(category, name, help, labelNames);
    }

    @Override
    public LabelledGauge createLabelledGauge(final MetricCategory category, final String name, final String help, final String... labelNames) {
        return delegate.createLabelledGauge(category, name, help, labelNames);
    }

    @Override
    public LabelledMetric<OperationTimer> createLabelledTimer(final MetricCategory category, final String name, final String help, final String... labelNames) {
        return delegate.createLabelledTimer(category, name, help, labelNames);
    }

    @Override
    public void createGauge(final MetricCategory category, final String name, final String help, final DoubleSupplier valueSupplier) {
        delegate.createGauge(category, name, help, valueSupplier);
    }


}

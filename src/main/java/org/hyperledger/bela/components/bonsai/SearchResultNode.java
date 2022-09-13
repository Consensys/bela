package org.hyperledger.bela.components.bonsai;

import java.util.List;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Panel;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;

public class SearchResultNode extends AbstractBonsaiNode {
    private final List<BonsaiNode> results;
    private final KeyValueStorage storage;

    public SearchResultNode(final KeyValueStorage storage, final List<BonsaiNode> results) {
        super("Search Result " + results.size());
        this.results = results;
        this.storage = storage;
    }

    @Override
    public Component createComponent() {
        Panel panel = new Panel();
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Entries", String.valueOf(results.size()))
                .createComponent());
        return panel.withBorder(Borders.singleLine("Search result"));
    }

    @Override
    public List<BonsaiNode> getChildren() {
        return results;
    }

    @Override
    public void log() {
        log.info("Search result");
        log.info("Entries: {}", results.size());
        results.forEach(BonsaiNode::log);
    }
}

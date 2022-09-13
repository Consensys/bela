package org.hyperledger.bela.components.bonsai;

import java.util.List;
import java.util.stream.Collectors;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Panel;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;

public class SearchResultBlocksNode extends AbstractBonsaiNode {
    private final List<Hash> blocks;
    private final KeyValueStorage storage;

    public SearchResultBlocksNode(final KeyValueStorage storage, final List<Hash> blocks) {
        super("Search Result " + blocks.size());
        this.blocks = blocks;
        this.storage = storage;
    }

    @Override
    public Component createComponent() {
        Panel panel = new Panel();
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Entries", String.valueOf(blocks.size()))
                .createComponent());
        return panel.withBorder(Borders.singleLine("Search result"));
    }

    @Override
    public List<BonsaiNode> getChildren() {
        return blocks.stream().map(block -> new LazyTrieLogNode(storage, block)).collect(Collectors.toList());
    }

    @Override
    public void log() {

    }
}

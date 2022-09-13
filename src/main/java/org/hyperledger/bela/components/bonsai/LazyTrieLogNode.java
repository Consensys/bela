package org.hyperledger.bela.components.bonsai;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Panel;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;

public class LazyTrieLogNode extends AbstractBonsaiNode {
    private final KeyValueStorage storage;
    @org.jetbrains.annotations.NotNull
    private final Hash blockHash;

    public LazyTrieLogNode(final KeyValueStorage storage, final Hash blockHash) {
        super(blockHash.toHexString());
        this.storage = storage;
        this.blockHash = blockHash;
    }

    @Override
    public Component createComponent() {
        Panel panel = new Panel();
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Block Hash:", blockHash.toHexString()).createComponent());
        return panel.withBorder(Borders.singleLine("Search result"));
    }

    @Override
    public List<BonsaiNode> getChildren() {
        final Optional<BonsaiTrieLogNode> bonsaiTrieLogNode = BonsaiTrieLogView.getTrieLog(storage, blockHash).map(
                l -> new BonsaiTrieLogNode(blockHash, l));
        return (bonsaiTrieLogNode.<List<BonsaiNode>>map(List::of).orElseGet(ArrayList::new));
    }

    @Override
    public void log() {
        log.info("Trie for block {}", blockHash);
    }
}

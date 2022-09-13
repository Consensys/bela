package org.hyperledger.bela.components.bonsai;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.rlp.RLP;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.trie.Node;

public class StorageValueNode extends AbstractBonsaiNode {

    private final Hash accountHash;
    private final Node<Bytes> node;
    private final BonsaiStorageView bonsaiStorageView;
    private final Bytes value;

    public StorageValueNode(final BonsaiStorageView bonsaiStorageView, final Hash accountHash, final Node<Bytes> node) {
        super("Storage value");
        this.bonsaiStorageView = bonsaiStorageView;
        this.accountHash = accountHash;
        this.node = node;
        this.value = Bytes32.leftPad(RLP.decodeValue(node.getValue()
                .orElseThrow()));

    }

    @Override
    public List<BonsaiNode> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public Component createComponent() {
        Panel panel = new Panel();
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Account hash:", accountHash.toHexString())
                .createComponent());
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Location:", node.getLocation().map(Bytes::toHexString)
                .orElse("")).createComponent());
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Hash:", node.getHash().toHexString()).createComponent());
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Value:", value.toHexString()).createComponent());

        final Optional<Bytes> storageInFlatDB = bonsaiStorageView.getStorageInFlatDB(accountHash, node.getLocation()
                .orElseThrow(), node.getPath());
        if (storageInFlatDB.isPresent()) {
            panel.addComponent(LabelWithTextBox.labelWithTextBox("Hash in Flat DB:", storageInFlatDB.get()
                    .toHexString()).createComponent());
        } else {
            panel.addComponent(new Label("No value in flat DB"));
        }
        return panel.withBorder(Borders.singleLine("Storage Value Node"));
    }

    @Override
    public void log() {
        log.info("Storage value node: {}", value.toHexString());
        log.info("Account hash: {}", accountHash.toHexString());
        log.info("Location: {}", node.getLocation().map(Bytes::toHexString)
                .orElse(""));
        log.info("Hash: {}", node.getHash().toHexString());
        log.info("Value: {}", value.toHexString());
        final Optional<Bytes> storageInFlatDB = bonsaiStorageView.getStorageInFlatDB(accountHash, node.getLocation()
                .orElseThrow(), node.getPath());
        if (storageInFlatDB.isPresent()) {
            log.info("Value in Flat DB: {}", storageInFlatDB.get()
                    .toHexString());
        } else {
            log.info("No value in flat DB");
        }
    }
}

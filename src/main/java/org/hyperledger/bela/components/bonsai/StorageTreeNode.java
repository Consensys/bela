package org.hyperledger.bela.components.bonsai;

import java.util.List;
import java.util.stream.Collectors;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Panel;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.trie.Node;
import org.hyperledger.besu.ethereum.trie.TrieNodeDecoder;

public class StorageTreeNode extends AbstractBonsaiNode {
    private final BonsaiStorageView bonsaiStorageView;
    private final Hash accountHash;
    private final Node<Bytes> node;
    private final Bytes32 storageRootHash;

    public StorageTreeNode(final BonsaiStorageView bonsaiStorageView, final Hash accountHash, final Node<Bytes> storageNodeValue, final Bytes32 storageRootHash) {
        super(label(storageNodeValue, storageRootHash));
        this.bonsaiStorageView = bonsaiStorageView;
        this.accountHash = accountHash;
        this.node = storageNodeValue;
        this.storageRootHash = storageRootHash;
    }

    private static String label(final Node<Bytes> storageNodeValue, final Bytes32 storageRootHash) {
        if (storageRootHash.equals(storageNodeValue.getHash())) {
            return storageNodeValue.getLocation().map(Bytes::toHexString)
                    .orElse("");
        } else {
            return "!!!" + storageNodeValue.getLocation().map(Bytes::toHexString)
                    .orElse("");
        }

    }

    @Override
    public List<BonsaiNode> getChildren() {
        final List<Node<Bytes>> nodes =
                TrieNodeDecoder.decodeNodes(node.getLocation().orElseThrow(), node.getRlp());
        final List<BonsaiNode> children = nodes.stream()
                .map(node -> {
                    if (bonsaiStorageView.nodeIsHashReferencedDescendant(this.node, node)) {
                        return new StorageTreeNode(bonsaiStorageView, accountHash, bonsaiStorageView.getStorageNodeValue(node.getHash(), accountHash, node.getLocation()
                                .orElseThrow()), node.getHash());
                    } else if (node.getValue().isPresent()) {
                        return new StorageValueNode(bonsaiStorageView, accountHash, bonsaiStorageView.getStorageNodeValue(node.getHash(), accountHash, node.getLocation()
                                .orElseThrow()));

                    } else {
                        return new LabelNode("Missing value in storage", accountHash.toHexString() + " on " + label(node, storageRootHash));
                    }

                }).collect(Collectors.toList());
        if (children.size() > 1) {
            return (children.stream().filter(child -> !(child instanceof LabelNode))
                    .collect(Collectors.toList()));
        }
        return children;

    }

    @Override
    public Component createComponent() {
        Panel panel = new Panel();
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Account", accountHash.toHexString()).createComponent());
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Hash", node.getHash().toHexString()).createComponent());
        panel.addComponent(LabelWithTextBox.labelWithTextBox("ExpectedHash", storageRootHash.toHexString())
                .createComponent());
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Location", node.getLocation().map(Bytes::toHexString)
                .orElse("")).createComponent());
        return panel.withBorder(Borders.singleLine("Storage Tree Node"));
    }

    @Override
    public void log() {
        log.info("Storage Tree Node");
        log.info("Account: {}", accountHash.toHexString());
        log.info("Location: {}", node.getLocation().map(Bytes::toHexString).orElse(""));
        log.info("Hash: {}", node.getHash().toHexString());
        log.info("Expected Hash: {}", storageRootHash.toHexString());
    }
}

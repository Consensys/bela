package org.hyperledger.bela.components.bonsai;

import java.util.List;
import java.util.stream.Collectors;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.trie.Node;
import org.hyperledger.besu.ethereum.trie.TrieNodeDecoder;

public class StorageTreeNode extends AbstractBonsaiNode {
    private final BonsaiStorageView bonsaiStorageView;
    private final Hash accountHash;
    private final Node<Bytes> parentNode;

    public StorageTreeNode(final BonsaiStorageView bonsaiStorageView, final Hash accountHash, final Node<Bytes> storageNodeValue, final int depth) {
        super(label(storageNodeValue), depth);
        this.bonsaiStorageView = bonsaiStorageView;
        this.accountHash = accountHash;
        this.parentNode = storageNodeValue;
    }

    private static String label(final Node<Bytes> storageNodeValue) {
        return "S(" + storageNodeValue.getLocation().map(Bytes::toHexString)
                .orElse("") + ")" + storageNodeValue.getHash()
                .toHexString();
    }

    @Override
    public List<BonsaiNode> getChildren() {
        final List<Node<Bytes>> nodes =
                TrieNodeDecoder.decodeNodes(parentNode.getLocation().orElseThrow(), parentNode.getRlp());
        final List<BonsaiNode> children = nodes.stream()
                .map(node -> {
                    if (bonsaiStorageView.nodeIsHashReferencedDescendant(parentNode, node)) {
                        return new StorageTreeNode(bonsaiStorageView, accountHash, bonsaiStorageView.getStorageNodeValue(node.getHash(), accountHash, node.getLocation()
                                .orElseThrow()), depth + 1);
                    } else if (node.getValue().isPresent()) {
                        return new StorageValueNode(bonsaiStorageView, accountHash, bonsaiStorageView.getStorageNodeValue(node.getHash(), accountHash, node.getLocation()
                                .orElseThrow()), depth + 1);

                    } else {
                        return new LabelNode("Missing value in storage for account " + accountHash.toHexString() + " on " + label(node), depth + 1);
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
        panel.addComponent(new Label("Account: " + accountHash.toHexString()));
        panel.addComponent(new Label("Location: " + parentNode.getLocation().map(Bytes::toHexString).orElse("")));
        panel.addComponent(new Label("Hash: " + parentNode.getHash().toHexString()));
        return panel.withBorder(Borders.singleLine("Storage Tree Node"));
    }
}

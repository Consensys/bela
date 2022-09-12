package org.hyperledger.bela.components.bonsai;

import java.util.List;
import java.util.stream.Collectors;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Panel;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.trie.CompactEncoding;
import org.hyperledger.besu.ethereum.trie.Node;
import org.hyperledger.besu.ethereum.trie.TrieNodeDecoder;
import org.jetbrains.annotations.NotNull;

public class AccountTreeNode extends AbstractBonsaiNode {
    private final BonsaiStorageView bonsaiStorageView;
    private final Node<Bytes> node;


    public AccountTreeNode(final BonsaiStorageView bonsaiStorageView, final Node<Bytes> node, final int depth) {
        super(calculateLabel(node), depth);
        this.bonsaiStorageView = bonsaiStorageView;
        this.node = node;
    }

    @NotNull
    private static String calculateLabel(final Node<Bytes> node) {
        return node.getLocation().map(Bytes::toHexString).orElse("");
    }

    @Override
    public List<BonsaiNode> getChildren() {
        final List<Node<Bytes>> nodes =
                TrieNodeDecoder.decodeNodes(node.getLocation().orElseThrow(), node.getRlp());
        final List<BonsaiNode> children = nodes.stream()
                .map(node -> {
                    if (bonsaiStorageView.nodeIsHashReferencedDescendant(this.node, node)) {
                        return new AccountTreeNode(bonsaiStorageView, bonsaiStorageView.getAccountNodeValue(node.getHash(), node.getLocation()
                                .orElseThrow()), depth + 1);
                    } else if (node.getValue().isPresent()) {
                        final Hash accountHash = Hash.wrap(Bytes32.wrap(CompactEncoding.pathToBytes(Bytes.concatenate(this.node.getLocation()
                                .orElseThrow(), node.getPath()))));
                        return new AccountValueNode(bonsaiStorageView, accountHash, node.getValue()
                                .get(), depth + 1, AccountValueNode.StorageLocation.BONSAI);

                    } else {
                        return new LabelNode("Missing value for ", calculateLabel(node), depth + 1);
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
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Location", node.getLocation()
                .map(Bytes::toHexString).orElse("")).createComponent());
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Hash", node.getHash().toHexString())
                .createComponent());
        panel.addComponent(LabelWithTextBox.labelWithTextBox("RLP", node.getRlp().toHexString())
                .createComponent());
        return panel.withBorder(Borders.singleLine("Account Tree Node"));
    }


    public Bytes getLocation() {
        return node.getLocation().orElseThrow();
    }

    @Override
    public void log() {
        log.info("AccountTreeNode");
        log.info("Location: {}", node.getLocation().map(Bytes::toHexString).orElse(""));
        log.info("Hash: {}", node.getHash().toHexString());
        log.info("RLP: {}", node.getRlp().toHexString());
    }
}

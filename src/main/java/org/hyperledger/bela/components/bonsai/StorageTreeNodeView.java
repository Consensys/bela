package org.hyperledger.bela.components.bonsai;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.bela.components.BonsaiStorageView;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.trie.Node;
import org.hyperledger.besu.ethereum.trie.TrieNodeDecoder;

public class StorageTreeNodeView extends AbstractBonsaiNodeView {
    private final BonsaiStorageView bonsaiStorageView;
    private final Hash accountHash;
    private final Node<Bytes> parentNode;

    public StorageTreeNodeView(final BonsaiStorageView bonsaiStorageView, final Hash accountHash, final Node<Bytes> storageNodeValue, final int depth) {
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
    public void expand() {
        final List<Node<Bytes>> nodes =
                TrieNodeDecoder.decodeNodes(parentNode.getLocation().orElseThrow(), parentNode.getRlp());
        final List<BonsaiView> children = nodes.stream()
                .map(node -> {
                    if (bonsaiStorageView.nodeIsHashReferencedDescendant(parentNode, node)) {
                        return new StorageTreeNodeView(bonsaiStorageView, accountHash, bonsaiStorageView.getStorageNodeValue(node.getHash(), accountHash, node.getLocation()
                                .orElseThrow()), depth + 1);
                    } else if (node.getValue().isPresent()) {
                        return new StorageValueView(bonsaiStorageView,accountHash,bonsaiStorageView.getStorageNodeValue(node.getHash(), accountHash, node.getLocation()
                                .orElseThrow()), depth + 1);

                    }  else {
                        return new LabelNodeView("Missing value in storage for account "+ accountHash.toHexString() + " on "+ label(node), depth + 1);
                    }

                }).collect(Collectors.toList());
        if (children.size() > 1) {
            setChildren(children.stream().filter(child -> !(child instanceof LabelNodeView))
                    .collect(Collectors.toList()));
        } else {
            setChildren(children);
        }
        redraw();
        takeFocus();

    }
}

package org.hyperledger.bela.components.bonsai;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.bela.components.BonsaiStorageView;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.trie.CompactEncoding;
import org.hyperledger.besu.ethereum.trie.Node;
import org.hyperledger.besu.ethereum.trie.TrieNodeDecoder;
import org.jetbrains.annotations.NotNull;

public class AccountTreeNodeView extends AbstractBonsaiNodeView {

    private final BonsaiStorageView bonsaiStorageView;
    private final Node<Bytes> parentNode;


    public AccountTreeNodeView(final BonsaiStorageView bonsaiStorageView, final Node<Bytes> node, final int depth) {
        super(label(node), depth);
        this.bonsaiStorageView = bonsaiStorageView;
        this.parentNode = node;
    }

    @NotNull
    private static String label(final Node<Bytes> node) {
        return "(" + node.getLocation().map(bytes -> bytes.toHexString()).orElse("") + ")" + node.getHash()
                .toHexString();
    }


    public void expand() {
        final List<Node<Bytes>> nodes =
                TrieNodeDecoder.decodeNodes(parentNode.getLocation().orElseThrow(), parentNode.getRlp());
        final List<BonsaiView> children = nodes.stream()
                .map(node -> {
                    if (bonsaiStorageView.nodeIsHashReferencedDescendant(parentNode, node)) {
                        return new AccountTreeNodeView(bonsaiStorageView, bonsaiStorageView.getAccountNodeValue(node.getHash(), node.getLocation()
                                .orElseThrow()), depth + 1);
                    } else if (node.getValue().isPresent()) {
                        final Hash accountHash =
                                Hash.wrap(
                                        Bytes32.wrap(
                                                CompactEncoding.pathToBytes(
                                                        Bytes.concatenate(
                                                                parentNode.getLocation()
                                                                        .orElseThrow(), node.getPath()))));
                        return new AccountValueView(bonsaiStorageView, accountHash, node.getValue().get(), depth + 1);

                    } else {
                        return new LabelNodeView("Missing value for " + label(node), depth + 1);
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

package org.hyperledger.bela.components.bonsai;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.rlp.RLP;
import org.hyperledger.bela.components.BonsaiStorageView;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.trie.Node;

public class StorageValueView extends AbstractBonsaiNodeView{

    private BonsaiStorageView bonsaiStorageView;
    private final Hash accountHash;
    private final Node<Bytes> node;

    public StorageValueView(final BonsaiStorageView bonsaiStorageView, final Hash accountHash, final Node<Bytes> node, final int depth) {
        super("V:" + label(node), depth);
        this.bonsaiStorageView = bonsaiStorageView;
        this.accountHash = accountHash;
        this.node = node;

    }

    private static String label(final Node<Bytes> node) {
        final Bytes value = Bytes32.leftPad(RLP.decodeValue(node.getValue()
                .orElseThrow()));
        return value.toHexString();
    }

    @Override
    public void expand() {
        final List<BonsaiView> children = new ArrayList<>();
        // check the storage in the flat database
        final Optional<Bytes> storageInFlatDB = bonsaiStorageView.getStorageInFlatDB(accountHash, node.getLocation()
                .orElseThrow(), node.getPath());

        if (storageInFlatDB.isPresent() ) {
            children.add(new LabelNodeView("F: " + storageInFlatDB.get().toHexString(), depth + 1));
        } else {
            children.add(new LabelNodeView("No data in Flat db", depth + 1));
        }

        setChildren(children);
    }
}

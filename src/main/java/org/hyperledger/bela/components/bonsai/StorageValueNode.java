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

    public StorageValueNode(final BonsaiStorageView bonsaiStorageView, final Hash accountHash, final Node<Bytes> node, final int depth) {
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
    public List<BonsaiNode> getChildren() {
        final List<BonsaiNode> children = new ArrayList<>();
        // check the storage in the flat database
        final Optional<Bytes> storageInFlatDB = bonsaiStorageView.getStorageInFlatDB(accountHash, node.getLocation()
                .orElseThrow(), node.getPath());

        if (storageInFlatDB.isPresent()) {
            children.add(new LabelNode("F: " + storageInFlatDB.get().toHexString(), depth + 1));
        } else {
            children.add(new LabelNode("No data in Flat db", depth + 1));
        }

        return children;
    }

    @Override
    public Component createComponent() {
        Panel panel = new Panel();
        panel.addComponent(new Label("Account: " + accountHash.toHexString()));
        panel.addComponent(new Label("Location: " + node.getLocation().map(Bytes::toHexString).orElse("")));
        panel.addComponent(new Label("Hash: " + node.getHash().toHexString()));
        return panel.withBorder(Borders.singleLine("Storage Value Node"));
    }
}

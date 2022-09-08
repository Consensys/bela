package org.hyperledger.bela.components.bonsai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.googlecode.lanterna.gui2.Panel;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.bonsai.BonsaiValue;

public class AddressStorageNodeView extends AbstractBonsaiNodeView{
    private final Map<Hash, BonsaiValue<UInt256>> tree;

    public AddressStorageNodeView(final Address address, Map<Hash, BonsaiValue<UInt256>> tree, final int depth) {
        super("Ad:" + address.toHexString(), depth);
        this.tree = tree;

    }

    @Override
    public void expand() {
        final List<BonsaiView> children = new ArrayList<>();
        tree.forEach((key, value) -> {
            final UInt256 prior = value.getPrior();
            final UInt256 updated = value.getUpdated();
            children.add(new LabelNodeView(key.toHexString() + ":" + prior.toHexString() + " -> " + updated.toHexString(), depth + 1));
        });
        setChildren(children);
        redraw();
        takeFocus();
    }
}

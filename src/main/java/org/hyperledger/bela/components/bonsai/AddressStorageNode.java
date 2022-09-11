package org.hyperledger.bela.components.bonsai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Panel;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.bonsai.BonsaiValue;

public class AddressStorageNode extends AbstractBonsaiNode {
    private final Map<Hash, BonsaiValue<UInt256>> tree;
    private final Address address;

    public AddressStorageNode(final Address address, Map<Hash, BonsaiValue<UInt256>> tree, final int depth) {
        super("Storage", depth);
        this.address = address;
        this.tree = tree;

    }

    @Override
    public List<BonsaiNode> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public Component createComponent() {
        Panel panel = new Panel();
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Address:", address.toHexString()).createComponent());
        tree.forEach((key, value) -> {
            final UInt256 prior = value.getPrior();
            final UInt256 updated = value.getUpdated();
            panel.addComponent(LabelWithTextBox.labelWithTextBox("Key:", key.toHexString()).createComponent());
            panel.addComponent(LabelWithTextBox.labelWithTextBox("Prior:", prior.toString()).createComponent());
            panel.addComponent(LabelWithTextBox.labelWithTextBox("Updated:", updated.toString()).createComponent());
        });
        return panel.withBorder(Borders.singleLine("Address Storage Node"));
    }
}

package org.hyperledger.bela.components.bonsai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Panel;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.datatypes.StorageSlotKey;
import org.hyperledger.besu.ethereum.bonsai.BonsaiValue;

public class AddressStorageNode extends AbstractBonsaiNode {
  private final Map<StorageSlotKey, BonsaiValue<UInt256>> tree;
  private final Address address;

  public AddressStorageNode(final Address address, Map<StorageSlotKey, BonsaiValue<UInt256>> tree) {
    super(address.toHexString());
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
    panel.addComponent(
        LabelWithTextBox.labelWithTextBox("Address:", address.toHexString()).createComponent());
    tree.forEach((key, value) -> {
      final UInt256 prior = value.getPrior();
      final UInt256 updated = value.getUpdated();
      if (!Objects.equals(prior, updated)) {
        panel.addComponent(LabelWithTextBox
            .labelWithTextBox("Key:", key.getSlotKey().map(UInt256::toHexString).orElse(""))
            .createComponent());
        panel.addComponent(LabelWithTextBox
            .labelWithTextBox("Key Hash:", key.getSlotHash().toHexString())
            .createComponent());
        panel.addComponent(
            LabelWithTextBox.labelWithTextBox("Prior:", String.valueOf(prior)).createComponent());
        panel.addComponent(LabelWithTextBox
            .labelWithTextBox("Updated:", String.valueOf(updated))
            .createComponent());
      }
    });
    return panel.withBorder(Borders.singleLine("Address Storage Node"));
  }

  @Override
  public void log() {
    log.info("Address Storage Node");
    log.info("Address: {}", address.toHexString());
    tree.forEach((key, value) -> {
      final UInt256 prior = value.getPrior();
      final UInt256 updated = value.getUpdated();
      log.info("Slot Key: {}", key.getSlotKey().map(UInt256::toHexString).orElse(""));
      log.info("Slto Key Hash: {}", key.getSlotHash().toHexString());
      log.info("Prior: {}", prior.toString());
      log.info("Updated: {}", updated.toString());
    });
  }
}

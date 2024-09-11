package org.hyperledger.bela.components.bonsai;

import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Panel;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.AccountValue;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.datatypes.StorageSlotKey;
import org.hyperledger.besu.ethereum.trie.diffbased.common.DiffBasedValue;
import org.hyperledger.besu.ethereum.trie.diffbased.common.trielog.TrieLogLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BonsaiTrieLogNode extends AbstractBonsaiNode {

  private final TrieLogLayer layer;
  private final Hash blockHash;

  public BonsaiTrieLogNode(final Hash blockHash, final TrieLogLayer layer) {
    super(blockHash.toHexString());
    this.blockHash = blockHash;
    this.layer = layer;
  }

  @Override
  public List<BonsaiNode> getChildren() {
    final List<BonsaiNode> children = new ArrayList<>();

    final List<AccountChangeNode> accounts =
        layer.getAccountChanges().entrySet().stream().map(account -> {
          final Address address = account.getKey();
          final AccountValue prior = account.getValue().getPrior();
          final AccountValue updated = account.getValue().getUpdated();
          return new AccountChangeNode(address, prior, updated);
        }).toList();
    if (!accounts.isEmpty()) {
      children.add(new BonsaiListNode("Account changes", accounts));
    } else {
      children.add(new LabelNode("No accounts changes", blockHash.toHexString()));
    }
    final List<CodeChangeNode> codeChanges =
        layer.getCodeChanges().entrySet().stream().map(codeChange -> {
          final Address address = codeChange.getKey();
          final Bytes prior = codeChange.getValue().getPrior();
          final Bytes updated = codeChange.getValue().getUpdated();
          return new CodeChangeNode(address, prior, updated);
        }).toList();
    if (!codeChanges.isEmpty()) {
      children.add(new BonsaiListNode("Code Changes", codeChanges));
    } else {
      children.add(new LabelNode("No code changes", blockHash.toHexString()));
    }
    final List<AddressStorageNode> storageChanges =
        layer.getStorageChanges().entrySet().stream().map(storageChange -> {
          final Address address = storageChange.getKey();
          final Map<StorageSlotKey, DiffBasedValue<UInt256>> tree = storageChange.getValue();
          return new AddressStorageNode(address, tree);
        }).toList();
    if (!storageChanges.isEmpty()) {
      children.add(new BonsaiListNode("Storage Changes", storageChanges));
    } else {
      children.add(new LabelNode("No storage changes", blockHash.toHexString()));
    }
    return children;
  }

  public TrieLogLayer getLayer() {
    return layer;
  }

  @Override
  public Component createComponent() {
    Panel panel = new Panel();
    panel.addComponent(
        LabelWithTextBox.labelWithTextBox("Block Hash", blockHash.toHexString()).createComponent());
    return panel.withBorder(Borders.singleLine("Bonsai Trie Log Node"));
  }

  @Override
  public void log() {
    log.info("Bonsai Trie Log Node: {}", blockHash.toHexString());
  }
}

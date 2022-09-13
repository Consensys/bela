package org.hyperledger.bela.components.bonsai;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.bonsai.BonsaiValue;
import org.hyperledger.besu.ethereum.bonsai.TrieLogLayer;
import org.hyperledger.besu.ethereum.worldstate.StateTrieAccountValue;

public class BonsaiTrieLogNode extends AbstractBonsaiNode {

    private final TrieLogLayer layer;
    private final Hash blockHash;

    public BonsaiTrieLogNode(final Hash blockHash, final TrieLogLayer layer, final int depth) {
        super(blockHash.toHexString(), depth);
        this.blockHash = blockHash;
        this.layer = layer;
    }

    @Override
    public List<BonsaiNode> getChildren() {
        final List<BonsaiNode> children = new ArrayList<>();

        final List<AccountChangeNode> accounts = streamAccounts().map(account -> {
            final Address address = account.getKey();
            final StateTrieAccountValue prior = account.getValue().getPrior();
            final StateTrieAccountValue updated = account.getValue().getUpdated();
            return new AccountChangeNode(address, prior, updated, depth + 2);
        }).collect(Collectors.toList());
        if (!accounts.isEmpty()) {
            children.add(new BonsaiListNode("Account changes", accounts, depth + 1));
        } else {
            children.add(new LabelNode("No accounts changes", blockHash.toHexString(), depth + 1));
        }
        final List<LabelNode> codeChanges = streamCodeChanges().map(codeChange -> {
            final Address address = codeChange.getKey();
            final Bytes prior = codeChange.getValue().getPrior();
            final Bytes updated = codeChange.getValue().getUpdated();
            return new LabelNode(address.toHexString(), prior.toHexString() + " -> " + updated.toHexString(), depth + 2);
        }).collect(Collectors.toList());
        if (!codeChanges.isEmpty()) {
            children.add(new BonsaiListNode("Code Changes", codeChanges, depth + 1));
        } else {
            children.add(new LabelNode("No code changes", blockHash.toHexString(), depth + 1));
        }
        final List<AddressStorageNode> storageChanges = streamStorageChanges().map(storageChange -> {
            final Address address = storageChange.getKey();
            final Map<Hash, BonsaiValue<UInt256>> tree = storageChange.getValue();
            return new AddressStorageNode(address, tree, depth + 2);
        }).collect(Collectors.toList());
        if (!storageChanges.isEmpty()) {
            children.add(new BonsaiListNode("Storage Changes", storageChanges, depth + 1));
        } else {
            children.add(new LabelNode("No storage changes", blockHash.toHexString(), depth + 1));
        }
        return children;
    }

    private Stream<Map.Entry<Address, BonsaiValue<StateTrieAccountValue>>> streamAccounts() {
        try {
            final Method streamAccounts = layer.getClass().getDeclaredMethod("streamAccountChanges");
            streamAccounts.setAccessible(true);
            return (Stream<Map.Entry<Address, BonsaiValue<StateTrieAccountValue>>>) streamAccounts.invoke(layer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //        return layer.streamAccountChanges();
    }

    private Stream<Map.Entry<Address, BonsaiValue<Bytes>>> streamCodeChanges() {
        try {
            final Method streamAccounts = layer.getClass().getDeclaredMethod("streamCodeChanges");
            streamAccounts.setAccessible(true);
            return (Stream<Map.Entry<Address, BonsaiValue<Bytes>>>) streamAccounts.invoke(layer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //        return layer.streamCodeChanges();
    }

    private Stream<Map.Entry<Address, Map<Hash, BonsaiValue<UInt256>>>> streamStorageChanges() {
        try {
            final Method streamAccounts = layer.getClass().getDeclaredMethod("streamStorageChanges");
            streamAccounts.setAccessible(true);
            return (Stream<Map.Entry<Address, Map<Hash, BonsaiValue<UInt256>>>>) streamAccounts.invoke(layer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //        return layer.streamStorageChanges();
    }

    public TrieLogLayer getLayer() {
        return layer;
    }

    @Override
    public Component createComponent() {
        Panel panel = new Panel();
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Block Hash", blockHash.toHexString()).createComponent());
        return panel.withBorder(Borders.singleLine("Bonsai Trie Log Node"));
    }

    @Override
    public void log() {
        log.info("Bonsai Trie Log Node: {}", blockHash.toHexString());
    }
}

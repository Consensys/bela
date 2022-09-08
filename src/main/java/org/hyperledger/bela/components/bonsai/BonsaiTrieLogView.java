package org.hyperledger.bela.components.bonsai;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.bonsai.BonsaiValue;
import org.hyperledger.besu.ethereum.bonsai.TrieLogLayer;
import org.hyperledger.besu.ethereum.worldstate.StateTrieAccountValue;

public class BonsaiTrieLogView extends AbstractBonsaiNodeView {

    private final TrieLogLayer layer;

    public BonsaiTrieLogView(final Hash blockHash, final TrieLogLayer layer, final int depth) {
        super("B:" + blockHash.toHexString(), depth);
        this.layer = layer;
    }

    @Override
    public void expand() {
        final List<BonsaiView> children = new ArrayList<>();

        final List<AccountChangeView> accounts = streamAccounts().map(account -> {
            final Address address = account.getKey();
            final StateTrieAccountValue prior = account.getValue().getPrior();
            final StateTrieAccountValue updated = account.getValue().getUpdated();
            return new AccountChangeView(address, prior, updated, depth + 2);
        }).collect(Collectors.toList());
        if (!accounts.isEmpty()) {
            children.add(new BonsaiListView("Account changes", accounts, depth + 1));
        } else {
            children.add(new LabelNodeView("No accounts changes", depth + 1));
        }
        final List<LabelNodeView> codeChanges = streamCodeChanges().map(codeChange -> {
            final Address address = codeChange.getKey();
            final Bytes prior = codeChange.getValue().getPrior();
            final Bytes updated = codeChange.getValue().getUpdated();
            return new LabelNodeView(address.toHexString() + ":" + prior.toHexString() + " -> " + updated.toHexString(), depth + 2);
        }).collect(Collectors.toList());
        if (!codeChanges.isEmpty()) {
            children.add(new BonsaiListView("Code Changes", codeChanges, depth + 1));
        } else {
            children.add(new LabelNodeView("No code changes", depth + 1));
        }
        final List<AddressStorageNodeView> storageChanges = streamStorageChanges().map(storageChange -> {
            final Address address = storageChange.getKey();
            final Map<Hash, BonsaiValue<UInt256>> tree = storageChange.getValue();
            return new AddressStorageNodeView(address, tree, depth + 2);
        }).collect(Collectors.toList());
        if (!storageChanges.isEmpty()) {
            children.add(new BonsaiListView("Storage Changes", storageChanges, depth + 1));
        } else {
            children.add(new LabelNodeView("No storage changes", depth + 1));
        }
        setChildren(children);
        redraw();
        takeFocus();
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
}

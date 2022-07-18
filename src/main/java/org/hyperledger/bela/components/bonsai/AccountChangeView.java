package org.hyperledger.bela.components.bonsai;

import java.util.ArrayList;
import java.util.List;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.worldstate.StateTrieAccountValue;

public class AccountChangeView extends AbstractBonsaiNodeView {

    private final StateTrieAccountValue prior;
    private final StateTrieAccountValue updated;

    public AccountChangeView(final Address address, final StateTrieAccountValue prior, final StateTrieAccountValue updated, final int depth) {
        super("A:" + address.toHexString(), depth);
        this.prior = prior;
        this.updated = updated;
    }

    @Override
    public void expand() {
        final List<BonsaiView> children = new ArrayList<>();
        children.add(new LabelNodeView("Nonce: " + prior.getNonce() + " -> " + updated.getNonce(), depth + 1));
        children.add(new LabelNodeView("Balance: " + prior.getBalance() + " -> " + updated.getBalance(), depth + 1));
        children.add(new LabelNodeView("Code: " + prior.getCodeHash() + " -> " + updated.getCodeHash(), depth + 1));
        children.add(new LabelNodeView("Storage: " + prior.getStorageRoot() + " -> " + updated.getStorageRoot(), depth + 1));
        setChildren(children);
        redraw();
        takeFocus();
    }
}

package org.hyperledger.bela.components.bonsai;

import java.util.ArrayList;
import java.util.List;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.worldstate.StateTrieAccountValue;

public class AccountChangeNode extends AbstractBonsaiNode {

    private final StateTrieAccountValue prior;
    private final StateTrieAccountValue updated;
    private final Address address;

    public AccountChangeNode(final Address address, final StateTrieAccountValue prior, final StateTrieAccountValue updated, final int depth) {
        super("A:" + address.toHexString(), depth);
        this.address = address;
        this.prior = prior;
        this.updated = updated;
    }

    @Override
    public List<BonsaiNode> getChildren() {
        final List<BonsaiNode> children = new ArrayList<>();
        children.add(new LabelNode("Nonce: " + prior.getNonce() + " -> " + updated.getNonce(), depth + 1));
        children.add(new LabelNode("Balance: " + prior.getBalance() + " -> " + updated.getBalance(), depth + 1));
        children.add(new LabelNode("Code: " + prior.getCodeHash() + " -> " + updated.getCodeHash(), depth + 1));
        children.add(new LabelNode("Storage: " + prior.getStorageRoot() + " -> " + updated.getStorageRoot(), depth + 1));
        return children;
    }

    @Override
    public Component createComponent() {
        Panel panel = new Panel();
        panel.addComponent(new Label("Address: " + address.toHexString()));
        return panel.withBorder(Borders.singleLine("Account"));
    }
}

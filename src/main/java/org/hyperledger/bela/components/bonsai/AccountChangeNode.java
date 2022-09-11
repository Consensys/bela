package org.hyperledger.bela.components.bonsai;

import java.util.ArrayList;
import java.util.List;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Panel;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.worldstate.StateTrieAccountValue;

public class AccountChangeNode extends AbstractBonsaiNode {

    private final StateTrieAccountValue prior;
    private final StateTrieAccountValue updated;
    private final Address address;

    public AccountChangeNode(final Address address, final StateTrieAccountValue prior, final StateTrieAccountValue updated, final int depth) {
        super("Account Change", depth);
        this.address = address;
        this.prior = prior;
        this.updated = updated;
    }

    @Override
    public Component createComponent() {
        Panel panel = new Panel();
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Address:", address.toHexString()).createComponent());
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Nonce:", prior.getNonce() + " -> " + updated.getNonce())
                .createComponent());
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Balance:", prior.getBalance() + " -> " + updated.getBalance())
                .createComponent());
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Code:", prior.getCodeHash() + " -> " + updated.getCodeHash())
                .createComponent());
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Storage:", prior.getStorageRoot() + " -> " + updated.getStorageRoot())
                .createComponent());
        return panel.withBorder(Borders.singleLine("Account Change"));
    }

    @Override
    public List<BonsaiNode> getChildren() {
        return new ArrayList<>();
    }
}

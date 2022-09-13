package org.hyperledger.bela.components.bonsai;

import java.util.ArrayList;
import java.util.List;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.worldstate.StateTrieAccountValue;

public class AccountChangeNode extends AbstractBonsaiNode {
    private final StateTrieAccountValue prior;
    private final StateTrieAccountValue updated;
    private final Address address;

    public AccountChangeNode(final Address address, final StateTrieAccountValue prior, final StateTrieAccountValue updated) {
        super(address.toHexString());
        this.address = address;
        this.prior = prior;
        this.updated = updated;
    }

    @Override
    public Component createComponent() {
        Panel panel = new Panel();
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Address:", address.toHexString()).createComponent());
        if (prior.getNonce() != updated.getNonce()) {
            panel.addComponent(new Label("Nonce:"));
            panel.addComponent(LabelWithTextBox.labelWithTextBox("Prior:", String.valueOf(prior.getNonce()))
                    .createComponent());
            panel.addComponent(LabelWithTextBox.labelWithTextBox("Updated:", String.valueOf(updated.getNonce()))
                    .createComponent());
            panel.addComponent(new EmptySpace());
        }
        if (!prior.getBalance().equals(updated.getBalance())) {
            panel.addComponent(new Label("Balance:"));
            panel.addComponent(LabelWithTextBox.labelWithTextBox("Prior:", String.valueOf(prior.getBalance()))
                    .createComponent());
            panel.addComponent(LabelWithTextBox.labelWithTextBox("Updated:", String.valueOf(updated.getBalance()))
                    .createComponent());
            panel.addComponent(new EmptySpace());
        }
        if (!prior.getCodeHash().equals(updated.getCodeHash())) {
            panel.addComponent(new Label("Code Hash:"));
            panel.addComponent(LabelWithTextBox.labelWithTextBox("Prior:", String.valueOf(prior.getCodeHash()))
                    .createComponent());
            panel.addComponent(LabelWithTextBox.labelWithTextBox("Updated:", String.valueOf(updated.getCodeHash()))
                    .createComponent());
            panel.addComponent(new EmptySpace());
        }
        if (!prior.getStorageRoot().equals(updated.getStorageRoot())) {
            panel.addComponent(new Label("Storage Root:"));
            panel.addComponent(LabelWithTextBox.labelWithTextBox("Prior:", String.valueOf(prior.getStorageRoot()))
                    .createComponent());
            panel.addComponent(LabelWithTextBox.labelWithTextBox("Updated:", String.valueOf(updated.getStorageRoot()))
                    .createComponent());
        }
        return panel.withBorder(Borders.singleLine("Account Change"));
    }

    @Override
    public void log() {
        log.info("Account Change");
        log.info("Address: {}", address.toHexString());
        log.info("Nonce: {} -> {}", prior!=null?prior.getNonce():"null", updated.getNonce());
        log.info("Balance: {} -> {}", prior!=null?prior.getBalance():"null", updated.getBalance());
        log.info("Code: {} -> {}", prior!=null?prior.getCodeHash().toHexString():"null", updated.getCodeHash());
        log.info("Storage: {} -> {}", prior!=null?prior.getStorageRoot().toHexString():"null", updated.getStorageRoot());
    }

    @Override
    public List<BonsaiNode> getChildren() {
        return new ArrayList<>();
    }
}

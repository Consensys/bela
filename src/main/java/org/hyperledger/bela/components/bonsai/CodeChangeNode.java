package org.hyperledger.bela.components.bonsai;

import java.util.ArrayList;
import java.util.List;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Panel;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.datatypes.Address;

public class CodeChangeNode extends AbstractBonsaiNode {
    @org.jetbrains.annotations.NotNull
    private final Address address;
    private final Bytes prior;
    private final Bytes updated;

    public CodeChangeNode(final Address address, final Bytes prior, final Bytes updated) {
        super(address.toHexString());
        this.address = address;
        this.prior = prior;
        this.updated = updated;
    }

    @Override
    public Component createComponent() {
        Panel panel = new Panel();
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Address:", address.toHexString()).createComponent());
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Prior:", String.valueOf(prior)).createComponent());
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Updated:", String.valueOf(updated)).createComponent());
        return panel.withBorder(Borders.singleLine("Code Change"));
    }

    @Override
    public List<BonsaiNode> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public void log() {
        log.info("Code Change");
        log.info("Address: {}", address.toHexString());
        log.info("Prior: {}", prior.toHexString());
        log.info("Updated: {}", updated.toHexString());


    }
}

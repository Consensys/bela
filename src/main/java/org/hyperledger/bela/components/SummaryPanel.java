package org.hyperledger.bela.components;

import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;

public class SummaryPanel implements LanternaComponent<Panel> {
    private final Label worldStateRoot = new Label("empty");
    private final Label chainHead = new Label("empty");
    private final Label chainHeadHash = new Label("empty");

    public SummaryPanel(final String worldStateRoot, final String chainHead, final String chainHeadhHash) {
        updateWith(worldStateRoot, chainHead, chainHeadhHash);
    }

    public void updateWith(final String worldStateRoot, final String chainHead, final String chainHeadHash) {
        this.worldStateRoot.setText(worldStateRoot);
        this.chainHead.setText(chainHead);
        this.chainHeadHash.setText(chainHeadHash);
    }

    @Override
    public Panel createComponent() {
        Panel panel = new Panel();
        panel.setLayoutManager(new GridLayout(2));

        panel.addComponent(new Label("db world state root:"));
        panel.addComponent(worldStateRoot);

        panel.addComponent(new Label("chain head:"));
        panel.addComponent(chainHead);

        panel.addComponent(new Label("chain head hash:"));
        panel.addComponent(chainHeadHash);


        return panel;
    }
}

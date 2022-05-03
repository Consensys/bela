package org.hyperledger.bela.components;

import org.hyperledger.besu.ethereum.chain.ChainHead;

import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;

public class SummaryPanel implements LanternaComponent<Panel>{
  private final String worldStateRoot;
  private final ChainHead chainHead;

  public SummaryPanel(final String worldStateRoot, final ChainHead chainHead) {
    this.worldStateRoot = worldStateRoot;
    this.chainHead = chainHead;
  }

  @Override
  public Panel createComponent() {
    Panel panel = new Panel();
    panel.setLayoutManager(new GridLayout(2));

    panel.addComponent(new Label("db world state root:"));
    panel.addComponent(new Label(worldStateRoot));

    panel.addComponent(new Label("chain head:"));
    panel.addComponent(new Label(String.valueOf(chainHead.getHeight())));

    panel.addComponent(new Label("chain head hash:"));
    panel.addComponent(new Label(chainHead.getHash().toHexString()));


    return panel;
  }
}

package org.hyperledger.bela.components;

import org.hyperledger.besu.ethereum.chain.ChainHead;

import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;

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

    panel.addComponent(new Label("db world state root"));
    panel.addComponent(new TextBox(worldStateRoot));
    panel.addComponent(new EmptySpace());

    panel.addComponent(new Label("chain head"));
    panel.addComponent(new TextBox(""+chainHead.getHeight()));
    panel.addComponent(new EmptySpace());

    return panel;
  }
}

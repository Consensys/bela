package org.hyperledger.bela.components;

import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import org.hyperledger.besu.ethereum.core.Block;

import java.util.Optional;

public class SummaryPanel implements LanternaComponent<Panel>{
  private final String worldStateRoot;
  private final Optional<Block> block;

  public SummaryPanel(final String worldStateRoot, final Optional<Block> block) {
    this.worldStateRoot = worldStateRoot;
    this.block = block;
  }

  @Override
  public Panel createComponent() {
    Panel panel = new Panel();
    panel.setLayoutManager(new GridLayout(2));

    panel.addComponent(new Label("db world state root:"));
    panel.addComponent(new Label(worldStateRoot));

    panel.addComponent(new Label("chain head:"));
    panel.addComponent(new Label(String.valueOf(block.get().getHeader().getNumber())));

    panel.addComponent(new Label("chain head hash:"));
    panel.addComponent(new Label(block.get().getHash().toHexString()));


    return panel;
  }
}

/*
 *
 *  * Copyright Hyperledger Besu Contributors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  * the License. You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  * specific language governing permissions and limitations under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.hyperledger.bela.components;

import java.time.Instant;

import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.ScrollBar;
import org.hyperledger.bela.model.BlockResult;

public class BlockPanel implements LanternaComponent<Panel> {
  private final BlockResult block;

  public BlockPanel(final BlockResult block) {
    this.block = block;
  }

  @Override
  public Panel createComponent() {
    Panel panel = new Panel();
    panel.setLayoutManager(new GridLayout(2));

    panel.addComponent(new Label("block number:"));
    panel.addComponent(new Label(String.valueOf(block.getNumber())));

    panel.addComponent(new Label("block time:"));
    panel.addComponent(new Label(Instant.ofEpochSecond(block.getTimestamp()).toString()));

    panel.addComponent(new Label("block hash:"));
    panel.addComponent(new Label(block.getHash()));

    panel.addComponent(new Label("parent hash:"));
    panel.addComponent(new Label(block.getParentHash()));

    panel.addComponent(new Label("difficulty:"));
    panel.addComponent(new Label(block.getDifficulty()));

    panel.addComponent(new Label("total difficulty:"));
    panel.addComponent(new Label(block.getTotalDifficulty()));

    panel.addComponent(new Label("state root:"));
    panel.addComponent(new Label(block.getStateRoot()));

    panel.addComponent(new Label("miner coinbase:"));
    panel.addComponent(new Label(block.getMiner()));

    panel.addComponent(new Label("gas limit:"));
    panel.addComponent(new Label(String.valueOf(block.getGasLimit())));
    panel.addComponent(new Label("gas used:"));
    panel.addComponent(new Label(String.valueOf(block.getGasUsed())));
    panel.addComponent(new Label("base fee:"));
    panel.addComponent(new Label(String.valueOf(block.getBaseFeePerGas())));

    panel.addComponent(new Label("transaction count:"));
    panel.addComponent(new Label(String.valueOf(block.getTransactions().size())));
    panel.addComponent(new Label("transactions root:"));
    panel.addComponent(new Label(block.getTransactionsRoot()));
    panel.addComponent(new Label("receipts root:"));
    panel.addComponent(new Label(block.getReceiptsRoot()));

    Panel outerPanel = new Panel();
    outerPanel.setLayoutManager(new BorderLayout());
    outerPanel.addComponent(panel, BorderLayout.Location.LEFT);
//    outerPanel.addComponent(new ScrollBar(Direction.VERTICAL), BorderLayout.Location.RIGHT);

    return outerPanel;
  }
}

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
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import org.hyperledger.bela.model.BlockResult;

public class BlockPanel implements LanternaComponent<Panel> {
    private final Label number = new Label("empty");
    private final Label time = new Label("empty");
    private final Label hash = new Label("empty");
    private final Label parentHash = new Label("empty");
    private final Label difficulty = new Label("empty");
    private final Label totalDifficulty = new Label("empty");
    private final Label stateRoot = new Label("empty");
    private final Label miner = new Label("empty");
    private final Label gasLimit = new Label("empty");
    private final Label gasUsed = new Label("empty");
    private final Label baseFee = new Label("empty");
    private final Label transactionCount = new Label("empty");
    private final Label transactionRoot = new Label("empty");
    private final Label receiptsRoot = new Label("empty");

    public BlockPanel(final BlockResult block) {
        updateWithBlock(block);
    }

    public void updateWithBlock(final BlockResult block) {
        number.setText(String.valueOf(block.getNumber()));
        time.setText(Instant.ofEpochSecond(block.getTimestamp()).toString());
        hash.setText(block.getHash());
        parentHash.setText(block.getParentHash());
        difficulty.setText(block.getDifficulty());
        totalDifficulty.setText(block.getTotalDifficulty());
        stateRoot.setText(block.getStateRoot());
        miner.setText(block.getMiner());
        gasLimit.setText(String.valueOf(block.getGasLimit()));
        gasUsed.setText(String.valueOf(block.getGasUsed()));
        baseFee.setText(String.valueOf(block.getBaseFeePerGas()));
        transactionCount.setText(String.valueOf(block.getTransactions().size()));
        transactionRoot.setText(block.getTransactionsRoot());
        receiptsRoot.setText(block.getReceiptsRoot());
    }

    @Override
    public Panel createComponent() {
        Panel panel = new Panel();
        panel.setLayoutManager(new GridLayout(2));

        panel.addComponent(new Label("block number:"));
        panel.addComponent(number);

        panel.addComponent(new Label("block time:"));
        panel.addComponent(time);

        panel.addComponent(new Label("block hash:"));
        panel.addComponent(hash);

        panel.addComponent(new Label("parent hash:"));
        panel.addComponent(parentHash);

        panel.addComponent(new Label("difficulty:"));
        panel.addComponent(difficulty);

        panel.addComponent(new Label("total difficulty:"));
        panel.addComponent(totalDifficulty);

        panel.addComponent(new Label("state root:"));
        panel.addComponent(stateRoot);

        panel.addComponent(new Label("miner coinbase:"));
        panel.addComponent(miner);

        panel.addComponent(new Label("gas limit:"));
        panel.addComponent(gasLimit);
        panel.addComponent(new Label("gas used:"));
        panel.addComponent(gasUsed);
        panel.addComponent(new Label("base fee:"));
        panel.addComponent(baseFee);

        panel.addComponent(new Label("transaction count:"));
        panel.addComponent(transactionCount);
        panel.addComponent(new Label("transactions root:"));
        panel.addComponent(transactionRoot);
        panel.addComponent(new Label("receipts root:"));
        panel.addComponent(receiptsRoot);

        Panel outerPanel = new Panel();
        outerPanel.setLayoutManager(new BorderLayout());
        outerPanel.addComponent(panel, BorderLayout.Location.LEFT);
//    outerPanel.addComponent(new ScrollBar(Direction.VERTICAL), BorderLayout.Location.RIGHT);

        return outerPanel;
    }
}

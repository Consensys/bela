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
 *  *   SPDX-License-Identifier: Apache-2.0
 *
 */

package org.hyperledger.bela.components;

import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import org.hyperledger.bela.BlockUtils;
import org.hyperledger.bela.model.BlockResult;

public class BlockPanelComponent implements LanternaComponent<Panel> {
  private final BlockResult blockResult;

  public BlockPanelComponent(final BlockResult blockResult) {

    this.blockResult = blockResult;
  }

  @Override
  public Panel createComponent() {
    Panel panel = new Panel();
    panel.setLayoutManager(new GridLayout(2));

    panel.addComponent(new Label("Block " + blockResult.getNumber()));
    String blockText = BlockUtils.prettyPrintBlockHeader(blockResult);
    panel.addComponent(new Label(blockText));
    return panel;
  }
}

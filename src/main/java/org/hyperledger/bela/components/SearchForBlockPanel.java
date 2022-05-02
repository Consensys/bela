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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;

public class SearchForBlockPanel implements LanternaComponent<Panel> {
  long blockNumber;
  private final List<BlockNumberSearchSubscriber> subscribers = new ArrayList<>();

  @Override
  public Panel createComponent() {
    Panel panel = new Panel();
    panel.setLayoutManager(new GridLayout(2));

    final Label blkLbl = new Label("");

    panel.addComponent(new Label("Block Number"));
    final TextBox blockNmr =
        new TextBox().setValidationPattern(Pattern.compile("[0-9]*")).addTo(panel);

    final Button button = new Button("Search!", () -> change(Long.parseLong(blockNmr.getText())));
    button.addTo(panel);

    panel.addComponent(new EmptySpace(new TerminalSize(0, 0)));
    panel.addComponent(blkLbl);
    return panel;
  }

  private void change(final long newVal) {
    this.blockNumber = newVal;
    subscribers.forEach(s -> s.newBlockNumber(newVal));
  }

  public void onChange(final BlockNumberSearchSubscriber subscriber) {
    this.subscribers.add(subscriber);
  }
}

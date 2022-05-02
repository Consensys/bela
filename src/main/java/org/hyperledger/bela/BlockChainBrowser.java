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

package org.hyperledger.bela;

import org.hyperledger.besu.ethereum.bonsai.BonsaiWorldStateKeyValueStorage;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.chain.DefaultBlockchain;
import org.hyperledger.besu.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;

import java.util.Optional;

import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.bela.components.BlockPanelComponent;
import org.hyperledger.bela.components.LanternaComponent;
import org.hyperledger.bela.components.MessagePanel;
import org.hyperledger.bela.components.SummaryPanel;

public class BlockChainBrowser {

  private final BonsaiWorldStateKeyValueStorage worldStateStorage;
  private final Blockchain blockchain;
//  private final BonsaiWorldStateArchive worldStateArchive;


  public BlockChainBrowser(
      final Blockchain blockchain,
//      final BonsaiWorldStateArchive worldStateArchive,
      final BonsaiWorldStateKeyValueStorage worldStateStorage) {
    this.blockchain = blockchain;
//    this.worldStateArchive = worldStateArchive;
    this.worldStateStorage = worldStateStorage;
  }

  public static BlockChainBrowser fromProvider(final StorageProvider provider) {
    var blockchainStorage = new KeyValueStoragePrefixedKeyBlockchainStorage(
        provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.BLOCKCHAIN),
        new MainnetBlockHeaderFunctions());

    var blockchain = DefaultBlockchain
        .create(blockchainStorage,new NoOpMetricsSystem(), 0L);

    var worldStateStorage = new BonsaiWorldStateKeyValueStorage(provider);
//    var worldStateArchive = new BonsaiWorldStateArchive(
//        provider, blockchain);
    return new BlockChainBrowser(blockchain/*, worldStateArchive*/, worldStateStorage);
  }

  public LanternaComponent<? extends Component> findBlockPanel(final long blockNumber) {
    return getBlockHeaderByNumber(blockNumber)
        .<LanternaComponent<? extends Component>>map(BlockPanelComponent::new)
        .orElseGet(() -> new MessagePanel("Not found block " + blockNumber));
  }

  public LanternaComponent<? extends Component> showSummaryPanel() {
    return new SummaryPanel(
        worldStateStorage.getWorldStateRootHash().map(Bytes::toHexString).orElse(null),
        blockchain.getChainHead());
  }

  public void showFindBlockDialog(final WindowBasedTextGUI parentWindow, final long blockNumber) {
    MessageDialog.showMessageDialog(parentWindow,
        "Search Result",
        getBlockHeaderByNumber(blockNumber)
            .map(BlockUtils::prettyPrintBlockHeader)
            .orElse("Not found block " + blockNumber),
        MessageDialogButton.OK);
  }
  private Optional<BlockUtils.BlockSearchResult> getBlockHeaderByNumber(final long blockNumber) {
    return blockchain.getBlockHeader(blockNumber)
        .map(BlockUtils.BlockSearchResult::new);
  }
}

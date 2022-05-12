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

import static org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier.BLOCKCHAIN;

import org.hyperledger.bela.ConsensusDetector.CONSENSUS_TYPE;
import org.hyperledger.besu.consensus.common.bft.BftBlockHeaderFunctions;
import org.hyperledger.besu.consensus.qbft.QbftExtraDataCodec;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.bonsai.BonsaiWorldStateKeyValueStorage;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.chain.DefaultBlockchain;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.core.BlockHeaderFunctions;
import org.hyperledger.besu.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;

import java.util.Optional;

import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Panel;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.bela.components.BlockPanel;
import org.hyperledger.bela.components.LanternaComponent;
import org.hyperledger.bela.components.MessagePanel;
import org.hyperledger.bela.components.SummaryPanel;
import org.hyperledger.bela.model.BlockResult;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;

public class BlockChainBrowser {

  private final BonsaiWorldStateKeyValueStorage worldStateStorage;
  private final Blockchain blockchain;
  private Optional<BlockResult> blockResult;


  public BlockChainBrowser(
      final Blockchain blockchain,
      final BonsaiWorldStateKeyValueStorage worldStateStorage) {
    this.blockchain = blockchain;
    this.worldStateStorage = worldStateStorage;
    //fixme
    this.blockResult = getChainHead();
  }

  public static BlockChainBrowser fromProvider(final StorageProvider provider) {
    final KeyValueStorage keyValueStorage = provider.getStorageBySegmentIdentifier(BLOCKCHAIN);
    final CONSENSUS_TYPE consensusType = ConsensusDetector.detectConsensusMechanism(
        keyValueStorage);
    final BlockHeaderFunctions blockHeaderFunction = switch (consensusType) {
      case IBFT2 -> BftBlockHeaderFunctions.forOnchainBlock(new QbftExtraDataCodec());
      case QBFT -> BftBlockHeaderFunctions.forOnchainBlock(new QbftExtraDataCodec());
      default -> new MainnetBlockHeaderFunctions();
    };

    var blockchainStorage = new KeyValueStoragePrefixedKeyBlockchainStorage(keyValueStorage,
        blockHeaderFunction);

    var blockchain = DefaultBlockchain
        .create(blockchainStorage,new NoOpMetricsSystem(), 0L);

    var worldStateStorage = new BonsaiWorldStateKeyValueStorage(provider);
    return new BlockChainBrowser(blockchain/*, worldStateArchive*/, worldStateStorage);
  }


  public LanternaComponent<Panel> blockPanel() {
    return (blockResult)
        .<LanternaComponent<Panel>>map(BlockPanel::new)
        .orElseGet(() -> new MessagePanel("block not found"));
  }

  public LanternaComponent<? extends Component> showSummaryPanel() {
    return new SummaryPanel(
        worldStateStorage.getWorldStateRootHash().map(Bytes::toHexString).orElse(null),
        blockchain.getChainHead());
  }

  public Optional<BlockResult> getChainHead() {
    return getBlockByHash(blockchain.getChainHead().getHash());
  }

  public BlockChainBrowser moveBackward() {
    blockResult.ifPresent(res -> getBlockByHash(Hash.fromHexString(res.getParentHash()))
        .ifPresent(newResult -> this.blockResult = Optional.of(newResult)));
    return this;
  }

  public BlockChainBrowser moveForward() {
    blockResult.ifPresent(res ->
        getBlockByNumber(res.getNumber() + 1)
            .ifPresent(newResult -> this.blockResult = Optional.of(newResult)));
    return this;
  }

  public Optional<BlockResult> getBlockByNumber(final long blockNumber) {
    return blockchain.getBlockHashByNumber(blockNumber)
        .flatMap(this::getBlockByHash);
  }

  public Optional<BlockResult> getBlockByHash(final Hash blockHash) {
    return blockchain.getBlockHeader(blockHash)
        .flatMap(header -> blockchain.getBlockBody(header.getHash())
            .map(body -> new Block(header, body)))
        .map(block -> new BlockResult(
            block,
            blockchain.getTotalDifficultyByHash(block.getHash()))
        );
  }
}

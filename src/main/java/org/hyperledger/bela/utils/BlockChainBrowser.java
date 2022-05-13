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

package org.hyperledger.bela.utils;

import java.util.Optional;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Panel;
import org.hyperledger.bela.components.BlockPanel;
import org.hyperledger.bela.components.LanternaComponent;
import org.hyperledger.bela.components.SummaryPanel;
import org.hyperledger.bela.model.BlockResult;
import org.hyperledger.bela.utils.ConsensusDetector.CONSENSUS_TYPE;
import org.hyperledger.besu.consensus.common.bft.BftBlockHeaderFunctions;
import org.hyperledger.besu.consensus.qbft.QbftExtraDataCodec;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.bonsai.BonsaiWorldStateKeyValueStorage;
import org.hyperledger.besu.ethereum.chain.DefaultBlockchain;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.core.BlockHeaderFunctions;
import org.hyperledger.besu.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;

import static org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier.BLOCKCHAIN;

public class BlockChainBrowser {

    private final BonsaiWorldStateKeyValueStorage worldStateStorage;
    private final DefaultBlockchain blockchain;
    private Optional<BlockResult> blockResult;
    private BlockPanel blockPanel;
    private SummaryPanel summaryPanel;


    public BlockChainBrowser(
            final DefaultBlockchain blockchain,
            final BonsaiWorldStateKeyValueStorage worldStateStorage) {
        this.blockchain = blockchain;
        this.worldStateStorage = worldStateStorage;
        //fixme
        this.blockResult = getChainHead();
        blockResult.ifPresent(result -> this.blockPanel = new BlockPanel(result));
        blockResult.ifPresent(result -> this.summaryPanel = new SummaryPanel(
                result.getStateRoot(),
                String.valueOf(result.getNumber()), result.getHash()));

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
                .create(blockchainStorage, new NoOpMetricsSystem(), 0L);

        var worldStateStorage = new BonsaiWorldStateKeyValueStorage(provider);
        return new BlockChainBrowser((DefaultBlockchain) blockchain/*, worldStateArchive*/, worldStateStorage);
    }


    public LanternaComponent<Panel> blockPanel() {
        return blockPanel;
    }

    public LanternaComponent<? extends Component> showSummaryPanel() {
        return summaryPanel;
    }

    public Optional<BlockResult> getChainHead() {
        return getBlockByHash(blockchain.getChainHead().getHash());
    }

    public BlockChainBrowser moveBackward() {
        if (blockResult.isPresent()) {
            blockResult = getBlockByHash(Hash.fromHexString(blockResult.get().getParentHash()));
        } else {
            blockResult = getChainHead();
        }
        updatePanels(blockResult);
        return this;
    }

    private void updatePanels(final Optional<BlockResult> blockResult) {
        this.blockResult.ifPresent(result -> blockPanel.updateWithBlock(result));
        this.blockResult.ifPresent(result -> this.summaryPanel.updateWith(blockchain.getChainHeadBlock()));
    }

    public BlockChainBrowser moveForward() {
        if (blockResult.isPresent()) {
            blockResult = getBlockByNumber(blockResult.get().getNumber() + 1);
        } else {
            blockResult = getBlockByNumber(0);
        }
        updatePanels(blockResult);
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

    public String getBlockHash() {
        return blockResult.get().getHash();
    }

    public void rollHead() {
        if (blockResult.isPresent()) {
            blockchain.rewindToBlock(Hash.fromHexString(blockResult.get().getHash()));
            updateSummary();
        }
    }

    private void updateSummary() {
        final Block chainHeadBlock = blockchain.getChainHeadBlock();
        summaryPanel.updateWith(chainHeadBlock);
    }

    public void moveByHash(final Hash hash) {
        blockResult = getBlockByHash(hash);
        updatePanels(blockResult);
    }

    public void moveByNumber(final long number) {
        blockResult = getBlockByNumber(number);
        updatePanels(blockResult);
    }
}

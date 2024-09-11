package org.hyperledger.bela.utils;

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.consensus.ibft.IbftExtraDataCodec;
import org.hyperledger.besu.consensus.qbft.QbftExtraDataCodec;
import org.hyperledger.besu.ethereum.chain.VariablesStorage;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.hyperledger.besu.ethereum.rlp.RLPException;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;

public class ConsensusDetector {
    public enum CONSENSUS_TYPE {ETH_HASH, IBFT2, QBFT}

    public static CONSENSUS_TYPE detectConsensusMechanism(
        final KeyValueStorage keyValueStorage,
        final VariablesStorage variablesStorage) {
        // TODO: should detect this from file, centralize this logic in a common util
        var storage = new KeyValueStoragePrefixedKeyBlockchainStorage(keyValueStorage, variablesStorage, new MainnetBlockHeaderFunctions(), false);
        var genesisHash = storage.getBlockHash(BlockHeader.GENESIS_BLOCK_NUMBER).orElseThrow();
        var genesisBlockHeader = storage.getBlockHeader(genesisHash).orElseThrow();
        var extraData = genesisBlockHeader.getExtraData();

        try {
            if (isIbft2ExtraData(extraData)) {
                return CONSENSUS_TYPE.IBFT2;
            } else if (isQbftExtraData(extraData)) {
                return CONSENSUS_TYPE.QBFT;
            } else {
                return CONSENSUS_TYPE.ETH_HASH;
            }
        } catch (Exception e) {
            return CONSENSUS_TYPE.ETH_HASH;
        }
    }

    private static boolean isIbft2ExtraData(final Bytes extraData) {
        try {
            new IbftExtraDataCodec().decodeRaw(extraData);
            return true;
        } catch (RLPException e) {
            return false;
        }
    }

    private static boolean isQbftExtraData(final Bytes extraData) {
        try {
            new QbftExtraDataCodec().decodeRaw(extraData);
            return true;
        } catch (RLPException e) {
            return false;
        }
    }


}

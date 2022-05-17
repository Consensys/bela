package org.hyperledger.bela.trie.database;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.bela.trie.StorageNodeFinder;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.ethereum.trie.MerklePatriciaTrie;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;

import java.util.Optional;

public class ForestWorldStateReader implements StorageNodeFinder {


    private final KeyValueStorage forestBranchStorage;

    public ForestWorldStateReader(StorageProvider storageProvider) {
        forestBranchStorage = storageProvider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.WORLD_STATE);
    }

    @Override
    public Optional<Bytes> getAccountNode(Bytes location, Bytes32 hash) {
        if (hash.equals(MerklePatriciaTrie.EMPTY_TRIE_NODE_HASH)) {
            return Optional.of(MerklePatriciaTrie.EMPTY_TRIE_NODE);
        } else {
            return forestBranchStorage.get(hash.toArrayUnsafe()).map(Bytes::wrap);
        }

    }

    @Override
    public Optional<Bytes> getStorageNode(Bytes32 accountHash, Bytes location, Bytes32 hash) {
        if (hash.equals(MerklePatriciaTrie.EMPTY_TRIE_NODE_HASH)) {
            return Optional.of(MerklePatriciaTrie.EMPTY_TRIE_NODE);
        } else {
            return forestBranchStorage.get(hash.toArrayUnsafe()).map(Bytes::wrap);
        }
    }

    @Override
    public Optional<Bytes> getCode(Bytes32 accountHash, Bytes32 hash) {
        return forestBranchStorage.get(hash.toArrayUnsafe()).map(Bytes::wrap);
    }

}
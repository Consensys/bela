package org.hyperledger.bela.converter;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.bela.trie.TrieTraversalListener;
import org.hyperledger.bela.trie.NodeFoundListener;
import org.hyperledger.bela.trie.TrieTraversal;
import org.hyperledger.bela.trie.database.BonsaiWorldStateReader;
import org.hyperledger.bela.trie.database.ForestWorldStateReader;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorageTransaction;

import java.util.Optional;

public class DatabaseConverter {

    private final StorageProvider provider;
    private final TrieTraversalListener trieTraversalListener;

    public DatabaseConverter(final StorageProvider storageProvider, final TrieTraversalListener trieTraversalListener) {
        this.provider = storageProvider;
        this.trieTraversalListener = trieTraversalListener;
    }


    public void convertToBonsai() {
        KeyValueStorage forestBranchStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.WORLD_STATE);
        KeyValueStorage trieBranchStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.TRIE_BRANCH_STORAGE);
        KeyValueStorage codeStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.CODE_STORAGE);
        TrieTraversal tr = new TrieTraversal(Optional.empty(), provider, new ForestWorldStateReader(provider), new NodeFoundListener() {
            @Override
            public void onAccountNode(Bytes location, Bytes value) {
                KeyValueStorageTransaction keyValueStorageTransaction = trieBranchStorage.startTransaction();
                keyValueStorageTransaction.put(location.toArrayUnsafe(), value.toArrayUnsafe());
                keyValueStorageTransaction.commit();
            }

            @Override
            public void onStorageNode(Bytes32 accountHash, Bytes location, Bytes value) {
                KeyValueStorageTransaction keyValueStorageTransaction = trieBranchStorage.startTransaction();
                keyValueStorageTransaction.put(Bytes.concatenate(accountHash, location)
                        .toArrayUnsafe(), value.toArrayUnsafe());
                keyValueStorageTransaction.commit();
            }

            @Override
            public void onCode(Bytes32 accountHash, Bytes value) {
                KeyValueStorageTransaction keyValueStorageTransaction = codeStorage.startTransaction();
                keyValueStorageTransaction.put(accountHash.toArrayUnsafe(), value.toArrayUnsafe());
                keyValueStorageTransaction.commit();
            }
        }, trieTraversalListener);
        tr.traverse();
        if(!tr.isInvalidWorldstate()) {
            forestBranchStorage.clear();
        }

    }

    public void convertToForest() {
        KeyValueStorage forestBranchStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.WORLD_STATE);
        KeyValueStorage trieBranchStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.TRIE_BRANCH_STORAGE);
        KeyValueStorage codeStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.CODE_STORAGE);

        TrieTraversal tr = new TrieTraversal(Optional.empty(), provider, new BonsaiWorldStateReader(provider), new NodeFoundListener() {
            @Override
            public void onAccountNode(Bytes location, Bytes value) {
                KeyValueStorageTransaction keyValueStorageTransaction = forestBranchStorage.startTransaction();
                keyValueStorageTransaction.put(Hash.hash(value).toArrayUnsafe(), value.toArrayUnsafe());
                keyValueStorageTransaction.commit();
            }

            @Override
            public void onStorageNode(Bytes32 accountHash, Bytes location, Bytes value) {
                KeyValueStorageTransaction keyValueStorageTransaction = forestBranchStorage.startTransaction();
                keyValueStorageTransaction.put(Hash.hash(value).toArrayUnsafe(), value.toArrayUnsafe());
                keyValueStorageTransaction.commit();
            }

            @Override
            public void onCode(Bytes32 accountHash, Bytes value) {
                KeyValueStorageTransaction keyValueStorageTransaction = forestBranchStorage.startTransaction();
                keyValueStorageTransaction.put(Hash.hash(value).toArrayUnsafe(), value.toArrayUnsafe());
                keyValueStorageTransaction.commit();
            }
        }, trieTraversalListener);
        tr.traverse();
        if(!tr.isInvalidWorldstate()) {
            trieBranchStorage.clear();
            codeStorage.clear();
        }

    }

}

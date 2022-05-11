package org.hyperledger.bela.converter;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.bela.trie.NodeFoundListener;
import org.hyperledger.bela.trie.StorageNodeFinder;
import org.hyperledger.bela.trie.TrieTraversal;
import org.hyperledger.bela.utils.DataUtils;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorageTransaction;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class DatabaseConverter {

    private final StorageProvider provider;

    public DatabaseConverter(final Path dataDir){
        this.provider =
                DataUtils.createKeyValueStorageProvider(dataDir, dataDir.resolve("database"));
    }


    public void convertToBonsai(){
        KeyValueStorage forestBranchStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.WORLD_STATE);
        KeyValueStorage trieBranchStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.TRIE_BRANCH_STORAGE);
        KeyValueStorage codeStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.CODE_STORAGE);
        TrieTraversal tr = new TrieTraversal(provider, new StorageNodeFinder() {
            @Override
            public Optional<Bytes> getAccountNode(Bytes location, Bytes32 hash) {
                return forestBranchStorage.get(hash.toArrayUnsafe()).map(Bytes::wrap);
            }

            @Override
            public Optional<Bytes> getStorageNode(Bytes32 accountHash, Bytes location, Bytes32 hash) {
                return forestBranchStorage
                        .get(hash.toArrayUnsafe())
                        .map(Bytes::wrap);
            }

            @Override
            public Optional<Bytes> getCode(Bytes32 accountHash, Bytes32 hash) {
                return forestBranchStorage.get(hash.toArrayUnsafe()).map(Bytes::wrap);
            }
        }, new NodeFoundListener() {
            @Override
            public void onAccountNode(Bytes location, Bytes value) {
                KeyValueStorageTransaction keyValueStorageTransaction = trieBranchStorage.startTransaction();
                keyValueStorageTransaction.put(location.toArrayUnsafe(), value.toArrayUnsafe());
                keyValueStorageTransaction.commit();
            }

            @Override
            public void onStorageNode(Bytes32 accountHash, Bytes location, Bytes value) {
                KeyValueStorageTransaction keyValueStorageTransaction = trieBranchStorage.startTransaction();
                keyValueStorageTransaction.put(Bytes.concatenate(accountHash, location).toArrayUnsafe(), value.toArrayUnsafe());
                keyValueStorageTransaction.commit();
            }

            @Override
            public void onCode(Bytes32 accountHash, Bytes value) {
                KeyValueStorageTransaction keyValueStorageTransaction = codeStorage.startTransaction();
                keyValueStorageTransaction.put(accountHash.toArrayUnsafe(), value.toArrayUnsafe());
                keyValueStorageTransaction.commit();
            }
        });
        tr.start();
    }

    public void convertToForest(){
        KeyValueStorage forestBranchStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.WORLD_STATE);
        KeyValueStorage trieBranchStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.TRIE_BRANCH_STORAGE);
        TrieTraversal tr = new TrieTraversal(provider, new StorageNodeFinder() {
            @Override
            public Optional<Bytes> getAccountNode(Bytes location, Bytes32 hash) {
                return trieBranchStorage.get(location.toArrayUnsafe()).map(Bytes::wrap);
            }

            @Override
            public Optional<Bytes> getStorageNode(Bytes32 accountHash, Bytes location, Bytes32 hash) {
                return trieBranchStorage
                        .get(Bytes.concatenate(accountHash, location).toArrayUnsafe())
                        .map(Bytes::wrap);
            }

            @Override
            public Optional<Bytes> getCode(Bytes32 accountHash, Bytes32 hash) {
                return trieBranchStorage.get(accountHash.toArrayUnsafe()).map(Bytes::wrap);
            }
        }, new NodeFoundListener() {
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
        });
        tr.start();
    }

    public static void main(final String[] args) {
        final Path dataDir = Paths.get(args[0]);
        DatabaseConverter d = new DatabaseConverter(dataDir);
        final String convertTo = args[1];
        if(convertTo.equals("Bonsai")){
            d.convertToBonsai();
        }else {
            d.convertToForest();
        }
    }


}

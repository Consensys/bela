package org.hyperledger.bela.utils.bonsai;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.ethereum.trie.CompactEncoding;
import org.hyperledger.besu.ethereum.trie.MerklePatriciaTrie;
import org.hyperledger.besu.ethereum.trie.Node;
import org.hyperledger.besu.ethereum.trie.TrieNodeDecoder;
import org.hyperledger.besu.ethereum.worldstate.StateTrieAccountValue;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;

public class BonsaiTraversal {

    int visited = 0;

    //    private final KeyValueStorage accountStorage;
    //    private final KeyValueStorage storageStorage;
    private final KeyValueStorage trieBranchStorage;
    private BonsaiListener listener;
    private final KeyValueStorage codeStorage;
    private Node<Bytes> root;
    //    private final KeyValueStorage trieLogStorage;
    //    private final Pair<KeyValueStorage, KeyValueStorage> snapTrieBranchBucketsStorage;

    public BonsaiTraversal(final StorageProvider provider, BonsaiListener listener) {
        //        accountStorage =
        //
        // provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.ACCOUNT_INFO_STATE);
        codeStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.CODE_STORAGE);
        //        storageStorage =
        //
        // provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.ACCOUNT_STORAGE_STORAGE);
        trieBranchStorage =
                provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.TRIE_BRANCH_STORAGE);
        //        trieLogStorage =
        //
        // provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.TRIE_LOG_STORAGE);

        this.listener = listener;
    }

    public void traverse() {

        final Hash x =
                trieBranchStorage
                        .get(Bytes.EMPTY.toArrayUnsafe())
                        .map(Bytes::wrap)
                        .map(Hash::hash)
                        .orElseThrow();
        root = getAccountNodeValue(x, Bytes.EMPTY);
        //        breakTree(x);
        listener.root(root.getHash());
        traverseAccountTrie(root);
    }

  /*    private void breakTree(final Hash x) {
      MerklePatriciaTrie<Bytes,Bytes> trie =  new StoredMerklePatriciaTrie<>(
              new StoredNodeFactory<>(
                      (location, hash) -> Optional.ofNullable(getAccountNodeValue(hash, location).getRlp()), Function.identity(), Function.identity()),
              x);
      trie.entriesFrom(Bytes32.ZERO, 1).entrySet().stream().findFirst().ifPresent(
              account -> {
                  final StateTrieAccountValue accountValue = StateTrieAccountValue.readFrom(RLP.input(account.getValue()));
                  final StateTrieAccountValue updatedAccountValue
                          = new StateTrieAccountValue(accountValue.getNonce(), accountValue.getBalance().add(Wei.ONE), accountValue.getStorageRoot(), accountValue.getCodeHash());
                  final Bytes dataToSave = RLP.encode(updatedAccountValue::writeTo);
                  trie.put(account.getKey(), dataToSave);
                  final KeyValueStorageTransaction transaction = trieBranchStorage.startTransaction();
                  final AtomicInteger countNode = new AtomicInteger();
                  trie.commit((location, hash, value) -> {
                      if(countNode.getAndIncrement()==0){
                          System.out.println("Updated account node "+account.getKey()+" with "+value);
                          transaction.put(location.toArrayUnsafe(), dataToSave.toArrayUnsafe());
                      }
                  });
                  transaction.commit();
              }
      );
  }*/

    public void traverseAccountTrie(final Node<Bytes> parentNode) {
        if (parentNode == null) {
            return;
        }
        listener.visited(BonsaiTraversalTrieType.Account);

        final List<Node<Bytes>> nodes =
                TrieNodeDecoder.decodeNodes(parentNode.getLocation().orElseThrow(), parentNode.getRlp());
        nodes.forEach(
                node -> {
                    if (nodeIsHashReferencedDescendant(parentNode, node)) {
                        traverseAccountTrie(
                                getAccountNodeValue(node.getHash(), node.getLocation().orElseThrow()));
                    } else {
                        if (node.getValue().isPresent()) {
                            final StateTrieAccountValue accountValue =
                                    StateTrieAccountValue.readFrom(RLP.input(node.getValue().orElseThrow()));
                            final Hash accountHash =
                                    Hash.wrap(
                                            Bytes32.wrap(
                                                    CompactEncoding.pathToBytes(
                                                            Bytes.concatenate(
                                                                    parentNode.getLocation()
                                                                            .orElseThrow(), node.getPath()))));
                            // Add code, if appropriate
                            if (!accountValue.getCodeHash().equals(Hash.EMPTY)) {
                                // traverse code
                                final Optional<Bytes> code =
                                        codeStorage.get(accountHash.toArrayUnsafe()).map(Bytes::wrap);
                                if (code.isEmpty()) {
                                    listener.missingCodeHash(accountValue.getCodeHash(), accountHash);
                                } else {
                                    final Hash foundCodeHash = Hash.hash(code.orElseThrow());
                                    if (!foundCodeHash.equals(accountValue.getCodeHash())) {
                                        listener.invalidCode(accountHash, accountValue.getCodeHash(), foundCodeHash);
                                    }
                                }
                            }
                            // Add storage, if appropriate
                            if (!accountValue.getStorageRoot().equals(MerklePatriciaTrie.EMPTY_TRIE_NODE_HASH)) {
                                traverseStorageTrie(
                                        accountHash,
                                        getStorageNodeValue(accountValue.getStorageRoot(), accountHash, Bytes.EMPTY));
                            }
                        } else {
                            listener.missingValueForNode(node.getHash());
                        }
                    }
                });
    }

    public void traverseStorageTrie(final Bytes32 accountHash, final Node<Bytes> parentNode) {
        if (parentNode == null) {
            return;
        }
        listener.visited(BonsaiTraversalTrieType.Storage);

        final List<Node<Bytes>> nodes =
                TrieNodeDecoder.decodeNodes(parentNode.getLocation().orElseThrow(), parentNode.getRlp());
        nodes.forEach(
                node -> {
                    if (nodeIsHashReferencedDescendant(parentNode, node)) {
                        traverseStorageTrie(
                                accountHash,
                                getStorageNodeValue(node.getHash(), accountHash, node.getLocation().orElseThrow()));
                    }
                });
    }

    @Nullable
    private Node<Bytes> getAccountNodeValue(final Bytes32 hash, final Bytes location) {
        final Optional<Bytes> bytes = trieBranchStorage.get(location.toArrayUnsafe()).map(Bytes::wrap);
        if (bytes.isEmpty()) {
            listener.missingAccountTrieForHash(hash, location);
            return null;
        }
        final Hash foundHashNode = Hash.hash(bytes.orElseThrow());
        if (!foundHashNode.equals(hash)) {
            listener.invalidAccountTrieForHash(hash, location, foundHashNode);
            return null;
        }
        return TrieNodeDecoder.decode(location, bytes.get());
    }

    private Node<Bytes> getStorageNodeValue(
            final Bytes32 hash, final Bytes32 accountHash, final Bytes location) {
        final Optional<Bytes> bytes =
                trieBranchStorage
                        .get(Bytes.concatenate(accountHash, location).toArrayUnsafe())
                        .map(Bytes::wrap);
        if (bytes.isEmpty()) {
            listener.missingStorageTrieForHash(hash, location);
            return null;
        }
        final Hash foundHashNode = Hash.hash(bytes.orElseThrow());
        if (!foundHashNode.equals(hash)) {
            listener.invalidStorageTrieForHash(hash, location, foundHashNode);
            return null;
        }
        return TrieNodeDecoder.decode(location, bytes.get());
    }

    private boolean nodeIsHashReferencedDescendant(
            final Node<Bytes> parentNode, final Node<Bytes> node) {
        return !Objects.equals(node.getHash(), parentNode.getHash()) && node.isReferencedByHash();
    }


    public String getRoot() {
        return root.getHash().toHexString();
    }
}

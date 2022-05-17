package org.hyperledger.bela.trie;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.datatypes.Hash;

public interface TrieTraversalListener {
    void root(Bytes32 hash);

    void missingCodeHash(Hash codeHash, Hash accountHash);

    void invalidCode(Hash accountHash, Hash codeHash, Hash foundCodeHash);

    void missingValueForNode(Bytes32 hash);

    void visited(final TraversalTrieType type);

    void missingAccountTrieForHash(Bytes32 hash, Bytes location);

    void invalidAccountTrieForHash(Bytes32 hash, Bytes location, Hash foundHashNode);

    void missingStorageTrieForHash(Bytes32 hash, Bytes location);

    void invalidStorageTrieForHash(Bytes32 hash, Bytes location, Hash foundHashNode);
}

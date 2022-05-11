package org.hyperledger.bela.trie;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.datatypes.Hash;

import java.util.Optional;

public interface StorageNodeFinder {

    Optional<Bytes> getAccountNode(final Bytes location, final Bytes32 hash);

    Optional<Bytes> getStorageNode(final Bytes32 accountHash, final Bytes location, final Bytes32 hash);

    Optional<Bytes> getCode(final Bytes32 accountHash, final Bytes32 hash);

}

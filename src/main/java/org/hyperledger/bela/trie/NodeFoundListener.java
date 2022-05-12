package org.hyperledger.bela.trie;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public interface NodeFoundListener {

    void onAccountNode(final Bytes location, final Bytes value);

    void onStorageNode(final Bytes32 accountHash, final Bytes location, final Bytes value);

    void onCode(final Bytes32 accountHash, final Bytes value);
}

package org.hyperledger.bela.utils.hacks;

import java.util.ArrayList;
import java.util.List;
import org.hyperledger.bela.utils.SentMessageSubscriber;
import org.hyperledger.besu.ethereum.p2p.peers.Peer;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.Capability;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.MessageData;

public class ReadOnlyDatabaseDecider {
    private static final ReadOnlyDatabaseDecider instance = new ReadOnlyDatabaseDecider();
    private boolean readOnly = true;

    public static ReadOnlyDatabaseDecider getInstance() {
        return instance;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
    }
}

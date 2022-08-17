package org.hyperledger.bela.utils;

import org.hyperledger.besu.ethereum.p2p.peers.Peer;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.Capability;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.MessageData;

public interface SentMessageSubscriber {

    public void onSentMessage(final Peer peer, final Capability capability, final MessageData message);
}

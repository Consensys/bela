package org.hyperledger.bela.utils.hacks;

import java.util.ArrayList;
import java.util.List;
import org.hyperledger.bela.utils.SentMessageSubscriber;
import org.hyperledger.besu.ethereum.p2p.peers.Peer;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.Capability;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.MessageData;

public class SentMessageMonitor {

    private static final SentMessageMonitor instance = new SentMessageMonitor();
    private final List<SentMessageSubscriber> subscribers = new ArrayList<>();

    public static SentMessageMonitor getInstance() {
        return instance;
    }

    public void subscribe(final SentMessageSubscriber subscriber) {
        this.subscribers.add(subscriber);
    }

    public void message(final Peer peer, final Capability capability, final MessageData messageData) {
        for (final SentMessageSubscriber subscriber : subscribers) {
            subscriber.onSentMessage(peer, capability, messageData);
        }
    }
}

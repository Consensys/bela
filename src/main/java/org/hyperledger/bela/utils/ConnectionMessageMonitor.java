package org.hyperledger.bela.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.ethereum.p2p.peers.Peer;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.Message;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.MessageData;

public class ConnectionMessageMonitor {

    Map<Bytes, List<DirectedMessage>> currentConversations = new ConcurrentHashMap<>();
    Map<Bytes, List<List<DirectedMessage>>> pastConversations = new ConcurrentHashMap<>();

    public void addReceivedMessage(Message message) {
        getCurrentConversation(message.getConnection().getPeer()).add(new IncomingMessage(message.getData()));
    }

    public void addSentMessage(final Peer peer, final MessageData messageData) {
        getCurrentConversation(peer).add(new OutgoingMessage(messageData));
    }

    public void disconnected(final Peer peer) {
        final List<DirectedMessage> removed = currentConversations.remove(peer.getId());
        if (removed != null) {
            getPastConversations(peer).add(removed);
        }
    }

    public int countAllMessages(final Peer peer) {
        return getCurrentConversation(peer)
                .size() + getPastConversations(peer).stream().mapToInt(List::size).sum();
    }

    private List<List<DirectedMessage>> getPastConversations(final Peer peer) {
        return pastConversations.computeIfAbsent(peer.getId(), k -> new ArrayList<>());
    }

    private List<DirectedMessage> getCurrentConversation(final Peer peer) {
        return currentConversations.computeIfAbsent(peer.getId(), k -> new ArrayList<>());
    }

    public List<List<DirectedMessage>> getConversations(final Peer peer) {
        final List<List<DirectedMessage>> result = new ArrayList<>(getPastConversations(peer));
        result.add(getCurrentConversation(peer));
        return result;
    }

    public interface DirectedMessage{

        MessageData getMessageData();

    }
    public static class IncomingMessage  implements DirectedMessage{
        private final MessageData data;

        public IncomingMessage(final MessageData data) {
            this.data = data;
        }

        @Override
        public MessageData getMessageData() {
            return data;
        }

        @Override
        public String toString() {
            return "I:"+data.getCode();
        }
    }

    public static class OutgoingMessage implements DirectedMessage{
        private final MessageData data;

        public OutgoingMessage(final MessageData data) {
            this.data = data;
        }
        @Override
        public MessageData getMessageData() {
            return data;
        }

        @Override
        public String toString() {
            return "O:"+data.getCode();
        }
    }
}



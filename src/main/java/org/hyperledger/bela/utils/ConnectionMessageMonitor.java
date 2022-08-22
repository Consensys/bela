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

    Map<Bytes, List<DirectedMessage>> conversations = new ConcurrentHashMap<>();

    public void addReceivedMessage(Message message) {
        getCurrentConversation(message.getConnection().getPeer()).add(new IncomingMessage(message.getData()));
    }

    public void addSentMessage(final Peer peer, final MessageData messageData) {
        getCurrentConversation(peer).add(new OutgoingMessage(messageData));
    }

    public int countAllMessages(final Peer peer) {
        return getCurrentConversation(peer).size();
    }

    private List<DirectedMessage> getCurrentConversation(final Peer peer) {
        return conversations.computeIfAbsent(peer.getId(), k -> new ArrayList<>());
    }

    public List<DirectedMessage> getConversation(final Peer peer) {
        return getCurrentConversation(peer);

    }

    public enum MessageType {
        INCOMING, OUTGOING;


        @Override
        public String toString() {
            return String.valueOf(name().charAt(0));
        }
    }

    public interface DirectedMessage{

        MessageData getMessageData();
        MessageType getMessageType();
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
        public MessageType getMessageType() {
            return MessageType.INCOMING;
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
        public MessageType getMessageType() {
            return MessageType.OUTGOING;
        }

        @Override
        public String toString() {
            return "O:"+data.getCode();
        }
    }
}



package org.hyperledger.bela.context;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.hyperledger.besu.ethereum.p2p.discovery.DiscoveryPeer;
import org.hyperledger.besu.ethereum.p2p.network.P2PNetwork;
import org.hyperledger.besu.ethereum.p2p.peers.Peer;
import org.hyperledger.besu.ethereum.p2p.rlpx.ConnectCallback;
import org.hyperledger.besu.ethereum.p2p.rlpx.DisconnectCallback;
import org.hyperledger.besu.ethereum.p2p.rlpx.MessageCallback;
import org.hyperledger.besu.ethereum.p2p.rlpx.connections.PeerConnection;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.Capability;
import org.hyperledger.besu.plugin.data.EnodeURL;

public class BelaP2PNetworkFacade implements P2PNetwork {

    private final P2PNetwork delegate;

    public BelaP2PNetworkFacade(P2PNetwork delegate) {
        this.delegate = delegate;
    }

    @Override
    public void start() {
        delegate.start();
    }

    @Override
    public Collection<PeerConnection> getPeers() {
        return delegate.getPeers();
    }

    @Override
    public Stream<DiscoveryPeer> streamDiscoveredPeers() {
        return delegate.streamDiscoveredPeers();
    }

    @Override
    public CompletableFuture<PeerConnection> connect(final Peer peer) {
        return delegate.connect(peer);
    }

    @Override
    public void subscribe(final Capability capability, final MessageCallback callback) {
        delegate.subscribe(capability, callback);
    }

    @Override
    public void subscribeConnect(final ConnectCallback callback) {
        delegate.subscribeConnect(callback);
    }

    @Override
    public void subscribeDisconnect(final DisconnectCallback callback) {
        delegate.subscribeDisconnect(callback);
    }

    @Override
    public boolean addMaintainedConnectionPeer(final Peer peer) {
        return delegate.addMaintainedConnectionPeer(peer);
    }

    @Override
    public boolean removeMaintainedConnectionPeer(final Peer peer) {
        return delegate.removeMaintainedConnectionPeer(peer);
    }

    @Override
    public void stop() {
        delegate.stop();
    }

    @Override
    public void awaitStop() {
        delegate.awaitStop();
    }

    @Override
    public boolean isListening() {
        return delegate.isListening();
    }

    @Override
    public boolean isP2pEnabled() {
        return delegate.isP2pEnabled();
    }

    @Override
    public boolean isDiscoveryEnabled() {
        return delegate.isDiscoveryEnabled();
    }

    @Override
    public Optional<EnodeURL> getLocalEnode() {
        return delegate.getLocalEnode();
    }

    @Override
    public void updateNodeRecord() {
        delegate.updateNodeRecord();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}

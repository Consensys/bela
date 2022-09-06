package org.hyperledger.bela.windows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import org.hyperledger.bela.components.Counter;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.context.BelaContext;
import org.hyperledger.bela.context.BelaP2PNetworkFacade;
import org.hyperledger.bela.context.MainNetContext;
import org.hyperledger.bela.dialogs.BelaDialog;
import org.hyperledger.bela.utils.ConnectionMessageMonitor;
import org.hyperledger.bela.utils.hacks.SentMessageMonitor;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.eth.manager.task.RetryingGetHeadersEndingAtFromPeerByHashTask;
import org.hyperledger.besu.ethereum.p2p.network.P2PNetwork;
import org.hyperledger.besu.ethereum.p2p.network.ProtocolManager;
import org.hyperledger.besu.ethereum.p2p.peers.DefaultPeer;
import org.hyperledger.besu.ethereum.p2p.peers.Peer;
import org.hyperledger.besu.ethereum.p2p.rlpx.ConnectCallback;
import org.hyperledger.besu.ethereum.p2p.rlpx.DisconnectCallback;
import org.hyperledger.besu.ethereum.p2p.rlpx.MessageCallback;
import org.hyperledger.besu.ethereum.p2p.rlpx.connections.PeerConnection;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.Capability;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.Message;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.SubProtocol;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.messages.DisconnectMessage;
import org.jetbrains.annotations.NotNull;

import static kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory.getLogger;
import static org.hyperledger.bela.windows.Constants.KEY_CLOSE;

public class P2PManagementWindow implements BelaWindow, MessageCallback, ConnectCallback, DisconnectCallback {
    private static final LambdaLogger log = getLogger(P2PManagementWindow.class);

    private final StorageProviderFactory storageProviderFactory;
    private final Preferences preferences;
    private final WindowBasedTextGUI gui;
    private final ConnectionMessageMonitor monitor = new ConnectionMessageMonitor();
    private final PeerDetailWindow peerDetailWindow;
    Map<Capability, Counter> counters = new HashMap<>();
    Counter connect = new Counter("connect");
    Counter disconnect = new Counter("disconnect");
    Map<DisconnectMessage.DisconnectReason, Counter> disconects = new ConcurrentHashMap<>();
    Panel rightCounters = new Panel();
    BelaContext belaContext;

    public P2PManagementWindow(final WindowBasedTextGUI gui, final StorageProviderFactory storageProviderFactory, final Preferences preferences) {
        this.gui = gui;
        this.storageProviderFactory = storageProviderFactory;
        this.preferences = preferences;
        belaContext = new MainNetContext(storageProviderFactory);
        peerDetailWindow = new PeerDetailWindow(gui);
    }


    @Override
    public String label() {
        return "P2P Management";
    }

    @Override
    public MenuGroup group() {
        return MenuGroup.P2P;
    }

    @Override
    public Window createWindow() {
        final Window window = new BasicWindow(label());
        window.setHints(List.of(Window.Hint.FULL_SCREEN));
        Panel panel = new Panel(new LinearLayout());

        KeyControls controls = new KeyControls()
                .addControl("Start P2P", 's', this::startP2P)
                .addControl("Stop P2P", 'x', this::stopP2P)
                .addControl("Close", KEY_CLOSE, window::close)
                .addSection("Peers")
                .addControl("Add Peer", 'a', this::addPeer)
                .addControl("Connections", 'c', this::connections)
                .addControl("Discovered Peers", 'd', this::showDiscoveredPeers)
                .addControl("Maintained Peers", 'm', this::showMaintainedPeers)
                .addSection("Operations")
                .addControl("Ask For Header", 'h', this::askForHeader);
        window.addWindowListener(controls);
        panel.addComponent(controls.createComponent());

        final List<Capability> supportedCapabilities = belaContext.getSupportedCapabilities();

        final Panel countersPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        panel.addComponent(countersPanel);

        final Panel firstColumn = new Panel();
        countersPanel.addComponent(firstColumn);
        countersPanel.addComponent(rightCounters);

        rightCounters.addComponent(connect.createComponent());
        rightCounters.addComponent(disconnect.createComponent());


        supportedCapabilities.forEach(c -> {
            final Counter counter = new Counter(c.getName() + "" + c.getVersion());
            this.counters.put(c, counter);
            firstColumn.addComponent(counter.createComponent());
        });


        window.setComponent(panel);
        return window;
    }

    private void showMaintainedPeers() {
        final BelaP2PNetworkFacade p2PNetwork = (BelaP2PNetworkFacade) belaContext.getP2PNetwork();
        showPeers(p2PNetwork.streamMaintainedPeers().collect(Collectors.toList()));
    }

    private void showPeers(final List<Peer> peers) {
        BelaDialog.showDelegateListDialog(gui, "Select a peer", peers,
                this::constructPeerString,
                peer -> {
                    peerDetailWindow.setActivePeer(peer, monitor.getConversation(peer));
                    final Window window = peerDetailWindow.createWindow();
                    gui.addWindowAndWait(window);
                });
    }

    @NotNull
    private String constructPeerString(final Peer peer) {
        return monitor.countAllMessages(peer) + ":" +peer.getEnodeURLString() ;
    }

    private void showDiscoveredPeers() {
        showPeers(belaContext.getP2PNetwork().streamDiscoveredPeers().collect(Collectors.toList()));
    }

    private void connections() {
        showPeers(belaContext.getP2PNetwork().getPeers().stream().map(PeerConnection::getPeer)
                .collect(Collectors.toList()));

    }

    private void addPeer() {
        String enodeUri = TextInputDialog.showDialog(gui, "Enter enode uri", "Enode", "enode://aaa619241464023c97b6ce2bfb7a5f3b61468014215619044965acc76f054405ff71719b09ab53e67da8769d0143bea270c25720b4b2db14d1a0ac18162ea748@118.189.184.62:30303");

        try {
            Peer maintainedPeer = DefaultPeer.fromURI(enodeUri);
            belaContext.getP2PNetwork().addMaintainedConnectionPeer(maintainedPeer);
        } catch (Exception e) {
            BelaDialog.showException(gui, e);
        }
    }

    private void askForHeader() {

        String hashString = TextInputDialog.showDialog(gui, "Enter enode uri", "Enode", "0x689e36772f649c947c8a8d94e502586dcf3351ec2577090a848ee241de766cbc");
        final Hash hash = Hash.fromHexString(hashString);
        final RetryingGetHeadersEndingAtFromPeerByHashTask
                retryingGetHeadersEndingAtFromPeerByHashTask =
                RetryingGetHeadersEndingAtFromPeerByHashTask.endingAtHash(
                        belaContext.getProtocolSchedule(),
                        belaContext.getEthContext(),
                        hash,
                        1,
                        belaContext.getMetricsSystem());

        final CompletableFuture<List<BlockHeader>> run = retryingGetHeadersEndingAtFromPeerByHashTask.run();

        try {
            final List<BlockHeader> blockHeaders = run.get(5, TimeUnit.SECONDS);

            BelaDialog.showMessage(gui, "header", "header for " + hash.toHexString() + " was retrieved");

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            BelaDialog.showException(gui, e);
        }
    }

    private void startP2P() {

        final P2PNetwork p2PNetwork = belaContext.getP2PNetwork();
        for (Capability capability : belaContext.getSupportedCapabilities()) {
            p2PNetwork.subscribe(capability, this);
        }

        p2PNetwork.subscribeConnect(this);
        p2PNetwork.subscribeDisconnect(this);


        final ProtocolManager protocolManager = belaContext.getProtocolManager();
        for (Capability supportedCapability : protocolManager.getSupportedCapabilities()) {
            final SubProtocol protocol = findSubProtocol(supportedCapability.getName());

            belaContext.getP2PNetwork().subscribe(supportedCapability,
                    (capability, message) -> {
                        final int code = message.getData().getCode();
                        if (!protocol.isValidMessageCode(capability.getVersion(), code)) {
                            message.getConnection().disconnect(DisconnectMessage.DisconnectReason.BREACH_OF_PROTOCOL);
                            return;
                        }
                        protocolManager.processMessage(capability, message);
                    });
        }

        p2PNetwork.start();
        new MessageDialogBuilder().setText("P2P started").setTitle("P2P started").build().showDialog(gui);
        SentMessageMonitor.getInstance()
                .subscribe((peer, capability, messageData) -> monitor.addSentMessage(peer, messageData));

    }

    private SubProtocol findSubProtocol(final String name) {
        for (SubProtocol subProtocol : belaContext.getSubProtocols()) {
            if (subProtocol.getName().equals(name)) {
                return subProtocol;
            }
        }
        throw new IllegalArgumentException("No subprotocol found for " + name);
    }

    private List<SubProtocol> getSubProtocols() {
        return new ArrayList<>();
    }

    private void stopP2P() {
        try {
            if (belaContext.getP2PNetwork() != null) {
                belaContext.getP2PNetwork().close();
            }
        } catch (IOException e) {
            log.error("There was an error when stopping the network", e);
            BelaDialog.showException(gui, e);
        }
    }


    @Override
    public void onMessage(final Capability capability, final Message message) {
        counters.get(capability).add(1);

        monitor.addReceivedMessage(message);
    }

    @Override
    public void onConnect(final PeerConnection peer) {
        connect.add(1);
    }

    @Override
    public void onDisconnect(final PeerConnection connection, final DisconnectMessage.DisconnectReason reason, final boolean initiatedByPeer) {
        Counter counter = disconects.computeIfAbsent(reason, disconnectReason -> {
            final Counter c = new Counter(reason.name());
            rightCounters.addComponent(c.createComponent());
            return c;
        });
        counter.add(1);
        disconnect.add(1);
    }
}



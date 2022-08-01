package org.hyperledger.bela.windows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.prefs.Preferences;
import com.google.common.collect.ImmutableList;
import com.googlecode.lanterna.gui2.BasicWindow;
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
import org.hyperledger.bela.context.MainNetContext;
import org.hyperledger.bela.dialogs.BelaDialog;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.eth.EthProtocol;
import org.hyperledger.besu.ethereum.eth.manager.task.RetryingGetHeadersEndingAtFromPeerByHashTask;
import org.hyperledger.besu.ethereum.p2p.network.P2PNetwork;
import org.hyperledger.besu.ethereum.p2p.peers.DefaultPeer;
import org.hyperledger.besu.ethereum.p2p.peers.EnodeURLImpl;
import org.hyperledger.besu.ethereum.p2p.peers.Peer;
import org.hyperledger.besu.ethereum.p2p.rlpx.ConnectCallback;
import org.hyperledger.besu.ethereum.p2p.rlpx.DisconnectCallback;
import org.hyperledger.besu.ethereum.p2p.rlpx.MessageCallback;
import org.hyperledger.besu.ethereum.p2p.rlpx.connections.PeerConnection;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.Capability;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.Message;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.SubProtocol;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.messages.DisconnectMessage;
import org.hyperledger.besu.plugin.data.EnodeURL;

import static kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory.getLogger;
import static org.hyperledger.bela.windows.Constants.KEY_CLOSE;

public class P2PManagementWindow implements BelaWindow, MessageCallback, ConnectCallback, DisconnectCallback {
    private static final LambdaLogger log = getLogger(P2PManagementWindow.class);

    private final StorageProviderFactory storageProviderFactory;
    Map<Capability, Counter> counters = new HashMap<>();
    Counter connect = new Counter("connect");
    Counter disconnect = new Counter("disconnect");
    BelaContext belaContext;
    private Preferences preferences;
    private WindowBasedTextGUI gui;

    public P2PManagementWindow(final WindowBasedTextGUI gui, final StorageProviderFactory storageProviderFactory, final Preferences preferences) {
        this.gui = gui;
        this.storageProviderFactory = storageProviderFactory;
        this.preferences = preferences;
        belaContext = new MainNetContext(storageProviderFactory);
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
                .addControl("Add Peer", 'p', this::addPeer)
                .addControl("Ask For Body", 'b', this::askForBody)
                .addControl("Close", KEY_CLOSE, window::close);
        window.addWindowListener(controls);
        panel.addComponent(controls.createComponent());

        final List<Capability> supportedCapabilities = calculateCapabilities(false);

        panel.addComponent(connect.createComponent());
        panel.addComponent(disconnect.createComponent());


        supportedCapabilities.forEach(c -> {
            final Counter counter = new Counter(c.getName() + "" + c.getVersion());
            counters.put(c, counter);
            panel.addComponent(counter.createComponent());
        });


        window.setComponent(panel);
        return window;
    }

    private void addPeer() {
        final String enodeUri = TextInputDialog.showDialog(gui, "Enter enode uri", "Enode", "");

        try {
            Peer maintainedPeer = DefaultPeer.fromURI(enodeUri);
            belaContext.getP2PNetwork().addMaintainedConnectionPeer(maintainedPeer);
        } catch (Exception e) {
            BelaDialog.showException(gui,e);
        }
    }

    private void askForBody() {


        final Hash hash = Hash.fromHexString("0x689e36772f649c947c8a8d94e502586dcf3351ec2577090a848ee241de766cbc");
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


        p2PNetwork.start();
        new MessageDialogBuilder().setText("P2P started").setTitle("P2P started").build().showDialog(gui);

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


    private List<Capability> calculateCapabilities(final boolean fastSyncEnabled) {
        final ImmutableList.Builder<Capability> capabilities = ImmutableList.builder();
        if (!fastSyncEnabled) {
            capabilities.add(EthProtocol.ETH62);
        }
        capabilities.add(EthProtocol.ETH63);
        capabilities.add(EthProtocol.ETH64);
        capabilities.add(EthProtocol.ETH65);
        capabilities.add(EthProtocol.ETH66);

        return capabilities.build();
    }

    @Override
    public void onMessage(final Capability capability, final Message message) {
        counters.get(capability).add(1);
    }

    @Override
    public void onConnect(final PeerConnection peer) {
        connect.add(1);
    }

    @Override
    public void onDisconnect(final PeerConnection connection, final DisconnectMessage.DisconnectReason reason, final boolean initiatedByPeer) {
        disconnect.add(1);
    }
}



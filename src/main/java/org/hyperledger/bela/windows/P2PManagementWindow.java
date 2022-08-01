package org.hyperledger.bela.windows;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import io.vertx.core.Vertx;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.bela.components.Counter;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.context.BelaContext;
import org.hyperledger.bela.context.MainNetContext;
import org.hyperledger.bela.dialogs.BelaDialog;
import org.hyperledger.bela.dialogs.BelaExceptionDialog;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.besu.BesuInfo;
import org.hyperledger.besu.crypto.KeyPair;
import org.hyperledger.besu.crypto.KeyPairSecurityModule;
import org.hyperledger.besu.crypto.NodeKey;
import org.hyperledger.besu.crypto.SignatureAlgorithm;
import org.hyperledger.besu.crypto.SignatureAlgorithmFactory;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.eth.EthProtocol;
import org.hyperledger.besu.ethereum.eth.manager.EthContext;
import org.hyperledger.besu.ethereum.eth.manager.task.GetBodiesFromPeerTask;
import org.hyperledger.besu.ethereum.eth.manager.task.RetryingGetHeadersEndingAtFromPeerByHashTask;
import org.hyperledger.besu.ethereum.eth.messages.GetBlockHeadersMessage;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;
import org.hyperledger.besu.ethereum.p2p.config.NetworkingConfiguration;
import org.hyperledger.besu.ethereum.p2p.config.RlpxConfiguration;
import org.hyperledger.besu.ethereum.p2p.network.DefaultP2PNetwork;
import org.hyperledger.besu.ethereum.p2p.network.P2PNetwork;
import org.hyperledger.besu.ethereum.p2p.rlpx.ConnectCallback;
import org.hyperledger.besu.ethereum.p2p.rlpx.DisconnectCallback;
import org.hyperledger.besu.ethereum.p2p.rlpx.MessageCallback;
import org.hyperledger.besu.ethereum.p2p.rlpx.connections.PeerConnection;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.Capability;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.Message;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.SubProtocol;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.messages.DisconnectMessage;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.plugin.data.EnodeURL;
import org.hyperledger.besu.plugin.services.MetricsSystem;

import static kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory.getLogger;
import static org.hyperledger.bela.windows.Constants.KEY_CLOSE;

public class P2PManagementWindow implements BelaWindow, MessageCallback, ConnectCallback, DisconnectCallback {
    private static final LambdaLogger log = getLogger(P2PManagementWindow.class);

    private final StorageProviderFactory storageProviderFactory;
    private Preferences preferences;
    private P2PNetwork network;
    private WindowBasedTextGUI gui;

    Map<Capability, Counter> counters = new HashMap<>();
    Counter connect = new Counter("connect");
    Counter disconnect = new Counter("disconnect");

    BelaContext belaContext = new MainNetContext();

    public P2PManagementWindow(final WindowBasedTextGUI gui, final StorageProviderFactory storageProviderFactory, final Preferences preferences) {
        this.gui = gui;
        this.storageProviderFactory = storageProviderFactory;
        this.preferences = preferences;
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

            BelaDialog.showMessage(gui,"header", "header for " + hash.toHexString() + " was retrieved");

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            BelaDialog.showException(gui,e);
        }
    }

    private void startP2P() {
        try {

            String dnsDiscoveryUrl;
            String genesisConfig;
            BigInteger networkId;
            List<EnodeURL> bootNodes;
            final String p2pListenInterface = "0.0.0.0";
            final int p2pListenPort = 30302;
            final int maxPeers = 10;

            final List<SubProtocol> subProtocols = getSubProtocols();

            final Optional<String> identityString=Optional.of("bela");
            final boolean limitRemoteWireConnectionsEnabled = false;
            final float fractionRemoteConnectionsAllowed=.5f;



//        BesuControllerBuilder builder = new MainnetBesuControllerBuilder();
//        final BesuController controller = BesuControllerBuilde();

            NetworkingConfiguration networkingConfiguration = NetworkingConfiguration.create();
            final RlpxConfiguration rlpxConfiguration = RlpxConfiguration.create()
                    .setBindHost(p2pListenInterface)
                    .setBindPort(p2pListenPort)
                    .setMaxPeers(maxPeers)
                    .setSupportedProtocols(subProtocols)
                    .setClientId(BesuInfo.nodeName(identityString))
                    .setLimitRemoteWireConnectionsEnabled(limitRemoteWireConnectionsEnabled)
                    .setFractionRemoteWireConnectionsAllowed(fractionRemoteConnectionsAllowed);

            networkingConfiguration.setRlpx(rlpxConfiguration);
//

//
//        final MetricsSystem metricsSystem  = new LanternaMetricsSystem(new PrometheusMetricsSystem());
            if (network != null) {
                network.close();
            }
            final List<Capability> supportedCapabilities = calculateCapabilities(false);
            network = DefaultP2PNetwork.builder()
                    .vertx(Vertx.vertx())
                    .nodeKey(NodeKeyUtils.generate())
                    .config(networkingConfiguration)
                    .supportedCapabilities(supportedCapabilities)
                    .metricsSystem(belaContext.getMetricsSystem())
                    .storageProvider(storageProviderFactory.createProvider())
//                .natService(natService)
//                .randomPeerPriority(randomPeerPriority)
                    .forkIdSupplier(Collections::emptyList)
//                .p2pTLSConfiguration(p2pTLSConfiguration)
                    .build();

//
//        final RlpxAgent build = RlpxAgent.builder().build();


            for (Capability capability : supportedCapabilities) {
                network.subscribe(capability, this);
            }

            network.subscribeConnect(this);
            network.subscribeDisconnect(this);



            network.start();
            new MessageDialogBuilder().setText("P2P started").setTitle("P2P started").build().showDialog(gui);

        } catch (IOException e) {
            log.error("There was an error when starting network", e);
            BelaDialog.showException(gui, e);

        }

    }

    private List<SubProtocol> getSubProtocols() {
        return new ArrayList<>();
    }

    private void stopP2P() {
        try {
            if (network != null) {
                network.close();
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

class NodeKeyUtils {

    public static NodeKey createFrom(final KeyPair keyPair) {
        return new NodeKey(new KeyPairSecurityModule(keyPair));
    }

    public static NodeKey createFrom(final Bytes32 privateKey) {
        final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithmFactory.getInstance();
        final KeyPair keyPair =
                signatureAlgorithm.createKeyPair(signatureAlgorithm.createPrivateKey(privateKey));
        return new NodeKey(new KeyPairSecurityModule(keyPair));
    }

    public static NodeKey generate() {
        return new NodeKey(
                new KeyPairSecurityModule(SignatureAlgorithmFactory.getInstance().generateKeyPair()));
    }
}

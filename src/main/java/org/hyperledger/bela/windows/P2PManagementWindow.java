package org.hyperledger.bela.windows;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import io.vertx.core.Vertx;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.besu.crypto.KeyPair;
import org.hyperledger.besu.crypto.KeyPairSecurityModule;
import org.hyperledger.besu.crypto.NodeKey;
import org.hyperledger.besu.crypto.SignatureAlgorithm;
import org.hyperledger.besu.crypto.SignatureAlgorithmFactory;
import org.hyperledger.besu.ethereum.eth.EthProtocol;
import org.hyperledger.besu.ethereum.p2p.config.NetworkingConfiguration;
import org.hyperledger.besu.ethereum.p2p.network.DefaultP2PNetwork;
import org.hyperledger.besu.ethereum.p2p.network.P2PNetwork;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.Capability;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;

import static kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory.getLogger;
import static org.hyperledger.bela.windows.Constants.KEY_CLOSE;

public class P2PManagementWindow implements LanternaWindow {
private static final LambdaLogger log = getLogger(P2PManagementWindow.class);

    private final StorageProviderFactory storageProviderFactory;
    private P2PNetwork network;
    private WindowBasedTextGUI gui;

    public P2PManagementWindow(final WindowBasedTextGUI gui, final StorageProviderFactory storageProviderFactory) {
        this.gui = gui;
        this.storageProviderFactory = storageProviderFactory;
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
                .addControl("Close", KEY_CLOSE, window::close);
        window.addWindowListener(controls);
        panel.addComponent(controls.createComponent());
        window.setComponent(panel);
        return window;
    }

    private void startP2P() {
        try {

//        BesuControllerBuilder builder = new MainnetBesuControllerBuilder();
//        final BesuController controller = BesuControllerBuilde();

            NetworkingConfiguration networkingConfiguration = NetworkingConfiguration.create();
//

//
//        final MetricsSystem metricsSystem  = new LanternaMetricsSystem(new PrometheusMetricsSystem());
            if (network != null) {
                network.close();
            }
            network = DefaultP2PNetwork.builder()
                    .vertx(Vertx.vertx())
                    .nodeKey(NodeKeyUtils.generate())
                    .config(networkingConfiguration)
                    .supportedCapabilities(calculateCapabilities(false))
                    .metricsSystem(new NoOpMetricsSystem())
                    .storageProvider(storageProviderFactory.createProvider())
//                .natService(natService)
//                .randomPeerPriority(randomPeerPriority)
                    .forkIdSupplier(Collections::emptyList)
//                .p2pTLSConfiguration(p2pTLSConfiguration)
                    .build();

//
//        final RlpxAgent build = RlpxAgent.builder().build();

            network.start();
        } catch (IOException e) {
            log.error("There was an error when starting network",e);
            MessageDialog.showMessageDialog(gui, "error", CharMatcher.javaIsoControl().removeFrom(e.getMessage()));

        }

    }

    private void stopP2P() {
        try {
            if (network != null) {
                network.close();
            }
        } catch (IOException e) {
            log.error("There was an error when stopping the network",e);
            MessageDialog.showMessageDialog(gui, "error", CharMatcher.javaIsoControl().removeFrom(e.getMessage()));
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

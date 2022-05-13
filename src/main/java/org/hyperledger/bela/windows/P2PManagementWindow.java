package org.hyperledger.bela.windows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import io.vertx.core.Vertx;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.components.LanternaMetricsSystem;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.besu.controller.BesuController;
import org.hyperledger.besu.controller.BesuControllerBuilder;
import org.hyperledger.besu.controller.MainnetBesuControllerBuilder;
import org.hyperledger.besu.crypto.KeyPair;
import org.hyperledger.besu.crypto.KeyPairSecurityModule;
import org.hyperledger.besu.crypto.NodeKey;
import org.hyperledger.besu.crypto.SignatureAlgorithm;
import org.hyperledger.besu.crypto.SignatureAlgorithmFactory;
import org.hyperledger.besu.ethereum.eth.EthProtocol;
import org.hyperledger.besu.ethereum.eth.manager.EthProtocolManager;
import org.hyperledger.besu.ethereum.eth.manager.ForkIdManager;
import org.hyperledger.besu.ethereum.p2p.config.NetworkingConfiguration;
import org.hyperledger.besu.ethereum.p2p.network.DefaultP2PNetwork;
import org.hyperledger.besu.ethereum.p2p.network.P2PNetwork;
import org.hyperledger.besu.ethereum.p2p.permissions.PeerPermissions;
import org.hyperledger.besu.ethereum.p2p.rlpx.RlpxAgent;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.Capability;
import org.hyperledger.besu.metrics.MetricsSystemFactory;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.metrics.prometheus.PrometheusMetricsSystem;
import org.hyperledger.besu.plugin.services.MetricsSystem;

import static org.hyperledger.bela.windows.Constants.KEY_CLOSE;

public class P2PManagementWindow implements LanternaWindow {

    private final StorageProviderFactory storageProviderFactory;

    public P2PManagementWindow(final StorageProviderFactory storageProviderFactory) {
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
                .addControl("Close", KEY_CLOSE, window::close);
        window.addWindowListener(controls);
        panel.addComponent(controls.createComponent());
        window.setComponent(panel);
        return window;
    }

    private void startP2P() {

//        BesuControllerBuilder builder = new MainnetBesuControllerBuilder();
//        final BesuController controller = BesuControllerBuilde();

        final Vertx vertx = Vertx.vertx();
        final NodeKey nodeKey = NodeKeyUtils.generate();
        NetworkingConfiguration networkingConfiguration = NetworkingConfiguration.create();
//

//
//        final MetricsSystem metricsSystem  = new LanternaMetricsSystem(new PrometheusMetricsSystem());
        final P2PNetwork network = DefaultP2PNetwork.builder()
                .vertx(vertx)
                .nodeKey(nodeKey)
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

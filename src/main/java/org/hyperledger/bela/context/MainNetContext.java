package org.hyperledger.bela.context;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.vertx.core.Vertx;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.besu.BesuInfo;
import org.hyperledger.besu.config.GenesisConfigFile;
import org.hyperledger.besu.config.GenesisConfigOptions;
import org.hyperledger.besu.config.JsonGenesisConfigOptions;
import org.hyperledger.besu.ethereum.core.PrivacyParameters;
import org.hyperledger.besu.ethereum.eth.EthProtocol;
import org.hyperledger.besu.ethereum.eth.manager.EthContext;
import org.hyperledger.besu.ethereum.eth.manager.EthMessages;
import org.hyperledger.besu.ethereum.eth.manager.EthPeers;
import org.hyperledger.besu.ethereum.eth.manager.EthScheduler;
import org.hyperledger.besu.ethereum.mainnet.MainnetProtocolSchedule;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;
import org.hyperledger.besu.ethereum.p2p.config.NetworkingConfiguration;
import org.hyperledger.besu.ethereum.p2p.config.RlpxConfiguration;
import org.hyperledger.besu.ethereum.p2p.network.DefaultP2PNetwork;
import org.hyperledger.besu.ethereum.p2p.network.P2PNetwork;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.Capability;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.SubProtocol;
import org.hyperledger.besu.evm.internal.EvmConfiguration;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.plugin.data.EnodeURL;
import org.hyperledger.besu.plugin.services.MetricsSystem;

import static org.hyperledger.besu.config.JsonUtil.normalizeKeys;

public class MainNetContext implements BelaContext {
    private static final BigInteger CHAIN_ID = BigInteger.ONE;
    private P2PNetwork network;
    final StorageProviderFactory storageProviderFactory;

    public MainNetContext(final StorageProviderFactory storageProviderFactory) {
        this.storageProviderFactory = storageProviderFactory;
    }

    private static GenesisConfigOptions getMainNetConfigOptions() {
        // this method avoids reading all the alloc accounts when all we want is the "config" section
        try (final JsonParser jsonParser =
                     new JsonFactory().createParser(GenesisConfigFile.class.getResource("/mainnet.json"))) {

            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                if ("config".equals(jsonParser.getCurrentName())) {
                    jsonParser.nextToken();
                    return JsonGenesisConfigOptions.fromJsonObject(
                            normalizeKeys(new ObjectMapper().readTree(jsonParser)));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed open or parse mainnet genesis json", e);
        }
        throw new IllegalArgumentException("mainnet json file had no config section");
    }

    @Override
    public ProtocolSchedule getProtocolSchedule() {
        return MainnetProtocolSchedule.fromConfig(
                getMainNetConfigOptions(), PrivacyParameters.DEFAULT, false, EvmConfiguration.DEFAULT);
    }

    @Override
    public EthContext getEthContext() {
        final Clock clock = Clock.systemUTC();
        final EthPeers ethPeers =
                new EthPeers(
                        "eth",
                        clock,
                        getMetricsSystem(),
                        10,
                        1000,
                        Collections.emptyList());
        final EthScheduler scheduler = new EthScheduler(1, 1, 1, getMetricsSystem());
        return new EthContext(ethPeers, new EthMessages(), new EthMessages(), scheduler);
    }

    @Override
    public MetricsSystem getMetricsSystem() {
        return new NoOpMetricsSystem();
    }

    @Override
    public P2PNetwork getP2PNetwork() {
        if (network!= null){
            return network;
        }

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

        final List<Capability> supportedCapabilities = getSupportedCapabilities(false);
        network = DefaultP2PNetwork.builder()
                .vertx(Vertx.vertx())
                .nodeKey(NodeKeyUtils.generate())
                .config(networkingConfiguration)
                .supportedCapabilities(supportedCapabilities)
                .metricsSystem(getMetricsSystem())
                .storageProvider(storageProviderFactory.createProvider())
//                .natService(natService)
//                .randomPeerPriority(randomPeerPriority)
                .forkIdSupplier(Collections::emptyList)
//                .p2pTLSConfiguration(p2pTLSConfiguration)
                .build();

        return network;
    }

    @Override
    public List<Capability> getSupportedCapabilities() {
        return getSupportedCapabilities(false);
    }


    private List<SubProtocol> getSubProtocols() {
        return new ArrayList<>();
    }

    public List<Capability> getSupportedCapabilities(final boolean fastSyncEnabled) {
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

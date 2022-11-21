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
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.besu.BesuInfo;
import org.hyperledger.besu.config.GenesisConfigFile;
import org.hyperledger.besu.config.GenesisConfigOptions;
import org.hyperledger.besu.config.JsonGenesisConfigOptions;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.bonsai.BonsaiWorldStateArchive;
import org.hyperledger.besu.ethereum.bonsai.BonsaiWorldStateKeyValueStorage;
import org.hyperledger.besu.ethereum.bonsai.LayeredTrieLogManager;
import org.hyperledger.besu.ethereum.bonsai.TrieLogManager;
import org.hyperledger.besu.ethereum.chain.BlockchainStorage;
import org.hyperledger.besu.ethereum.chain.DefaultBlockchain;
import org.hyperledger.besu.ethereum.chain.MutableBlockchain;
import org.hyperledger.besu.ethereum.core.MiningParameters;
import org.hyperledger.besu.ethereum.core.PrivacyParameters;
import org.hyperledger.besu.ethereum.core.Synchronizer;
import org.hyperledger.besu.ethereum.eth.EthProtocol;
import org.hyperledger.besu.ethereum.eth.EthProtocolConfiguration;
import org.hyperledger.besu.ethereum.eth.manager.EthContext;
import org.hyperledger.besu.ethereum.eth.manager.EthMessages;
import org.hyperledger.besu.ethereum.eth.manager.EthPeers;
import org.hyperledger.besu.ethereum.eth.manager.EthProtocolManager;
import org.hyperledger.besu.ethereum.eth.manager.EthScheduler;
import org.hyperledger.besu.ethereum.eth.manager.MergePeerFilter;
import org.hyperledger.besu.ethereum.eth.peervalidation.PeerValidator;
import org.hyperledger.besu.ethereum.eth.sync.SynchronizerConfiguration;
import org.hyperledger.besu.ethereum.eth.sync.state.SyncState;
import org.hyperledger.besu.ethereum.eth.transactions.ImmutableTransactionPoolConfiguration;
import org.hyperledger.besu.ethereum.eth.transactions.TransactionPool;
import org.hyperledger.besu.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.hyperledger.besu.ethereum.eth.transactions.TransactionPoolFactory;
import org.hyperledger.besu.ethereum.forkid.ForkIdManager;
import org.hyperledger.besu.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.hyperledger.besu.ethereum.mainnet.MainnetProtocolSchedule;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;
import org.hyperledger.besu.ethereum.p2p.config.NetworkingConfiguration;
import org.hyperledger.besu.ethereum.p2p.config.RlpxConfiguration;
import org.hyperledger.besu.ethereum.p2p.network.DefaultP2PNetwork;
import org.hyperledger.besu.ethereum.p2p.network.P2PNetwork;
import org.hyperledger.besu.ethereum.p2p.network.ProtocolManager;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.Capability;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.SubProtocol;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.messages.DisconnectMessage;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage;
import org.hyperledger.besu.ethereum.worldstate.DataStorageConfiguration;
import org.hyperledger.besu.ethereum.worldstate.WorldStateArchive;
import org.hyperledger.besu.evm.internal.EvmConfiguration;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.plugin.data.EnodeURL;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;
import org.jetbrains.annotations.NotNull;

import static org.hyperledger.besu.config.JsonUtil.normalizeKeys;
import static org.hyperledger.besu.ethereum.core.MiningParameters.DEFAULT_MAX_OMMERS_DEPTH;
import static org.hyperledger.besu.ethereum.core.MiningParameters.DEFAULT_POW_JOB_TTL;
import static org.hyperledger.besu.ethereum.core.MiningParameters.DEFAULT_REMOTE_SEALERS_LIMIT;
import static org.hyperledger.besu.ethereum.core.MiningParameters.DEFAULT_REMOTE_SEALERS_TTL;
import static org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier.BLOCKCHAIN;

public class MainNetContext implements BelaContext {
    private static final BigInteger CHAIN_ID = BigInteger.ONE;
    private P2PNetwork network;
    final StorageProviderFactory storageProviderFactory;
    private EthContext ethContext;
    private NoOpMetricsSystem metricsSystem;
    private EthProtocolManager protocolManager;

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
        if (ethContext!= null){
            return ethContext;
        }
        final Clock clock = getClock();
        final EthPeers ethPeers =
                new EthPeers(
                        "eth",
                        clock,
                        getMetricsSystem(),
                        10,
                        1000,
                        Collections.emptyList());
        final EthScheduler scheduler = getEthScheduler();
        ethContext = new EthContext(ethPeers, new EthMessages(), new EthMessages(), scheduler);
        return ethContext;
    }

    private Clock getClock() {
        return Clock.systemUTC();
    }

    @NotNull
    private EthScheduler getEthScheduler() {
        return new EthScheduler(1, 1, 1, getMetricsSystem());
    }

    @Override
    public MetricsSystem getMetricsSystem() {
        if (metricsSystem != null){
            return metricsSystem;
        }
        metricsSystem = new NoOpMetricsSystem();
        return metricsSystem;
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
                .setPeerUpperBound(maxPeers)
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
//                .p2pTLSConfiguration(p2pTLSConfiguration)
                .build();

        network = new BelaP2PNetworkFacade(network);

        network.subscribeConnect(connection -> {
            if (Collections.disjoint(
                    connection.getAgreedCapabilities(), getSupportedCapabilities())) {
                return;
            }
            getEthContext().getEthPeers().registerConnection(connection, getPeerValidators());

        });
        network.subscribeDisconnect((connection, reason, initiatedByPeer) -> {
                getEthContext().getEthPeers().registerDisconnect(connection);
        });

        return network;
    }



    private List<PeerValidator> getPeerValidators() {
        return new ArrayList<>();
    }

    @Override
    public List<Capability> getSupportedCapabilities() {
        return getSupportedCapabilities(false);
    }


    public List<SubProtocol> getSubProtocols() {
        final ArrayList<SubProtocol> subProtocols = new ArrayList<>();
        subProtocols.add(EthProtocol.get());
        return subProtocols;
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

    public ProtocolManager getProtocolManager() {
        if (protocolManager != null){
            return protocolManager;
        }
        protocolManager = new EthProtocolManager(
                getBlockChain(),
                CHAIN_ID,
                getWorldStateArchive(),
                getTransactionPool(),
                getEthProtocolConfiguration(),
                getEthContext().getEthPeers(),
                getEthContext().getEthMessages(),
                ethContext,
                getPeerValidators(),
                getMergePeerFilter(),
                new SynchronizerConfiguration.Builder().build(),
                getEthScheduler(),
                getForkIdManager());
        return protocolManager;
    }

    private ForkIdManager getForkIdManager() {
        return new ForkIdManager(
                getBlockChain(),
                Collections.emptyList(),
                getEthProtocolConfiguration().isLegacyEth64ForkIdEnabled());
    }

    private Optional<MergePeerFilter> getMergePeerFilter() {
        return Optional.empty();
    }

    private EthProtocolConfiguration getEthProtocolConfiguration() {
        return EthProtocolConfiguration.builder().build();
    }

    private TransactionPool getTransactionPool() {
        return TransactionPoolFactory.createTransactionPool(
                getProtocolSchedule(),
                getProtocolContext(),
                getEthContext(),
                getClock(),
                getMetricsSystem(),
                getSyncState(),
                getMiningParameters(),
                getTransactionPoolConfiguration()
        );
    }

    private TransactionPoolConfiguration getTransactionPoolConfiguration() {
        return ImmutableTransactionPoolConfiguration.DEFAULT;
    }

    private MiningParameters getMiningParameters() {
        final Wei minTransactionGasPrice = Wei.ZERO;
        // Extradata and coinbase can be configured on a per-block level via the json file
        final Address coinbase = Address.ZERO;
        final Bytes extraData = Bytes.EMPTY;
        return new MiningParameters.Builder()
                .coinbase(coinbase)
                .minTransactionGasPrice(minTransactionGasPrice)
                .extraData(extraData)
                .miningEnabled(false)
                .stratumMiningEnabled(false)
                .stratumNetworkInterface("0.0.0.0")
                .stratumPort(8008)
                .stratumExtranonce("080c")
                .maybeNonceGenerator(new IncrementingNonceGenerator(0))
                .minBlockOccupancyRatio(0.0)
                .remoteSealersLimit(DEFAULT_REMOTE_SEALERS_LIMIT)
                .remoteSealersTimeToLive(DEFAULT_REMOTE_SEALERS_TTL)
                .powJobTimeToLive(DEFAULT_POW_JOB_TTL)
                .maxOmmerDepth(DEFAULT_MAX_OMMERS_DEPTH)
                .build();
    }

    private ProtocolContext getProtocolContext() {
        return ProtocolContext.init(getBlockChain(), getWorldStateArchive(), getProtocolSchedule(),
                (blockchain, worldStateArchive, protocolSchedule) -> null);
    }

    private WorldStateArchive getWorldStateArchive() {
        return new BonsaiWorldStateArchive(getProvider(), getBlockChain());
    }

    private BonsaiWorldStateKeyValueStorage getWorldStateStorage() {
        return new BonsaiWorldStateKeyValueStorage(getProvider());
    }

    private MutableBlockchain getBlockChain() {
        return   (MutableBlockchain) DefaultBlockchain
                .create(getBlockChainStorage(), new NoOpMetricsSystem(), 0L);
    }

    private BlockchainStorage getBlockChainStorage() {
        final KeyValueStorage keyValueStorage = getProvider().getStorageBySegmentIdentifier(BLOCKCHAIN);
        return new KeyValueStoragePrefixedKeyBlockchainStorage(keyValueStorage, new MainnetBlockHeaderFunctions());
    }

    private StorageProvider getProvider() {
        return storageProviderFactory.createProvider();
    }

    private SyncState getSyncState() {
        return new SyncState(getBlockChain(), getEthContext().getEthPeers());
    }
}

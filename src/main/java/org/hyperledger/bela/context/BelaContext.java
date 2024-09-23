package org.hyperledger.bela.context;

import java.nio.file.Path;
import java.util.List;

import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.besu.ethereum.chain.MutableBlockchain;
import org.hyperledger.besu.ethereum.eth.manager.EthContext;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;
import org.hyperledger.besu.ethereum.p2p.network.P2PNetwork;
import org.hyperledger.besu.ethereum.p2p.network.ProtocolManager;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.Capability;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.SubProtocol;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.storage.DataStorageFormat;

public interface BelaContext {
    ProtocolSchedule getProtocolSchedule();
    EthContext getEthContext();
    MetricsSystem getMetricsSystem();
    P2PNetwork getP2PNetwork();

    List<Capability> getSupportedCapabilities();

    ProtocolManager getProtocolManager();

    List<SubProtocol> getSubProtocols();

    StorageProvider getProvider();

    StorageProviderFactory getStorageProviderFactory();

    DataStorageFormat getDataStorageFormat();

    MutableBlockchain getBlockChain();

    Path getDataPath();

}

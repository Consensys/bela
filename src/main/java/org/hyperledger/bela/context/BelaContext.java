package org.hyperledger.bela.context;

import org.hyperledger.besu.ethereum.eth.manager.EthContext;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;
import org.hyperledger.besu.plugin.services.MetricsSystem;

public interface BelaContext {
    ProtocolSchedule getProtocolSchedule();
    EthContext getEthContext();
    MetricsSystem getMetricsSystem();
}

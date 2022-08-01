package org.hyperledger.bela.context;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Clock;
import java.util.Collections;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyperledger.besu.config.GenesisConfigFile;
import org.hyperledger.besu.config.GenesisConfigOptions;
import org.hyperledger.besu.config.JsonGenesisConfigOptions;
import org.hyperledger.besu.ethereum.core.PrivacyParameters;
import org.hyperledger.besu.ethereum.eth.manager.EthContext;
import org.hyperledger.besu.ethereum.eth.manager.EthMessages;
import org.hyperledger.besu.ethereum.eth.manager.EthPeers;
import org.hyperledger.besu.ethereum.eth.manager.EthScheduler;
import org.hyperledger.besu.ethereum.mainnet.MainnetProtocolSchedule;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;
import org.hyperledger.besu.evm.internal.EvmConfiguration;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.plugin.services.MetricsSystem;

import static org.hyperledger.besu.config.JsonUtil.normalizeKeys;

public class MainNetContext implements BelaContext {
    private static final BigInteger CHAIN_ID = BigInteger.ONE;

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
}

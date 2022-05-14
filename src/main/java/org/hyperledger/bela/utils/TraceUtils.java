package org.hyperledger.bela.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.prefs.Preferences;
import java.util.stream.Stream;
import org.hyperledger.bela.windows.Constants;
import org.hyperledger.besu.cli.config.EthNetworkConfig;
import org.hyperledger.besu.cli.config.NetworkName;
import org.hyperledger.besu.config.GenesisConfigFile;
import org.hyperledger.besu.config.GenesisConfigOptions;
import org.hyperledger.besu.consensus.clique.CliqueProtocolSchedule;
import org.hyperledger.besu.consensus.ibft.IbftExtraDataCodec;
import org.hyperledger.besu.consensus.ibft.IbftForksSchedulesFactory;
import org.hyperledger.besu.consensus.ibft.IbftProtocolSchedule;
import org.hyperledger.besu.consensus.qbft.QbftExtraDataCodec;
import org.hyperledger.besu.consensus.qbft.QbftForksSchedulesFactory;
import org.hyperledger.besu.consensus.qbft.QbftProtocolSchedule;
import org.hyperledger.besu.crypto.KeyPairSecurityModule;
import org.hyperledger.besu.crypto.KeyPairUtil;
import org.hyperledger.besu.crypto.NodeKey;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.methods.TraceTransaction;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.processor.BlockReplay;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.processor.BlockTracer;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.tracing.flat.FlatTrace;
import org.hyperledger.besu.ethereum.api.query.BlockchainQueries;
import org.hyperledger.besu.ethereum.core.PrivacyParameters;
import org.hyperledger.besu.ethereum.mainnet.MainnetProtocolSchedule;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;
import org.hyperledger.besu.evm.internal.EvmConfiguration;

import static org.hyperledger.bela.windows.Constants.NODE_KEY;
import static org.hyperledger.bela.windows.Constants.NODE_PATH_DEFAULT;

public class TraceUtils {

    public static Stream<FlatTrace> traceTransaction(final Preferences preferences,
                                                     final BlockChainContext context, final Hash transactionHash) {
        final String genesisConfig = getGenesisConfig(preferences);
        final ProtocolSchedule protocolSchedule = getProtocolSchedule(preferences, genesisConfig);
        var blockchain = context.getBlockchain();
        var bonsaiWorldStateArchive = context.getBonsaiWorldStateArchive();
        final BlockReplay blockReplay = new BlockReplay(protocolSchedule, blockchain,
                bonsaiWorldStateArchive);
        final BlockchainQueries blockchainQueries = new BlockchainQueries(blockchain,
                bonsaiWorldStateArchive);
        final TraceTransaction traceTransaction = new TraceTransaction(
                () -> new BlockTracer(blockReplay), protocolSchedule, blockchainQueries);
        return traceTransaction.resultByTransactionHash(transactionHash);
    }

    private static String getGenesisConfig(final Preferences preferences) {
        final String networkOrGenesisPath = preferences.get(Constants.GENESIS_PATH, "");
        final Optional<NetworkName> maybeNetworkName = Arrays.stream(NetworkName.values())
                .filter(n -> n.name().equalsIgnoreCase(networkOrGenesisPath))
                .findFirst();
        return maybeNetworkName.map(EthNetworkConfig::getNetworkConfig)
                .map(EthNetworkConfig::getGenesisConfig)
                .orElseGet(() -> {
                            try {
                                return Files.readString(Path.of(networkOrGenesisPath));
                            } catch (IOException e) {
                                throw new IllegalStateException("Unable to load genesis file " + networkOrGenesisPath);
                            }
                        }
                );
    }

    private static ProtocolSchedule getProtocolSchedule(final Preferences preferences,
                                                        final String genesisConfigString) {
        final GenesisConfigFile config = GenesisConfigFile.fromConfig(
                genesisConfigString);
        final GenesisConfigOptions configOptions = config.getConfigOptions();
        if (configOptions.isEthHash()) {
            return MainnetProtocolSchedule.fromConfig(configOptions, true,
                    EvmConfiguration.DEFAULT);
        } else if (configOptions.isClique()) {
            final Path nodeKeyPath = Path.of(preferences.get(NODE_KEY, NODE_PATH_DEFAULT));
            final NodeKey nodeKey = new NodeKey(
                    new KeyPairSecurityModule(KeyPairUtil.loadKeyPair(nodeKeyPath)));
            return CliqueProtocolSchedule.create(configOptions, nodeKey, true,
                    EvmConfiguration.DEFAULT);
        } else if (configOptions.isIbft2()) {
            var forkSchedule = IbftForksSchedulesFactory.create(configOptions);
            return IbftProtocolSchedule.create(configOptions, forkSchedule, PrivacyParameters.DEFAULT,
                    true, new IbftExtraDataCodec(),
                    EvmConfiguration.DEFAULT);
        } else if (configOptions.isQbft()) {
            var forkSchedule = QbftForksSchedulesFactory.create(configOptions);
            return QbftProtocolSchedule.create(configOptions, forkSchedule, PrivacyParameters.DEFAULT,
                    true, new QbftExtraDataCodec(),
                    EvmConfiguration.DEFAULT);
        } else {
            throw new UnsupportedOperationException("Unknown protocol schedule");
        }
    }

}

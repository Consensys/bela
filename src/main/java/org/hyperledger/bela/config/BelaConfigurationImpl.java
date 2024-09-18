package org.hyperledger.bela.config;

import java.nio.file.Path;
import java.util.Optional;

import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.plugin.services.BesuConfiguration;
import org.hyperledger.besu.ethereum.worldstate.DataStorageConfiguration;
import org.hyperledger.besu.plugin.services.storage.DataStorageFormat;

public class BelaConfigurationImpl implements BesuConfiguration {

    private final Path storagePath;
    private final Path dataPath;
    private final DataStorageConfiguration dataStorageConfiguration;

    public BelaConfigurationImpl(final Path dataPath, final Path storagePath, final DataStorageConfiguration dataStorageConfiguration) {
        this.dataPath = dataPath;
        this.storagePath = storagePath;
        this.dataStorageConfiguration = dataStorageConfiguration;
    }

    @Override
    public Optional<String> getRpcHttpHost() {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getRpcHttpPort() {
        return Optional.empty();
    }

    @Override
    public Path getStoragePath() {
        return storagePath;
    }

    @Override
    public Path getDataPath() {
        return dataPath;
    }

    @Override
    public DataStorageFormat getDatabaseFormat() {
        return null;
    }

    @Override
    public Wei getMinGasPrice() {
        return null;
    }

    @Override
    public org.hyperledger.besu.plugin.services.storage.DataStorageConfiguration getDataStorageConfiguration() {
        return new DataStorageConfigurationImpl(dataStorageConfiguration);
    }

    /**
     * A concrete implementation of DataStorageConfiguration which is used in Besu plugin framework.
     */
    public static class DataStorageConfigurationImpl
            implements org.hyperledger.besu.plugin.services.storage.DataStorageConfiguration {

        private final DataStorageConfiguration dataStorageConfiguration;

        /**
         * Instantiate the concrete implementation of the plugin DataStorageConfiguration.
         *
         * @param dataStorageConfiguration The Ethereum core module data storage configuration
         */
        public DataStorageConfigurationImpl(final DataStorageConfiguration dataStorageConfiguration) {
            this.dataStorageConfiguration = dataStorageConfiguration;
        }

        @Override
        public DataStorageFormat getDatabaseFormat() {
            return dataStorageConfiguration.getDataStorageFormat();
        }

        @Override
        public boolean getReceiptCompactionEnabled() {
            return dataStorageConfiguration.getReceiptCompactionEnabled();
        }
    }
}

package org.hyperledger.bela.config;

import java.nio.file.Path;
import java.util.Optional;

import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.plugin.services.BesuConfiguration;
import org.hyperledger.besu.plugin.services.storage.DataStorageConfiguration;
import org.hyperledger.besu.plugin.services.storage.DataStorageFormat;

public class BelaConfigurationImpl implements BesuConfiguration {

    private final Path storagePath;
    private final Path dataPath;

    public BelaConfigurationImpl(final Path dataPath, final Path storagePath) {
        this.dataPath = dataPath;
        this.storagePath = storagePath;
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
    public DataStorageConfiguration getDataStorageConfiguration() {
        return null;
    }
}

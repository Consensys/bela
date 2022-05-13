package org.hyperledger.bela.utils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.prefs.Preferences;
import org.hyperledger.bela.config.BelaConfigurationImpl;
import org.hyperledger.bela.converter.RocksDBKeyValueStorageConverterFactory;

import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueStorageProviderBuilder;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBKeyValueStorageFactory;
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBMetricsFactory;
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBCLIOptions;
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBFactoryConfiguration;

import static org.hyperledger.bela.windows.Constants.DATA_PATH;
import static org.hyperledger.bela.windows.Constants.DATA_PATH_DEFAULT;
import static org.hyperledger.bela.windows.Constants.STORAGE_PATH;
import static org.hyperledger.bela.windows.Constants.STORAGE_PATH_DEFAULT;

public class StorageProviderFactory {
    private final Preferences preferences;
    private StorageProvider provider;
    private Path dataPath;
    private Path storagePath;

    public StorageProviderFactory(final Preferences preferences) {
        this.preferences = preferences;
    }

    public StorageProvider createProvider() {
        Path data = Path.of(preferences.get(DATA_PATH, DATA_PATH_DEFAULT));
        Path storage = Path.of(preferences.get(STORAGE_PATH, STORAGE_PATH_DEFAULT));
        if (data.equals(dataPath) && storage.equals(storagePath)) {
            return provider;
        }

        if (provider != null) {
            try {
                provider.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        dataPath = data;
        storagePath = storage;
        provider = createKeyValueStorageProvider(dataPath, storagePath);
        return provider;
    }

    private static StorageProvider createKeyValueStorageProvider(
            final Path dataDir, final Path dbDir) {
        return new KeyValueStorageProviderBuilder()
                .withStorageFactory(
                        new RocksDBKeyValueStorageConverterFactory(
                                () ->
                                        new RocksDBFactoryConfiguration(
                                                RocksDBCLIOptions.DEFAULT_MAX_OPEN_FILES,
                                                RocksDBCLIOptions.DEFAULT_MAX_BACKGROUND_COMPACTIONS,
                                                RocksDBCLIOptions.DEFAULT_BACKGROUND_THREAD_COUNT,
                                                RocksDBCLIOptions.DEFAULT_CACHE_CAPACITY),
                                Arrays.asList(KeyValueSegmentIdentifier.values()),
                                RocksDBMetricsFactory.PUBLIC_ROCKS_DB_METRICS))
                .withCommonConfiguration(new BelaConfigurationImpl(dataDir, dbDir))
                .withMetricsSystem(new NoOpMetricsSystem())
                .build();
    }

    public Path getDataPath() {
        return dataPath;
    }

}

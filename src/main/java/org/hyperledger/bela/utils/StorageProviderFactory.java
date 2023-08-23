package org.hyperledger.bela.utils;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import org.hyperledger.bela.config.BelaConfigurationImpl;
import org.hyperledger.bela.converter.RocksDBKeyValueStorageConverterFactory;
import org.hyperledger.bela.dialogs.NonClosableMessage;
import org.hyperledger.bela.utils.hacks.ReadOnlyDatabaseDecider;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueStorageProviderBuilder;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.plugin.services.exception.StorageException;
import org.hyperledger.besu.plugin.services.storage.SegmentIdentifier;
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBMetricsFactory;
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBCLIOptions;
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBFactoryConfiguration;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory.getLogger;
import static org.hyperledger.bela.windows.Constants.DATA_PATH;
import static org.hyperledger.bela.windows.Constants.DATA_PATH_DEFAULT;
import static org.hyperledger.bela.windows.Constants.DETECT_COLUMNS;
import static org.hyperledger.bela.windows.Constants.READ_ONLY_DB;
import static org.hyperledger.bela.windows.Constants.STORAGE_PATH;
import static org.hyperledger.bela.windows.Constants.STORAGE_PATH_DEFAULT;

public class StorageProviderFactory implements AutoCloseable {

    private static final LambdaLogger log = getLogger(StorageProviderFactory.class);

    private final Preferences preferences;
    private final WindowBasedTextGUI gui;
    private StorageProvider provider;
    private Path dataPath;
    private Path storagePath;
    private final Supplier<String> storagePathConfig;
    private final Supplier<String> dataPathConfig;

    public StorageProviderFactory(final WindowBasedTextGUI gui, final Preferences preferences) {
        this.preferences=preferences;
        this.gui = gui;
        this.storagePathConfig = () -> preferences.get(STORAGE_PATH, STORAGE_PATH_DEFAULT);
        this.dataPathConfig = () -> preferences.get(DATA_PATH, DATA_PATH_DEFAULT);

    }

    public StorageProvider createProvider() {
        Path data = Path.of(dataPathConfig.get());
        Path storage = Path.of(storagePathConfig.get());
        if (data.equals(dataPath) && storage.equals(storagePath) && provider != null) {
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

        final NonClosableMessage nonClosableMessage = NonClosableMessage.showMessage(gui, "Creating storage provider...");
        ReadOnlyDatabaseDecider.getInstance().setReadOnly(preferences.getBoolean(READ_ONLY_DB,true));

        if (preferences.getBoolean(DETECT_COLUMNS, true)) {
            provider = createKeyValueStorageProvider(dataPath, storagePath, detect());
        } else {
            provider = createKeyValueStorageProvider(dataPath, storagePath, Arrays.asList(KeyValueSegmentIdentifier.values()));

        }
        gui.removeWindow(nonClosableMessage);
        if (provider == null) {
            throw new RuntimeException("Could not create provider....");
        }
        return provider;
    }

    private static StorageProvider createKeyValueStorageProvider(
            final Path dataDir, final Path dbDir, final List<SegmentIdentifier> segments) {
        return new KeyValueStorageProviderBuilder()
                .withStorageFactory(
                        new RocksDBKeyValueStorageConverterFactory(
                                () ->
                                        new RocksDBFactoryConfiguration(
                                                RocksDBCLIOptions.DEFAULT_MAX_OPEN_FILES,
                                                RocksDBCLIOptions.DEFAULT_BACKGROUND_THREAD_COUNT,
                                                RocksDBCLIOptions.DEFAULT_CACHE_CAPACITY,
                                                RocksDBCLIOptions.DEFAULT_IS_HIGH_SPEC),
                                segments,
                                RocksDBMetricsFactory.PUBLIC_ROCKS_DB_METRICS))
                .withCommonConfiguration(new BelaConfigurationImpl(dataDir, dbDir))
                .withMetricsSystem(new NoOpMetricsSystem())
                .build();
    }

    public Path getDataPath() {
        return dataPath;
    }

    @Override
    public void close() throws IOException {
        if (provider != null) {
            provider.close();
        }
        dataPath = null;
        storagePath = null;
    }

    public StorageProvider createProvider(final List<SegmentIdentifier> listOfSegments, final boolean readOnly) {
        if (provider != null) {
            try {
                provider.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        ReadOnlyDatabaseDecider.getInstance().setReadOnly(readOnly);

        dataPath = Path.of(preferences.get(DATA_PATH, DATA_PATH_DEFAULT));
        storagePath = Path.of(preferences.get(STORAGE_PATH, STORAGE_PATH_DEFAULT));

        provider = createKeyValueStorageProvider(dataPath, storagePath, listOfSegments);
        if (provider == null) {
            throw new RuntimeException("Could not create provider....");
        }
        return provider;
    }

    public List<SegmentIdentifier> detect() {
        final Set<KeyValueSegmentIdentifier> allSegments = EnumSet.allOf(KeyValueSegmentIdentifier.class);
        try {
            // load all existing column families or rocks will complain:
            var existing = new HashSet<>(RocksDB.listColumnFamilies(new Options(), storagePathConfig.get()));
            return allSegments.stream()
                .filter(seg -> existing.stream().filter(rocksSeg -> Arrays.equals(rocksSeg,seg.getId())).findFirst().isPresent())
                .collect(Collectors.toList());
        } catch(RocksDBException ex) {
            throw new StorageException(ex);
        }
    }

    public StorageProvider createProvider(final List<SegmentIdentifier> listOfSegments) {
        return createProvider(listOfSegments,preferences.getBoolean(READ_ONLY_DB,true));
    }
}

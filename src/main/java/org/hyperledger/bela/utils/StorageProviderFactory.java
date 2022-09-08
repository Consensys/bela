package org.hyperledger.bela.utils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import org.hyperledger.bela.config.BelaConfigurationImpl;
import org.hyperledger.bela.converter.RocksDBKeyValueStorageConverterFactory;
import org.hyperledger.bela.dialogs.BelaDialog;
import org.hyperledger.bela.dialogs.NonClosableMessage;
import org.hyperledger.bela.utils.hacks.ReadOnlyDatabaseDecider;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueStorageProviderBuilder;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.plugin.services.storage.SegmentIdentifier;
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBMetricsFactory;
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBCLIOptions;
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBFactoryConfiguration;
import org.jetbrains.annotations.NotNull;
import org.rocksdb.RocksDBException;

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

    public StorageProviderFactory(final WindowBasedTextGUI gui, final Preferences preferences) {
        this.preferences=preferences;
        this.gui = gui;
    }

    public StorageProvider createProvider() {
        Path data = Path.of(preferences.get(DATA_PATH, DATA_PATH_DEFAULT));
        Path storage = Path.of(preferences.get(STORAGE_PATH, STORAGE_PATH_DEFAULT));
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
                                                RocksDBCLIOptions.DEFAULT_MAX_BACKGROUND_COMPACTIONS,
                                                RocksDBCLIOptions.DEFAULT_BACKGROUND_THREAD_COUNT,
                                                RocksDBCLIOptions.DEFAULT_CACHE_CAPACITY),
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

    public List<SegmentIdentifier> detectReadWrite() {
        try (StorageProvider ignored = createWritableProvider(new ArrayList<>())) {
            close();
            return new ArrayList<>();
        } catch (Exception e) {
            final List<Byte> columns = parseColumns(e);
            final KeyValueSegmentIdentifier[] values = KeyValueSegmentIdentifier.values();
            return columns.stream().map(aByte -> values[aByte - 1]).collect(Collectors.toList());
        }
    }

    private List<SegmentIdentifier> detect() {
        if (preferences.getBoolean(READ_ONLY_DB,true)) {
            return detectReadOnly();
        } else {
            return detectReadWrite();
        }
    }

    private List<SegmentIdentifier> detectReadOnly() {
        final SegmentIdentifier[] listOfSegments = KeyValueSegmentIdentifier.values();
        final List<SegmentIdentifier> detectedSegments = new ArrayList<>();

        for (SegmentIdentifier segment : listOfSegments) {
            try {
                final StorageProvider provider = createProvider(Collections.singletonList(segment), true);
                provider.close();
//                accessLongPropertyForSegment(provider, segment, LongRocksDbProperty.LIVE_SST_FILES_SIZE);
                detectedSegments.add(segment);
            } catch (Exception e) {
                //ignore on purpouse
            }
        }
        return detectedSegments;
    }

    private StorageProvider createWritableProvider(final List<SegmentIdentifier> listOfSegments) {
        return createProvider(listOfSegments, false);
    }

    @NotNull
    private List<Byte> parseColumns(@Nonnull final Exception e) {
        Throwable cause = e;
        while (cause != null && !(cause instanceof RocksDBException)) {
            cause = cause.getCause();
        }
        if (cause == null || cause.getMessage() == null || !cause.getMessage()
                .startsWith("Column families not opened: ")) {
            throw new RuntimeException(e);
        }
        byte[] bytes = cause.getMessage().getBytes();
        List<Byte> columns = new ArrayList<>();
        for (int i = 28 /*Column families not opened: */; i < bytes.length; i += 3 /* ,*/) {
            columns.add(bytes[i]);
        }
        log.info("Columns: {}", columns);
        return columns;
    }

    public StorageProvider createProvider(final List<SegmentIdentifier> listOfSegments) {
        return createProvider(listOfSegments,preferences.getBoolean(READ_ONLY_DB,true));
    }
}

package org.hyperledger.bela.converter;

import org.hyperledger.besu.plugin.services.BesuConfiguration;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.exception.StorageException;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorageFactory;
import org.hyperledger.besu.plugin.services.storage.SegmentIdentifier;
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBMetricsFactory;
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.DatabaseMetadata;
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBConfiguration;
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBConfigurationBuilder;
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBFactoryConfiguration;
import org.hyperledger.besu.plugin.services.storage.rocksdb.segmented.RocksDBColumnarKeyValueStorage;
import org.hyperledger.besu.services.kvstore.SegmentedKeyValueStorage;
import org.hyperledger.besu.services.kvstore.SegmentedKeyValueStorageAdapter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocksDBKeyValueStorageConverterFactory implements KeyValueStorageFactory {

  private static final Logger LOG = LoggerFactory.getLogger(RocksDBKeyValueStorageConverterFactory.class);
  private RocksDBConfiguration rocksDBConfiguration;
  private SegmentedKeyValueStorage<?> segmentedStorage;
  private final List<SegmentIdentifier> segments;
  private final RocksDBMetricsFactory rocksDBMetricsFactory;
  private static final Set<Integer> SUPPORTED_VERSIONS = Set.of(1, 2);



  private final Supplier<RocksDBFactoryConfiguration> configuration;


  public RocksDBKeyValueStorageConverterFactory(
      final Supplier<RocksDBFactoryConfiguration> configuration,
      final List<SegmentIdentifier> segments,
      final RocksDBMetricsFactory rocksDBMetricsFactory) {
    this.configuration = configuration;
    this.segments = segments;
    this.rocksDBMetricsFactory = rocksDBMetricsFactory;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public KeyValueStorage create(
      final SegmentIdentifier segment,
      final BesuConfiguration commonConfiguration,
      final MetricsSystem metricsSystem)
      throws StorageException {

    if (requiresInit()) {
      init(commonConfiguration);
    }
    if (segmentedStorage == null) {
      final List<SegmentIdentifier> segmentsForVersion =
          segments.stream()
              .collect(Collectors.toList());
      segmentedStorage =
          new RocksDBColumnarKeyValueStorage(
              rocksDBConfiguration, segmentsForVersion, metricsSystem, rocksDBMetricsFactory);
    }
    return new SegmentedKeyValueStorageAdapter<>(segment, segmentedStorage);
  }


  @Override
  public boolean isSegmentIsolationSupported() {
    return false;
  }

  private void init(final BesuConfiguration commonConfiguration) {
    try {
      checkDatabaseVersion(commonConfiguration);
    } catch (final IOException e) {
      throw new StorageException("Failed to retrieve the RocksDB database meta version", e);
    }
    rocksDBConfiguration =
        RocksDBConfigurationBuilder.from(configuration.get())
            .databaseDir(commonConfiguration.getStoragePath())
            .build();
  }

  private boolean requiresInit() {
    return segmentedStorage == null;
  }

  private void checkDatabaseVersion(final BesuConfiguration commonConfiguration) throws IOException {
    final Path dataDir = commonConfiguration.getDataPath();
    final boolean databaseExists = commonConfiguration.getStoragePath().toFile().exists();
    final int databaseVersion;

    if (databaseExists) {
      databaseVersion = DatabaseMetadata.lookUpFrom(dataDir).getVersion();
      LOG.info("Existing database detected at {}. Version {}", dataDir, databaseVersion);
    } else {
      final String message = "No existing database detected at " + dataDir;
      LOG.error(message);
      throw new StorageException(message);
    }

    if (!SUPPORTED_VERSIONS.contains(databaseVersion)) {
      final String message = "Unsupported RocksDB Metadata version of: " + databaseVersion;
      LOG.error(message);
      throw new StorageException(message);
    }

  }

  @Override
  public void close() throws IOException {
    if (segmentedStorage != null) {
      segmentedStorage.close();
    }
  }
}

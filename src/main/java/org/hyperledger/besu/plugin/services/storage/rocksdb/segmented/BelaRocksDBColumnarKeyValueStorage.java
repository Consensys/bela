/*
 * Copyright Hyperledger Besu Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.plugin.services.storage.rocksdb.segmented;

import org.hyperledger.bela.utils.hacks.ReadOnlyDatabaseDecider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.exception.StorageException;
import org.hyperledger.besu.plugin.services.storage.SegmentIdentifier;
import org.hyperledger.besu.plugin.services.storage.SegmentedKeyValueStorageTransaction;
import org.hyperledger.besu.plugin.services.storage.SnappableKeyValueStorage;
import org.hyperledger.besu.plugin.services.storage.SnappedKeyValueStorage;
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBMetricsFactory;
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBTransaction;
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDbSegmentIdentifier;
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBConfiguration;
import org.hyperledger.besu.services.kvstore.LayeredKeyValueStorage;
import org.hyperledger.besu.services.kvstore.SegmentedKeyValueStorageAdapter;
import org.hyperledger.besu.services.kvstore.SegmentedKeyValueStorageTransactionValidatorDecorator;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.OptimisticTransactionDB;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

/** Optimistic RocksDB Columnar key value storage for Bela */
public class BelaRocksDBColumnarKeyValueStorage extends RocksDBColumnarKeyValueStorage
    implements SnappableKeyValueStorage {
  private final RocksDB db;

  /**
   * Instantiates a new Rocks db columnar key value optimistic storage.
   *
   * @param configuration         the configuration
   * @param segments              the segments
   * @param ignorableSegments     the ignorable segments
   * @param metricsSystem         the metrics system
   * @param rocksDBMetricsFactory the rocks db metrics factory
   * @throws StorageException the storage exception
   */
  public BelaRocksDBColumnarKeyValueStorage(final RocksDBConfiguration configuration,
      final List<SegmentIdentifier> segments, final List<SegmentIdentifier> ignorableSegments,
      final MetricsSystem metricsSystem, final RocksDBMetricsFactory rocksDBMetricsFactory)
      throws StorageException {
    super(configuration, segments, ignorableSegments, metricsSystem, rocksDBMetricsFactory);
    try {

      if (ReadOnlyDatabaseDecider.getInstance().isReadOnly()) {
        db = RocksDB.openReadOnly(options, configuration.getDatabaseDir().toString(),
            columnDescriptors, columnHandles);


      } else {
        db = OptimisticTransactionDB.open(options, configuration.getDatabaseDir().toString(),
            columnDescriptors, columnHandles);
      }
      initMetrics();
      initColumnHandles();

    } catch (final RocksDBException e) {
      throw new StorageException(e);
    }
  }

  @Override
  RocksDB getDB() {
    return db;
  }

  /**
   * Start a transaction
   *
   * @return the new transaction started
   * @throws StorageException the storage exception
   */
  @Override
  public SegmentedKeyValueStorageTransaction startTransaction() throws StorageException {
    throwIfClosed();
    if (db instanceof OptimisticTransactionDB tdb) {
      final WriteOptions writeOptions = new WriteOptions();
      writeOptions.setIgnoreMissingColumnFamilies(true);
      return new SegmentedKeyValueStorageTransactionValidatorDecorator(
          new RocksDBTransaction(this::safeColumnHandle, tdb.beginTransaction(writeOptions),
              writeOptions, this.metrics), this.closed::get);
    } else {
      throw new UnsupportedOperationException("RocksDB is not in transaction mode");
    }
  }

  @Override
  public SnappedKeyValueStorage takeSnapshot() {
    return new LayeredKeyValueStorage(new HashMap<>(), this);
  }

  public void remove(SegmentIdentifier segment) {
    RocksDbSegmentIdentifier toRemove = this.columnHandlesBySegmentIdentifier.get(segment);
    try {
      if (toRemove.get() != null) {
        db.dropColumnFamily(toRemove.get());
      }
    } catch (RocksDBException e) {
      throw new StorageException("Failed to drop column family " + segment.getName(), e);
    }
  }
}

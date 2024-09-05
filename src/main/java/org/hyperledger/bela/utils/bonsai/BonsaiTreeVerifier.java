/*
 *
 *  * Copyright Hyperledger Besu Contributors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  * the License. You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  * specific language governing permissions and limitations under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.hyperledger.bela.utils.bonsai;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.bela.config.BelaConfigurationImpl;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueStorageProviderBuilder;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBKeyValueStorageFactory;
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBMetricsFactory;
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBCLIOptions;
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBFactoryConfiguration;

public class BonsaiTreeVerifier implements BonsaiListener {

    private int visited;
    private static final AtomicInteger errorCount = new AtomicInteger(0);

    public static void main(final String[] args) {
        Instant start = Instant.now();
        final Path dataDir = Paths.get(args[0]);
        System.out.println("We are verifying : " + dataDir);
        final StorageProvider provider =
                createKeyValueStorageProvider(dataDir, dataDir.resolve("database"));
        final BonsaiTreeVerifier listener = new BonsaiTreeVerifier();
        BonsaiTraversal tr = new BonsaiTraversal(provider, listener);
        System.out.println();
        System.out.println("ޏ₍ ὸ.ό₎ރ");
        System.out.println(
                "\uD83E\uDD1E\uD83E\uDD1E\uD83E\uDD1E\uD83E\uDD1E\uD83E\uDD1E\uD83E\uDD1E\uD83E\uDD1E");

        try {
            tr.traverse();
            if (errorCount.get() > 0) {
                System.out.println();
                System.out.println();
                System.out.println("Found " + errorCount.get() + " errors (see above)");
                System.out.println();
                System.out.println("Could not verify root " + tr.getRoot() + " with " + listener.getVisited() + " nodes");
            } else {
                System.out.println();
                System.out.println();
                System.out.println("ޏ₍ ὸ.ό₎ރ World state was verified... ޏ₍ ὸ.ό₎ރ");
                System.out.println("Verified root " + tr.getRoot() + " with " + listener.getVisited() + " nodes");
            }
        } catch (Exception e) {
            System.out.println("There was a problem (╯°□°)╯︵ ┻━┻: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("AAAAAAAAAA!!!!!!!");
        Duration duration = Duration.between(start, Instant.now());
        System.out.printf("Duration: %d hours, %d minutes, %d seconds%n", duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
    }

    private int getVisited() {
        return visited;
    }

    private static StorageProvider createKeyValueStorageProvider(
            final Path dataDir, final Path dbDir) {
        return new KeyValueStorageProviderBuilder()
                .withStorageFactory(
                        new RocksDBKeyValueStorageFactory(
                                () ->
                                        new RocksDBFactoryConfiguration(
                                                RocksDBCLIOptions.DEFAULT_MAX_OPEN_FILES,
                                                RocksDBCLIOptions.DEFAULT_BACKGROUND_THREAD_COUNT,
                                                RocksDBCLIOptions.DEFAULT_CACHE_CAPACITY,
                                                RocksDBCLIOptions.DEFAULT_IS_HIGH_SPEC),
                                Arrays.asList(KeyValueSegmentIdentifier.values()),
                                RocksDBMetricsFactory.PUBLIC_ROCKS_DB_METRICS))
                .withCommonConfiguration(new BelaConfigurationImpl(dataDir, dbDir))
                .withMetricsSystem(new NoOpMetricsSystem())
                .build();
    }

    @Override
    public void root(final Bytes32 hash) {
        System.err.println("Working with root " + hash);
    }

    @Override
    public void missingCodeHash(final Hash codeHash, final Hash accountHash) {
        errorCount.incrementAndGet();
        System.err.format(
                "\nmissing code hash %s for account %s",
                codeHash, accountHash);
    }

    @Override
    public void invalidCode(final Hash accountHash, final Hash codeHash, final Hash foundCodeHash) {
        errorCount.incrementAndGet();
        System.err.format(
                "\ninvalid code for account %s (expected %s and found %s)",
                accountHash, codeHash, foundCodeHash);
    }

    @Override
    public void missingValueForNode(final Bytes32 hash) {
        errorCount.incrementAndGet();
        System.err.println("\nMissing value for node " + hash.toHexString());
    }

    @Override
    public void visited(final BonsaiTraversalTrieType type) {
        visited++;
        if (visited % 10000 == 0) {
            System.out.print(type.getText());
        }
        if (visited % 1000000 == 0) {
            System.out.println();
            System.out.println("So far processed " + visited + " nodes");
        }
    }

    @Override
    public void missingAccountTrieForHash(final Bytes32 hash, final Bytes location) {
        errorCount.incrementAndGet();
        System.err.format("\nmissing account trie node for hash %s and location %s", hash, location);
    }

    @Override
    public void invalidAccountTrieForHash(final Bytes32 hash, final Bytes location, final Hash foundHashNode) {
        errorCount.incrementAndGet();
        System.err.format(
                "\ninvalid account trie node for hash %s and location %s (found %s)",
                hash, location, foundHashNode);
    }

    @Override
    public void missingStorageTrieForHash(final Bytes32 accountHash, final Bytes32 hash, final Bytes location) {
        errorCount.incrementAndGet();
        System.err.format("account hash %s missing storage trie node for hash %s and location %s", accountHash, hash, location);
    }

    @Override
    public void invalidStorageTrieForHash(final Bytes32 accountHash, final Bytes32 hash, final Bytes location, final Hash foundHashNode) {
        errorCount.incrementAndGet();
        System.err.format(
                "\ninvalid storage trie node for account %s hash %s and location %s (found %s)",
                accountHash, hash, location, foundHashNode);
    }

    @Override
    public void differentDataInFlatDatabaseForAccount(final Hash accountHash) {
        errorCount.incrementAndGet();
        System.err.format("\ninconsistent data in flat database for account %s", accountHash);
    }

    @Override
    public void differentDataInFlatDatabaseForStorage(final Bytes32 accountHash, final Bytes32 slotHash) {
        errorCount.incrementAndGet();
        System.err.format("inconsistent data in flat database for account %s on slot %s", accountHash, slotHash);

    }
}


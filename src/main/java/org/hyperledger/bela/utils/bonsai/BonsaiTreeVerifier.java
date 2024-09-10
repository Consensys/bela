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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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

    private static boolean VERBOSE = false;
    private final AtomicLong visited = new AtomicLong(0);
    private static final AtomicInteger errorCount = new AtomicInteger(0);

    /*
        ./bonsaitreeverifier /data/besu --verbose
     */
    public static void main(final String[] args) {
        Instant start = Instant.now();
        final Path dataDir = Paths.get(args[0]);
        String verboseArg = args.length > 1 ? args[1] : "";
        if (verboseArg.startsWith("--verbose") || verboseArg.startsWith("-v")) {
            System.out.println("Verbose mode enabled, tree traversal progress will be printed");
            VERBOSE = true;
        } else {
            System.out.println("Verbose mode disabled, tree traversal progress will not be printed but errors will.");
            System.out.println("Enable verbose mode with -v or --verbose as the second argument");
        }
        System.out.println();
        System.out.println("We are verifying : " + dataDir);
        final StorageProvider provider =
                createKeyValueStorageProvider(dataDir, dataDir.resolve("database"));
        final BonsaiTreeVerifier listener = new BonsaiTreeVerifier();
        BonsaiTraversal tr = new BonsaiTraversal(provider, listener);
        System.out.println();
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
                System.out.println("World state was successfully verified...");
                System.out.println("Verified root " + tr.getRoot() + " with " + listener.getVisited() + " nodes");
            }
        } catch (Exception e) {
            System.out.println("There was a problem (╯°□°)╯︵ ┻━┻: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println();
        Duration duration = Duration.between(start, Instant.now());
        System.out.printf("Duration: %d hours, %d minutes, %d seconds%n", duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
    }

    private long getVisited() {
        return visited.get();
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
                "\nmissing code hash %s for account %s\n",
                codeHash, accountHash);
    }

    @Override
    public void invalidCode(final Hash accountHash, final Hash codeHash, final Hash foundCodeHash) {
        errorCount.incrementAndGet();
        System.err.format(
                "\ninvalid code for account %s (expected %s and found %s)\n",
                accountHash, codeHash, foundCodeHash);
    }

    @Override
    public void missingValueForNode(final Bytes32 hash) {
        errorCount.incrementAndGet();
        System.err.format("\nMissing value for node %s\n", hash.toHexString());
    }

    @Override
    public void visited(final BonsaiTraversalTrieType type) {
        long currentVisited = visited.incrementAndGet();
        if (VERBOSE) {
            if (currentVisited % 10000 == 0) {
                System.out.print(type.getText());
            }
            if (currentVisited % 1000000 == 0) {
                System.out.println();
                System.out.println("So far processed " + visited.get() + " nodes");
            }
        }
    }

    @Override
    public void missingAccountTrieForHash(final Bytes32 hash, final Bytes location) {
        errorCount.incrementAndGet();
        System.err.format("\nmissing account trie node for hash %s and location %s\n", hash, location);
    }

    @Override
    public void invalidAccountTrieForHash(final Bytes32 hash, final Bytes location, final Hash foundHashNode) {
        errorCount.incrementAndGet();
        System.err.format(
                "\ninvalid account trie node for hash %s and location %s (found %s)\n",
                hash, location, foundHashNode);
    }

    @Override
    public void missingStorageTrieForHash(final Bytes32 accountHash, final Bytes32 hash, final Bytes location) {
        errorCount.incrementAndGet();
        System.err.format("\naccount hash %s missing storage trie node for hash %s and location %s\n", accountHash, hash, location);
    }

    @Override
    public void invalidStorageTrieForHash(final Bytes32 accountHash, final Bytes32 hash, final Bytes location, final Hash foundHashNode) {
        errorCount.incrementAndGet();
        System.err.format(
                "\ninvalid storage trie node for account %s hash %s and location %s (found %s)\n",
                accountHash, hash, location, foundHashNode);
    }

    @Override
    public void differentDataInFlatDatabaseForAccount(final Hash accountHash) {
        errorCount.incrementAndGet();
        System.err.format("\ninconsistent data in flat database for account %s\n", accountHash);
    }

    @Override
    public void differentDataInFlatDatabaseForStorage(final Bytes32 accountHash, final Bytes32 slotHash) {
        errorCount.incrementAndGet();
        System.err.format("inconsistent data in flat database for account %s on slot %s\n", accountHash, slotHash);

    }
}


package org.hyperledger.bela.windows;

import com.googlecode.lanterna.gui2.CheckBox;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import org.apache.commons.io.FileUtils;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.dialogs.BelaDialog;
import org.hyperledger.bela.dialogs.ProgressBarPopup;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.plugin.services.exception.StorageException;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;
import org.hyperledger.besu.plugin.services.storage.SegmentIdentifier;
import org.hyperledger.besu.plugin.services.storage.rocksdb.segmented.BelaRocksDBColumnarKeyValueStorage;
import org.rocksdb.RocksDBException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory.getLogger;
import static org.hyperledger.bela.windows.Constants.KEY_BLOCKCHAIN_SIZES;
import static org.hyperledger.bela.windows.Constants.KEY_DETECT_COLUMNS;
import static org.hyperledger.bela.windows.Constants.KEY_LONG_PROPERTY;
import static org.hyperledger.bela.windows.Constants.KEY_PRUNE_COLUMNS;

enum LongRocksDbProperty {

    NUM_BLOB_FILES("rocksdb.num-blob-files") {
        @Override
        public String format(final long value) {
            return round(value, GIGABYTE, "GB ") + round(value % GIGABYTE, MEGABYTE, "MB ") + round(value % MEGABYTE, KILOBYTE, "KB ") + round(value % KILOBYTE, 1, "B");
        }
    },
    TOTAL_BLOB_FILE_SIZE("rocksdb.total-blob-file-size") {
        @Override
        public String format(final long value) {
            return round(value, GIGABYTE, "GB ") + round(value % GIGABYTE, MEGABYTE, "MB ") + round(value % MEGABYTE, KILOBYTE, "KB ") + round(value % KILOBYTE, 1, "B");
        }
    },
    LIVE_BLOB_FILE_SIZE("rocksdb.live-blob-file-size") {
        @Override
        public String format(final long value) {
            return round(value, GIGABYTE, "GB ") + round(value % GIGABYTE, MEGABYTE, "MB ") + round(value % MEGABYTE, KILOBYTE, "KB ") + round(value % KILOBYTE, 1, "B");
        }
    },
    LIVE_BLOB_FILES_GARBAGESIZE("rocksdb.live-blob-file-garbage-size") {
        @Override
        public String format(final long value) {
            return round(value, GIGABYTE, "GB ") + round(value % GIGABYTE, MEGABYTE, "MB ") + round(value % MEGABYTE, KILOBYTE, "KB ") + round(value % KILOBYTE, 1, "B");
        }
    },
    TOTAL_SST_FILES_SIZE("rocksdb.total-sst-files-size") {
        @Override
        public String format(final long value) {
            return round(value, GIGABYTE, "GB ") + round(value % GIGABYTE, MEGABYTE, "MB ") + round(value % MEGABYTE, KILOBYTE, "KB ") + round(value % KILOBYTE, 1, "B");
        }
    },
    LIVE_SST_FILES_SIZE("rocksdb.live-sst-files-size") {
        @Override
        public String format(final long value) {
            return round(value, GIGABYTE, "GB ") + round(value % GIGABYTE, MEGABYTE, "MB ") + round(value % MEGABYTE, KILOBYTE, "KB ") + round(value % KILOBYTE, 1, "B");
        }
    },
    SIZE_ALL_MEM_TABLES("rocksdb.size-all-mem-tables") {
        @Override
        public String format(final long value) {
            return round(value, GIGABYTE, "GB ") + round(value % GIGABYTE, MEGABYTE, "MB ") + round(value % MEGABYTE, KILOBYTE, "KB ") + round(value % KILOBYTE, 1, "B");
        }
    },
    ROCKSDB_ESTIMATE_NUM_KEYS("rocksdb.estimate-num-keys");


    private static final long KILOBYTE = 1024;
    private static final long MEGABYTE = KILOBYTE * 1024;
    private static final long GIGABYTE = MEGABYTE * 1024;
    private final String name;

    LongRocksDbProperty(final String name) {
        this.name = name;
    }

    private static String round(final long value, final long divisor, final String title) {
        return value / divisor > 0 ? value / divisor + title : "";
    }

    public String getName() {
        return name;
    }

    public String format(final long value) {
        return String.valueOf(value);
    }
}

public class SegmentManipulationWindow extends AbstractBelaWindow {
    private static final LambdaLogger log = getLogger(SegmentManipulationWindow.class);

    private final List<CheckBox> columnCheckBoxes = new ArrayList<>();
    private final Set<SegmentIdentifier> selected = new HashSet<>();
    private final Preferences preferences;
    private final StorageProviderFactory storageProviderFactory;
    private final WindowBasedTextGUI gui;

    public SegmentManipulationWindow(final WindowBasedTextGUI gui, final StorageProviderFactory storageProviderFactory, final Preferences preferences) {
        this.gui = gui;
        this.storageProviderFactory = storageProviderFactory;
        this.preferences = preferences;

        for (KeyValueSegmentIdentifier value : KeyValueSegmentIdentifier.values()) {
            columnCheckBoxes.add(new CheckBox(value.getName()).addListener(checked -> {
                if (checked) {
                    selected.add(value);
                } else selected.remove(value);
            }));
        }
    }

    public static long accessLongPropertyForSegment(StorageProvider provider, final SegmentIdentifier segment, final LongRocksDbProperty longRocksDbProperty) {
        try {
            final BelaRocksDBColumnarKeyValueStorage belaStorage = (BelaRocksDBColumnarKeyValueStorage) provider.getStorageBySegmentIdentifiers(List.of(segment));
            return belaStorage.getLongProperty(segment, longRocksDbProperty.getName());
        } catch (RocksDBException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public String label() {
        return "Segment manipulation";
    }

    @Override
    public MenuGroup group() {
        return MenuGroup.DATABASE;
    }

    @Override
    public KeyControls createControls() {
        return new KeyControls()
                .addControl("Detect", KEY_DETECT_COLUMNS, this::detect)
                .addControl("LongProp", KEY_LONG_PROPERTY, this::getLongProperty)
                .addControl("Blockchain Sizes", KEY_BLOCKCHAIN_SIZES, this::blockchainSizes)
                .addControl("Prune", KEY_PRUNE_COLUMNS, this::prune);
    }

    @Override
    public Panel createMainPanel() {

        Panel panel = new Panel(new LinearLayout());
        columnCheckBoxes.forEach(panel::addComponent);


        return panel;
    }

    private void getLongProperty() {
        BelaDialog.showDelegateListDialog(gui, "Select a property", Arrays.asList(LongRocksDbProperty.values()), LongRocksDbProperty::getName,
                this::getLongProperty);
    }

    private void getLongProperty(final LongRocksDbProperty longRocksDbProperty) {
        try {
            final ArrayList<SegmentIdentifier> listOfSegments = new ArrayList<>(selected);
            final StorageProvider provider = storageProviderFactory.createProvider(listOfSegments);
            provider.close();


            final List<String> segmentInfos = Arrays.stream(KeyValueSegmentIdentifier.values())
                    .filter(selected::contains).map(segment -> {
                        final long longPropertyValue;
                        longPropertyValue = accessLongPropertyForSegment(provider, segment, longRocksDbProperty);
                        return segment.getName() + ": " + longRocksDbProperty.format(longPropertyValue);
                    }).collect(Collectors.toList());


            BelaDialog.showListDialog(gui, "Segments information", segmentInfos);


        } catch (Exception e) {
            BelaDialog.showException(gui, e);
        }

    }

    private void prune() {
        try {
            List<SegmentIdentifier> toRemove = new ArrayList<>(EnumSet.allOf(KeyValueSegmentIdentifier.class));
            toRemove.removeAll(selected);
            detect();
            var storageToRemove = (BelaRocksDBColumnarKeyValueStorage) storageProviderFactory
                .createProvider(toRemove)
                .getStorageBySegmentIdentifiers(selected.stream().toList());

            for (SegmentIdentifier segmentIdentifier : toRemove) {
                storageToRemove.remove(segmentIdentifier);
            }
            detect();
        } catch (Exception e) {
            BelaDialog.showException(gui, e);
        }
    }

    private void detect() {
        columnCheckBoxes.forEach(checkBox -> checkBox.setChecked(false));
        storageProviderFactory.detect()
            .stream()
            .map(SegmentIdentifier::getId)
            .map(Bytes::of)
            .map(Bytes::toInt)
            .forEach(idx -> columnCheckBoxes.get(idx -1).setChecked(true));

    }

    enum BlockchainPrefix {
        VARIABLES,
        BLOCK_HEADER,
        BLOCK_BODY,
        TRANSACTION_RECEIPTS,
        BLOCK_HASH,
        TOTAL_DIFFICULTY,
        TRANSACTION_LOCATION;

        private static final Map<Bytes, BlockchainPrefix> PREFIX_MAPPING = Map.of(
            Bytes.of(1), VARIABLES,
            Bytes.of(2), BLOCK_HEADER,
            Bytes.of(3), BLOCK_BODY,
            Bytes.of(4), TRANSACTION_RECEIPTS,
            Bytes.of(5), BLOCK_HASH,
            Bytes.of(6), TOTAL_DIFFICULTY,
            Bytes.of(7), TRANSACTION_LOCATION
        );

        public static Optional<BlockchainPrefix> fromBytes(final Bytes prefix) {
            return Optional.ofNullable(PREFIX_MAPPING.get(prefix));
        }
    }

    private void blockchainSizes() {
        final StorageProvider provider = storageProviderFactory.createProvider(
            List.of(KeyValueSegmentIdentifier.BLOCKCHAIN));
        final long estimate = accessLongPropertyForSegment(provider,
            KeyValueSegmentIdentifier.BLOCKCHAIN,
            LongRocksDbProperty.ROCKSDB_ESTIMATE_NUM_KEYS);
        final KeyValueStorage blockChainStorage = provider.getStorageBySegmentIdentifier(
            KeyValueSegmentIdentifier.BLOCKCHAIN);

        final ProgressBarPopup progress = ProgressBarPopup.showPopup(gui, "Calculating Sizes",
            (int) estimate);
        final Map<BlockchainPrefix, Long> blockchainSizes = new HashMap<>();

        try {
            blockChainStorage.streamKeys().forEach(key -> {
                final byte[] value = blockChainStorage.get(key).orElse(new byte[]{});
                final Bytes prefix = Bytes.wrap(key, 0, 1);
                final Optional<BlockchainPrefix> blockchainPrefix = BlockchainPrefix.fromBytes(
                    prefix);
                blockchainPrefix.ifPresent(
                    p -> blockchainSizes.merge(p, (long) value.length, Long::sum));
                progress.increment();
            });
        } catch (Exception e) {
            BelaDialog.showException(gui, e);
        } finally {
            progress.close();
        }

        final List<String> segmentInfos = blockchainSizes.entrySet().stream().map(entry ->
                entry.getKey().name() + ": " + FileUtils.byteCountToDisplaySize(entry.getValue()))
            .collect(Collectors.toList());

        BelaDialog.showListDialog(gui, "Blockchain segment information", segmentInfos);
    }
}

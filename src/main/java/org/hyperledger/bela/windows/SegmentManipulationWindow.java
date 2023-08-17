package org.hyperledger.bela.windows;

import java.util.HashMap;
import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
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
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;
import org.hyperledger.besu.plugin.services.storage.SegmentIdentifier;
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDbSegmentIdentifier;
import org.hyperledger.besu.plugin.services.storage.rocksdb.segmented.RocksDBColumnarKeyValueStorage;
import org.hyperledger.besu.services.kvstore.SegmentedKeyValueStorageAdapter;
import org.jetbrains.annotations.NotNull;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.TransactionDB;

import static kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory.getLogger;
import static org.hyperledger.bela.windows.Constants.KEY_BLOCKCHAIN_SIZES;
import static org.hyperledger.bela.windows.Constants.KEY_DETECT_COLUMNS;
import static org.hyperledger.bela.windows.Constants.KEY_LONG_PROPERTY;
import static org.hyperledger.bela.windows.Constants.KEY_PRUNE_COLUMNS;
import static org.hyperledger.bela.windows.Constants.READ_ONLY_DB;

enum LongRocksDbProperty {

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
        final long longPropertyValue;
        try {
            final SegmentedKeyValueStorageAdapter storageBySegmentIdentifier = (SegmentedKeyValueStorageAdapter) provider.getStorageBySegmentIdentifier(segment);
            final Field segmentHandleField = storageBySegmentIdentifier.getClass()
                    .getDeclaredField("segmentHandle");
            segmentHandleField.setAccessible(true);
            final RocksDbSegmentIdentifier identifier = (RocksDbSegmentIdentifier) segmentHandleField.get(storageBySegmentIdentifier);
            final Field storageField = storageBySegmentIdentifier.getClass().getDeclaredField("storage");
            storageField.setAccessible(true);
            final RocksDBColumnarKeyValueStorage s = (RocksDBColumnarKeyValueStorage) storageField.get(storageBySegmentIdentifier);
            final Field dbField = s.getClass().getDeclaredField("db");
            dbField.setAccessible(true);
            final RocksDB db = (RocksDB) dbField.get(s);
            longPropertyValue = db.getLongProperty(identifier.get(), longRocksDbProperty.getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return longPropertyValue;
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
            Set<SegmentIdentifier> toRemove = new HashSet<>(Arrays.asList(KeyValueSegmentIdentifier.values()));
            toRemove.removeAll(selected);
            detect();
            final StorageProvider provider = storageProviderFactory.createProvider(new ArrayList<>(selected));

            for (SegmentIdentifier segmentIdentifier : toRemove) {
                final KeyValueStorage storageBySegmentIdentifier = provider.getStorageBySegmentIdentifier(segmentIdentifier);
                remove(storageBySegmentIdentifier);
            }
            detect();
        } catch (Exception e) {
            BelaDialog.showException(gui, e);
        }
    }

    private void remove(final KeyValueStorage storageBySegmentIdentifier) {
        try {
            final Field segmentHandleField = storageBySegmentIdentifier.getClass().getDeclaredField("segmentHandle");
            segmentHandleField.setAccessible(true);
            final RocksDbSegmentIdentifier identifier = (RocksDbSegmentIdentifier) segmentHandleField.get(storageBySegmentIdentifier);
            if (identifier == null) {
                return;
            }

            Field storageField = storageBySegmentIdentifier.getClass().getDeclaredField("storage");
            storageField.setAccessible(true);
            final RocksDBColumnarKeyValueStorage storage = (RocksDBColumnarKeyValueStorage) storageField.get(storageBySegmentIdentifier);

            Field handlesByNameField = storage.getClass().getDeclaredField("columnHandlesByName");
            handlesByNameField.setAccessible(true);

            final Map<String, RocksDbSegmentIdentifier> columnHandlesByName = (Map<String, RocksDbSegmentIdentifier>) handlesByNameField.get(storage);

            final Field dbField = RocksDbSegmentIdentifier.class.getDeclaredField("db");
            dbField.setAccessible(true);
            final Field referenceField = RocksDbSegmentIdentifier.class.getDeclaredField("reference");
            referenceField.setAccessible(true);

            final Optional<RocksDbSegmentIdentifier> any = columnHandlesByName.values().stream()
                    .filter(e -> e.equals(identifier))
                    .findAny();

            if (any.isPresent()) {
                final TransactionDB db = (TransactionDB) dbField.get(any.get());
                final AtomicReference<ColumnFamilyHandle> ref = (AtomicReference<ColumnFamilyHandle>) referenceField.get(any.get());
                db.dropColumnFamily(ref.get());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void detect() {
        if (preferences.getBoolean(READ_ONLY_DB, true)) {
            detectReadOnly();
        } else {
            detectReadWrite();
        }
    }

    private void detectReadOnly() {
        final List<SegmentIdentifier> listOfSegments = Arrays.asList(KeyValueSegmentIdentifier.values());
        final ProgressBarPopup progress = ProgressBarPopup.showPopup(gui, "Detecting", listOfSegments.size());

        columnCheckBoxes.forEach(checkBox -> checkBox.setChecked(false));
        selected.clear();

        for (SegmentIdentifier segment : listOfSegments) {
            try {
                final StorageProvider provider = storageProviderFactory.createProvider(Collections.singletonList(segment), true);
                provider.close();
//                accessLongPropertyForSegment(provider, segment, LongRocksDbProperty.LIVE_SST_FILES_SIZE);
                selected.add(segment);
                columnCheckBoxes.get(listOfSegments.indexOf(segment)).setChecked(true);
            } catch (Exception e) {
                //ignore on purpouse
            } finally {
                progress.increment();
            }
        }
        progress.close();
    }

    private void detectReadWrite() {
        try {
            final StorageProvider provider = storageProviderFactory.createProvider(new ArrayList<>(), false);
        } catch (Exception e) {
            final List<Byte> columns;
            try {
                columns = parseColumns(e);
                columnCheckBoxes.forEach(checkBox -> checkBox.setChecked(false));
                selected.clear();
                columns.forEach(column -> {
                    CheckBox box = columnCheckBoxes.get(column - 1);
                    box.setChecked(true);
                    selected.add(KeyValueSegmentIdentifier.values()[column - 1]);
                });
            } catch (Exception ex) {
                BelaDialog.showException(gui, e);
            }
        }
    }

    @NotNull
    private List<Byte> parseColumns(@Nonnull final Exception e) throws Exception {
        Throwable cause = e;
        while (cause != null && !(cause instanceof RocksDBException)) {
            cause = cause.getCause();
        }
        if (cause == null || cause.getMessage() == null || !cause.getMessage()
                .startsWith("Column families not opened: ")) {
            throw e;
        }
        byte[] bytes = cause.getMessage().getBytes();
        List<Byte> columns = new ArrayList<>();
        for (int i = 28 /*Column families not opened: */; i < bytes.length; i += 3 /* ,*/) {
            columns.add(bytes[i]);
        }
        log.info("Columns: {}", columns);
        return columns;
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

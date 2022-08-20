package org.hyperledger.bela.windows;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.CheckBox;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.dialogs.BelaDialog;
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
import org.rocksdb.RocksDBException;
import org.rocksdb.TransactionDB;

import static kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory.getLogger;
import static org.hyperledger.bela.windows.Constants.KEY_CLOSE;
import static org.hyperledger.bela.windows.Constants.KEY_DETECT_COLUMNS;
import static org.hyperledger.bela.windows.Constants.KEY_LONG_PROPERTY;
import static org.hyperledger.bela.windows.Constants.KEY_PRUNE_COLUMNS;

enum LongRocksDbProperty {

    TOTAL_SST_FILES_SIZE("rocksdb.total-sst-files-size") {
        @Override
        public String format( final long value) {
            return round(value, GIGABYTE, "GB ") + round(value % GIGABYTE, MEGABYTE, "MB ") + round(value % MEGABYTE, KILOBYTE, "KB ") + round(value % KILOBYTE, 1, "B");
        }
    };

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

public class SegmentManipulationWindow implements BelaWindow {
    private static final LambdaLogger log = getLogger(SegmentManipulationWindow.class);

    private final List<CheckBox> columnCheckBoxes = new ArrayList<>();
    private final Set<SegmentIdentifier> selected = new HashSet<>();
    private StorageProviderFactory storageProviderFactory;
    private WindowBasedTextGUI gui;

    public SegmentManipulationWindow(final WindowBasedTextGUI gui, final StorageProviderFactory storageProviderFactory) {
        this.gui = gui;
        this.storageProviderFactory = storageProviderFactory;

        for (KeyValueSegmentIdentifier value : KeyValueSegmentIdentifier.values()) {
            columnCheckBoxes.add(new CheckBox(value.getName()).addListener(checked -> {
                if (checked) {
                    selected.add(value);
                } else selected.remove(value);
            }));
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
    public Window createWindow() {
        final BasicWindow window = new BasicWindow(label());
        window.setHints(List.of(Window.Hint.FULL_SCREEN));

        Panel panel = new Panel(new LinearLayout());
        window.setComponent(panel);

        KeyControls controls = new KeyControls()
                .addControl("Detect", KEY_DETECT_COLUMNS, this::detect)
                .addControl("LongProp", KEY_LONG_PROPERTY, this::getLongProperty)
                .addControl("Prune", KEY_PRUNE_COLUMNS, this::prune)
                .addControl("Close", KEY_CLOSE, window::close);
        window.addWindowListener(controls);
        panel.addComponent(controls.createComponent());

        columnCheckBoxes.forEach(panel::addComponent);


        return window;
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


            final List<String> segmentInfos = listOfSegments.stream().map(segment -> {
                final SegmentedKeyValueStorageAdapter<RocksDbSegmentIdentifier> storageBySegmentIdentifier = (SegmentedKeyValueStorageAdapter) provider.getStorageBySegmentIdentifier(segment);
                try {
                    final Field segmentHandleField = storageBySegmentIdentifier.getClass()
                            .getDeclaredField("segmentHandle");
                    segmentHandleField.setAccessible(true);
                    final RocksDbSegmentIdentifier identifier = (RocksDbSegmentIdentifier) segmentHandleField.get(storageBySegmentIdentifier);
                    final Field storageField = storageBySegmentIdentifier.getClass().getDeclaredField("storage");
                    storageField.setAccessible(true);
                    final RocksDBColumnarKeyValueStorage s = (RocksDBColumnarKeyValueStorage) storageField.get(storageBySegmentIdentifier);
                    final Field dbField = s.getClass().getDeclaredField("db");
                    dbField.setAccessible(true);
                    final TransactionDB db = (TransactionDB) dbField.get(s);
                    final long longPropertyValue = db.getLongProperty(identifier.get(), longRocksDbProperty.getName());
                    return segment.getName() + ": " + longRocksDbProperty.format(longPropertyValue);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());


            BelaDialog.showListDialog(gui, "Segments information", segmentInfos);


        } catch (Exception e) {
            BelaDialog.showException(gui, e);
        } catch (Throwable t) {
            log.error("There was an error", t);
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
        try {
            storageProviderFactory.createProvider(new ArrayList<>());
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
}

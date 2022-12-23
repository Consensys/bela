package org.hyperledger.bela.windows;

import java.util.Optional;
import java.util.regex.Pattern;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.ComboBox;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.dialogs.BelaDialog;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;

import static org.hyperledger.bela.windows.Constants.KEY_DELETE;
import static org.hyperledger.bela.windows.Constants.KEY_SEARCH;
import static org.hyperledger.bela.windows.Constants.KEY_UPDATE;

public class RocksDBViewer extends AbstractBelaWindow {
    private static final Pattern HEX_ONLY = Pattern.compile("^[0-9A-Fa-f]+$");
    private final ComboBox<KeyValueSegmentIdentifier> identifierCombo = new ComboBox<>(KeyValueSegmentIdentifier.values());
    private final TextBox keyBox = new TextBox(new TerminalSize(80, 1));
    private final TextBox valueBox = new TextBox(new TerminalSize(80, 1));
    private final StorageProviderFactory storageProviderFactory;
    private final WindowBasedTextGUI gui;

    public RocksDBViewer(final WindowBasedTextGUI gui, final StorageProviderFactory storageProviderFactory) {
        this.gui = gui;

        this.storageProviderFactory = storageProviderFactory;
        keyBox.setValidationPattern(HEX_ONLY);

    }

    @Override
    public String label() {
        return "Rocks DB Browser";
    }

    @Override
    public MenuGroup group() {
        return MenuGroup.DATABASE;
    }


    @Override
    public KeyControls createControls() {
        return new KeyControls()
                .addControl("Search", KEY_SEARCH, this::search)
                .addControl("Update", KEY_UPDATE, this::update)
                .addControl("Delete", KEY_DELETE, this::delete);
    }

    @Override
    public Panel createMainPanel() {
        Panel panel = new Panel(new LinearLayout());


        Panel columnFamily = new Panel(new LinearLayout(Direction.HORIZONTAL));
        columnFamily.addComponent(new Label("Column family: "));
        columnFamily.addComponent(identifierCombo);
        panel.addComponent(columnFamily);
        Panel keyPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));

        keyPanel.addComponent(new Label("Key (hex):"));
        keyPanel.addComponent(keyBox);
        panel.addComponent(keyPanel);

        Panel valuePanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        valuePanel.addComponent(new Label("Value (hex):"));
        valuePanel.addComponent(valueBox);
        panel.addComponent(valuePanel);


        return panel;
    }

    private void search() {
        try {
            final StorageProvider provider = storageProviderFactory.createProvider();
            final KeyValueStorage storageBySegmentIdentifier = provider.getStorageBySegmentIdentifier(identifierCombo.getSelectedItem());
            final Optional<byte[]> value = storageBySegmentIdentifier.get(Bytes.fromHexString(keyBox.getText())
                    .toArrayUnsafe());
            if (value.isPresent()) {
                valueBox.setText(Bytes.wrap(value.get()).toHexString());
            } else {
                valueBox.setText("Not found...");
            }
        } catch (Exception e) {
            BelaDialog.showException(gui, e);
        }
    }

    private void update() {
        try {
            final StorageProvider provider = storageProviderFactory.createProvider();
            final KeyValueStorage storageBySegmentIdentifier = provider.getStorageBySegmentIdentifier(identifierCombo.getSelectedItem());
            final Optional<byte[]> key = Optional.ofNullable(Bytes.fromHexString(keyBox.getText())
                .toArrayUnsafe());
            final Optional<byte[]> value = Optional.ofNullable(Bytes.fromHexString(keyBox.getText())
                .toArrayUnsafe());
            String message;
            if (key.isPresent() && value.isPresent()) {
                var tx = storageBySegmentIdentifier.startTransaction();
                tx.put(key.get(), value.get());
                tx.commit();
                message = "Updated";
            } else {
                message = "Both key and value are required for update";
            }
            BelaDialog.showMessage(gui, "Update Key", message);
        } catch (Exception e) {
            BelaDialog.showException(gui, e);
        }
    }


    private void delete() {
        try {
            final StorageProvider provider = storageProviderFactory.createProvider();
            final KeyValueStorage storageBySegmentIdentifier = provider.getStorageBySegmentIdentifier(identifierCombo.getSelectedItem());
            final boolean deleted = storageBySegmentIdentifier.tryDelete(Bytes.fromHexString(keyBox.getText())
                .toArrayUnsafe());
            if (deleted) {
                BelaDialog.showMessage(gui, "Delete key", "deleted " + keyBox.getText());
            } else {
                BelaDialog.showMessage(gui, "Delete key", "'" + keyBox.getText() + "' not found");
            }
        } catch (Exception e) {
            BelaDialog.showException(gui, e);
        }
    }

}

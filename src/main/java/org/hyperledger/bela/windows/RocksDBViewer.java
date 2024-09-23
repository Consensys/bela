package org.hyperledger.bela.windows;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.ComboBox;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.context.BelaContext;
import org.hyperledger.bela.dialogs.BelaDialog;
import org.hyperledger.bela.model.KeyValueConstants;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;

import java.util.Optional;
import java.util.regex.Pattern;

import static org.hyperledger.bela.utils.TextUtils.abbreviateForDisplay;
import static org.hyperledger.bela.utils.TextUtils.unWrapDisplayBytes;
import static org.hyperledger.bela.utils.TextUtils.wrapBytesForDisplayAtCols;
import static org.hyperledger.bela.windows.Constants.KEY_DELETE;
import static org.hyperledger.bela.windows.Constants.KEY_RESET;
import static org.hyperledger.bela.windows.Constants.KEY_SEARCH;
import static org.hyperledger.bela.windows.Constants.KEY_UPDATE;

public class RocksDBViewer extends AbstractBelaWindow {
    private static final Pattern HEX_ONLY = Pattern.compile("^(0x)?[0-9A-Fa-f]*$");
    private static final Pattern HEX_AND_WRAP_ONLY = Pattern.compile("^(0x)?[0-9A-Fa-f]*(\\\\(\n)?)?$");
    private final ComboBox<KeyValueSegmentIdentifier> identifierCombo = new ComboBox<>(KeyValueSegmentIdentifier.values());
    private final ComboBox<KeyValueConstants> kvConstantsCombo = new ComboBox<>(KeyValueConstants.values());;
    private final TextBox keyBox = new TextBox(new TerminalSize(80, 1));
    private final TextBox valueBox = new TextBox(new TerminalSize(80, 25));
    private final WindowBasedTextGUI gui;
    private final BelaContext belaContext;

    public RocksDBViewer(final WindowBasedTextGUI gui, final BelaContext belaContext) {
        this.gui = gui;

        this.belaContext = belaContext;
        keyBox.setValidationPattern(HEX_ONLY);
        valueBox.setValidationPattern(HEX_AND_WRAP_ONLY);
        kvConstantsCombo.addListener((selected, previous, changedByUser) -> {
            if (changedByUser) {
                keyBox.setText(KeyValueConstants.values()[selected].getKeyValue());
            }
        });
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
                .addControl("Delete", KEY_DELETE, this::delete)
                .addControl("Reset", KEY_RESET, this::reset);
    }

    @Override
    public Panel createMainPanel() {
        Panel panel = new Panel(new LinearLayout());


        Panel columnFamily = new Panel(new LinearLayout(Direction.HORIZONTAL));
        columnFamily.addComponent(new Label("Column family:"));
        columnFamily.addComponent(identifierCombo);
        panel.addComponent(columnFamily);
        Panel constantsPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        constantsPanel.addComponent(new Label("Autofill Key Constant:"));
        constantsPanel.addComponent(kvConstantsCombo);
        panel.addComponent(constantsPanel);
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
            final StorageProvider provider = belaContext.getProvider();
            final KeyValueStorage storageBySegmentIdentifier = provider.getStorageBySegmentIdentifier(identifierCombo.getSelectedItem());
            final Optional<byte[]> value = storageBySegmentIdentifier.get(Bytes.fromHexString(keyBox.getText())
                    .toArrayUnsafe());
            if (value.isPresent()) {
                valueBox.setText(wrapBytesForDisplayAtCols(value.get(), 78));
            } else {
                BelaDialog.showMessage(gui, "Search Key",
                        "Key '" + abbreviateForDisplay(keyBox.getText()) + "' not found");
                valueBox.setText("");
            }
        } catch (Exception e) {
            BelaDialog.showException(gui, e);
        }
    }

    private void update() {
        try {
            StorageProvider provider = belaContext.getProvider();
            final KeyValueStorage storageBySegmentIdentifier = provider.getStorageBySegmentIdentifier(identifierCombo.getSelectedItem());
            if (keyBox.getText().length() > 0) {
                final Optional<byte[]> key = Optional.ofNullable(Bytes.fromHexString(keyBox.getText())
                        .toArrayUnsafe());
                final Optional<byte[]> value = Optional.ofNullable(unWrapDisplayBytes(valueBox.getText()));

                final MessageDialogButton messageDialogButton = new MessageDialogBuilder()
                        .setTitle("Are you sure?")
                        .setText("Updating Key: \n\t" + abbreviateForDisplay(key.get())
                                + "\nwith Value:\n\t" + abbreviateForDisplay(value.get()))
                        .addButton(MessageDialogButton.Cancel)
                        .addButton(MessageDialogButton.OK)
                        .build()
                        .showDialog(gui);

                if (messageDialogButton.equals(MessageDialogButton.OK)) {
                    var tx = storageBySegmentIdentifier.startTransaction();
                    tx.put(key.get(), value.get());
                    tx.commit();
                }
            } else {
                BelaDialog.showMessage(gui, "Update Key", "Key is required for update");
            }

        } catch (Exception e) {
            BelaDialog.showException(gui, e);
        }
    }


    private void delete() {
        try {
            final StorageProvider provider = belaContext.getProvider();
            final KeyValueStorage storageBySegmentIdentifier = provider.getStorageBySegmentIdentifier(identifierCombo.getSelectedItem());
            final boolean deleted = storageBySegmentIdentifier.tryDelete(Bytes.fromHexString(keyBox.getText())
                .toArrayUnsafe());
            if (deleted) {
                BelaDialog.showMessage(gui, "Delete key", "deleted " + keyBox.getText());
                valueBox.setText("");
            } else {
                BelaDialog.showMessage(gui, "Delete key", "'" + keyBox.getText() + "' not found");
            }
        } catch (Exception e) {
            BelaDialog.showException(gui, e);
        }
    }

    private void reset() {
        keyBox.setText("");
        valueBox.setText("");
    }

}

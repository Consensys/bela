package org.hyperledger.bela.windows;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.ComboBox;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.dialogs.BelaExceptionDialog;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;

import static org.hyperledger.bela.windows.Constants.KEY_CLOSE;
import static org.hyperledger.bela.windows.Constants.KEY_SEARCH;

public class RocksDBViewer implements BelaWindow {
    private final ComboBox<KeyValueSegmentIdentifier> identifierCombo = new ComboBox<>(KeyValueSegmentIdentifier.values());
    private static final Pattern HEX_ONLY = Pattern.compile("^[0-9A-Fa-f]+$");
    private StorageProviderFactory storageProviderFactory;
    private final TextBox keyBox = new TextBox(new TerminalSize(80, 1));
    private final Label valueLabel = new Label("");
    private WindowBasedTextGUI gui;

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
    public Window createWindow() {

        Window window = new BasicWindow(label());
        window.setHints(List.of(Window.Hint.FULL_SCREEN));

        Panel panel = new Panel(new LinearLayout());


        // add possible actions
        KeyControls controls = new KeyControls()
                .addControl("Search", KEY_SEARCH, this::search)
                .addControl("Close", KEY_CLOSE, window::close);
        window.addWindowListener(controls);
        panel.addComponent(controls.createComponent());

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
        valuePanel.addComponent(valueLabel);
        panel.addComponent(valuePanel);


        window.setComponent(panel);
        return window;
    }

    private void search() {
        try {
            final StorageProvider provider = storageProviderFactory.createProvider();
            final KeyValueStorage storageBySegmentIdentifier = provider.getStorageBySegmentIdentifier(identifierCombo.getSelectedItem());
            final Optional<byte[]> value = storageBySegmentIdentifier.get(Bytes32.fromHexStringStrict(keyBox.getText())
                    .toArrayUnsafe());
            if (value.isPresent()) {
                valueLabel.setText(Bytes32.wrap(value.get()).toHexString());
            } else {
                valueLabel.setText("Not found...");
            }
        } catch (Exception e) {
            BelaExceptionDialog.showException(gui, e);
        }
    }
}

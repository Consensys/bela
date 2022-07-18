package org.hyperledger.bela.windows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.components.bonsai.BonsaiTrieLogView;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.bonsai.TrieLogLayer;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;

import static org.hyperledger.bela.windows.Constants.KEY_CLOSE;
import static org.hyperledger.bela.windows.Constants.KEY_FOCUS;

public class BonsaiTrieLogLayersViewer implements BelaWindow {
    private final WindowBasedTextGUI gui;
    private final StorageProviderFactory storageProviderFactory;
    private ArrayList<BonsaiTrieLogView> children = new ArrayList<>();

    public BonsaiTrieLogLayersViewer(final WindowBasedTextGUI gui, final StorageProviderFactory storageProviderFactory) {

        this.gui = gui;
        this.storageProviderFactory = storageProviderFactory;
    }

    @Override
    public String label() {
        return "Bonsai Trie Log Layers Viewer";
    }

    @Override
    public MenuGroup group() {
        return MenuGroup.DATABASE;
    }

    @Override
    public Window createWindow() {
        Window window = new BasicWindow(label());
        window.setHints(List.of(Window.Hint.FULL_SCREEN));
        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));

        KeyControls controls = new KeyControls()
                .addControl("Focus", KEY_FOCUS, this::checkFocus)
                .addControl("Close", KEY_CLOSE, window::close);
        window.addWindowListener(controls);
        panel.addComponent(controls.createComponent());
        panel.addComponent(new EmptySpace());

        addComponents(panel);


        window.setComponent(panel);
        return window;
    }

    private void checkFocus() {
        for (BonsaiTrieLogView child : children) {
            child.focus();
        }
    }

    private void addComponents(final Panel panel) {
        final StorageProvider provider = storageProviderFactory.createProvider();
        final KeyValueStorage storage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.TRIE_LOG_STORAGE);
        this.children = new ArrayList<>();
        storage.streamKeys().limit(10).forEach(key -> {
            final Hash hash = Hash.wrap(Bytes32.wrap(key));
            final Optional<TrieLogLayer> layer = getTrieLog(storage, hash);
            if (layer.isPresent()) {
                final BonsaiTrieLogView bonsaiTrieLogView = new BonsaiTrieLogView(hash, layer.get(), 0);
                panel.addComponent(bonsaiTrieLogView.createComponent());
                children.add(bonsaiTrieLogView);
            } else {
                panel.addComponent(new Label("No Trie Log Layer for " + hash));
            }
        });
    }

    public Optional<TrieLogLayer> getTrieLog(final KeyValueStorage storage, final Hash blockHash) {
        return storage.get(blockHash.toArrayUnsafe()).map(bytes -> {
            try {
                Method method = TrieLogLayer.class.getDeclaredMethod("fromBytes", byte[].class);
                method.setAccessible(true);
                return (TrieLogLayer) method.invoke(null, bytes);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
//            return TrieLogLayer.fromBytes(bytes);
        });
    }
}

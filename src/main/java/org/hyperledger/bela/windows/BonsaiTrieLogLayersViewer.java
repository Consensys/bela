package org.hyperledger.bela.windows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.components.bonsai.BonsaiTrieLogView;
import org.hyperledger.bela.dialogs.BelaExceptionDialog;
import org.hyperledger.bela.utils.BlockChainContext;
import org.hyperledger.bela.utils.BlockChainContextFactory;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.bonsai.BonsaiPersistedWorldState;
import org.hyperledger.besu.ethereum.bonsai.BonsaiWorldStateArchive;
import org.hyperledger.besu.ethereum.bonsai.BonsaiWorldStateKeyValueStorage;
import org.hyperledger.besu.ethereum.bonsai.BonsaiWorldStateUpdater;
import org.hyperledger.besu.ethereum.bonsai.TrieLogLayer;
import org.hyperledger.besu.ethereum.chain.ChainHead;
import org.hyperledger.besu.ethereum.chain.DefaultBlockchain;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;

import static kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory.getLogger;
import static org.hyperledger.bela.windows.Constants.KEY_CLOSE;
import static org.hyperledger.bela.windows.Constants.KEY_FOCUS;
import static org.hyperledger.bela.windows.Constants.KEY_HEAD;
import static org.hyperledger.bela.windows.Constants.KEY_LOOKUP_BY_HASH;
import static org.hyperledger.bela.windows.Constants.KEY_ROLL_BACKWARD;
import static org.hyperledger.bela.windows.Constants.KEY_ROLL_FORWARD;

public class BonsaiTrieLogLayersViewer implements BelaWindow {
    private static final LambdaLogger log = getLogger(BonsaiTrieLogLayersViewer.class);

    private final WindowBasedTextGUI gui;
    private final StorageProviderFactory storageProviderFactory;
    private BonsaiTrieLogView view;
    private final Panel triesPanel = new Panel();

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
                .addControl("By Hash", KEY_LOOKUP_BY_HASH, this::lookupByHash)
                .addControl("Head", KEY_HEAD, this::lookupByChainHead)
                .addControl("Roll Forward", KEY_ROLL_FORWARD, this::rollForward)
                .addControl("Roll Backward", KEY_ROLL_BACKWARD, this::rollBackward)
                .addControl("Close", KEY_CLOSE, window::close);
        window.addWindowListener(controls);
        panel.addComponent(controls.createComponent());
        panel.addComponent(new EmptySpace());


        panel.addComponent(triesPanel);


        window.setComponent(panel);
        return window;
    }


    private void rollForward() {
        try {
            final BonsaiWorldStateUpdater updater = getBonsaiWorldStateUpdater();
            updater.rollForward(view.getLayer());
            updater.commit();
            storageProviderFactory.close();
        } catch (Exception e) {
            BelaExceptionDialog.showException(gui, e);
        }
    }

    private void rollBackward() {
        try {
            final BonsaiWorldStateUpdater updater = getBonsaiWorldStateUpdater();
            updater.rollBack(view.getLayer());
            updater.commit();
            storageProviderFactory.close();
        } catch (Exception e) {
            BelaExceptionDialog.showException(gui, e);
        }

    }

    private BonsaiWorldStateUpdater getBonsaiWorldStateUpdater() {
        final StorageProvider provider = storageProviderFactory.createProvider();
        final BlockChainContext blockChainContext = BlockChainContextFactory.createBlockChainContext(provider);

        final BonsaiWorldStateArchive archive = new BonsaiWorldStateArchive(provider, blockChainContext.getBlockchain());
        final BonsaiPersistedWorldState bonsaiState =
                new BonsaiPersistedWorldState(
                        archive,
                        new BonsaiWorldStateKeyValueStorage(
                                provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.ACCOUNT_INFO_STATE),
                                provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.CODE_STORAGE),
                                provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.ACCOUNT_STORAGE_STORAGE),
                                provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.TRIE_BRANCH_STORAGE),
                                provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.TRIE_LOG_STORAGE)));
        return (BonsaiWorldStateUpdater) bonsaiState.updater();

    }


    private void lookupByChainHead() {
        final BlockChainContext blockChainContext = BlockChainContextFactory.createBlockChainContext(storageProviderFactory.createProvider());
        final ChainHead chainHead = blockChainContext.getBlockchain().getChainHead();
        updateTrieFromHash(chainHead.getHash());

    }

    private void lookupByHash() {
        final BlockChainContext blockChainContext = BlockChainContextFactory.createBlockChainContext(storageProviderFactory.createProvider());
        final ChainHead chainHead = blockChainContext.getBlockchain().getChainHead();
        final String s = TextInputDialog.showDialog(gui, "Enter Hash", "Hash", chainHead.getHash().toHexString());
        if (s == null) {
            return;
        }
        try {
            final Hash hash = Hash.fromHexString(s);
            updateTrieFromHash(hash);
        } catch (Exception e) {
            log.error("There was an error when moving browser", e);
            BelaExceptionDialog.showException(gui, e);
        }
    }

    private void updateTrieFromHash(final Hash hash) {
        final StorageProvider provider = storageProviderFactory.createProvider();
        final KeyValueStorage storage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.TRIE_LOG_STORAGE);
        final Optional<TrieLogLayer> trieLog = getTrieLog(storage, hash);
        if (trieLog.isPresent()) {
            triesPanel.removeAllComponents();
            final BonsaiTrieLogView bonsaiTrieLogView = new BonsaiTrieLogView(hash, trieLog.get(), 0);
            triesPanel.addComponent(bonsaiTrieLogView.createComponent());
            view = bonsaiTrieLogView;
        } else {
            log.error("Trie log not found for hash: {}", hash);
        }
    }

    private void checkFocus() {
        if (view != null) {
            view.focus();
        }
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

package org.hyperledger.bela.windows;

import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.components.bonsai.BonsaiTrieLogView;
import org.hyperledger.bela.dialogs.BelaDialog;
import org.hyperledger.bela.utils.BlockChainContext;
import org.hyperledger.bela.utils.BlockChainContextFactory;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.bonsai.BonsaiWorldStateArchive;
import org.hyperledger.besu.ethereum.bonsai.BonsaiWorldStateKeyValueStorage;
import org.hyperledger.besu.ethereum.bonsai.BonsaiWorldStateUpdater;
import org.hyperledger.besu.ethereum.bonsai.TrieLogManager;
import org.hyperledger.besu.ethereum.chain.ChainHead;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.ethereum.worldstate.DataStorageConfiguration;

import static kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory.getLogger;
import static org.hyperledger.bela.windows.Constants.KEY_HEAD;
import static org.hyperledger.bela.windows.Constants.KEY_LOOKUP_BY_HASH;
import static org.hyperledger.bela.windows.Constants.KEY_ROLL_BACKWARD;
import static org.hyperledger.bela.windows.Constants.KEY_ROLL_FORWARD;
import static org.hyperledger.bela.windows.Constants.KEY_SHOW_ALL;

public class BonsaiTrieLogLayersViewer extends AbstractBelaWindow {
    private static final LambdaLogger log = getLogger(BonsaiTrieLogLayersViewer.class);

    private final WindowBasedTextGUI gui;
    private final StorageProviderFactory storageProviderFactory;

    private final BonsaiTrieLogView view;

    public BonsaiTrieLogLayersViewer(final WindowBasedTextGUI gui, final StorageProviderFactory storageProviderFactory) {

        this.gui = gui;
        this.storageProviderFactory = storageProviderFactory;

        this.view = new BonsaiTrieLogView(storageProviderFactory);
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
    public KeyControls createControls() {
        return new KeyControls()
                .addControl("By Hash", KEY_LOOKUP_BY_HASH, this::lookupByHash)
                .addControl("Head", KEY_HEAD, this::lookupByChainHead)
                .addControl("All", KEY_SHOW_ALL, this::showAll)
                .addControl("Roll Forward", KEY_ROLL_FORWARD, this::rollForward)
                .addControl("Roll Backward", KEY_ROLL_BACKWARD, this::rollBackward);
    }

    private void showAll() {
        view.showAllTries();
    }

    @Override
    public Panel createMainPanel() {
        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));

        panel.addComponent(view.createComponent());

        return panel;
    }


    private void rollForward() {
        try {
            final BonsaiWorldStateUpdater updater = getBonsaiWorldStateUpdater();
            updater.rollForward(view.getLayer());
            updater.commit();
            storageProviderFactory.close();
        } catch (Exception e) {
            BelaDialog.showException(gui, e);
        }
    }

    private void rollBackward() {
        try {
            final BonsaiWorldStateUpdater updater = getBonsaiWorldStateUpdater();
            updater.rollBack(view.getLayer());
            updater.commit();
            storageProviderFactory.close();
        } catch (Exception e) {
            BelaDialog.showException(gui, e);
        }

    }

    private BonsaiWorldStateUpdater getBonsaiWorldStateUpdater() {
        final StorageProvider provider = storageProviderFactory.createProvider();
        final BlockChainContext blockChainContext = BlockChainContextFactory.createBlockChainContext(provider);

        final BonsaiWorldStateArchive archive = new BonsaiWorldStateArchive(
                new TrieLogManager(blockChainContext.getBlockchain(),
                        new BonsaiWorldStateKeyValueStorage(
                                provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.ACCOUNT_INFO_STATE),
                                provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.CODE_STORAGE),
                                provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.ACCOUNT_STORAGE_STORAGE),
                                provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.TRIE_BRANCH_STORAGE),
                                provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.TRIE_LOG_STORAGE)),
                        DataStorageConfiguration.DEFAULT_CONFIG.getBonsaiMaxLayersToLoad()),
                provider, blockChainContext.getBlockchain());

        return (BonsaiWorldStateUpdater) archive.getMutable().updater();

    }


    private void lookupByChainHead() {
        final BlockChainContext blockChainContext = BlockChainContextFactory.createBlockChainContext(storageProviderFactory.createProvider());
        final ChainHead chainHead = blockChainContext.getBlockchain().getChainHead();
        updateTrieFromHash(chainHead.getHash());

    }

    private void lookupByHash() {
        final BlockChainContext blockChainContext = BlockChainContextFactory.createBlockChainContext(storageProviderFactory.createProvider());
        final ChainHead chainHead = blockChainContext.getBlockchain().getChainHead();
        final String s = TextInputDialog.showDialog(gui, "Enter Block Hash", "Hash", chainHead.getHash().toHexString());
        if (s == null) {
            return;
        }
        try {
            final Hash hash = Hash.fromHexString(s);
            updateTrieFromHash(hash);
        } catch (Exception e) {
            log.error("There was an error when showing Trie", e);
            BelaDialog.showException(gui, e);
        }
    }

    private void updateTrieFromHash(final Hash hash) {
        view.updateFromHash(hash);
    }


}

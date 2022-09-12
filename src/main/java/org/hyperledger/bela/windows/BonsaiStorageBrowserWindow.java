package org.hyperledger.bela.windows;

import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.components.bonsai.BonsaiStorageView;
import org.hyperledger.bela.dialogs.BelaDialog;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;

import static kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory.getLogger;
import static org.hyperledger.bela.windows.Constants.KEY_ACCOUNT_BY_ADDRESS;
import static org.hyperledger.bela.windows.Constants.KEY_LOG;
import static org.hyperledger.bela.windows.Constants.KEY_LOOKUP_BY_HASH;
import static org.hyperledger.bela.windows.Constants.KEY_ROOT;

public class BonsaiStorageBrowserWindow extends AbstractBelaWindow {
    private static final LambdaLogger log = getLogger(BonsaiStorageBrowserWindow.class);

    private final WindowBasedTextGUI gui;
    private final StorageProviderFactory storageProviderFactory;
    private final BonsaiStorageView storageView;

    public BonsaiStorageBrowserWindow(final WindowBasedTextGUI gui, final StorageProviderFactory storageProviderFactory) {

        this.gui = gui;
        this.storageProviderFactory = storageProviderFactory;
        this.storageView = new BonsaiStorageView(storageProviderFactory);
    }


    @Override
    public String label() {
        return "Bonsai Storage Browser";
    }

    @Override
    public MenuGroup group() {
        return MenuGroup.DATABASE;
    }

    @Override
    public KeyControls createControls() {
        return new KeyControls()
                .addControl("Root", KEY_ROOT, storageView::selectRoot)
                .addControl("To Log", KEY_LOG, storageView::logCurrent)
                .addControl("Address?", KEY_ACCOUNT_BY_ADDRESS, this::findByAddress)
                .addControl("Hash?", KEY_LOOKUP_BY_HASH, this::findByHash)
                ;
    }

    private void findByHash() {
        final String s = TextInputDialog.showDialog(gui, "Enter Account Hash", "Hash", "0x033424ed0313251e88bd49730108ee8901a506c0a4309478fbf8a14adea9d02a");
        if (s == null) {
            return;
        }
        try {
            storageView.findByHash(Hash.fromHexString(s));
        } catch (Exception e) {
            log.error("There was an error when moving browser", e);
            BelaDialog.showException(gui, e);
        }
    }

    private void findByAddress() {
        final String s = TextInputDialog.showDialog(gui, "Enter Account Address", "Address", "0xb3a16c2b68bbb0111ebd27871a5934b949837d95");
        if (s == null) {
            return;
        }
        try {
            storageView.findByAddress(Address.fromHexString(s));
        } catch (Exception e) {
            log.error("There was an error when moving browser", e);
            BelaDialog.showException(gui, e);
        }
    }


    @Override
    public Panel createMainPanel() {
        Panel panel = new Panel(new LinearLayout());

        panel.addComponent(storageView.createComponent());

        return panel;
    }

}


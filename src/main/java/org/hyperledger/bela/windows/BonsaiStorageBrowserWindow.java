package org.hyperledger.bela.windows;

import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.components.bonsai.BonsaiStorageView;
import org.hyperledger.bela.utils.StorageProviderFactory;

import static org.hyperledger.bela.windows.Constants.KEY_FOCUS;
import static org.hyperledger.bela.windows.Constants.KEY_ROOT;

public class BonsaiStorageBrowserWindow extends AbstractBelaWindow {

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
                .addControl("Root", KEY_ROOT, storageView::findRoot)
                .addControl("Focus", KEY_FOCUS, storageView::checkFocus);
    }


    @Override
    public Panel createMainPanel() {
        Panel panel = new Panel(new LinearLayout());

        panel.addComponent(storageView.createComponent());

        return panel;
    }

}


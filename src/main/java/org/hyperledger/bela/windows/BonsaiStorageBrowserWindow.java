package org.hyperledger.bela.windows;

import java.util.List;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import org.hyperledger.bela.components.BonsaiStorageView;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.utils.StorageProviderFactory;

import static org.hyperledger.bela.windows.Constants.KEY_CLOSE;
import static org.hyperledger.bela.windows.Constants.KEY_ROOT;

public class BonsaiStorageBrowserWindow implements LanternaWindow {

    private final WindowBasedTextGUI gui;
    private final StorageProviderFactory storageProviderFactory;
    private final BonsaiStorageView storageView;
    private BasicWindow window;

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
    public Window createWindow() {
        window = new BasicWindow("BonsaiTreeVerifier");
        window.setHints(List.of(Window.Hint.FULL_SCREEN));

        Panel panel = new Panel(new LinearLayout());

        KeyControls controls = new KeyControls()
                .addControl("Root", KEY_ROOT,storageView::findRoot)
                .addControl("Close", KEY_CLOSE, window::close);
        window.addWindowListener(controls);
        panel.addComponent(controls.createComponent());

        panel.addComponent(new EmptySpace());
        panel.addComponent(storageView.createComponent());

        window.setComponent(panel);

        return window;
    }

}


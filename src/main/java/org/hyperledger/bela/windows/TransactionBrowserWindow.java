package org.hyperledger.bela.windows;

import java.util.List;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.utils.BlockChainContext;
import org.hyperledger.bela.utils.TransactionBrowser;
import org.hyperledger.besu.datatypes.Hash;

import static com.googlecode.lanterna.input.KeyType.ArrowLeft;
import static com.googlecode.lanterna.input.KeyType.ArrowRight;

public class TransactionBrowserWindow implements LanternaWindow {


    private TransactionBrowser browser;
    private BasicWindow window;
    private final BlockChainContext context;
    private final WindowBasedTextGUI gui;
    private final Hash blockHash;

    public TransactionBrowserWindow(final BlockChainContext context,
                                    final WindowBasedTextGUI gui, final Hash blockHash) {
        this.context = context;
        this.gui = gui;
        this.blockHash = blockHash;
    }

    @Override
    public String label() {
        return "Transaction Browser";
    }

    @Override
    public MenuGroup group() {
        return MenuGroup.ACTIONS;
    }

    @Override
    public Window createWindow() {
        browser = new TransactionBrowser(context, blockHash);

        // Create window to hold the panel

        window = new BasicWindow("Bela Transaction Browser");
        window.setHints(List.of(Window.Hint.FULL_SCREEN));

        Panel panel = new Panel(new LinearLayout());
        KeyControls controls = new KeyControls()
                .addControl("prev Block", ArrowLeft, () -> browser = browser.moveBackward())
                .addControl("next Block", ArrowRight, () -> browser = browser.moveForward())
                .addControl("close", 'c', window::close)
                .addControl("Hash?", 'h', this::findByHash);
        window.addWindowListener(controls);
        panel.addComponent(controls.createComponent());

        // add transaction detail panel
        panel.addComponent(browser.transactionPanel().createComponent());

        window.setComponent(panel);
        return window;
    }

    private void findByHash() {
        final String s = TextInputDialog.showDialog(gui, "Enter Hash", "Hash", "");
        if (s == null) {
            return;
        }
        try {
            browser.moveByHash(Hash.fromHexStringLenient(s));
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showMessageDialog(gui, "error", e.getMessage());
        }
    }
}

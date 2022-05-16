package org.hyperledger.bela.windows;

import java.util.List;
import java.util.prefs.Preferences;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.utils.BlockChainBrowser;
import org.hyperledger.bela.utils.BlockChainContext;
import org.hyperledger.bela.utils.BlockChainContextFactory;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.besu.datatypes.Hash;

import static kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory.getLogger;
import static org.hyperledger.bela.windows.Constants.KEY_BACK;
import static org.hyperledger.bela.windows.Constants.KEY_BEGINNING;
import static org.hyperledger.bela.windows.Constants.KEY_CLOSE;
import static org.hyperledger.bela.windows.Constants.KEY_END;
import static org.hyperledger.bela.windows.Constants.KEY_FORWARD;
import static org.hyperledger.bela.windows.Constants.KEY_LOOKUP_BY_HASH;
import static org.hyperledger.bela.windows.Constants.KEY_LOOKUP_BY_NUMBER;
import static org.hyperledger.bela.windows.Constants.KEY_OPEN_TRANSACTION;
import static org.hyperledger.bela.windows.Constants.KEY_ROLL_HEAD;

public class BlockChainBrowserWindow implements LanternaWindow {
    private static final LambdaLogger log = getLogger(BlockChainBrowserWindow.class);


    private BlockChainBrowser browser;
    private BasicWindow window;
    private StorageProviderFactory storageProviderFactory;
    private WindowBasedTextGUI gui;
    private final Preferences preferences;
    private BlockChainContext context;

    public BlockChainBrowserWindow(final StorageProviderFactory storageProviderFactory,
                                   final WindowBasedTextGUI gui, final Preferences preferences) {
        this.storageProviderFactory = storageProviderFactory;
        this.gui = gui;
        this.preferences = preferences;
    }

    @Override
    public String label() {
        return "Blockchain Browser";
    }

    @Override
    public MenuGroup group() {
        return MenuGroup.DATABASE;
    }

    @Override
    public Window createWindow() {
        context = BlockChainContextFactory.createBlockChainContext(storageProviderFactory.createProvider());
        browser = BlockChainBrowser.fromBlockChainContext(context);

        // Create window to hold the panel

        window = new BasicWindow("Bela DB Browser");
        window.setHints(List.of(Window.Hint.FULL_SCREEN));

        Panel panel = new Panel(new LinearLayout());


        // add possible actions
        KeyControls controls = new KeyControls()
                .addControl(" <--", KEY_BACK, () -> browser = browser.moveBackward())
                .addControl("Start", KEY_BEGINNING, () -> browser = browser.moveToStart())
                .addControl("End", KEY_END, () -> browser = browser.moveToHead())
                .addControl(" -->", KEY_FORWARD, () -> browser = browser.moveForward())
                .addControl("Transactions", KEY_OPEN_TRANSACTION, this::viewTransactions)
                .addControl("Close", KEY_CLOSE, window::close)
                .addControl("Roll Head", KEY_ROLL_HEAD, this::rollHead)
                .addControl("Hash?", KEY_LOOKUP_BY_HASH, this::findByHash)
                .addControl("Number?", KEY_LOOKUP_BY_NUMBER, this::findByNumber);
        window.addWindowListener(controls);
        panel.addComponent(controls.createComponent());

        // add summary panel
        panel.addComponent(browser.showSummaryPanel().createComponent()
                .withBorder(Borders.singleLine()));

        // add block detail panel
        panel.addComponent(browser.blockPanel().createComponent());

        window.setComponent(panel);
        return window;
    }


    private void rollHead() {

        final MessageDialogButton messageDialogButton = new MessageDialogBuilder()
                .setTitle("Are you sure?")
                .setText("Danger! You will override current head:\n" + browser.getChainHead().orElseThrow().getHash())
                .addButton(MessageDialogButton.Cancel)
                .addButton(MessageDialogButton.OK)
                .build()
                .showDialog(gui);
        if (messageDialogButton.equals(MessageDialogButton.OK)) {
            browser.rollHead();
        }

    }

    private void findByNumber() {
        final String s = TextInputDialog.showDialog(gui, "Enter Number", "Number", "");
        if (s == null) {
            return;
        }
        try {
            browser.moveByNumber(Long.parseLong(s));
        } catch (Exception e) {
            log.error("There was an error when moving browser", e);
            MessageDialog.showMessageDialog(gui, "error", e.getMessage());
        }
    }

    private void findByHash() {
        final String s = TextInputDialog.showDialog(gui, "Enter Hash", "Hash", browser.getBlockHash());
        if (s == null) {
            return;
        }
        try {
            browser.moveByHash(Hash.fromHexString(s));
        } catch (Exception e) {
            log.error("There was an error when moving browser", e);
            MessageDialog.showMessageDialog(gui, "error", e.getMessage());
        }
    }

    private void viewTransactions() {
        if (browser.hasTransactions()) {
            final TransactionBrowserWindow transactionBrowserWindow = new TransactionBrowserWindow(preferences,
                    context, gui, Hash.fromHexString(browser.getBlockHash()));
            gui.addWindowAndWait(transactionBrowserWindow.createWindow());
        } else {
            MessageDialog.showMessageDialog(gui, "Nothing...", "No transactions in this block");
        }
    }

}

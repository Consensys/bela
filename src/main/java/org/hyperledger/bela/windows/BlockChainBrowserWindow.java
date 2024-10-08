package org.hyperledger.bela.windows;

import java.util.prefs.Preferences;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.context.BelaContext;
import org.hyperledger.bela.dialogs.BelaDialog;
import org.hyperledger.bela.utils.BlockChainBrowser;
import org.hyperledger.bela.utils.BlockChainContext;
import org.hyperledger.bela.utils.BlockChainContextFactory;
import org.hyperledger.besu.datatypes.Hash;

import static kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory.getLogger;
import static org.hyperledger.bela.windows.Constants.KEY_BACK;
import static org.hyperledger.bela.windows.Constants.KEY_BEGINNING;
import static org.hyperledger.bela.windows.Constants.KEY_END;
import static org.hyperledger.bela.windows.Constants.KEY_FORWARD;
import static org.hyperledger.bela.windows.Constants.KEY_LOOKUP_BY_HASH;
import static org.hyperledger.bela.windows.Constants.KEY_LOOKUP_BY_NUMBER;
import static org.hyperledger.bela.windows.Constants.KEY_OPEN_TRANSACTION;
import static org.hyperledger.bela.windows.Constants.KEY_ROLL_HEAD;

public class BlockChainBrowserWindow extends AbstractBelaWindow {
    private static final LambdaLogger log = getLogger(BlockChainBrowserWindow.class);
    private final Preferences preferences;
    private final BelaContext belaContext;
    private final WindowBasedTextGUI gui;
    private BlockChainBrowser browser;
    private BlockChainContext context;

    public BlockChainBrowserWindow(final BelaContext belaContext,
                                   final WindowBasedTextGUI gui, final Preferences preferences) {
        this.belaContext = belaContext;
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
    public KeyControls createControls() {
        return new KeyControls()
                .addControl("<--", KEY_BACK, () -> browser = browser.moveBackward())
                .addControl("Start", KEY_BEGINNING, () -> browser = browser.moveToStart())
                .addControl("End", KEY_END, () -> browser = browser.moveToHead())
                .addControl("-->", KEY_FORWARD, () -> browser = browser.moveForward())
                .addControl("Transactions", KEY_OPEN_TRANSACTION, this::viewTransactions)
                .addControl("Roll Head", KEY_ROLL_HEAD, this::rollHead)
                .addControl("Hash?", KEY_LOOKUP_BY_HASH, this::findByHash)
                .addControl("Number?", KEY_LOOKUP_BY_NUMBER, this::findByNumber);
    }


    @Override
    public Panel createMainPanel() {
        context = BlockChainContextFactory.createBlockChainContext(belaContext);
        browser = BlockChainBrowser.fromBlockChainContext(context);

        Panel panel = new Panel(new LinearLayout());

        // add summary panel
        panel.addComponent(browser.showSummaryPanel().createComponent()
                .withBorder(Borders.singleLine()));

        // add block detail panel
        panel.addComponent(browser.blockPanel().createComponent());

        return panel;
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
            BelaDialog.showException(gui, e);
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
            BelaDialog.showException(gui, e);
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

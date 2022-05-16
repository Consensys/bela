package org.hyperledger.bela.windows;

import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.model.TransactionResult;
import org.hyperledger.bela.utils.BlockChainContext;
import org.hyperledger.bela.utils.TraceUtils;
import org.hyperledger.bela.utils.TransactionBrowser;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.tracing.flat.FlatTrace;

import static kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory.getLogger;
import static org.hyperledger.bela.windows.Constants.KEY_BACK;
import static org.hyperledger.bela.windows.Constants.KEY_CLOSE;
import static org.hyperledger.bela.windows.Constants.KEY_FORWARD;
import static org.hyperledger.bela.windows.Constants.KEY_LOOKUP_BY_HASH;
import static org.hyperledger.bela.windows.Constants.KEY_TRACE_TRANSACTION;

public class TransactionBrowserWindow implements LanternaWindow {
    private static final LambdaLogger log = getLogger(TransactionBrowserWindow.class);


    private TransactionBrowser browser;
    private BasicWindow window;
    private final Preferences preferences;
    private final BlockChainContext context;
    private final WindowBasedTextGUI gui;
    private final Hash blockHash;

    public TransactionBrowserWindow(final Preferences preferences, final BlockChainContext context,
                                    final WindowBasedTextGUI gui, final Hash blockHash) {
        this.preferences = preferences;
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
        return MenuGroup.DATABASE;
    }

    @Override
    public Window createWindow() {
        browser = new TransactionBrowser(context, blockHash);

        // Create window to hold the panel

        window = new BasicWindow("Bela Transaction Browser");
        window.setHints(List.of(Window.Hint.FULL_SCREEN));

        Panel panel = new Panel(new LinearLayout());

        KeyControls controls = new KeyControls()
                .addControl("<--", KEY_BACK, () -> browser = browser.moveBackward())
                .addControl("-->", KEY_FORWARD, () -> browser = browser.moveForward())
                .addControl("Close", KEY_CLOSE, window::close)
                .addControl("Hash?", KEY_LOOKUP_BY_HASH, this::findByHash)
                .addControl("Trace", KEY_TRACE_TRANSACTION, this::traceTransaction);
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
            log.error("There was an error when moving browser", e);
            MessageDialog.showMessageDialog(gui, "error", e.getMessage());
        }
    }

    private void traceTransaction() {
        final TransactionResult transactionResult = browser.getTransactionResult();
        final Hash transactionHash = Hash.fromHexString(transactionResult.getHash());
        final List<FlatTrace> traces = TraceUtils.traceTransaction(preferences, context, transactionHash).collect(
                Collectors.toList());
        final TransactionTraceBrowserWindow traceTransactionBrowserWindow = new TransactionTraceBrowserWindow(
                context, gui, traces);
        gui.addWindowAndWait(traceTransactionBrowserWindow.createWindow());
    }

}

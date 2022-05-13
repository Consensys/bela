package org.hyperledger.bela.windows;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.WindowListener;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import com.googlecode.lanterna.input.KeyStroke;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.hyperledger.bela.utils.BlockChainContext;
import org.hyperledger.bela.utils.TransactionBrowser;
import org.hyperledger.besu.datatypes.Hash;
import org.jetbrains.annotations.NotNull;

public class TransactionBrowserWindow implements LanternaWindow, WindowListener {

    private static final String[] PREV_NEXT_BLOCK_COMMANDS = {"prev Transaction", "'<-'", "next Transaction", "'->'", "Close", "'c'", "Hash?", "'h'"};

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

        Panel commands = getCommandsPanel(PREV_NEXT_BLOCK_COMMANDS);

        // add possible actions
        panel.addComponent(commands);

        // add transaction detail panel
        panel.addComponent(browser.transactionPanel().createComponent());

        window.addWindowListener(this);
        window.setComponent(panel);
        return window;
    }


    @NotNull
    private static Panel getCommandsPanel(final String[] strings) {
        Panel commands = new Panel(new LinearLayout(Direction.HORIZONTAL));
        Panel key = new Panel(new LinearLayout());
        key.addComponent(new Label("action").addStyle(SGR.BOLD));
        key.addComponent(new Label("key").addStyle(SGR.BOLD));
        commands.addComponent(key.withBorder(Borders.singleLine()));

        int i = 0;
        while (i < strings.length) {
            Panel a = new Panel(new LinearLayout());
            a.addComponent(new Label(strings[i++]).setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center, LinearLayout.GrowPolicy.None)));
            a.addComponent(new Label(strings[i++]).setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center, LinearLayout.GrowPolicy.None)));
            commands.addComponent(a.withBorder(Borders.singleLine()));
        }
        return commands;
    }

    @Override
    public void onResized(final Window window, final TerminalSize oldSize, final TerminalSize newSize) {

    }

    @Override
    public void onMoved(final Window window, final TerminalPosition oldPosition, final TerminalPosition newPosition) {

    }

    @Override
    public void onInput(final Window basePane, final KeyStroke keyStroke, final AtomicBoolean deliverEvent) {
        switch (keyStroke.getKeyType()) {
            case ArrowLeft:
                browser = browser.moveBackward();
                break;

            case ArrowRight:
                browser = browser.moveForward();
                break;

            case Escape:
                window.close();
                break;
            case Character:
                switch (keyStroke.getCharacter()) {
                    case 'c' -> window.close();
                    case 'h' -> findByHash();
                    default -> {}
                }
                break;
            default:
        }
    }

    @Override
    public void onUnhandledInput(final Window basePane, final KeyStroke keyStroke, final AtomicBoolean hasBeenHandled) {

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

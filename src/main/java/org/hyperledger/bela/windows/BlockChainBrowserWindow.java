package org.hyperledger.bela.windows;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import com.googlecode.lanterna.input.KeyStroke;
import org.hyperledger.bela.utils.BlockChainBrowser;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.besu.datatypes.Hash;
import org.jetbrains.annotations.NotNull;

public class BlockChainBrowserWindow implements LanternaWindow, WindowListener {

    private static final String[] PREV_NEXT_BLOCK_COMMANDS = {"prev Block", "'<-'", "next Block", "'->'", "Close", "'c'", "roll Head", "'r'", "Hash?", "'h'", "Number?", "'n'"};

    private BlockChainBrowser browser;
    private BasicWindow window;
    private StorageProviderFactory storageProviderFactory;
    private WindowBasedTextGUI gui;

    public BlockChainBrowserWindow(final StorageProviderFactory storageProviderFactory, final WindowBasedTextGUI gui) {
        this.storageProviderFactory = storageProviderFactory;

        this.gui = gui;
    }

    @Override
    public String label() {
        return "Blockchain Browser";
    }

    @Override
    public MenuGroup group() {
        return MenuGroup.ACTIONS;
    }

    @Override
    public Window createWindow() {
        browser = BlockChainBrowser.fromProvider(storageProviderFactory.createProvider());

        // Create window to hold the panel

        window = new BasicWindow("Bela DB Browser");
        window.setHints(List.of(Window.Hint.FULL_SCREEN));

        Panel panel = new Panel(new LinearLayout());

        Panel commands = getCommandsPanel(PREV_NEXT_BLOCK_COMMANDS);

        // add possible actions
        panel.addComponent(commands);

        // add summary panel
        panel.addComponent(browser.showSummaryPanel().createComponent()
                .withBorder(Borders.singleLine()));

        // add block detail panel
        panel.addComponent(browser.blockPanel().createComponent());

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
                    case 'c':
                        window.close();
                        break;
                    case 'r':

                        rollHead();
                        break;
                    case 'h':
                        findByHash();
                        break;
                    case 'n':
                        findByNumber();
                        break;
                    default:
                }
                break;
            default:
        }
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
            e.printStackTrace();
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
            e.printStackTrace();
            MessageDialog.showMessageDialog(gui, "error", e.getMessage());
        }
    }

    @Override
    public void onUnhandledInput(final Window basePane, final KeyStroke keyStroke, final AtomicBoolean hasBeenHandled) {

    }
}

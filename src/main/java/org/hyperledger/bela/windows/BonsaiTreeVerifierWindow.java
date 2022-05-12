package org.hyperledger.bela.windows;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowListener;
import com.googlecode.lanterna.input.KeyStroke;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.bela.utils.bonsai.BonsaiListener;
import org.hyperledger.bela.utils.bonsai.BonsaiTraversal;
import org.hyperledger.bela.utils.bonsai.BonsaiTraversalTrieType;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.jetbrains.annotations.NotNull;

public class BonsaiTreeVerifierWindow implements LanternaWindow, WindowListener, BonsaiListener {
    private BasicWindow window;
    private static final String[] START_STOP_VERIFIER_COMMANDS = {"start", "'a'", "stop", "'s'", "Close", "'c'"};
    private final StorageProviderFactory storageProviderFactory;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Future<?> execution;
    private final Label runningLabel = new Label("Not Running...");
    private final Label counterLabel = new Label("0");
    AtomicInteger visited = new AtomicInteger(0);
    private final TextBox logTextBox = new TextBox(new TerminalSize(80, 7));

    public BonsaiTreeVerifierWindow(final StorageProviderFactory storageProviderFactory) {
        this.storageProviderFactory = storageProviderFactory;
    }

    @Override
    public String label() {
        return "Bonsai Tree Verifier";
    }

    @Override
    public MenuGroup group() {
        return MenuGroup.ACTIONS;
    }

    @Override
    public Window createWindow() {
        if (window != null) {
            return window;
        }
        window = new BasicWindow("BonsaiTreeVerifier");
        window.setHints(List.of(Window.Hint.FULL_SCREEN));

        Panel panel = new Panel(new LinearLayout());
        Panel commands = getCommandsPanel(START_STOP_VERIFIER_COMMANDS);

        panel.addComponent(commands);

        panel.addComponent(runningLabel);
        panel.addComponent(counterLabel);
        panel.addComponent(logTextBox);


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
            case Character:
                switch (keyStroke.getCharacter()) {
                    case 'c':
                        window.close();
                        break;
                    case 'a':
                        startVerifier();
                        break;
                    case 's':
                        stopVerifier();
                        break;
                    default:
                }
                break;
            default:
        }
    }


    private void stopVerifier() {
        if (execution != null) {
            execution.cancel(true);
            execution = null;
            runningLabel.setText("Not Running...");
        }
    }

    private void startVerifier() {
        if (execution == null) {
            visited.set(0);
            final StorageProvider provider = storageProviderFactory.createProvider();
            execution = executorService.submit(() -> {
                new BonsaiTraversal(provider, this).traverse();
                runningLabel.setText("NotRunning");

            });
            runningLabel.setText("Running...");
        }
    }

    @Override
    public void onUnhandledInput(final Window basePane, final KeyStroke keyStroke, final AtomicBoolean hasBeenHandled) {

    }

    @Override
    public void root(final Bytes32 hash) {
        logTextBox.addLine("Working with root " + hash);
    }

    @Override
    public void missingCodeHash(final Hash codeHash, final Hash accountHash) {
        logTextBox.addLine(String.format("missing code hash %s for account %s", codeHash, accountHash));

    }

    @Override
    public void invalidCode(final Hash accountHash, final Hash codeHash, final Hash foundCodeHash) {
        logTextBox.addLine(String.format("invalid code for account %s (expected %s and found %s)", accountHash, codeHash, foundCodeHash));
    }

    @Override
    public void missingValueForNode(final Bytes32 hash) {
        logTextBox.addLine("Missing value for node " + hash.toHexString());

    }

    @Override
    public void visited(final BonsaiTraversalTrieType type) {
        counterLabel.setText(String.valueOf(visited.incrementAndGet()));
    }

    @Override
    public void missingAccountTrieForHash(final Bytes32 hash, final Bytes location) {
        logTextBox.addLine(String.format("missing account trie node for hash %s and location %s", hash, location));

    }

    @Override
    public void invalidAccountTrieForHash(final Bytes32 hash, final Bytes location, final Hash foundHashNode) {
        logTextBox.addLine(String.format("invalid account trie node for hash %s and location %s (found %s)", hash, location, foundHashNode));
    }

    @Override
    public void missingStorageTrieForHash(final Bytes32 hash, final Bytes location) {
        logTextBox.addLine(String.format("missing storage trie node for hash %s and location %s", hash, location));
    }

    @Override
    public void invalidStorageTrieForHash(final Bytes32 hash, final Bytes location, final Hash foundHashNode) {
        logTextBox.addLine(String.format("invalid storage trie node for hash %s and location %s (found %s)", hash, location, foundHashNode));
    }
}

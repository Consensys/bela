package org.hyperledger.bela.windows;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.bela.utils.bonsai.BonsaiListener;
import org.hyperledger.bela.utils.bonsai.BonsaiTraversal;
import org.hyperledger.bela.utils.bonsai.BonsaiTraversalTrieType;
import org.hyperledger.besu.datatypes.Hash;

import static org.hyperledger.bela.windows.Constants.KEY_CLOSE;
import static org.hyperledger.bela.windows.Constants.KEY_START;

public class BonsaiTreeVerifierWindow implements LanternaWindow, BonsaiListener {
    private BasicWindow window;
    private final StorageProviderFactory storageProviderFactory;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Future<?> execution;
    private final Label runningLabel = new Label("Not Running...");
    private final Label counterLabel = new Label("0");
    AtomicInteger visited = new AtomicInteger(0);
    private final TextBox logTextBox = new TextBox(new TerminalSize(80, 7));

    public BonsaiTreeVerifierWindow(final StorageProviderFactory storageProviderFactory) {
        this.storageProviderFactory = storageProviderFactory;
        logTextBox.setReadOnly(true);
    }

    @Override
    public String label() {
        return "Bonsai Tree Verifier";
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
                .addControl("Start", KEY_START, this::startVerifier)
                .addControl("Close", KEY_CLOSE, window::close);
        window.addWindowListener(controls);
        panel.addComponent(controls.createComponent());


        panel.addComponent(runningLabel);
        panel.addComponent(counterLabel);
        panel.addComponent(logTextBox);


        window.setComponent(panel);

        return window;
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
            runningLabel.setText("Initialising...");
            Thread.yield();
            logTextBox.setText("");
            visited.set(0);
            execution = executorService.submit(() -> {
                new BonsaiTraversal(storageProviderFactory.createProvider(), this).traverse();
                stopVerifier();
            });
            runningLabel.setText("Running...");
        }
    }

    @Override
    public void root(final Bytes32 hash) {
        logTextBox.setText("Working with root " + hash);
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
        Thread.yield();
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

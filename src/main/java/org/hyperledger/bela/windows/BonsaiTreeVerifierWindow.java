package org.hyperledger.bela.windows;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.context.BelaContext;
import org.hyperledger.bela.dialogs.BelaDialog;
import org.hyperledger.bela.utils.bonsai.BonsaiListener;
import org.hyperledger.bela.utils.bonsai.BonsaiTraversal;
import org.hyperledger.bela.utils.bonsai.BonsaiTraversalTrieType;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.storage.StorageProvider;

import static kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory.getLogger;
import static org.hyperledger.bela.windows.Constants.KEY_LOOKUP_BY_HASH;
import static org.hyperledger.bela.windows.Constants.KEY_START;
import static org.hyperledger.bela.windows.Constants.KEY_STOP;

public class BonsaiTreeVerifierWindow extends AbstractBelaWindow implements BonsaiListener {
    public static final String NOT_RUNNING = "Not Running...";
    private static final LambdaLogger log = getLogger(BonsaiTreeVerifierWindow.class);
    private final WindowBasedTextGUI gui;
    private final BelaContext belaContext;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Label runningLabel = new Label(NOT_RUNNING);
    private final Label counterLabel = new Label("0");
    private final TextBox logTextBox = new TextBox(new TerminalSize(120, 30));
    private final AtomicReference<BonsaiTraversal> bonsaiTraversal = new AtomicReference<>();
    AtomicLong visited = new AtomicLong(0);
    private Future<?> execution;

    public BonsaiTreeVerifierWindow(final WindowBasedTextGUI gui, final BelaContext belaContext) {
        this.gui = gui;
        this.belaContext = belaContext;
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
    public KeyControls createControls() {
        return new KeyControls()
                .addControl("Start", KEY_START, this::startVerifier)
                .addControl("Stop", KEY_STOP, this::stopVerifier)
                .addControl("Hash?", KEY_LOOKUP_BY_HASH, this::startFromHash);
    }

    private void startFromHash() {
        final String s = TextInputDialog.showDialog(gui, "Enter Node Hash", "Hash", "0x033424ed0313251e88bd49730108ee8901a506c0a4309478fbf8a14adea9d02a");
        if (s == null) {
            return;
        }
        try {
            startVerifier(Hash.fromHexString(s));
        } catch (Exception e) {
            log.error("There was an error when moving browser", e);
            BelaDialog.showException(gui, e);
        }
    }

    private synchronized void startVerifier(final Hash fromHash) {
        if (execution != null && !execution.isDone()) {
            BelaDialog.showMessage(gui, "Already running", "Already running");
            return;
        }
        runningLabel.setText("Initialising...");
        Thread.yield();
        logTextBox.setText("");
        visited.set(0);
        final StorageProvider storageProvider = belaContext.getProvider();
        this.bonsaiTraversal.set(new BonsaiTraversal(storageProvider, this));
        execution = executorService.submit(() -> {
            try {
                runningLabel.setText("Running...");
                this.bonsaiTraversal.get().traverse(fromHash);
                runningLabel.setText("Stopped...");
            } catch (Exception e) {
                runningLabel.setText("There was an error...");
                log.error("There was an error", e);
            }
        });
    }

    @Override
    public Panel createMainPanel() {
        Panel panel = new Panel(new LinearLayout());

        panel.addComponent(runningLabel);
        panel.addComponent(counterLabel);
        panel.addComponent(logTextBox);

        return panel;
    }


    private synchronized void stopVerifier() {
        if (execution != null) {
            bonsaiTraversal.get().stop();
            execution = null;
        }
    }

    private synchronized void startVerifier() {
        if (execution != null && !execution.isDone()) {
            BelaDialog.showMessage(gui, "Already running", "Already running");
            return;
        }
        runningLabel.setText("Initialising...");
        Thread.yield();
        counterLabel.setText("0");
        logTextBox.setText("");
        visited.set(0);

        final StorageProvider provider = belaContext.getProvider();
        this.bonsaiTraversal.set(new BonsaiTraversal(provider, this));

        execution = executorService.submit(() -> {
            try {
                runningLabel.setText("Running...");
                this.bonsaiTraversal.get().traverse();
                runningLabel.setText("Stopped...");
            } catch (Exception e) {
                runningLabel.setText("There was an error...");
                log.error("There was an error", e);
            } finally {
                stopVerifier();
            }
        });
    }

    @Override
    public void root(final Bytes32 hash) {
        logTextBox.setText("Working with root " + hash);
    }

    @Override
    public void missingCodeHash(final Hash codeHash, final Hash accountHash) {
        log.info("Missing code hash {} for account {}", codeHash, accountHash);
        logTextBox.addLine(String.format("missing code hash %s for account %s", codeHash, accountHash));
    }

    @Override
    public void invalidCode(final Hash accountHash, final Hash codeHash, final Hash foundCodeHash) {
        log.info("Invalid code for account {} with code hash {} found code hash {}", accountHash, codeHash, foundCodeHash);
        logTextBox.addLine(String.format("invalid code for account %s (expected %s and found %s)", accountHash, codeHash, foundCodeHash));
    }

    @Override
    public void missingValueForNode(final Bytes32 hash) {
        log.info("Missing value for node {}", hash);
        logTextBox.addLine("Missing value for node " + hash.toHexString());

    }

    @Override
    public void visited(final BonsaiTraversalTrieType type) {
        counterLabel.setText(String.valueOf(visited.incrementAndGet()));
        Thread.yield();
    }

    @Override
    public void missingAccountTrieForHash(final Bytes32 hash, final Bytes location) {
        log.info("Missing account trie for hash {} at location {}", hash, location);
        logTextBox.addLine(String.format("missing account trie node for hash %s and location %s", hash, location));

    }

    @Override
    public void invalidAccountTrieForHash(final Bytes32 hash, final Bytes location, final Hash foundHashNode) {
        log.info("Invalid account trie for hash {} at location {} found hash {}", hash, location, foundHashNode);
        logTextBox.addLine(String.format("invalid account trie node for hash %s and location %s (found %s)", hash, location, foundHashNode));
    }

    @Override
    public void missingStorageTrieForHash(final Bytes32 accountHash, final Bytes32 hash, final Bytes location) {
        log.info("Missing storage trie for hash {} at location {}", hash, location);
        logTextBox.addLine(String.format("accounthash %s missing storage trie node for hash %s and location %s", accountHash, hash, location));
    }

    @Override
    public void invalidStorageTrieForHash(final Bytes32 accountHash, final Bytes32 hash, final Bytes location, final Hash foundHashNode) {
        log.info("Invalid storage trie for account {} hash {} at location {} found hash {}",accountHash, hash, location, foundHashNode);
        logTextBox.addLine(String.format("invalid storage trie node for account %s hash %s and location %s (found %s)",accountHash, hash, location, foundHashNode));
    }

    @Override
    public void differentDataInFlatDatabaseForAccount(final Hash accountHash) {
        log.info("Different data in flat database for account {}", accountHash);
        logTextBox.addLine(String.format("inconsistent data in flat database for account %s", accountHash));
    }

    @Override
    public void differentDataInFlatDatabaseForStorage(final Bytes32 accountHash, final Bytes32 slotHash) {
        log.info("Different data in flat database for account {} and slot {}", accountHash, slotHash);
        logTextBox.addLine(String.format("inconsistent data in flat database for account %s on slot %s", accountHash, slotHash));
    }

    @Override
    public void close() {
        executorService.shutdownNow();
        if (execution!=null){
            bonsaiTraversal.get().stop();
        }
    }
}

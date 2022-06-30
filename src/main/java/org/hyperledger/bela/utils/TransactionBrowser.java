package org.hyperledger.bela.utils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.googlecode.lanterna.gui2.Panel;
import org.hyperledger.bela.components.BelaComponent;
import org.hyperledger.bela.components.TransactionPanel;
import org.hyperledger.bela.model.TransactionResult;
import org.hyperledger.besu.datatypes.Hash;

public class TransactionBrowser {

    private final List<TransactionResult> transactionResults;
    private TransactionPanel transactionPanel;
    private int transactionIndex;

    public TransactionBrowser(final BlockChainContext context, final Hash blockHash) {
        this.transactionResults = context.getBlockchain().getBlockBody(blockHash)
                .map(bb ->
                        bb.getTransactions().stream()
                                .map(TransactionResult::new).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
        if (!transactionResults.isEmpty()) {
            transactionPanel = new TransactionPanel(transactionResults.get(0));
        }
    }

    public BelaComponent<Panel> transactionPanel() {
        return transactionPanel;
    }

    public TransactionBrowser moveBackward() {
        transactionIndex = transactionIndex == 0 ? transactionResults.size() - 1 : transactionIndex - 1;
        updatePanels(transactionResults.get(transactionIndex));
        return this;
    }

    public TransactionBrowser moveForward() {
        transactionIndex = transactionIndex == transactionResults.size() - 1 ? 0 : transactionIndex + 1;
        updatePanels(transactionResults.get(transactionIndex));
        return this;
    }

    public void moveByHash(final Hash transactionHex) {
        final Optional<TransactionResult> maybeRransactionResult = transactionResults.stream()
                .filter(t -> t.getHash().startsWith(transactionHex.toHexString())).findFirst();
        if (maybeRransactionResult.isPresent()) {
            final TransactionResult transactionResult = maybeRransactionResult.get();
            transactionIndex = transactionResults.indexOf(transactionResult);
            updatePanels(transactionResult);
        }
    }

    private void updatePanels(final TransactionResult transactionResult) {
        transactionPanel.updateWithTransaction(transactionResult);
    }

    public TransactionResult getTransactionResult() {
        return transactionResults.get(transactionIndex);
    }
}

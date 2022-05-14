package org.hyperledger.bela.utils;

import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.lanterna.gui2.Panel;
import org.hyperledger.bela.components.LanternaComponent;
import org.hyperledger.bela.components.TraceTransactionPanel;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.tracing.flat.FlatTrace;

public class TransactionTraceBrowser {

    final ObjectMapper mapper = new ObjectMapper();
    private final List<String> traces;
    private TraceTransactionPanel transactionTracePanel;
    private int traceIndex;

    public TransactionTraceBrowser(final List<FlatTrace> traces) {
        this.traces = traces.stream().map(this::jsonStringFromTrace).collect(Collectors.toList());
        if (!this.traces.isEmpty()) {
            transactionTracePanel = new TraceTransactionPanel(this.traces.get(0));
        }
    }

    public LanternaComponent<Panel> transactionTracePanel() {
        return transactionTracePanel;
    }

    public TransactionTraceBrowser moveBackward() {
        traceIndex = traceIndex == 0 ? traces.size() - 1 : traceIndex - 1;
        updatePanels(traces.get(traceIndex));
        return this;
    }

    public TransactionTraceBrowser moveForward() {
        traceIndex = traceIndex == traces.size() - 1 ? 0 : traceIndex + 1;
        updatePanels(traces.get(traceIndex));
        return this;
    }

    private void updatePanels(final String trace) {
        transactionTracePanel.updateWithTrace(trace);
    }

    private String jsonStringFromTrace(final FlatTrace trace) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(trace);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to create json representation of trace", e);
        }
    }

}

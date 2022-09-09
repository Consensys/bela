package org.hyperledger.bela.windows;

import java.util.List;
import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.utils.BlockChainContext;
import org.hyperledger.bela.utils.TransactionTraceBrowser;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.tracing.flat.FlatTrace;
import org.jetbrains.annotations.NotNull;

public class TransactionTraceBrowserWindow extends AbstractBelaWindow {

    private static final String[] PREV_NEXT_TRACE_COMMANDS = {"prev Frame", "'<-'", "next Frame", "'->'", "Close", "'c'"};
    private final BlockChainContext context;
    private final WindowBasedTextGUI gui;
    private final List<FlatTrace> traces;
    private TransactionTraceBrowser browser;

    public TransactionTraceBrowserWindow(final BlockChainContext context,
                                         final WindowBasedTextGUI gui, final List<FlatTrace> traces) {
        this.context = context;
        this.gui = gui;
        this.traces = traces;
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
    public String label() {
        return "Transaction Browser";
    }

    @Override
    public MenuGroup group() {
        return MenuGroup.DATABASE;
    }

    @Override
    public KeyControls createControls() {
        return new KeyControls().addControl("Back", Constants.KEY_BACK, this::back)
                .addControl("Forward", Constants.KEY_FORWARD, this::forward);
    }

    private void forward() {
        browser = browser.moveForward();

    }

    private void back() {
        browser = browser.moveBackward();
    }

    @Override
    public Panel createMainPanel() {
        browser = new TransactionTraceBrowser(traces);

        Panel panel = new Panel(new LinearLayout());

        Panel commands = getCommandsPanel(PREV_NEXT_TRACE_COMMANDS);

        // add possible actions
        panel.addComponent(commands);

        // add transaction detail panel
        panel.addComponent(browser.transactionTracePanel().createComponent());

        return panel;
    }
}

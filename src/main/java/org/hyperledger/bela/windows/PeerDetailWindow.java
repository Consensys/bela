package org.hyperledger.bela.windows;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.bela.dialogs.BelaDialog;
import org.hyperledger.bela.utils.ConnectionMessageMonitor;
import org.hyperledger.besu.ethereum.p2p.peers.Peer;

import static org.hyperledger.bela.windows.Constants.KEY_CLOSE;

public class PeerDetailWindow implements BelaWindow {
    private Peer activePeer;
    private List<List<ConnectionMessageMonitor.DirectedMessage>> conversations = new ArrayList<>();
    private final WindowBasedTextGUI gui;

    public PeerDetailWindow(final WindowBasedTextGUI gui) {
        this.gui = gui;
    }

    @Override
    public String label() {
        return "Peer Detail";
    }

    @Override
    public MenuGroup group() {
        return MenuGroup.P2P;
    }

    @Override
    public Window createWindow() {
        final Window window = new BasicWindow(label());
        window.setHints(List.of(Window.Hint.FULL_SCREEN));
        Panel panel = new Panel(new LinearLayout());

        KeyControls controls = new KeyControls()
                .addControl("Close", KEY_CLOSE, window::close);
        window.addWindowListener(controls);
        panel.addComponent(controls.createComponent());

        panel.addComponent(new Label("Peer ID: " + activePeer.getId()));
        panel.addComponent(new Label("Peer URL: " + activePeer.getEnodeURLString()));
        final ActionListBox actionListBox = new ActionListBox(new TerminalSize(20, 10));

        for (List<ConnectionMessageMonitor.DirectedMessage> conversation : conversations) {
            actionListBox.addItem(""+ conversation.size(),() -> {
                final List<String> messages = conversation.stream().map(m->{
                    return m.toString()+" "+m.getMessageData().getSize();
                }).collect(Collectors.toList());

                BelaDialog.showListDialog(gui,"Conversation",messages);

            });
        }


        panel.addComponent(actionListBox);

        window.setComponent(panel);
        return window;
    }

    public void setActivePeer(final Peer peer, final List<List<ConnectionMessageMonitor.DirectedMessage>> conversations) {
        this.activePeer = peer;
        this.conversations = conversations;
    }
}

package org.hyperledger.bela.windows;

import java.util.List;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import org.hyperledger.bela.components.KeyControls;
import org.hyperledger.besu.ethereum.p2p.peers.Peer;

import static org.hyperledger.bela.windows.Constants.KEY_CLOSE;

public class PeerDetailWindow implements BelaWindow {
    private Peer activePeer;

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
        return window;
    }

    public void setActivePeer(final Peer peer) {
        this.activePeer = peer;
    }
}

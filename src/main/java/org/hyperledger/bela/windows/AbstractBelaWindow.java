package org.hyperledger.bela.windows;

import java.util.List;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import org.hyperledger.bela.components.KeyControls;

import static org.hyperledger.bela.windows.Constants.KEY_CLOSE;

public abstract class AbstractBelaWindow implements BelaWindow {

    @Override
    public Window createWindow() {
        BasicWindow window = new BasicWindow(label());
        window.setHints(List.of(Window.Hint.FULL_SCREEN));

        Panel fullScreenPanel = new Panel();


        KeyControls controls = createControls()
                .addControl("Close", KEY_CLOSE, window::close);
        window.addWindowListener(controls);
        fullScreenPanel.addComponent(controls.createComponent());

        fullScreenPanel.addComponent(new EmptySpace());

        fullScreenPanel.addComponent(createMainPanel());
        window.setComponent(fullScreenPanel);

        return window;
    }
}

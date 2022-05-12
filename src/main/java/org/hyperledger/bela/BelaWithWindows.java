package org.hyperledger.bela;

import java.io.IOException;
import java.nio.file.Path;
import java.util.prefs.Preferences;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import org.hyperledger.bela.config.BelaConfigurationImpl;
import org.hyperledger.bela.windows.BlockChainBrowserWindow;
import org.hyperledger.bela.windows.ConfigWindow;
import org.hyperledger.bela.windows.MainWindow;

public class BelaWithWindows {
    public static void main(String[] args) throws Exception {
        DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();
        try (Screen screen = terminalFactory.createScreen();) {
            screen.startScreen();
            final WindowBasedTextGUI textGUI = new MultiWindowTextGUI(screen);

            MainWindow mainWindow = new MainWindow(textGUI);

            final Preferences preferences = Preferences.userNodeForPackage(Bela.class);
            final ConfigWindow config = new ConfigWindow(preferences);
            mainWindow.registerWindow(config);
            mainWindow.registerWindow(new BlockChainBrowserWindow(config));

            textGUI.addWindowAndWait(mainWindow.createWindow());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

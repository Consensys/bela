package org.hyperledger.bela;

import java.io.IOException;
import java.util.prefs.Preferences;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.bela.windows.BlockChainBrowserWindow;
import org.hyperledger.bela.windows.BonsaiTreeVerifierWindow;
import org.hyperledger.bela.windows.Constants;
import org.hyperledger.bela.windows.DatabaseConversionWindow;
import org.hyperledger.bela.windows.LogoWindow;
import org.hyperledger.bela.windows.MainWindow;
import org.hyperledger.bela.windows.P2PManagementWindow;
import org.hyperledger.bela.windows.SettingsWindow;

import static org.hyperledger.bela.windows.Constants.DATA_PATH;

public class Bela {
    public static void main(String[] args) throws Exception {
        final Preferences preferences = Preferences.userNodeForPackage(Bela.class);
        processArgs(preferences, args);

        DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();
        try (Screen screen = terminalFactory.createScreen();
             StorageProviderFactory storageProviderFactory = new StorageProviderFactory(preferences)) {
            screen.startScreen();
            final WindowBasedTextGUI gui = new MultiWindowTextGUI(screen);

            MainWindow mainWindow = new MainWindow(gui);
            final SettingsWindow config = new SettingsWindow(gui, preferences);
            mainWindow.registerWindow(config);
            mainWindow.registerWindow(new BlockChainBrowserWindow(storageProviderFactory, gui, preferences));
            mainWindow.registerWindow(new BonsaiTreeVerifierWindow(storageProviderFactory));
            mainWindow.registerWindow(new DatabaseConversionWindow(storageProviderFactory));
            mainWindow.registerWindow(new LogoWindow());
            mainWindow.registerWindow(new P2PManagementWindow(gui, storageProviderFactory));
            final Window window = mainWindow.createWindow();
            gui.addWindowAndWait(window);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Bye bye...");
    }

    private static void processArgs(final Preferences preferences, final String[] args) {
        if (args.length > 0) {
            preferences.put(DATA_PATH, args[0]);
            preferences.put(Constants.STORAGE_PATH, args[0] + "/database");
        }
        if (args.length > 1) {
            preferences.put(Constants.GENESIS_PATH, args[1]);
        }
    }
}

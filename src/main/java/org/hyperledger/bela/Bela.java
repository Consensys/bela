package org.hyperledger.bela;

import java.io.IOException;
import java.nio.file.Path;
import java.util.prefs.Preferences;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.bundle.LanternaThemes;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import org.hyperledger.bela.context.MainNetContext;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.bela.windows.BlockChainBrowserWindow;
import org.hyperledger.bela.windows.BonsaiStorageBrowserWindow;
import org.hyperledger.bela.windows.BonsaiTreeVerifierWindow;
import org.hyperledger.bela.windows.BonsaiTrieLogLayersViewer;
import org.hyperledger.bela.windows.Constants;
import org.hyperledger.bela.windows.DatabaseConversionWindow;
import org.hyperledger.bela.windows.LogoWindow;
import org.hyperledger.bela.windows.MainWindow;
import org.hyperledger.bela.windows.P2PManagementWindow;
import org.hyperledger.bela.windows.RocksDBViewer;
import org.hyperledger.bela.windows.SegmentManipulationWindow;
import org.hyperledger.bela.windows.SettingsWindow;
import org.hyperledger.besu.BesuInfo;
import org.hyperledger.besu.ethereum.worldstate.DataStorageConfiguration;
import org.hyperledger.besu.plugin.services.storage.DataStorageFormat;

import static kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory.getLogger;
import static org.hyperledger.bela.config.BesuDataStorageConfigurationUtil.getDataStorageConfiguration;
import static org.hyperledger.bela.windows.Constants.*;

public class Bela {
    private static final LambdaLogger log = getLogger(Bela.class);

    public static void main(String[] args) throws Exception {
        final Preferences preferences = Preferences.userNodeForPackage(Bela.class);
        processArgs(preferences, args);

        DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();
        terminalFactory.setInitialTerminalSize(new TerminalSize(120, 35));
        System.out.println("Built with Besu Version " + BesuInfo.version());
        try (Screen screen = terminalFactory.createScreen();
             ) {
            screen.startScreen();

            final WindowBasedTextGUI gui = new MultiWindowTextGUI(screen);

            StorageProviderFactory storageProviderFactory = new StorageProviderFactory(gui,preferences);
            DataStorageConfiguration dataStorageConfig = getDataStorageConfiguration(Path.of(preferences.get(DATA_PATH, DATA_PATH_DEFAULT)));
            DataStorageFormat dataStorageFormat = dataStorageConfig.getDataStorageFormat();
            MainNetContext mainNetContext = new MainNetContext(dataStorageConfig, storageProviderFactory);

            gui.setTheme(LanternaThemes.getRegisteredTheme(preferences.get(THEME_KEY, DEFAULT_THEME)));
            MainWindow mainWindow = new MainWindow(gui, preferences);
            final SettingsWindow config = new SettingsWindow(gui, preferences);
            mainWindow.registerWindow(config);
            mainWindow.registerWindow(new BlockChainBrowserWindow(mainNetContext, gui, preferences));
            mainWindow.registerWindow(new DatabaseConversionWindow(mainNetContext));
            mainWindow.registerWindow(new LogoWindow());
            mainWindow.registerWindow(new P2PManagementWindow(gui, mainNetContext));
            mainWindow.registerWindow(new RocksDBViewer(gui, mainNetContext));
            // TODO SegmentManipulationWindow is the main culprit for the storageProviderFactory dependency
            mainWindow.registerWindow(new SegmentManipulationWindow(gui, storageProviderFactory, preferences));
            if (DataStorageFormat.BONSAI.equals(dataStorageFormat)) {
              mainWindow.registerWindow(new BonsaiStorageBrowserWindow(gui, mainNetContext));
              mainWindow.registerWindow(new BonsaiTrieLogLayersViewer(gui, mainNetContext));
              mainWindow.registerWindow(new BonsaiTreeVerifierWindow(gui, mainNetContext));
            }
            final Window window = mainWindow.createWindow();
            gui.addWindowAndWait(window);
            mainWindow.close();
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

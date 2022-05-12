package org.hyperledger.bela.windows;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowListener;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import org.hyperledger.bela.BlockChainBrowser;
import org.hyperledger.bela.config.BelaConfigurationImpl;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueStorageProviderBuilder;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBKeyValueStorageFactory;
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBMetricsFactory;
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBCLIOptions;
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBFactoryConfiguration;
import org.jetbrains.annotations.NotNull;

public class BlockChainBrowserWindow implements LanternaWindow, WindowListener {

    private static final String[] PREV_NEXT_BLOCK_COMMANDS = {"prev Block", "'<-'", "next Block", "'->'", "Close", "'c'"};

    private ConfigWindow config;
    private BlockChainBrowser browser;
    private StorageProvider provider;
    private BasicWindow window;
    private Panel panel;

    public BlockChainBrowserWindow(final ConfigWindow config) {

        this.config = config;
    }

    @Override
    public String label() {
        return "Blockchain Browser";
    }

    @Override
    public MenuGroup group() {
        return MenuGroup.ACTIONS;
    }

    @Override
    public Window createWindow() {
        BelaConfigurationImpl belaConfiguration = config.createBelaConfiguration();
        if (provider != null) {
            try {
                provider.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        provider = createKeyValueStorageProvider(belaConfiguration.getDataPath(), belaConfiguration.getStoragePath());
        browser = BlockChainBrowser.fromProvider(provider);

        // Create window to hold the panel

        window = new BasicWindow("Bela DB Browser");
        window.setHints(List.of(Window.Hint.FULL_SCREEN));

        panel = new Panel(new LinearLayout());

        Panel commands = getCommandsPanel(PREV_NEXT_BLOCK_COMMANDS);

        // add possible actions
        panel.addComponent(commands);

        // add summary panel
        panel.addComponent(browser.showSummaryPanel().createComponent()
                .withBorder(Borders.singleLine()));

        // add block detail panel
        panel.addComponent(browser.blockPanel().createComponent());

        window.addWindowListener(this);
        window.handleInput(new KeyStroke(KeyType.Escape));
        window.handleInput(new KeyStroke(KeyType.ArrowLeft));
        window.handleInput(new KeyStroke(KeyType.ArrowRight));
        window.handleInput(new KeyStroke('c',false,false,false));
        window.setComponent(panel);
        return window;
    }


    private static StorageProvider createKeyValueStorageProvider(
            final Path dataDir, final Path dbDir) {
        return new KeyValueStorageProviderBuilder()
                .withStorageFactory(
                        new RocksDBKeyValueStorageFactory(
                                () ->
                                        new RocksDBFactoryConfiguration(
                                                RocksDBCLIOptions.DEFAULT_MAX_OPEN_FILES,
                                                RocksDBCLIOptions.DEFAULT_MAX_BACKGROUND_COMPACTIONS,
                                                RocksDBCLIOptions.DEFAULT_BACKGROUND_THREAD_COUNT,
                                                RocksDBCLIOptions.DEFAULT_CACHE_CAPACITY),
                                Arrays.asList(KeyValueSegmentIdentifier.values()),
                                RocksDBMetricsFactory.PUBLIC_ROCKS_DB_METRICS))
                .withCommonConfiguration(new BelaConfigurationImpl(dataDir, dbDir))
                .withMetricsSystem(new NoOpMetricsSystem())
                .build();
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
    public void onResized(final Window window, final TerminalSize oldSize, final TerminalSize newSize) {

    }

    @Override
    public void onMoved(final Window window, final TerminalPosition oldPosition, final TerminalPosition newPosition) {

    }

    @Override
    public void onInput(final Window basePane, final KeyStroke keyStroke, final AtomicBoolean deliverEvent) {
        switch(keyStroke.getKeyType()) {
            case ArrowLeft:
                browser = browser.moveBackward();
                break;

            case ArrowRight:
                browser = browser.moveForward();
                break;

            case Escape:
                window.close();
                break;
            case Character:
                switch (keyStroke.getCharacter()) {
                    case 'c':
                        window.close();
                        break;
                    default:
                }
                break;
            default:
        }
    }

    @Override
    public void onUnhandledInput(final Window basePane, final KeyStroke keyStroke, final AtomicBoolean hasBeenHandled) {

    }
}

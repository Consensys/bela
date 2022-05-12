package org.hyperledger.bela.windows;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
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

public class BlockChainBrowserWindow implements LanternaWindow {

    private ConfigWindow config;
    private BlockChainBrowser browser;
    private StorageProvider provider;

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
        provider = createKeyValueStorageProvider(belaConfiguration.getStoragePath(), belaConfiguration.getDataPath());
        browser = BlockChainBrowser.fromProvider(provider);

        // Create window to hold the panel

        BasicWindow window = new BasicWindow("Bela DB Browser");
        window.setHints(List.of(Window.Hint.FULL_SCREEN));
        Panel panel = new Panel(new BorderLayout());

        var blockPanelHolder = new Panel();

        // add summary panel
        panel.addComponent(browser.showSummaryPanel().createComponent()
                .withBorder(Borders.singleLine()), BorderLayout.Location.TOP);

        // add block detail panel
        blockPanelHolder.addComponent(browser.blockPanel().createComponent());
        panel.addComponent(blockPanelHolder, BorderLayout.Location.BOTTOM);

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
}

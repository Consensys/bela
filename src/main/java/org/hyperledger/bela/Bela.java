/*
 *
 *  * Copyright Hyperledger Besu Contributors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  * the License. You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  * specific language governing permissions and limitations under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.hyperledger.bela;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueStorageProviderBuilder;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBKeyValueStorageFactory;
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBMetricsFactory;
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBCLIOptions;
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBFactoryConfiguration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.dialogs.DirectoryDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import org.hyperledger.bela.config.BelaConfigurationImpl;
import org.identityconnectors.common.StringUtil;
import org.jetbrains.annotations.NotNull;

public class Bela {

  private static final String[] PREV_NEXT_BLOCK_COMMANDS = {"prev Block", "'<-'", "next Block", "'->'", "Close", "'c'"};

  public static void main(final String[] args) {

    try (Terminal terminal = new DefaultTerminalFactory().createTerminal()) {
      Screen screen = new TerminalScreen(terminal);
      screen.startScreen();
      // Create gui and start gui
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);

      // Create window to hold the panel
      BasicWindow window = new BasicWindow("Bela DB Browser");
      window.setHints(List.of(Window.Hint.FULL_SCREEN));

      Path dataDir = List.of(args).stream()
          .findFirst()
          .filter(StringUtil::isNotBlank)
          .map(Paths::get)
          .orElseGet(() -> resolveDataDir(gui));

      final BlockChainBrowser browser = resolveDb(gui, dataDir);

      Panel panel = new Panel(new LinearLayout());

      Panel commands = getCommandsPanel(PREV_NEXT_BLOCK_COMMANDS);

      // add possible actions
      panel.addComponent(commands);

      // add summary panel
      panel.addComponent(browser.showSummaryPanel().createComponent()
          .withBorder(Borders.singleLine()));

      // add block detail panel
      panel.addComponent(browser.blockPanel().createComponent());

      window.setComponent(panel);
      gui.addWindow(window);
      gui.updateScreen();

      mainLoop(screen, panel, browser, gui);

    } catch (final Exception e) {
      System.out.println(e.getMessage());
      System.out.println(e.getStackTrace());
    }
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

  private static void mainLoop(final Screen screen, final Panel panel, BlockChainBrowser browser, final WindowBasedTextGUI gui) {
    mainLoop:
    while(true) {
      KeyStroke keyStroke = null;
      try {
        keyStroke = screen.readInput();
      } catch (IOException e) {
        e.printStackTrace();
      }
      switch(keyStroke.getKeyType()) {
        case EOF:
        case Escape:
          break;

        case ArrowUp:
          break;

        case ArrowDown:
          break;

        case ArrowLeft:
          browser = browser.moveBackward();
          panel.removeAllComponents();
          panel.addComponent(getCommandsPanel(PREV_NEXT_BLOCK_COMMANDS));
          panel.addComponent(browser.showSummaryPanel().createComponent()
                  .withBorder(Borders.singleLine()));
          panel.addComponent(browser.blockPanel().createComponent());
          break;

        case ArrowRight:
          browser = browser.moveForward();
          panel.removeAllComponents();
          panel.addComponent(getCommandsPanel(PREV_NEXT_BLOCK_COMMANDS));
          panel.addComponent(browser.showSummaryPanel().createComponent()
                  .withBorder(Borders.singleLine()));
          panel.addComponent(browser.blockPanel().createComponent());
          break;

        case Character:
          switch (keyStroke.getCharacter()) {
            case 'c':
              System.exit(0);
              break;
            default:
          }
        break;
        default:
      }

        try {
          gui.updateScreen();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
  }

  private static BlockChainBrowser resolveDb(MultiWindowTextGUI gui, Path dataDir) {
    while (true) {
      try {
        StorageProvider provider = createKeyValueStorageProvider(dataDir, dataDir.resolve("database"));
        return BlockChainBrowser.fromProvider(provider);

      } catch (IllegalArgumentException ex) {
        new MessageDialogBuilder()
            .setTitle("Error loading blockchain database")
            .setText("Failed to load blockchain data: \n" + ex.getMessage())
            .build()
            .showDialog(gui);
      }
      dataDir = resolveDataDir(gui);
    }
  }
  private static Path resolveDataDir(MultiWindowTextGUI gui) {
    return new DirectoryDialogBuilder()
        .setTitle("Open Data Directory")
        .setDescription("Choose a directory")
        .setActionLabel("Open")
        .build()
        .showDialog(gui).toPath();
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

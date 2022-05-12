package org.hyperledger.bela.config;

import org.hyperledger.besu.plugin.services.BesuConfiguration;

import java.nio.file.Path;

public class BelaConfigurationImpl implements BesuConfiguration {

  private final Path storagePath;
  private final Path dataPath;

  public BelaConfigurationImpl(final Path dataPath, final Path storagePath) {
    this.dataPath = dataPath;
    this.storagePath = storagePath;
  }

  @Override
  public Path getStoragePath() {
    return storagePath;
  }

  @Override
  public Path getDataPath() {
    return dataPath;
  }
}

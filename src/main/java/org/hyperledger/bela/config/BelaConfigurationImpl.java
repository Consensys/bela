package org.hyperledger.bela.config;

import org.hyperledger.besu.plugin.services.BesuConfiguration;

import java.nio.file.Path;

public class BelaConfigurationImpl implements BesuConfiguration {

  private Path storagePath;
  private  Path dataPath;

  public BelaConfigurationImpl(final Path dataPath, final Path storagePath) {
    this.dataPath = dataPath;
    this.storagePath = storagePath;
  }

  @Override
  public Path getStoragePath() {
    return storagePath;
  }

  public void setStoragePath(final Path storagePath) {
    this.storagePath = storagePath;
  }

  public void setDataPath(final Path dataPath) {
    this.dataPath = dataPath;
  }

  @Override
  public Path getDataPath() {
    return dataPath;
  }
}

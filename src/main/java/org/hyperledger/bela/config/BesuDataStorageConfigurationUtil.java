package org.hyperledger.bela.config;

import org.hyperledger.besu.ethereum.worldstate.DataStorageConfiguration;
import org.hyperledger.besu.ethereum.worldstate.ImmutableDataStorageConfiguration;
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.DatabaseMetadata;

import java.io.IOException;
import java.nio.file.Path;

import static org.hyperledger.besu.ethereum.worldstate.DataStorageConfiguration.DEFAULT_CONFIG;

public class BesuDataStorageConfigurationUtil {

    public static DataStorageConfiguration getDataStorageConfiguration(Path dataDir) {
        DatabaseMetadata databaseMetadata = null;
        try {
            databaseMetadata = DatabaseMetadata.lookUpFrom(dataDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ImmutableDataStorageConfiguration.copyOf(DEFAULT_CONFIG).withDataStorageFormat(databaseMetadata.getVersionedStorageFormat().getFormat());
    }
}

package org.hyperledger.bela.components;

import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;

public class BonsaiStorageView implements LanternaComponent<Panel>{

    public static final Panel PANEL = new Panel(new LinearLayout(Direction.HORIZONTAL));
    private StorageProviderFactory storageProviderFactory;

    public BonsaiStorageView(final StorageProviderFactory storageProviderFactory) {
        this.storageProviderFactory = storageProviderFactory;
    }

    @Override
    public Panel createComponent() {
        return PANEL;
    }

    public void findRoot() {
        final KeyValueStorage trieBranchStorage = storageProviderFactory.createProvider()
                .getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.TRIE_BRANCH_STORAGE);
        final Hash x =
                trieBranchStorage
                        .get(Bytes.EMPTY.toArrayUnsafe())
                        .map(Bytes::wrap)
                        .map(Hash::hash)
                        .orElseThrow();
//        root = getAccountNodeValue(x, Bytes.EMPTY);
    }

}

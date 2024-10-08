package org.hyperledger.bela.components.bonsai;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Panel;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.trie.Node;
import org.hyperledger.besu.ethereum.trie.patricia.SimpleMerklePatriciaTrie;
import org.hyperledger.besu.ethereum.worldstate.StateTrieAccountValue;

class AccountValueNode extends AbstractBonsaiNode {
    private final Hash accountHash;
    private final Bytes accountValueBytes;
    private final BonsaiStorageView bonsaiStorageView;
    private final StateTrieAccountValue accountValue;
    private final StorageLocation storageLocation;

    public AccountValueNode(final BonsaiStorageView bonsaiStorageView, final Hash accountHash, final Bytes accountValueBytes, final StorageLocation storageLocation) {
        super("Account " + storageLocation);
        this.bonsaiStorageView = bonsaiStorageView;
        this.accountHash = accountHash;
        this.accountValueBytes = accountValueBytes;
        accountValue = StateTrieAccountValue.readFrom(RLP.input(accountValueBytes));
        this.storageLocation = storageLocation;
    }

    @Override
    public List<BonsaiNode> getChildren() {
        final List<BonsaiNode> children = new ArrayList<>();

        if (storageLocation == StorageLocation.BONSAI) {
            final Optional<Bytes> accountInFlatDB = bonsaiStorageView.getAccountFromFlatDatabase(accountHash);
            if (accountInFlatDB.isPresent()) {
                children.add(new AccountValueNode(bonsaiStorageView, accountHash, accountInFlatDB.get(), StorageLocation.FLAT));
            } else {
                children.add(new LabelNode("No Flat DB Account", "Flat DB value not found "));
            }
        }
        // check the account in the flat database

        // Add code, if appropriate
        if (!accountValue.getCodeHash().equals(Hash.EMPTY)) {
            // traverse code
            final Optional<Bytes> code = bonsaiStorageView.getCode(accountValue.getCodeHash()).or(() -> bonsaiStorageView.getCode(accountHash));
            if (code.isEmpty()) {
                children.add(new LabelNode("Missing Code", "Code hash: " + accountValue.getCodeHash().toHexString() + "; account hash = " + accountHash.toHexString()));
            } else {
                final Hash foundCodeHash = Hash.hash(code.orElseThrow());
                if (!foundCodeHash.equals(accountValue.getCodeHash())) {
                    children.add(new LabelNode("Invalid Code hash", "found: " + foundCodeHash + " account code hash: " + accountValue.getCodeHash()));
                } else {
                    children.add(new LabelNode("Code", foundCodeHash.toHexString()));
                }
            }
        } else {
            children.add(new LabelNode("No Code", "No code"));
        }
        // Add storage, if appropriate
        if (!accountValue.getStorageRoot().equals(SimpleMerklePatriciaTrie.EMPTY_TRIE_NODE_HASH)) {
            final Node<Bytes> storageNodeValue = bonsaiStorageView.getStorageNodeValue(accountValue.getStorageRoot(), accountHash, Bytes.EMPTY);
            if (storageNodeValue == null) {
                children.add(new LabelNode("Missing Storage", "Storage root: " + accountValue.getStorageRoot()
                        .toHexString()));
            } else {
                children.add(new StorageTreeNode(bonsaiStorageView, accountHash, storageNodeValue, accountValue.getStorageRoot()));
            }
        } else {
            children.add(new LabelNode("No Storage", "No storage"));
        }
        return children;
    }

    @Override
    public Component createComponent() {
        Panel panel = new Panel();
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Account Hash:", accountHash.toHexString())
                .createComponent());

        panel.addComponent(LabelWithTextBox.labelWithTextBox("Balance:", accountValue.getBalance().getValue()
                        .toString())
                .createComponent());
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Nonce:", String.valueOf(accountValue.getNonce()))
                .createComponent());
        panel.addComponent(LabelWithTextBox.labelWithTextBox("RLP:", accountValueBytes.toHexString())
                .createComponent());
        panel.addComponent(LabelWithTextBox.labelWithTextBox("Storage Location:", storageLocation.toString())
                .createComponent());

        return panel.withBorder(Borders.singleLine("Account Value Node"));
    }

    @Override
    public void log() {
        log.info("Account Value Node: {}", accountHash.toHexString());
        log.info("Balance: {}", accountValue.getBalance().getValue().toString());
        log.info("Nonce: {}", accountValue.getNonce());
        log.info("RLP: {}", accountValueBytes.toHexString());
        log.info("Storage Location: {}", storageLocation.toString());
    }

    enum StorageLocation {
        BONSAI, FLAT
    }
}

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
import org.hyperledger.besu.ethereum.trie.MerklePatriciaTrie;
import org.hyperledger.besu.ethereum.worldstate.StateTrieAccountValue;

class AccountValueNode extends AbstractBonsaiNode {

    private final Hash accountHash;
    private final Bytes accountValueBytes;
    private final BonsaiStorageView bonsaiStorageView;
    private final StateTrieAccountValue accountValue;
    private final StorageLocation storageLocation;

    public AccountValueNode(final BonsaiStorageView bonsaiStorageView, final Hash accountHash, final Bytes accountValueBytes, final int depth, final StorageLocation storageLocation) {
        super("A:" + accountHash.toHexString(), depth);
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
                children.add(new AccountValueNode(bonsaiStorageView, accountHash, accountInFlatDB.get(), depth + 1, StorageLocation.FLAT));
            } else {
                children.add(new LabelNode("No Flat DB Account", "Flat DB value not found ", depth + 1));
            }
        }
        // check the account in the flat database

        // Add code, if appropriate
        if (!accountValue.getCodeHash().equals(Hash.EMPTY)) {
            // traverse code
            final Optional<Bytes> code = bonsaiStorageView.getCode(accountHash);
            if (code.isEmpty()) {
                children.add(new LabelNode("Missing Code", accountHash.toHexString(), depth + 1));
            } else {
                final Hash foundCodeHash = Hash.hash(code.orElseThrow());
                if (!foundCodeHash.equals(accountValue.getCodeHash())) {
                    children.add(new LabelNode("Invalid Code hash", "found: " + foundCodeHash + " account: " + accountValue.getCodeHash(), depth + 1));
                } else {
                    children.add(new LabelNode("Code", foundCodeHash.toHexString(), depth + 1));
                }
            }
        } else {
            children.add(new LabelNode("No Code", "No code", depth + 1));
        }
        // Add storage, if appropriate
        if (!accountValue.getStorageRoot().equals(MerklePatriciaTrie.EMPTY_TRIE_NODE_HASH)) {
            children.add(new StorageTreeNode(bonsaiStorageView, accountHash, bonsaiStorageView.getStorageNodeValue(accountValue.getStorageRoot(), accountHash, Bytes.EMPTY), depth + 1));
        } else {
            children.add(new LabelNode("No Storage", "No storage", depth + 1));
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

    enum StorageLocation {
        BONSAI, FLAT
    }
}

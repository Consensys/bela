package org.hyperledger.bela.components.bonsai;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Label;
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

    public AccountValueNode(final BonsaiStorageView bonsaiStorageView, final Hash accountHash, final Bytes accountValueBytes, final int depth) {
        super("A:" + accountHash.toHexString(), depth);
        this.bonsaiStorageView = bonsaiStorageView;
        this.accountHash = accountHash;
        this.accountValueBytes = accountValueBytes;

    }

    @Override
    public List<BonsaiNode> getChildren() {
        final List<BonsaiNode> children = new ArrayList<>();

        final StateTrieAccountValue accountValue =
                StateTrieAccountValue.readFrom(RLP.input(accountValueBytes));

        children.add(new LabelNode("Balance: " + accountValue.getBalance().getValue(), depth + 1));
        children.add(new LabelNode("Nonce: " + accountValue.getNonce(), depth + 1));


        // check the account in the flat database
        final Optional<Bytes> accountInFlatDB = bonsaiStorageView.getAccountFromFlatDatabase(accountHash);
        children.add(new LabelNode("V: " + accountValueBytes.toHexString(), depth + 1));
        if (accountInFlatDB.isPresent()) {
            children.add(new LabelNode("F: " + accountInFlatDB.get().toHexString(), depth + 1));
        } else {
            children.add(new LabelNode("Flat DB value not found ", depth + 1));
        }
        // Add code, if appropriate
        if (!accountValue.getCodeHash().equals(Hash.EMPTY)) {
            // traverse code
            final Optional<Bytes> code = bonsaiStorageView.getCode(accountHash);
            if (code.isEmpty()) {
                children.add(new LabelNode("Missing code hash", depth + 1));
            } else {
                final Hash foundCodeHash = Hash.hash(code.orElseThrow());
                if (!foundCodeHash.equals(accountValue.getCodeHash())) {
                    children.add(new LabelNode("Invalid code hash", depth + 1));
                } else {
                    children.add(new LabelNode("C: " + foundCodeHash.toHexString(), depth + 1));
                }
            }
        } else {
            children.add(new LabelNode("No code", depth + 1));
        }
        // Add storage, if appropriate
        if (!accountValue.getStorageRoot().equals(MerklePatriciaTrie.EMPTY_TRIE_NODE_HASH)) {
            children.add(new StorageTreeNode(bonsaiStorageView, accountHash, bonsaiStorageView.getStorageNodeValue(accountValue.getStorageRoot(), accountHash, Bytes.EMPTY), depth + 1));
        } else {
            children.add(new LabelNode("No storage", depth + 1));
        }
        return children;
    }

    @Override
    public Component createComponent() {
        Panel panel = new Panel();
        panel.addComponent(new Label("Account Hash: " + accountHash.toHexString()));
        panel.addComponent(new Label("RLP: " + accountValueBytes.toHexString()));
        return panel.withBorder(Borders.singleLine("Account Value Node"));
    }
}

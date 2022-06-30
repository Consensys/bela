package org.hyperledger.bela.components.bonsai;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.bela.components.BonsaiStorageView;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.trie.MerklePatriciaTrie;
import org.hyperledger.besu.ethereum.worldstate.StateTrieAccountValue;

class AccountValueView extends AbstractBonsaiNodeView {

    private BonsaiStorageView bonsaiStorageView;
    private final Hash accountHash;
    private final Bytes accountValueBytes;

    public AccountValueView(final BonsaiStorageView bonsaiStorageView, final Hash accountHash, final Bytes accountValueBytes, final int depth) {
        super("V:" + accountValueBytes.toHexString(), depth);
        this.bonsaiStorageView = bonsaiStorageView;
        this.accountHash = accountHash;
        this.accountValueBytes = accountValueBytes;

    }

    @Override
    public void expand() {
        final List<BonsaiView> children = new ArrayList<>();

        final StateTrieAccountValue accountValue =
                StateTrieAccountValue.readFrom(RLP.input(accountValueBytes));

        children.add(new LabelNodeView("Balance: " + accountValue.getBalance().getValue(), depth + 1));
        children.add(new LabelNodeView("Nonce: " + accountValue.getNonce(), depth + 1));


        // check the account in the flat database
        final Optional<Bytes> accountInFlatDB = bonsaiStorageView.getAccountFromFlatDatabase(accountHash);
        if (accountInFlatDB.isPresent()) {
            children.add(new LabelNodeView("F: " + accountInFlatDB.get().toHexString(), depth + 1));
        } else {
            children.add(new LabelNodeView("Flat DB value not found ", depth + 1));
        }
        // Add code, if appropriate
        if (!accountValue.getCodeHash().equals(Hash.EMPTY)) {
            // traverse code
            final Optional<Bytes> code = bonsaiStorageView.getCode(accountHash);
            if (code.isEmpty()) {
                children.add(new LabelNodeView("Missing code hash", depth + 1));
            } else {
                final Hash foundCodeHash = Hash.hash(code.orElseThrow());
                if (!foundCodeHash.equals(accountValue.getCodeHash())) {
                    children.add(new LabelNodeView("Invalid code hash", depth + 1));
                } else {
                    children.add(new LabelNodeView("C: " + foundCodeHash.toHexString(), depth + 1));
                }
            }
        } else {
            children.add(new LabelNodeView("No code", depth + 1));
        }
        // Add storage, if appropriate
        if (!accountValue.getStorageRoot().equals(MerklePatriciaTrie.EMPTY_TRIE_NODE_HASH)) {
            children.add(new StorageTreeNodeView(bonsaiStorageView, accountHash, bonsaiStorageView.getStorageNodeValue(accountValue.getStorageRoot(), accountHash, Bytes.EMPTY), depth + 1));
        } else {
            children.add(new LabelNodeView("No storage", depth + 1));
        }
        setChildren(children);
    }
}

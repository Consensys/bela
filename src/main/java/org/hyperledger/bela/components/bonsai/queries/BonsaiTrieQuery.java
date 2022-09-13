package org.hyperledger.bela.components.bonsai.queries;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import org.hyperledger.besu.datatypes.Hash;

public enum BonsaiTrieQuery {
    ACCOUNT_STORAGE_CHANGE("Account Storage Change") {
        @Override
        public TrieQueryValidator createValidator(final WindowBasedTextGUI gui) {
            final String s = TextInputDialog.showDialog(gui, "Target Storage Hash", "Hash", "0x1d6f3716c57c52bcf6609f8e48d02db87f08f0ff0ec48580db7e20ae5dd0d7e3");
            if (s == null) {
                return null;
            }
            return new ValidateStorageChange(Hash.fromHexString(s));

        }
    };

    private final String name;

    BonsaiTrieQuery(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract TrieQueryValidator createValidator(final WindowBasedTextGUI gui);

}

package org.hyperledger.bela.components.bonsai;

import java.util.List;
import com.googlecode.lanterna.gui2.Component;
import org.hyperledger.bela.components.BelaComponent;

public interface BonsaiNode extends BelaComponent<Component> {
    String getLabel();

    List<BonsaiNode> getChildren();

    void log();
}

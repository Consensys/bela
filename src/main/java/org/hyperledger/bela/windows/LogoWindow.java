package org.hyperledger.bela.windows;

import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import org.hyperledger.bela.components.KeyControls;

public class LogoWindow extends AbstractBelaWindow {
    @Override
    public String label() {
        return "About";
    }

    @Override
    public MenuGroup group() {
        return MenuGroup.FILE;
    }


    @Override
    public KeyControls createControls() {
        return new KeyControls();
    }

    @Override
    public Panel createMainPanel() {
        Panel panel = new Panel(new LinearLayout());
        final Label logo = new Label("""
                                                                                               \s
                                      /((/* Hyperledger Besu + Lanterna                        \s
                                        (.                                                     \s
                                        (.                                                     \s
                                        (.          /(((.                                      \s
                                        (.       *((*(/./((,                                   \s
                                        (.  ./((.   (*      *((/                               \s
                                      .((((/       /(           ./((/(                         \s
                                       /(/  .,/(((/              .(((*                         \s
                                        (.          *((/       .(( *(                          \s
                                        (.        *(    .((. ((,   *(                          \s
                                        (.        *(     *((. /    *(                          \s
                                        (.        ,( /((*      /(* *(                          \s
                                       ,(*    .*(/ (,            *(((.                         \s
                                      *((((*       /(            *((((                         \s
                                            /((*   ./,       //(,                              \s
                                                ,((/,(, .(((.                                  \s
                                                    /(((.                                      \s
                """);
        panel.addComponent(logo);
        return panel;
    }
}

package org.hyperledger.bela.windows;

import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;

public class LogoWindow implements LanternaWindow{
    @Override
    public String label() {
        return "Bela";
    }

    @Override
    public MenuGroup group() {
        return MenuGroup.FILE;
    }

    @Override
    public Window createWindow() {
        Window window = new BasicWindow(label());
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
        panel.addComponent(new Button("Acknowledge", window::close));
        window.setComponent(panel);
        return window;
    }
}

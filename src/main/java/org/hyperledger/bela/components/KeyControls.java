package org.hyperledger.bela.components;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowListener;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

public class KeyControls implements BelaComponent<Panel>, WindowListener {

    List<Section> sections = new ArrayList<>();

    public KeyControls() {
        this.sections.add(new Section());
    }

    Map<Character, Runnable> characterRunnableMap = new HashMap<>();
    Map<KeyType, Runnable> keyStrokeRunnableMap = new EnumMap<>(KeyType.class);

    public KeyControls addControl(String label, Character c, Runnable action) {
        characterRunnableMap.put(c, action);
        getLastSection().addControl(newControlPanel(label, "'" + c + "'", action));
        return this;
    }

    private Section getLastSection() {
        return this.sections.get(this.sections.size() - 1);
    }

    public KeyControls addControl(String label, KeyType keyType, Runnable action) {
        if (keyType == KeyType.Character) {
            throw new IllegalArgumentException("Use the other add control to add characters...");
        }
        keyStrokeRunnableMap.put(keyType, action);
        getLastSection().addControl(newControlPanel(label, key(keyType), action));
        return this;
    }

    private String key(final KeyType keyType) {
        return switch (keyType) {
            case ArrowLeft -> "<-";
            case ArrowRight -> "->";
            default -> keyType.name();
        };
    }

    ;

    private Panel newControlPanel(final String label, final String key, Runnable action) {
        Panel panel = new Panel(new LinearLayout());
        panel.addComponent(new Button(label, action).setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center, LinearLayout.GrowPolicy.None)));
        panel.addComponent(new Label("(" + key + ")").setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center, LinearLayout.GrowPolicy.None)));
        return panel;
    }

    @Override
    public Panel createComponent() {
        Panel panel = new Panel(new LinearLayout());
        for (Section section : sections) {
            panel.addComponent(section.createComponent());
        }

        return panel;
    }

    @Override
    public void onResized(final Window window, final TerminalSize oldSize, final TerminalSize newSize) {

    }

    @Override
    public void onMoved(final Window window, final TerminalPosition oldPosition, final TerminalPosition newPosition) {

    }

    @Override
    public void onInput(final Window basePane, final KeyStroke keyStroke, final AtomicBoolean deliverEvent) {
        final Runnable action;
        if (keyStroke.getCharacter() != null) {
            action = characterRunnableMap.get(keyStroke.getCharacter());
        } else {
            action = keyStrokeRunnableMap.get(keyStroke.getKeyType());
        }
        if (action != null) {
            action.run();
        }
    }

    @Override
    public void onUnhandledInput(final Window basePane, final KeyStroke keyStroke, final AtomicBoolean hasBeenHandled) {

    }

    public KeyControls addSection(final String title) {
        this.sections.add(new Section(title));
        return this;
    }

    public class Section {
        private final String title;
        List<Panel> controls = new ArrayList<>();

        public Section() {
            this.title = "";
        }
        public Section(final String title) {
            this.title = title;
        }

        public void addControl(final Panel control) {
            this.controls.add(control);
        }

        public Component createComponent() {
            Panel commands = new Panel(new LinearLayout(Direction.HORIZONTAL));
            for (Panel control : controls) {
                commands.addComponent(control.withBorder(Borders.singleLine()));
            }
            return commands;
        }
    }
}

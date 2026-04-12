package tests.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class UiTestUtils {

    private UiTestUtils() {
    }

    public static <T extends Component> List<T> findAllOfType(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        collectOfType(root, type, found);
        return found;
    }

    public static Optional<JButton> findButtonByText(Container root, String text) {
        return findAllOfType(root, JButton.class).stream()
                .filter(button -> text.equals(button.getText()))
                .findFirst();
    }

    public static Optional<JLabel> findLabelByText(Container root, String text) {
        return findAllOfType(root, JLabel.class).stream()
                .filter(label -> text.equals(label.getText()))
                .findFirst();
    }

    public static <T extends Component> List<T> findAllWithExactClass(Container root, Class<T> exactType) {
        List<T> found = new ArrayList<>();
        collectWithExactClass(root, exactType, found);
        return found;
    }

    private static <T extends Component> void collectOfType(Component current, Class<T> type, List<T> out) {
        if (type.isInstance(current)) {
            out.add(type.cast(current));
        }

        if (current instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectOfType(child, type, out);
            }
        }
    }

    private static <T extends Component> void collectWithExactClass(Component current, Class<T> exactType, List<T> out) {
        if (current.getClass().equals(exactType)) {
            out.add(exactType.cast(current));
        }

        if (current instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectWithExactClass(child, exactType, out);
            }
        }
    }
}

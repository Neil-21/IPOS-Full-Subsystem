package iposca;

import javafx.scene.Parent;
import java.net.URL;

public class ThemeManager {
    //stores the global state (Default is light mode / false)
    private static boolean isDarkMode = false;

    public static boolean isDarkMode() {
        return isDarkMode;
    }

    public static void setDarkMode(boolean darkMode) {
        isDarkMode = darkMode;
    }

    //applies or removes the css based on the global state
    public static void applyTheme(Parent root) {
        try {
            URL cssUrl = ThemeManager.class.getResource("/styles/darkmode.css");
            if (cssUrl != null) {
                String cssPath = cssUrl.toExternalForm();
                if (isDarkMode) {
                    if (!root.getStylesheets().contains(cssPath)) {
                        root.getStylesheets().add(cssPath);
                    }
                } else {
                    root.getStylesheets().remove(cssPath);
                }
            }
        } catch (Exception e) {
            System.err.println("ThemeManager: Could not load darkmode.css");
        }
    }
}

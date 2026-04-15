package iposca;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class Utils {
    public static void switchScene(Stage stage, String fxmlPath, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(Utils.class.getResource(fxmlPath));
        Parent root = loader.load();

        ThemeManager.applyTheme(root);

        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.show();
    }
}

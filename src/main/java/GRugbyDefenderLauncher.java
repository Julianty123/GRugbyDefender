import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionFormCreator;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GRugbyDefenderLauncher extends ExtensionFormCreator {

    @Override
    public ExtensionForm createForm(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GRugbyDefender.fxml"));
        Parent root = loader.load();
        // setUserAgentStylesheet(STYLESHEET_CASPIAN); // Interesting Theme
        stage.setTitle("RugbyDefender");
        stage.setScene(new Scene(root));
        // stage.getScene().getStylesheets().add(GEarthController.class.getResource("/gearth/ui/bootstrap3.css").toExternalForm());
        // stage.getIcons().add(new Image(Main.class.getResourceAsStream("G-EarthLogoSmaller.png")));
        stage.setResizable(false);
        stage.setAlwaysOnTop(true);

        return loader.getController();
    }

    public static void main(String[] args) {
        runExtensionForm(args, GRugbyDefenderLauncher.class);
    }
}
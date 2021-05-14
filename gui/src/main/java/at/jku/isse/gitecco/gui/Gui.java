package at.jku.isse.gitecco.gui;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Gui extends Application {

    public static void main(String[] args) {
        Application.launch(args);
    }

    private MainView mainView;

    private Group root;


    @Override
    public void start(Stage primaryStage) throws Exception {
        // INIT
        Application.setUserAgentStylesheet(STYLESHEET_MODENA);
        primaryStage.setTitle("Mining Feature Revisions");
        this.root = new Group();
        Scene scene = new Scene(root, 800, 600);


        // TOP LEVEL
        this.mainView = new MainView();
        // bind to take available space
        mainView.prefHeightProperty().bind(scene.heightProperty());
        mainView.prefWidthProperty().bind(scene.widthProperty());

        this.updateView();

        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    private void updateView() {
        this.root.getChildren().setAll(this.mainView);
    }
}

package at.jku.isse.gitecco.gui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class MainView extends BorderPane {
    private Button mineButton = new Button("OK");
    private Button cancelButton = new Button("Cancel");
    private Label headerLabel;
    //private ChangeAnalysis changeAnalysis;

    public MainView() {

        // tabs
        TabPane tabPane = new TabPane();
        this.setCenter(tabPane);

        // main content
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(10, 10, 10, 10));


        ColumnConstraints col1constraint = new ColumnConstraints();
        col1constraint.setMinWidth(GridPane.USE_PREF_SIZE);
        ColumnConstraints col2constraint = new ColumnConstraints();
        col2constraint.setFillWidth(true);
        col2constraint.setHgrow(Priority.ALWAYS);
        gridPane.getColumnConstraints().addAll(col1constraint, col2constraint);

        this.setCenter(gridPane);

        int row = 0;

        Label repositoryDirLabel = new Label("Git Repository: ");
        gridPane.add(repositoryDirLabel, 0, row, 1, 1);

        TextField repositoryDirTextField = new TextField("Open the folder directory");
        repositoryDirTextField.setDisable(false);
        repositoryDirLabel.setLabelFor(repositoryDirTextField);
        gridPane.add(repositoryDirTextField, 1, row, 1, 1);

        Button selectRepositoryDirectoryButton = new Button("...");
        gridPane.add(selectRepositoryDirectoryButton, 2, row, 1, 1);

        gridPane.add(mineButton, 3, row, 1, 1);
        gridPane.add(cancelButton, 4, row, 1, 1);

        row++;

        final ProgressBar pb = new ProgressBar();
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setVisible(false);
        pb.setProgress(0.0f);
        gridPane.add(pb, 0, row, 3, 1);
        gridPane.setFillWidth(pb, true);

        this.updateView();
        mineButton.setOnAction(event -> {
            //this.changeAnalysis.identification(repositoryDirTextField.getText());
        });

        //this.mineButton.setOnAction(event -> ChangeAnalysis.identification(repositoryDirTextField.getText()));
        cancelButton.setOnAction(event -> repositoryDirTextField.setText("Open the folder directory"));
        selectRepositoryDirectoryButton.setOnAction(event -> {
            final DirectoryChooser directoryChooser = new DirectoryChooser();
            try {
                Path directory = Paths.get(repositoryDirTextField.getText());
                if (Files.exists(directory) && Files.isDirectory(directory))
                    directoryChooser.setInitialDirectory(directory.toFile());
            } catch (Exception e) {
                // do nothing
            }
            final File selectedDirectory = directoryChooser.showDialog(this.getScene().getWindow());
            if (selectedDirectory != null) {
                repositoryDirTextField.setText(selectedDirectory.toPath().toString());
            }
        });
    }


    private void openDialog(String title, Parent content) {
        final Stage dialog = new Stage();
        dialog.initStyle(StageStyle.UTILITY);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(MainView.this.getScene().getWindow());

        Scene dialogScene = new Scene(content);
        dialog.setScene(dialogScene);
        dialog.setTitle(title);

        dialog.show();
        dialog.requestFocus();
    }


    private void updateView() {
        mineButton.setDisable(false);
        cancelButton.setDisable(false);
    }

}

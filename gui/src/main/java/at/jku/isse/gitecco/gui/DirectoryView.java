package at.jku.isse.gitecco.gui;

import at.jku.isse.gitecco.core.git.FileChange;
import at.jku.isse.gitecco.core.git.GitHelper;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

public class DirectoryView extends OperationView {

    private SplitPane splitPane;

    public DirectoryView(File mergeDir, String gitDir, String firstCommit, String secondCommit, ArrayList<String> filesAdded, ArrayList<String> filesRemoved, Map<String, FileChange> filesChanged) {
        super();

        // split pane
        this.splitPane = new SplitPane();
        this.splitPane.setOrientation(Orientation.VERTICAL);

        this.step1(mergeDir, gitDir, firstCommit, secondCommit, filesAdded, filesRemoved, filesChanged);
    }

    /**
     * Base directory and configuration string.
     */
    private void step1(File mergeDir, String gitDir, String firstCommit, String secondCommit, ArrayList<String> filesAdded, ArrayList<String> filesRemoved, Map<String, FileChange> filesChanged) {
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(event -> ((Stage) this.getScene().getWindow()).close());
        this.leftButtons.getChildren().setAll(cancelButton);

        this.headerLabel.setText("Directory to checkout the change propagation");

        Button checkoutButton = new Button("Checkout");
        this.rightButtons.getChildren().setAll(checkoutButton);


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


        Label baseDirLabel = new Label("Checkout Directory: ");
        gridPane.add(baseDirLabel, 0, row, 1, 1);

        TextField baseDirTextField = new TextField("Open a directory");
        baseDirTextField.setDisable(false);
        baseDirLabel.setLabelFor(baseDirTextField);
        gridPane.add(baseDirTextField, 1, row, 1, 1);

        Button selectBaseDirectoryButton = new Button("...");
        gridPane.add(selectBaseDirectoryButton, 2, row, 1, 1);
        row++;


        selectBaseDirectoryButton.setOnAction(event -> {
            final DirectoryChooser directoryChooser = new DirectoryChooser();
            try {
                Path directory = Paths.get(baseDirTextField.getText());
                if (Files.exists(directory) && Files.isDirectory(directory))
                    directoryChooser.setInitialDirectory(directory.toFile());
            } catch (Exception e) {
                // do nothing
            }
            final File selectedDirectory = directoryChooser.showDialog(this.getScene().getWindow());
            if (selectedDirectory != null) {
                baseDirTextField.setText(selectedDirectory.toPath().toString());
            }
        });


        checkoutButton.setOnAction(event -> {
            this.step2();

            Task<Boolean> checkoutTask = new Task<Boolean>() {
                @Override
                public Boolean call() {
                    try {
                        File mergeDir = new File(baseDirTextField.getText());
                        return GitHelper.changePropagation(mergeDir, gitDir, firstCommit, secondCommit, filesAdded, filesRemoved, filesChanged);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return false;
                    //return CheckoutView.this.service.checkout(configurationString);
                }

                @Override
                public void succeeded() {
                    super.succeeded();
                    //DirectoryView.this.stepSuccess("Checkout successful.");
                    // show value in checkout detail view
                    //DirectoryView.this.checkoutDetailView.showCheckout(this.getValue());
                    //DirectoryView.this.splitPane.getItems().setAll(DirectoryView.this.logTable, DirectoryView.this.checkoutDetailView);
                    DirectoryView.this.showSuccessHeader();
                }

                @Override
                public void cancelled() {
                    super.cancelled();
                    //CheckoutView.this.stepError("Checkout operation was cancelled.", this.getException());
                    // show exception textarea instead of checkout detail view
                    DirectoryView.this.showErrorHeader();
                }

                @Override
                public void failed() {
                    super.failed();
                    //CheckoutView.this.stepError("Error during checkout operation.", this.getException());
                    // show exception textarea instead of checkout detail view
                    DirectoryView.this.showErrorHeader();
                }
            };

            new Thread(checkoutTask).start();
        });


        this.fit();

    }


    /**
     * Log table and success or error.
     */
    private void step2() {
        Button cancelButton = new Button("Cancel");
        this.leftButtons.getChildren().setAll(cancelButton);

        this.headerLabel.setText("Checking out ...");

        this.rightButtons.getChildren().clear();


        this.setCenter(splitPane);


        this.fit();
    }


}

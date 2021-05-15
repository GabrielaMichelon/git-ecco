package at.jku.isse.gitecco.gui;


import at.jku.isse.ecco.gui.view.ArtifactsView;
import at.jku.isse.gitecco.translation.changepropagation.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

public class ChangeAnalysisView extends BorderPane {

    private Button mineButton = new Button("OK");
    private Button cancelButton = new Button("Cancel");
    private Label headerLabel = new Label();
    private ChangeAnalysis changeAnalysis;

    public ChangeAnalysisView() {

        // main content
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(10, 10, 10, 10));


        ColumnConstraints col1constraint = new ColumnConstraints();
        col1constraint.setMinWidth(GridPane.USE_PREF_SIZE);
        //col1constraint.setPercentWidth(30);
        ColumnConstraints col2constraint = new ColumnConstraints();
        col2constraint.setFillWidth(true);
        col2constraint.setHgrow(Priority.ALWAYS);
        //col2constraint.setPercentWidth(30);
        //col2constraint.setMinWidth(GridPane.USE_PREF_SIZE);
        gridPane.getColumnConstraints().addAll(col1constraint, col2constraint);

        this.setCenter(gridPane);


        int row[] = {0};

        Label repositoryDirLabel = new Label("Git Repository: ");
        gridPane.add(repositoryDirLabel, 0, row[0], 1, 1);

        TextField repositoryDirTextField = new TextField("C:\\Users\\gabil\\Desktop\\PHD\\New research\\ChangePropagation\\runningexample");//Open the folder directory");
        repositoryDirTextField.setDisable(false);
        repositoryDirLabel.setLabelFor(repositoryDirTextField);
        gridPane.add(repositoryDirTextField, 1, row[0], 1, 1);

        Button selectRepositoryDirectoryButton = new Button("...");
        gridPane.add(selectRepositoryDirectoryButton, 2, row[0], 1, 1);

        row[0]++;

        Label fistCommitLabel = new Label("First Commit: ");
        gridPane.add(fistCommitLabel, 0, row[0], 1, 1);

        TextField firstcommitTextField = new TextField("a29e19a4557aa53f123767a5ae0284c01c79390d");//Commit Hash");
        firstcommitTextField.setDisable(false);
        gridPane.add(firstcommitTextField, 1, row[0], 1, 1);

        row[0]++;

        Label secondCommitLabel = new Label("Second Commit: ");
        gridPane.add(secondCommitLabel, 0, row[0], 1, 1);

        TextField secondcommitTextField = new TextField("1d42a8d2bfa46c4f0874cdae2e9d8757e33b5da6");//Commit Hash");
        secondcommitTextField.setDisable(false);
        gridPane.add(secondcommitTextField, 1, row[0], 1, 1);

        row[0]++;

        Label featureLabel = new Label("Feature Name: ");
        gridPane.add(featureLabel, 0, row[0], 1, 1);

        TextField featureTextField = new TextField("featA");//"Feature Name");
        featureTextField.setDisable(false);
        gridPane.add(featureTextField, 1, row[0], 1, 1);

        row[0]++;
        gridPane.add(mineButton, 0, row[0], 1, 1);
        //gridPane.add(cancelButton, 4, row, 1, 1);

        final TableView<FileChange> table = new TableView<FileChange>();

        TableColumn selectCol = new TableColumn("Propagate Changes");
        selectCol.setCellValueFactory(new PropertyValueFactory<>("propagateChange"));
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        selectCol.setEditable(true);
        selectCol.setMinWidth(200);


        TableColumn fileCol = new TableColumn("File Name");
        fileCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        fileCol.setMinWidth(200);

        TableColumn changeCol = new TableColumn("Change Type");
        changeCol.setCellValueFactory(new PropertyValueFactory<>("changeType"));
        changeCol.setMinWidth(200);

        TableColumn linesCol = new TableColumn("Lines");
        linesCol.setCellValueFactory(new PropertyValueFactory<>("lines"));
        linesCol.setMinWidth(200);

        TableColumn featiCol = new TableColumn("Feature interactions");
        featiCol.setCellValueFactory(new PropertyValueFactory<>("feati"));
        featiCol.setMinWidth(200);

        TableColumn feataff = new TableColumn("Feature might be affected");
        feataff.setCellValueFactory(new PropertyValueFactory<>("feata"));
        feataff.setMinWidth(200);


        ObservableList<FileChange> data = FXCollections
                .observableArrayList();

        this.updateView();
        final int[] rowaux = {row[0]};
        mineButton.setOnAction(event -> {
            try {
                Map<String, Changes> returnMethod = this.changeAnalysis.identification(repositoryDirTextField.getText(), firstcommitTextField.getText(), secondcommitTextField.getText(), featureTextField.getText());
                rowaux[0]++;
                for (Map.Entry<String, Changes> changes : returnMethod.entrySet()) {
                    ArrayList<AddedFile> addedfiles = changes.getValue().getAddedFiles();
                    if (addedfiles.size() > 0) {
                        for (AddedFile addf : addedfiles) {
                            String featurei = "";
                            for (String feati : addf.getFeatureInteractions()) {
                                featurei += ", " + feati;
                            }
                            featurei = featurei.replaceFirst(", ", "");
                            String featurea = "";
                            for (String feati : addf.getFeatureMightAffected()) {
                                featurea += ", " + feati;
                            }
                            featurea = featurea.replaceFirst(", ", "");
                            FileChange fc = new FileChange(false, changes.getKey(), "New File", "Lines added: " + addf.getLinesInsert(), featurei, featurea);
                            data.add(fc);
                        }
                    }

                    ArrayList<ChangedFile> changedFiles = changes.getValue().getChangedFiles();
                    if (changedFiles.size() > 0) {
                        for (ChangedFile changf : changedFiles) {
                            String featurei = "";
                            for (String feati : changf.getFeatureInteractions()) {
                                featurei += ", " + feati;
                            }
                            featurei = featurei.replaceFirst(", ", "");
                            String featurea = "";
                            for (String feati : changf.getFeatureMightAffected()) {
                                featurea += ", " + feati;
                            }
                            featurea = featurea.replaceFirst(", ", "");
                            FileChange fc = new FileChange(false, changes.getKey(), "Changed File", "Lines added: " + changf.getLinesInsert() + " Lines removed: " + changf.getLinesRemoved(), featurei, featurea);
                            data.add(fc);

                        }
                    }

                    ArrayList<DeletedFile> deletedFiles = changes.getValue().getDeletedFiles();
                    if (deletedFiles.size() > 0) {
                        for (DeletedFile delf : deletedFiles) {
                            String featurei = "";
                            for (String feati : delf.getFeatureInteractions()) {
                                featurei += ", " + feati;
                            }
                            featurei = featurei.replaceFirst(", ", "");
                            String featurea = "";
                            for (String feati : delf.getFeatureMightAffected()) {
                                featurea += ", " + feati;
                            }
                            featurea = featurea.replaceFirst(", ", "");
                            FileChange fc = new FileChange(false, changes.getKey(), "Removed File", "Lines removed: " + delf.getLinesRemoved(), featurei, featurea);
                            data.add(fc);
                        }
                    }
                }


                table.setItems(data);
                table.getColumns().addAll(selectCol, fileCol, changeCol, linesCol, featiCol, feataff);

                // toolbar
                ToolBar toolBar = new ToolBar();
                this.setTop(toolBar);

                Button selectAllButton = new Button("Select All");
                Button unselectAllButton = new Button("Unselect All");
                Button propagateChangesButton = new Button("Propagate Changes Selected");

                toolBar.getItems().addAll(selectAllButton, unselectAllButton, propagateChangesButton);
                FilteredList<FileChange> filteredData = new FilteredList<>(data, p -> true);
                SortedList<FileChange> sortedData = new SortedList<>(filteredData);
                sortedData.comparatorProperty().bind(table.comparatorProperty());
                table.setItems(sortedData);
                //this line makes possible check each checkbox
                table.setEditable(true);


                SplitPane splitPane = new SplitPane();
                this.setCenter(splitPane);
                splitPane.setOrientation(Orientation.VERTICAL);
                splitPane.getItems().addAll(gridPane, toolBar, table);
                splitPane.setDividerPosition(0, 0);


                selectAllButton.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent e) {
                        toolBar.setDisable(true);

                        for (FileChange fileChange : data) {
                            fileChange.setPropagateChange(true);
                        }

                        toolBar.setDisable(false);
                    }
                });

                unselectAllButton.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent e) {
                        toolBar.setDisable(true);

                        for (FileChange fileChange : data) {
                            fileChange.setPropagateChange(false);
                        }

                        toolBar.setDisable(false);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        });


        //this.mineButton.setOnAction(event -> ChangeAnalysis.identification(repositoryDirTextField.getText()));
        //cancelButton.setOnAction(event -> repositoryDirTextField.setText("Open the folder directory"));
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


    public static class FileChange {
        private BooleanProperty propagateChange = new SimpleBooleanProperty(false);
        private final SimpleStringProperty fileName;
        private final SimpleStringProperty changeType;
        private final SimpleStringProperty lines;
        private final SimpleStringProperty feati;
        private final SimpleStringProperty feata;

        private FileChange(boolean propagateChange, String fName, String changeType, String lines, String feati, String feata) {
            this.propagateChange.set(propagateChange);
            this.fileName = new SimpleStringProperty(fName);
            this.changeType = new SimpleStringProperty(changeType);
            this.lines = new SimpleStringProperty(lines);
            this.feati = new SimpleStringProperty(feati);
            this.feata = new SimpleStringProperty(feata);
        }


        public boolean isPropagateChange() {
            return this.propagateChange.get();
        }

        public BooleanProperty propagateChangeProperty() {
            return this.propagateChange;
        }

        public void setPropagateChange(boolean propagateChange) {
            this.propagateChange.set(propagateChange);
        }

        public void setChangeType(String changeType) {
            this.changeType.set(changeType);
        }

        public String getFileName() {
            return fileName.get();
        }

        public String getChangeType() {
            return changeType.get();
        }

        public SimpleStringProperty changeTypeProperty() {
            return changeType;
        }

        public SimpleStringProperty fileNameProperty() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName.set(fileName);
        }

        public String getLines() {
            return lines.get();
        }

        public SimpleStringProperty linesProperty() {
            return lines;
        }

        public void setLines(String lines) {
            this.lines.set(lines);
        }

        public String getFeati() {
            return feati.get();
        }

        public SimpleStringProperty featiProperty() {
            return feati;
        }

        public void setFeati(String feati) {
            this.feati.set(feati);
        }

        public String getFeata() {
            return feata.get();
        }

        public SimpleStringProperty feataProperty() {
            return feata;
        }

        public void setFeata(String feata) {
            this.feata.set(feata);
        }
    }

    private void updateView() {
        mineButton.setDisable(false);
        cancelButton.setDisable(false);
    }

}

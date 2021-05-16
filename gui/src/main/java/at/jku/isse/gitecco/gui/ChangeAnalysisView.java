package at.jku.isse.gitecco.gui;


import at.jku.isse.gitecco.translation.changepropagation.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.fxmisc.richtext.InlineCssTextArea;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChangeAnalysisView extends BorderPane {

    private Button mineButton = new Button("OK");
    private Button cancelButton = new Button("Cancel");
    private Label headerLabel = new Label();
    private ChangeAnalysis changeAnalysis;
    final TableView<FileChange> table = new TableView<FileChange>();

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

        table.getColumns().addAll(selectCol, fileCol, changeCol, linesCol, featiCol, feataff);

        ObservableList<FileChange> data = FXCollections
                .observableArrayList();

        this.updateView();
        final int[] rowaux = {row[0]};

        mineButton.setOnAction(event -> {

            try {
                if (table.getItems().size() > 0) {
                    table.getSelectionModel().clearSelection();
                    data.removeAll(data);
                }
                Map<Map<String, List<String>>, Changes> returnMethod = this.changeAnalysis.identification(repositoryDirTextField.getText(), firstcommitTextField.getText(), secondcommitTextField.getText(), featureTextField.getText());
                rowaux[0]++;
                for (Map.Entry<Map<String, List<String>>, Changes> changes : returnMethod.entrySet()) {
                    Map<String, List<String>> changesKey = changes.getKey();
                    String fileName = "";
                    List<String> fileLines = new ArrayList<>();
                    for (Map.Entry<String, List<String>> changeskeyMap : changesKey.entrySet()) {
                        fileName = changeskeyMap.getKey();
                        fileLines = changeskeyMap.getValue();
                    }
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
                            FileChange fc = new FileChange(false, fileName, "New File", "Lines added: " + addf.getLinesInsert(), featurei, featurea, fileLines);
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
                            FileChange fc = new FileChange(false, fileName, "Changed File", "Lines added: " + changf.getLinesInsert() + " Lines removed: " + changf.getLinesRemoved(), featurei, featurea, fileLines);
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
                            FileChange fc = new FileChange(false, fileName, "Removed File", "Lines removed: " + delf.getLinesRemoved(), featurei, featurea, fileLines);
                            data.add(fc);
                        }
                    }
                }

                table.setItems(data);

                // toolbar
                ToolBar toolBar = new ToolBar();
                setTop(toolBar);

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

                table.setRowFactory(tv -> {
                    TableRow<FileChange> rowtable = new TableRow<>();
                    rowtable.setOnMouseClicked(eventRow -> {
                        if (eventRow.getClickCount() == 2 && (!rowtable.isEmpty())) {
                            FileChange rowData = rowtable.getItem();
                            //System.out.println(rowData);
                        }
                    });
                    return rowtable;
                });

                addButtonToTable();

                SplitPane splitPane = new SplitPane();
                setCenter(splitPane);
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

    private void stepProgress() {
        this.headerLabel.setText("Progress");

        // main content

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(10, 10, 10, 10));

        ColumnConstraints col1constraint = new ColumnConstraints();
        col1constraint.setFillWidth(true);
        gridPane.getColumnConstraints().addAll(col1constraint);

        this.setCenter(gridPane);

        int row = 0;

    }


    private void showDiff(FileChange data) {
        Stage stage = new Stage();
        stage.setTitle("Git Diff");

        Label label = new Label(data.getChangeType() + " " + data.getFileName());
        //TextArea result = new TextArea();
        InlineCssTextArea result = new InlineCssTextArea();
        result.setStyle("-fx-font-family: 'monospaced';");
        result.setStyle("-fx-font-size: 50px;");

        int i = 1;
        String[] index = data.getLines().split(",");
        String linesaddinit = "";
        String linesaddend = "";
        String linesremovedinit = "";
        String linesremovedend = "";

        String signal = "";
        if (data.getChangeType().equals("Removed File")) {
            // set style of line 4
            //result.setStyle(4,"-fx-fill: red;");
            result.setStyle("-fx-background-color: #ffdce0");
            linesremovedinit = index[0].substring(data.getLines().indexOf("removed: [") + 10).replaceAll(" ", "");
            linesremovedend = index[index.length - 1].replaceAll(" ", "");
            signal = "-";
        } else if (data.getChangeType().equals("New File")) {
            result.setStyle("-fx-background-color: #dcffe4");
            linesaddinit = index[0].substring(index[0].indexOf("added: [") + 8).replaceAll(" ", "");
            linesaddend = index[1].replaceAll(" ", "");
            signal = "+";
        } else {
            linesaddinit = index[0].substring(index[0].indexOf("added: [") + 8).replaceAll(" ", "");
            linesaddend = index[1].replaceAll(" ", "");
            linesremovedinit = index[0].substring(data.getLines().indexOf("removed: [") + 10).replaceAll(" ", "");
            linesremovedend = index[index.length - 1].replaceAll(" ", "");
        }


        if (data.getChangeType().equals("Changed File")) {
            for (String line : data.getFileLines()) {
                if (i >= Integer.valueOf(linesaddinit) && i <= Integer.valueOf(linesaddend) && i < data.getFileLines().size()) {
                    result.appendText(i + "\t+\t" + line + "\n");
                    result.setStyle(i, "-fx-background-color: #dcffe4");
                } else if (i >= Integer.valueOf(linesaddinit) && i <= Integer.valueOf(linesaddend)) {
                    result.appendText(i + "\t+\t" + line);
                    result.setStyle(i, "-fx-background-color: #dcffe4");
                } else if (i >= Integer.valueOf(linesremovedinit) && i <= Integer.valueOf(linesremovedend) && i < data.getFileLines().size()) {
                    result.appendText(i + "\t-\t" + line + "\n");
                    result.setStyle("-fx-background-color: #ffdce0");
                } else if (i >= Integer.valueOf(linesremovedinit) && i <= Integer.valueOf(linesremovedend)) {
                    result.appendText(i + "\t-\t" + line);
                    result.setStyle("-fx-background-color: #ffdce0");
                } else if (i < data.getFileLines().size()) {
                    result.appendText(i + "\t" + line + "\n");
                } else {
                    result.appendText(i + "\t" + line);
                }
                i++;
            }
        } else {
            for (String line : data.getFileLines()) {
                if (i < data.getFileLines().size())
                    result.appendText(i + "\t" + signal + "\t" + line + "\n");
                else
                    result.appendText(i + "\t" + signal + "\t" + line);
                i++;
            }
        }

        result.setEditable(false);

        VBox vbox = new VBox();
        result.setPrefWidth(600);
        Label title = new Label(data.getChangeType() + data.getFileName());
        vbox.getChildren().add(title);
        vbox.getChildren().add(result);
        Group root = new Group();
        root.getChildren().add(vbox);
        Scene scene = new Scene(root, 600, 600);
        stage.setScene(scene);
        stage.show();

    }

    private void addButtonToTable() {
        TableColumn<FileChange, Void> colBtn = new TableColumn("File Diff");

        Callback<TableColumn<FileChange, Void>, TableCell<FileChange, Void>> cellFactory = new Callback<TableColumn<FileChange, Void>, TableCell<FileChange, Void>>() {
            @Override
            public TableCell<FileChange, Void> call(final TableColumn<FileChange, Void> param) {
                final TableCell<FileChange, Void> cell = new TableCell<FileChange, Void>() {
                    private final Button btn = new Button("View");

                    {
                        btn.setOnAction((ActionEvent event) -> {
                            FileChange data = getTableView().getItems().get(getIndex());
                            showDiff(data);
                            //System.out.println("selectedData: " + data);
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                            this.setAlignment(Pos.CENTER);
                        } else {
                            this.setAlignment(Pos.CENTER);
                            setGraphic(btn);
                        }
                    }
                };
                return cell;
            }
        };

        colBtn.setCellFactory(cellFactory);

        table.getColumns().add(colBtn);

    }

    public static class FileChange {
        private BooleanProperty propagateChange = new SimpleBooleanProperty(false);
        private final SimpleStringProperty fileName;
        private final SimpleStringProperty changeType;
        private final SimpleStringProperty lines;
        private final SimpleStringProperty feati;
        private final SimpleStringProperty feata;
        private List<String> fileLines = new ArrayList<>();

        private FileChange(boolean propagateChange, String fName, String changeType, String lines, String feati, String feata, List<String> fileLines) {
            this.propagateChange.set(propagateChange);
            this.fileName = new SimpleStringProperty(fName);
            this.changeType = new SimpleStringProperty(changeType);
            this.lines = new SimpleStringProperty(lines);
            this.feati = new SimpleStringProperty(feati);
            this.feata = new SimpleStringProperty(feata);
            this.fileLines = fileLines;
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

        public List<String> getFileLines() {
            return fileLines;
        }

        public void setFileLines(List<String> fileLines) {
            this.fileLines = fileLines;
        }
    }

    private void updateView() {
        mineButton.setDisable(false);
        cancelButton.setDisable(false);
    }

}

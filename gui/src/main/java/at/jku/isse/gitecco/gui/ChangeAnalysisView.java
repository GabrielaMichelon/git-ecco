package at.jku.isse.gitecco.gui;

import at.jku.isse.gitecco.translation.changepropagation.*;
import javafx.application.Platform;
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
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.*;
import javafx.util.Callback;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ChangeAnalysisView extends BorderPane {

    private Button changeAnalysisButton = new Button("OK");
    private Button cancelButton = new Button("Cancel");
    private Label headerLabel = new Label();
    private ChangeAnalysis changeAnalysis;
    private final TableView<FileChange> table = new TableView<FileChange>();
    private static TextField firstcommitTextField = new TextField("10920fc67816f8184499d83ca5786885730fa4b8");//Commit Hash");
    private static TextField secondcommitTextField = new TextField("e0c969bb41008fc20871045b5d3e218ef5dda551");//Commit Hash");
    private static TextField repositoryDirTextField = new TextField("C:\\Users\\gabil\\Desktop\\PHD\\New research\\ChangePropagation\\libssh");//Open the folder directory");
    private static TextField featuresSystemTextField = new TextField("Select the file containing the features name of the system if it exists");
    private static TextField featureTextField = new TextField("WITH_SFTP");//"Feature Name");
    private GridPane gridPane = new GridPane();
    private ToolBar toolBar = new ToolBar();
    private TableColumn selectCol = new TableColumn("Propagate Changes");
    private TableColumn fileCol = new TableColumn("File Name");
    private TableColumn changeCol = new TableColumn("Change Type");
    private TableColumn linesCol = new TableColumn("Lines");
    private TableColumn featiCol = new TableColumn("Feature interactions");
    private TableColumn feataff = new TableColumn("Feature might be affected");
    private TableColumn colBtn = new TableColumn("File Diff");
    private Button selectAllButton = new Button("Select All");
    private Button unselectAllButton = new Button("Unselect All");
    private Button propagateChangesButton = new Button("Propagate Changes Selected");
    private Label loadingLabel = new Label("It can take some minutes. Please wait... ");
    private ObservableList<FileChange> data = FXCollections
            .observableArrayList();
    private ObservableList<FileChange> dataAux = FXCollections.observableArrayList();
    private int row[] = {0};
    private Stage stage = new Stage();

    public static TextField getFirstcommitTextField() {
        return firstcommitTextField;
    }

    public static void setFirstcommitTextField(TextField firstcommitTextField) {
        ChangeAnalysisView.firstcommitTextField = firstcommitTextField;
    }

    public static TextField getSecondcommitTextField() {
        return secondcommitTextField;
    }

    public static void setSecondcommitTextField(TextField secondcommitTextField) {
        ChangeAnalysisView.secondcommitTextField = secondcommitTextField;
    }

    public static TextField getRepositoryDirTextField() {
        return repositoryDirTextField;
    }

    public static void setRepositoryDirTextField(TextField repositoryDirTextField) {
        ChangeAnalysisView.repositoryDirTextField = repositoryDirTextField;
    }

    public static TextField getFeaturesSystemTextField() {
        return featuresSystemTextField;
    }

    public static void setFeaturesSystemTextField(TextField featuresSystemTextField) {
        ChangeAnalysisView.featuresSystemTextField = featuresSystemTextField;
    }

    public static TextField getFeatureTextField() {
        return featureTextField;
    }

    public static void setFeatureTextField(TextField featureTextField) {
        ChangeAnalysisView.featureTextField = featureTextField;
    }

    public ChangeAnalysisView() {
        // main content
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

        Label repositoryDirLabel = new Label("Git Repository: ");
        gridPane.add(repositoryDirLabel, 0, row[0], 1, 1);

        repositoryDirTextField.setDisable(false);
        repositoryDirLabel.setLabelFor(repositoryDirTextField);
        gridPane.add(repositoryDirTextField, 1, row[0], 1, 1);

        Button selectRepositoryDirectoryButton = new Button("...");
        gridPane.add(selectRepositoryDirectoryButton, 2, row[0], 1, 1);

        row[0]++;

        Label featuresDirLabel = new Label("TXT file containing features: ");
        gridPane.add(featuresDirLabel, 0, row[0], 1, 1);

        featuresSystemTextField.setDisable(false);
        featuresDirLabel.setLabelFor(featuresSystemTextField);
        gridPane.add(featuresSystemTextField, 1, row[0], 1, 1);

        Button selectFeaturesDirectoryButton = new Button("...");
        gridPane.add(selectFeaturesDirectoryButton, 2, row[0], 1, 1);

        row[0]++;

        Label fistCommitLabel = new Label("First Commit: ");
        gridPane.add(fistCommitLabel, 0, row[0], 1, 1);

        firstcommitTextField.setDisable(false);
        gridPane.add(firstcommitTextField, 1, row[0], 1, 1);

        row[0]++;

        Label secondCommitLabel = new Label("Second Commit: ");
        gridPane.add(secondCommitLabel, 0, row[0], 1, 1);

        secondcommitTextField.setDisable(false);
        gridPane.add(secondcommitTextField, 1, row[0], 1, 1);

        row[0]++;

        Label featureLabel = new Label("Feature Name: ");
        gridPane.add(featureLabel, 0, row[0], 1, 1);

        featureTextField.setDisable(false);
        gridPane.add(featureTextField, 1, row[0], 1, 1);

        row[0]++;
        gridPane.add(changeAnalysisButton, 0, row[0], 1, 1);

        row[0]+=8;


        selectCol.setCellValueFactory(new PropertyValueFactory<>("propagateChange"));
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        selectCol.setEditable(true);
        selectCol.setMinWidth(200);

        fileCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        fileCol.setMinWidth(200);

        changeCol.setCellValueFactory(new PropertyValueFactory<>("changeType"));
        changeCol.setMinWidth(200);


        linesCol.setCellValueFactory(new PropertyValueFactory<>("lines"));
        linesCol.setMinWidth(400);


        featiCol.setCellValueFactory(new PropertyValueFactory<>("feati"));
        featiCol.setMinWidth(200);

        feataff.setCellValueFactory(new PropertyValueFactory<>("feata"));
        feataff.setMinWidth(200);

        colBtn.setCellValueFactory(new PropertyValueFactory<>("btnview"));
        colBtn.setMinWidth(200);

        setTop(toolBar);

        Image image = new Image("image/loading.gif");
        ImageView imageView = new ImageView(image);

        //Setting the preserve ratio of the image view
        imageView.setPreserveRatio(true);

        this.updateView();
        final int[] rowaux = {row[0]};


        changeAnalysisButton.setOnAction(event -> {
            loadingLabel.setTextFill(Color.color(1, 0, 0));
            loadingLabel.setFont(new Font(15.0));
            gridPane.add(loadingLabel, 0, row[0], 2, 3);

            //StackPane root2 = new StackPane();
            //root2.getChildren().add(imageView);
            //Scene scene = new Scene(root2, 500, 500);
            //stage.setScene(scene);
            //stage.show();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Platform.runLater(() ->
                    changeAnalysisMethod(rowaux));
        });

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


        propagateChangesButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                toolBar.setDisable(true);
                Collection<FileChange> selectedChanges = new ArrayList<>();
                for (ChangeAnalysisView.FileChange fileChange : data) {
                    if (fileChange.isPropagateChange())
                        selectedChanges.add(fileChange);
                }

                if (!selectedChanges.isEmpty()) {
                    Map<String, at.jku.isse.gitecco.core.git.FileChange> filesChanged = new HashMap<>();
                    ArrayList<String> filesAdded = new ArrayList<>();
                    ArrayList<String> filesRemoved = new ArrayList<>();
                    Path baseDir = Paths.get(repositoryDirTextField.getText());
                    File mergeDir = new File(baseDir.getParent() + File.separator + "merge");
                    File mergeFolder = new File(String.valueOf(mergeDir));
                    at.jku.isse.gitecco.core.git.FileChange fileChangeAux;
                    if (!mergeFolder.exists())
                        mergeFolder.mkdir();
                    for (FileChange fileChange : selectedChanges) {
                        if (fileChange.getChangeType().equals("Removed File")) {
                            filesRemoved.add(fileChange.getFileName());
                        } else if (fileChange.getChangeType().equals("New File")) {
                            filesAdded.add(fileChange.getFileName());
                        } else {
                            if (filesChanged.get(fileChange.getFileName()) != null) {
                                fileChangeAux = filesChanged.get(fileChange.getFileName());
                                fileChangeAux.getLines().add(fileChange.getLines());
                                at.jku.isse.gitecco.core.git.FileChange finalFileChangeAux = fileChangeAux;
                                filesChanged.computeIfPresent(fileChange.getFileName(), (k, v) -> finalFileChangeAux);
                            } else {
                                ArrayList<String> lines = new ArrayList<>();
                                lines.add(fileChange.getLines());
                                fileChangeAux = new at.jku.isse.gitecco.core.git.FileChange(lines, fileChange.getFileLines(), fileChange.getPreviousfileLines());
                                filesChanged.put(fileChange.getFileName(), fileChangeAux);
                            }
                        }
                    }
                    try {
                        openDialog("Choose a Directory", new DirectoryView(mergeDir, repositoryDirTextField.getText(), firstcommitTextField.getText(), secondcommitTextField.getText(), filesAdded, filesRemoved, filesChanged));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                toolBar.setDisable(false);
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

        selectFeaturesDirectoryButton.setOnAction(event -> {
            final FileChooser fileChooser = new FileChooser();
            try {
                File directory = new File(featuresSystemTextField.getText());
                if (directory.exists() && directory.isFile())
                    fileChooser.setInitialDirectory(directory.getParentFile());
            } catch (Exception e) {
                // do nothing
            }
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Text Files", "*.txt")
            );
            final File selectedDirectory = fileChooser.showOpenDialog(this.getScene().getWindow());
            if (selectedDirectory != null) {
                featuresSystemTextField.setText(selectedDirectory.toPath().toString());
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

    private void openDialog(String title, Parent content) {
        final Stage dialog = new Stage();
        dialog.initStyle(StageStyle.UTILITY);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(ChangeAnalysisView.this.getScene().getWindow());

        Scene dialogScene = new Scene(content);
        dialog.setScene(dialogScene);
        dialog.setTitle(title);

//		dialog.setMinWidth(400);
//		dialog.setMinHeight(200);

        dialog.show();
        dialog.requestFocus();
    }

    private void showDiff(FileChange data) {
        Stage stage = new Stage();
        stage.setTitle("Git Diff");

        //Label label = new Label(data.getChangeType() + " " + data.getFileName());
        //TextArea result = new TextArea();
        InlineCssTextArea result = new InlineCssTextArea();
        //result.setStyle("-fx-font-family: 'monospaced';");
        //result.setStyle("-fx-font-size: 13px;");
        int i = 1;
        String[] index = data.getLines().split(",");
        String linesaddinit = "";
        String linesaddend = "";
        String linesremovedinit = "";
        String linesremovedend = "";
        int posfile = 0;

        String signal = "";
        if (data.getChangeType().equals("Removed File")) {
            result.setStyle(i - 1, "-rtfx-background-color: #ffdce0");
            linesremovedinit = index[0].substring(data.getLines().indexOf("removed: [") + 10).replaceAll(" ", "");
            linesremovedend = index[index.length - 1].replaceAll(" ", "");
            signal = "-";
        } else if (data.getChangeType().equals("New File")) {
            result.setStyle(i - 1, "-rtfx-background-color: #dcffe4");
            linesaddinit = index[0].substring(index[0].indexOf("added: [") + 8).replaceAll(" ", "");
            linesaddend = index[1].replaceAll(" ", "");
            signal = "+";
        } else {//changed file
            if (!index[0].contains("added: []") && !index[2].contains("removed: []")) {
                linesaddinit = index[0].substring(index[0].indexOf("added: [") + 8).replaceAll(" ", "");
                linesaddend = index[1].replaceAll(" ", "");
                linesremovedinit = index[2].substring(index[2].indexOf("removed: [") + 10).replaceAll(" ", "");
                linesremovedend = index[3].replaceAll(" ", "");
            } else if (!index[0].contains("added: []") && index[2].contains("removed: []")) {
                linesaddinit = index[0].substring(index[0].indexOf("added: [") + 8).replaceAll(" ", "");
                linesaddend = index[1].replaceAll(" ", "");
            } else {
                linesremovedinit = index[0].substring(index[0].indexOf("removed: [") + 10).replaceAll(" ", "");
                linesremovedend = index[1].replaceAll(" ", "");
            }
        }


        if (data.getChangeType().equals("Changed File")) {
            for (String line : data.getFileLines()) {
                //System.out.println("result.length "+result.getParagraphStyleForInsertionAt(i)+" i: "+i);
                if (!linesaddinit.equals("") && i - 1 >= (Integer.valueOf(linesaddinit) - 1) && i - 1 <= (Integer.valueOf(linesaddend) - 1) && !linesremovedinit.equals("") && i - 1 >= (Integer.valueOf(linesremovedinit) - 1) && i - 1 <= (Integer.valueOf(linesremovedend) - 1)) {
                    result.setStyle(i - 1, "-rtfx-background-color: #ffdce0");
                    result.appendText(i + " -\t" + data.getPreviousfileLines().get(i - 1) + "\n");
                    result.setStyle(result.getCurrentParagraph(), "-rtfx-background-color: #dcffe4");
                    result.appendText(i + " +\t" + line + "\n");
                } else if (!linesaddinit.equals("") && linesremovedinit.equals("") && i - 1 >= (Integer.valueOf(linesaddinit) - 2) && i - 1 <= (Integer.valueOf(linesaddend) - 2)) {
                    result.setStyle(result.getCurrentParagraph(), "-rtfx-background-color: #dcffe4");
                    result.appendText(i + " +\t" + line + "\n");
                } else if (!linesremovedinit.equals("") && i - 1 >= (Integer.valueOf(linesremovedinit) - 1) && i - 1 <= (Integer.valueOf(linesremovedend) - 1)) {
                    result.setStyle(i - 1, "-rtfx-background-color: #ffdce0");
                    result.appendText(i + " -\t" + data.getPreviousfileLines().get(i - 1) + "\n");
                } else if (!linesaddinit.equals("") && i - 1 < (Integer.valueOf(linesaddinit) - 1)) {
                    result.setStyle(i - 1, "-rtfx-background-color: transparent");
                    result.appendText(i + " \t" + line + "\n");
                } else if (!linesremovedinit.equals("") && i - 1 < data.getFileLines().size() - 1 && i - 1 > (Integer.valueOf(linesremovedend) - 1)) {
                    result.setStyle(result.getCurrentParagraph(), "-rtfx-background-color: transparent");
                    result.appendText(i + " \t" + line + "\n");
                } else if (!linesremovedinit.equals("") && i - 1 < data.getFileLines().size() && i - 1 > (Integer.valueOf(linesremovedend) - 1)) {
                    //result.setStyle(Integer.valueOf(linesremovedend)+1, "-rtfx-background-color: transparent");
                    result.appendText(i + " \t" + line + "\n");
                } else {
                    result.setStyle(i - 1, "-rtfx-background-color: transparent");
                    result.appendText(i + " \t" + line + "\n");
                }

                i++;
            }
        } else {
            for (String line : data.getFileLines()) {
                if (i < data.getFileLines().size())
                    result.appendText(i + " " + signal + "\t" + line + "\n");
                else
                    result.appendText(i + " " + signal + "\t" + line);
                i++;
            }
        }

        result.setEditable(false);

        //VBox vbox = new VBox();
        result.setPrefWidth(600);
        result.setPrefHeight(1000);
        VirtualizedScrollPane<InlineCssTextArea> vsPane = new VirtualizedScrollPane<>(result);
        vsPane.setMaxWidth(600);
        vsPane.setMaxHeight(1000);
        if (!linesremovedinit.equals(""))
            posfile = Integer.valueOf(linesremovedinit) - 1;
        else if (!linesaddinit.equals(""))
            posfile = Integer.valueOf(linesaddinit) - 2;
        result.moveTo(posfile, 0);//result.getLength());
        result.requestFollowCaret();
        //Scene scene = new Scene(root, 500, 200);
        // focus area so can see the caret
        result.requestFocus();
        Group root = new Group();
        root.getChildren().add(vsPane);
        root.setAutoSizeChildren(true);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle(data.getFileName());
        stage.setResizable(false);
        stage.show();


        //result.moveTo(0);
        //result.requestFollowCaret();
        /*Label title = new Label(data.getFileName());
        vbox.getChildren().add(title);
        vbox.setAlignment(Pos.CENTER);
        vbox.getChildren().add(vsPane);
        vbox.setVgrow(result,Priority.ALWAYS);
        Group root = new Group();
        root.getChildren().add(vbox);
        Scene scene = new Scene(root,600,vbox.getHeight());

        stage.setScene(scene);
        stage.setHeight(vbox.getHeight());
        stage.show();*/

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
        private List<String> previousfileLines = new ArrayList<>();
        private Button btnview;

        private FileChange(boolean propagateChange, String fName, String changeType, String lines, String feati, String feata, List<String> fileLines, List<String> previousfileLines) {
            this.propagateChange.set(propagateChange);
            this.fileName = new SimpleStringProperty(fName);
            this.changeType = new SimpleStringProperty(changeType);
            this.lines = new SimpleStringProperty(lines);
            this.feati = new SimpleStringProperty(feati);
            this.feata = new SimpleStringProperty(feata);
            this.fileLines = fileLines;
            this.previousfileLines = previousfileLines;
            this.btnview = new Button("View");
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

        public List<String> getPreviousfileLines() {
            return previousfileLines;
        }

        public void setPreviousfileLines(List<String> previousfileLines) {
            this.previousfileLines = previousfileLines;
        }

        public Button getBtnview() {
            return btnview;
        }

        public void setBtnview(Button btnview) {
            this.btnview = btnview;
        }
    }

    public void changeAnalysisMethod(int[] rowaux) {
        dataAux.clear();

        try {
            if (table.getItems().size() > 0) {
                table.getColumns().removeAll();
                table.getSelectionModel().clearSelection();
                data.removeAll(data);
            }
            Map<Map<String, List<String>>, Changes> returnMethod = this.changeAnalysis.identification(featuresSystemTextField.getText(), repositoryDirTextField.getText(), firstcommitTextField.getText(), secondcommitTextField.getText(), featureTextField.getText());
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
                        List<String> previousfileLines = new ArrayList<>();
                        FileChange fc = new FileChange(false, fileName, "New File", "Lines added: [" + String.valueOf(addf.getLinesInsert().get(0) + 1) + ", " + String.valueOf(addf.getLinesInsert().get(1) + 1) + ", " + String.valueOf(addf.getLinesInsert().get(2) + 1) + "]", featurei, featurea, fileLines, previousfileLines);
                        //FileChange fc = new FileChange(false, fileName, "New File", "Lines added: " + addf.getLinesInsert(), featurei, featurea, fileLines);
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
                        List<String> previousfileLines = new ArrayList<>();
                        //FileChange fc = new FileChange(false, fileName, "Changed File", "Lines deleted: [" + String.valueOf(changf.getLinesRemoved().get(0) + 1) + ", " + String.valueOf(changf.getLinesRemoved().get(1) + 1) + ", " + String.valueOf((changf.getLinesRemoved().get(2) + 1) / 2) + "]", featurei, featurea, fileLines);
                        FileChange fc = new FileChange(false, fileName, "Changed File", "Lines added: " + changf.getLinesInsert() + " Lines removed: " + changf.getLinesRemoved(), featurei, featurea, fileLines, changf.getPreviousLines());
                        dataAux.add(fc);

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
                        List<String> previousfileLines = new ArrayList<>();
                        FileChange fc = new FileChange(false, fileName, "Removed File", "Lines removed: [" + String.valueOf(delf.getLinesRemoved().get(0) + 1) + ", " + String.valueOf(delf.getLinesRemoved().get(1) + 1) + ", " + String.valueOf(delf.getLinesRemoved().get(2) + 1) + "]", featurei, featurea, fileLines, previousfileLines);
                        //FileChange fc = new FileChange(false, fileName, "Removed File", "Lines removed: " + delf.getLinesRemoved(), featurei, featurea, fileLines);
                        data.add(fc);
                    }
                }
            }

            //add files from the other changf containing the previousfilelines
            for (int j = 0; j < dataAux.size(); j++) {
                FileChange fc = dataAux.get(j);
                    /*if (fc.getLines().contains("Lines removed: []")) {
                        for (int j = 0; j < dataAux.size(); j++) {
                            FileChange fcaux = dataAux.get(j);
                            if (fc.getFileName().equals(fcaux.getFileName())) {
                                if (!fcaux.getLines().contains("Lines removed: []")) {
                                    String linesremoved = fcaux.getLines().substring(fcaux.getLines().indexOf("Lines removed: [") + 16);
                                    String replacelines = fc.getLines().replace("Lines removed: []", "Lines removed: [" + linesremoved);
                                    fc.setLines(replacelines);
                                    fc.setPreviousfileLines(fcaux.getFileLines());
                                    if(!data.contains(fc))
                                        data.add(fc);
                                    break;
                                }
                            }
                        }
                    }else if(!fc.getLines().contains("Lines added: []")){
                        if(!data.contains(fc))
                            data.add(fc);
                    }else */
                if (!data.contains(fc)) {
                    data.add(fc);
                }
            }

            table.getColumns().addAll(selectCol, fileCol, changeCol, linesCol, featiCol, feataff, colBtn);
            toolBar.getItems().addAll(selectAllButton, unselectAllButton, propagateChangesButton);
            table.setItems(data);

            for (FileChange filechange : data) {
                filechange.btnview.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent e) {
                        showDiff(filechange);
                    }
                });
            }

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
            //stage.close();
            gridPane.getChildren().remove(loadingLabel);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void updateView() {
        changeAnalysisButton.setDisable(false);
        cancelButton.setDisable(false);
    }
}

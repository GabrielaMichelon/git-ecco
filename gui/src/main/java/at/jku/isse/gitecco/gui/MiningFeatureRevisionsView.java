package at.jku.isse.gitecco.gui;

import at.jku.isse.gitecco.translation.mining.MiningFeatureRevisions;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MiningFeatureRevisionsView extends BorderPane {

    ObservableList<Release> data = FXCollections.observableArrayList();
    CheckBoxTreeItem<Object> dummyRoot = new CheckBoxTreeItem<>();
    List<String> selectedReleases = new ArrayList<>();
    private Button
            getReleasesButton = new Button("OK");
    private Button cancelButton = new Button("Cancel");
    TreeTableView<Object> releasesTable = new TreeTableView<Object>();
    private Button mineFeatureRevisions = new Button("Mine Feature Revisions");
    // toolbar
    ToolBar toolBar = new ToolBar();

    public MiningFeatureRevisionsView() {
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

        Label miningResultsDirLabel = new Label("Save Results: ");
        gridPane.add(miningResultsDirLabel, 0, row[0], 1, 1);

        TextField miningResultsDirTextField = new TextField("C:\\Users\\gabil\\Desktop\\PHD\\New research\\ChangePropagation\\results");//Open the folder directory to where you want to save the mining results");
        miningResultsDirTextField.setDisable(false);
        miningResultsDirLabel.setLabelFor(miningResultsDirTextField);
        gridPane.add(miningResultsDirTextField, 1, row[0], 1, 1);

        Button selectminingResultsDirectoryButton = new Button("...");
        gridPane.add(selectminingResultsDirectoryButton, 2, row[0], 1, 1);

        row[0]++;

        gridPane.add(getReleasesButton, 0, row[0], 1, 1);
        gridPane.add(cancelButton, 1, row[0], 1, 1);

        row[0]++;

        //button action
        cancelButton.setOnAction(event -> repositoryDirTextField.setText("Open the folder directory containing the Git project"));

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

        selectminingResultsDirectoryButton.setOnAction(event -> {
            final DirectoryChooser directoryChooser = new DirectoryChooser();
            try {
                Path directory = Paths.get(miningResultsDirTextField.getText());
                if (Files.exists(directory) && Files.isDirectory(directory))
                    directoryChooser.setInitialDirectory(directory.toFile());
            } catch (Exception e) {
                // do nothing
            }
            final File selectedDirectory = directoryChooser.showDialog(this.getScene().getWindow());
            if (selectedDirectory != null) {
                miningResultsDirTextField.setText(selectedDirectory.toPath().toString());
            }
        });

        TreeTableColumn<Object, String> treeTableColumnCommit = new TreeTableColumn<>("Commits");
        treeTableColumnCommit.setCellValueFactory(cellData -> {
            TreeItem<Object> rowItem = cellData.getValue();
            if (rowItem != null && rowItem.getValue() instanceof Commit) {
                Commit commit = (Commit) rowItem.getValue();
                return new SimpleStringProperty(commit.getCommitHash());
            } else {
                return new SimpleStringProperty("");
            }
        });
        treeTableColumnCommit.setMinWidth(300);

        TreeTableColumn<Object, String> treeTableColumnFeatures = new TreeTableColumn<>("Feature Revisions");
        treeTableColumnFeatures.setCellValueFactory(cellData -> {
            TreeItem<Object> rowItem = cellData.getValue();
            if (rowItem != null && rowItem.getValue() instanceof Commit) {
                Commit commit = (Commit) rowItem.getValue();
                return new SimpleStringProperty(commit.getFeatureRevisions().toString());
            } else {
                return new SimpleStringProperty("");
            }
        });
        treeTableColumnFeatures.setMinWidth(300);

        getReleasesButton.setOnAction(event -> {
            try {
                Map<Long, String> releases = MiningFeatureRevisions.showReleases(repositoryDirTextField.getText());

                TreeTableColumn<Object, Boolean> treeTableColumnCheckBox = new TreeTableColumn<>("Select");
                CheckBoxTreeTableCell checkCell = new CheckBoxTreeTableCell();
                treeTableColumnCheckBox.setPrefWidth(100);
                treeTableColumnCheckBox.setCellValueFactory(new TreeItemPropertyValueFactory<>("selectBoolean"));
                treeTableColumnCheckBox.setCellFactory(checkCell.forTreeTableColumn(getSelectedProperty));


                TreeTableColumn<Object, String> treeTableColumnRelease = new TreeTableColumn<>("Release");
                treeTableColumnRelease.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));
                treeTableColumnRelease.setMinWidth(200);


                TreeTableColumn<Object, String> treeTableColumnCommitNumber = new TreeTableColumn<>("Commit Number");
                treeTableColumnCommitNumber.setCellValueFactory(new TreeItemPropertyValueFactory<>("commitNumber"));
                treeTableColumnCommitNumber.setMinWidth(200);


                //this line makes possible check each checkbox
                releasesTable.setEditable(true);

                CheckBoxTreeItem<Object> root1;

                for (Map.Entry<Long, String> release : releases.entrySet()) {
                    data.add(new Release(release.getValue().substring(release.getValue().lastIndexOf("/") + 1), Long.toString(release.getKey()), new ArrayList<>()));
                    Release releaseadd = new Release(release.getValue().substring(release.getValue().lastIndexOf("/") + 1), Long.toString(release.getKey()), new ArrayList<>());
                    root1 = new CheckBoxTreeItem<Object>(releaseadd);
                    releaseadd.selectBooleanProperty().bind(root1.selectedProperty());
                    root1.setIndependent(false);
                    dummyRoot.getChildren().add(root1);
                    for (Commit c : releaseadd.getFeatureRevisionPerCommit()) {
                        CheckBoxTreeItem<Object> commitsItem = new CheckBoxTreeItem<>(c);
                        root1.getChildren().add(commitsItem);
                    }
                }

                releasesTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
                dummyRoot.setExpanded(true);
                releasesTable.setRoot(dummyRoot);
                dummyRoot.setIndependent(false);

                releasesTable.getColumns().setAll(treeTableColumnCheckBox, treeTableColumnRelease, treeTableColumnCommitNumber);
                setTop(toolBar);
                toolBar.getItems().addAll(mineFeatureRevisions);

                SplitPane splitPane = new SplitPane();
                setCenter(splitPane);
                splitPane.setOrientation(Orientation.VERTICAL);
                splitPane.getItems().addAll(gridPane, toolBar, releasesTable);
                splitPane.setDividerPosition(0, 0);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        mineFeatureRevisions.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                toolBar.setDisable(true);
                TreeItem root = releasesTable.getRoot();
                if (root != null) {
                    selectedReleases = new ArrayList<>();
                    selectChildren(root);
                }
                if (!selectedReleases.isEmpty()) {
                    for (String releaseName : selectedReleases) {
                        try {
                            Map<String, ArrayList<String>> featureRevisionsRelease = MiningFeatureRevisions.MiningFeatureRevisions(repositoryDirTextField.getText(), miningResultsDirTextField.getText(), selectedReleases, true);
                            releasesTable.getColumns().add(3, treeTableColumnCommit);
                            releasesTable.getColumns().add(4, treeTableColumnFeatures);
                            for (Release release : data) {
                                if (release.getName().equals(releaseName)) {
                                    Map<String, ArrayList<String>> commitHashes = new HashMap<>();
                                    ArrayList<Commit> commits = new ArrayList<>();
                                    for (Map.Entry<String, ArrayList<String>> featuresPerRelease : featureRevisionsRelease.entrySet()) {
                                        if (commitHashes.get(featuresPerRelease.getKey()) != null) {
                                            ArrayList<String> featuresName = commitHashes.get(featuresPerRelease.getKey());
                                            for (String featureName : featuresPerRelease.getValue()) {
                                                String[] features = featureName.split(",");
                                                for (String fRevision : features) {
                                                    if (!featuresName.contains(fRevision)) {
                                                        featuresName.add(fRevision);
                                                    }
                                                }

                                            }
                                            commitHashes.computeIfPresent(featuresPerRelease.getKey(), (k, v) -> featuresName);
                                        } else {
                                            ArrayList<String> featuresName = new ArrayList<>();
                                            for (String featureName : featuresPerRelease.getValue()) {
                                                String[] features = featureName.split(",");
                                                for (String fRevision : features) {
                                                    if (!featuresName.contains(fRevision)) {
                                                        featuresName.add(fRevision);
                                                    }
                                                }

                                            }
                                            commitHashes.put(featuresPerRelease.getKey(), featuresName);
                                        }
                                    }

                                    for (Map.Entry<String, ArrayList<String>> c : commitHashes.entrySet()) {
                                        commits.add(new Commit(c.getKey(), c.getValue()));
                                    }
                                    //update tree
                                    int i = 0;
                                    for (TreeItem<Object> object : dummyRoot.getChildren()) {
                                        Release rel = (Release) object.getValue();
                                        if (rel.getName().equals(releaseName)) {
                                            for (Commit c : commits) {
                                                CheckBoxTreeItem<Object> commitsItem = new CheckBoxTreeItem<>(c);
                                                commitsItem.setExpanded(true);
                                                dummyRoot.getChildren().get(i).setExpanded(true);
                                                dummyRoot.getChildren().get(i).getChildren().addAll(commitsItem);
                                            }
                                            break;
                                        }
                                        i++;
                                    }

                                }
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }

                releasesTable.refresh();
                toolBar.setDisable(false);
            }
        });

    }

    private void selectChildren(TreeItem<Release> root) {
        for (TreeItem<Release> child : root.getChildren()) {
            if (child.getValue().getSelectBoolean().equals(true)) {
                selectedReleases.add(child.getValue().name);
                ((CheckBoxTreeItem) child).setSelected(true);
            }

            // IF THERE ARE CHILD NODES, KEEP DIGGING RECURSIVELY
            if (!child.getChildren().isEmpty()) {
                selectChildren(child);
            }
        }
    }


    Callback<Integer, ObservableValue<Boolean>> getSelectedProperty = new Callback<Integer, ObservableValue<Boolean>>() {
        @Override
        public ObservableValue<Boolean> call(Integer param) {
            return ((CheckBoxTreeItem<?>) releasesTable.getTreeItem(param)).selectedProperty();
        }
    };

    public static class Commit {
        private String commitHash;
        private ArrayList<String> featureRevisions;

        public Commit(String commitHash, ArrayList<String> featureRevisions) {
            this.commitHash = commitHash;
            this.featureRevisions = featureRevisions;
        }

        public String getCommitHash() {
            return commitHash;
        }

        public void setCommitHash(String commitHash) {
            this.commitHash = commitHash;
        }

        public ArrayList<String> getFeatureRevisions() {
            return featureRevisions;
        }

        public void setFeatureRevisions(ArrayList<String> featureRevisions) {
            this.featureRevisions = featureRevisions;
        }
    }

    public static class Release {
        private String name;
        private String commitNumber;
        private ArrayList<Commit> featureRevisionPerCommit;
        private BooleanProperty selectBoolean;

        public Release(String name, String commitNumber, ArrayList<Commit> featureRevisionPerCommit) {
            this.name = name;
            this.commitNumber = commitNumber;
            this.featureRevisionPerCommit = featureRevisionPerCommit;
            this.selectBoolean = new SimpleBooleanProperty(false);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCommitNumber() {
            return commitNumber;
        }

        public void setCommitNumber(String commitNumber) {
            this.commitNumber = commitNumber;
        }

        public ArrayList<Commit> getFeatureRevisionPerCommit() {
            return featureRevisionPerCommit;
        }

        public void setFeatureRevisionPerCommit(ArrayList<Commit> featureRevisionPerCommit) {
            this.featureRevisionPerCommit = featureRevisionPerCommit;
        }

        public BooleanProperty selectBooleanProperty() {
            return selectBoolean;
        }

        public Boolean getSelectBoolean() {
            return selectBoolean.get();
        }

        public void setSelectBoolean(Boolean select) {
            this.selectBoolean.set(select);
        }
    }
}

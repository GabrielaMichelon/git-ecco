package at.jku.isse.gitecco.gui;

import at.jku.isse.ecco.gui.view.SettingsView;
import at.jku.isse.gitecco.translation.changepropagation.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;


public class MainView extends BorderPane {

    // tabs
    static TabPane tabPane = new TabPane();
    static Tab changeTab = new Tab();

    public MainView() {

        this.setCenter(tabPane);

        changeTab.setText("Change Analysis");
        changeTab.setClosable(false);
        tabPane.getTabs().add(changeTab);

        ChangeAnalysisView changesView = new ChangeAnalysisView();
        changeTab.setContent(changesView);

        Tab miningTab = new Tab();
        miningTab.setText("Mine Feature Revisions");
        miningTab.setClosable(false);
        tabPane.getTabs().add(miningTab);

        MiningFeatureRevisionsView miningView = new MiningFeatureRevisionsView();
        miningTab.setContent(miningView);

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




}

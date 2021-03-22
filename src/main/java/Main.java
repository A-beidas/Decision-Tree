package main.java;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class Main extends Application {

	private Stage primaryStage;
	private FileChooser chooser;
	private CheckBox checkBox;
	private File dataset;

	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
		Label label1 = new Label("Threshold:");
		TextField field1 = new TextField();
		Label label2 = new Label("Training Size (percent):");
		TextField field2 = new TextField();
		Label label3 = new Label("New Dataset?");
		checkBox = new CheckBox();
		Button button = new Button("Run");
		Label accuracy = new Label();
		VBox box = new VBox(label1, field1, label2, field2, label3, checkBox, button);
		BorderPane borderPane = new BorderPane(null, new Label("Decision Tree"), accuracy, null, box);
		button.setOnAction(e -> {
			executeTree(borderPane, accuracy, field2.getText(), field1.getText());
		});
		Scene scene = new Scene(borderPane, 600, 400);
		chooser = new FileChooser();
		chooser.setTitle("Choose dataset file");
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Dataset", "*.arff"));
		chooser.setInitialDirectory(new File("src/main/resources"));
		primaryStage.setScene(scene);
		primaryStage.setTitle("Title");
		primaryStage.show();
	}
	
	private void executeTree(BorderPane pane, Label accuracy, String trainingSize, String threshold) {
		if (checkBox.isSelected()) {
			dataset = chooser.showOpenDialog(primaryStage);
		}
		if (dataset == null)
			return;
		Table table = new Table("class", Float.parseFloat(trainingSize), Float.parseFloat(threshold), dataset);
		pane.setCenter(table.toTree());
		accuracy.setText(table.predict() + "%");
	}
}

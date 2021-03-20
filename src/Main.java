import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {
	public static void main(String[] args) {
		Table table = new Table("class", (float) 0.5, (float) 0.5);
		table.predict();
		Application.launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		
		Label label1 = new Label("Threshold:");
		TextField field1 = new TextField();
		Label label2 = new Label("Training Size (percent):");
		TextField field2 = new TextField();
		Button button = new Button("Run");
		Label accuracy = new Label();
		VBox box = new VBox(label1, field1, label2, field2, button);
		BorderPane borderPane = new BorderPane(null, new Label("Decision Tree"), accuracy, null, box);
		button.setOnAction(e -> {
			executeTree(borderPane, accuracy, field2.getText(), field1.getText());
		});
		Scene scene = new Scene(borderPane, 600, 400);
		primaryStage.setScene(scene);
		primaryStage.setTitle("Title");
		primaryStage.show();
	}
	
	private void executeTree(BorderPane pane, Label accuracy, String trainingSize, String threshold) {
		Table table = new Table("class", Float.parseFloat(trainingSize), Float.parseFloat(threshold));
		pane.setCenter(table.toTree());
		accuracy.setText(table.predict() + "%");
	}
}

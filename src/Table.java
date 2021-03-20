import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

public class Table {

	private String title;
	private Attribute[] attributes;
	private TargetClass targetClass;
	private ArrayList<String[]> observations;
	private Attribute.Node root;
	private int trainingSize;
	public Table(String targetClass, float trainingSize, float threshold) {
		Attribute.threshold = threshold;
		Scanner in;
		try {
			in = new Scanner(new File(ClassLoader.getSystemResource("mushroom.arff").toURI()));
		} catch (FileNotFoundException | URISyntaxException e) {
			e.printStackTrace();
			return;
		}
		headerReader(in, targetClass, trainingSize);
		dataReader(in);
		method1();
	}

	private void headerReader(Scanner in, String targetClass, float trainingSize) {
		String line = in.useDelimiter("@attribute ").next();
		String titleLine = line.substring(0, line.indexOf('\n'));
		title = titleLine.split("Title: ")[1];
		line = line.substring(line.indexOf("% 5. "));
		String instancesLine = line.substring(0, line.indexOf('\n'));
		int instances = Integer.parseInt(instancesLine.split("Number of Instances: ?")[1].split(" ")[0]);
		this.trainingSize = (int) (instances * trainingSize);
		observations = new ArrayList<String[]>(this.trainingSize);
		line = line.substring(line.indexOf("% 6."));
		String attributesLine = line.substring(0, line.indexOf('\n'));
		attributes = new Attribute[Integer.parseInt(attributesLine.split("Number of Attributes: ")[1].split(" ")[0])];
		Attribute.attributes = attributes;
		line = line.substring(line.indexOf("% 7. "));
		// TODO process attribute information
		line = in.useDelimiter("\n@data").next();
		String[] attributes_buffer = line.replace("^%.*\n", "").replace("@attribute ", "").split("\n");
		int i, j = 0;
		for (j = 0, i = 0; i < attributes_buffer.length; i++) {
			if (!attributes_buffer[i].matches("(('" + targetClass + "')|(" + targetClass + ")) ?+.*"))
				attributes[j++] = Attribute.instanceOf(attributes_buffer[i], i);
			else if (!attributes_buffer[i].matches(".*\\{.*\\}"))
				throw new IllegalArgumentException(attributes_buffer[i] + "Target Class must be categorical");
			else
				this.targetClass = new TargetClass(i, attributes_buffer[i]);
		}
		if (j == attributes_buffer.length)
			throw new IllegalArgumentException("Target class not found");
		Attribute.targetClass = this.targetClass;
		in.nextLine();
		in.nextLine();
	}

	private void dataReader(Scanner in) {
		observations = new ArrayList<String[]>();
		for (String line = readLineNotComment(in); line != null; line = readLineNotComment(in)) {
			String[] temp = line.replace("'", "").split(",");
			for (int i = 0; i < temp.length; i++) {
				if (i == this.targetClass.indexOfAttribute)
					continue;
				attributes[i].addValue(temp[i],
						this.targetClass.class1.equals(temp[this.targetClass.indexOfAttribute]));
			}
			observations.add(temp);
		}
	}

	void method1() {
		float maxGain = 0;
		int maxIndex = 0;
		for (int i = 0; i < attributes.length; i++) {
			float thisIG = attributes[i].calculateInformationGain();
			if (thisIG > maxGain) {
				maxGain = thisIG;
				maxIndex = i;
			}
		}
		Queue<Attribute.Node> queue = new LinkedList<Attribute.Node>();
		Attribute.queue = queue;
		root = new Attribute.Node(attributes[maxIndex], null);
		root.data.observations = observations;
		root.data.split(root);
		Attribute.Node current = root;
		while (!queue.isEmpty()) {
			current = queue.remove();
			current.data.split(current);
		}
	}
	
	protected float predict() {
		int correct = 0, total = 0;
		for (String[] strings : observations.subList(trainingSize, observations.size())) {
			if (Attribute.predictInstance(strings, root))
				correct++;
			total++;
		}
		return ((float) (correct) / total) * 100;
	}
	
	protected TreeView<String> toTree() { 
		TreeItem<String> root = new TreeItem<String>(this.root.toString());
		TreeView<String> tree = new TreeView<String>(root);
		treeBuild(root, this.root);
		return tree;
	}

	protected void treeBuild(TreeItem<String> item, Attribute.Node node) {
		for (int i = 0; i < node.i; i++) {
			Attribute.Variable.Node variable = node.variables[i];
			TreeItem<String> variableItem = new TreeItem<String>(variable.toString());
			item.getChildren().add(variableItem);
			Attribute.Node nextNode = variable.child;
			if (nextNode == null)
				continue;
			TreeItem<String> nextItem = new TreeItem<String>(variable.child.toString());
			variableItem.getChildren().add(nextItem);
			treeBuild(nextItem, variable.child);
		}
	}
	
	/**
	 * ******************************** Inner class
	 * @author Abeidas
	 *
	 */
	static class TargetClass {

		int indexOfAttribute;
		String class1;
		String class2;
		TargetClass(int i, String string) {
			indexOfAttribute = i;
			String[] temp = string.split("\\{")[1].split(",");
			if (temp.length != 2)
				throw new IllegalArgumentException("Target Class must be binary");
			class1 = temp[0].replace("'", "").trim();
			class2 = temp[1].replaceAll("'|\\}", "").trim();
		}
	}

	private String readLineNotComment(Scanner in) {
		String line = "";
		while (in.hasNextLine()) {
			line = in.nextLine();
			if (!line.startsWith("%"))
				return line;
		}
		return null;
	}
	
	@Override
	public String toString() {
		return "title=" + title + ", number ofinstances=" + observations.size();
	}
}

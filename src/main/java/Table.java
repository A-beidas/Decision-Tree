package main.java;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;


public class Table {

	private String title;
	private Attribute[] attributes;
	private TargetClass targetClass;
	private String[][][] subsets;
	private Attribute.Node root;
	private int trainingSize;
	public static ArrayList<Integer> avoid_attributes;
	private int no_of_instances;
	private int k_fold;

	public Table(String targetClass, float trainingSize, float threshold, File dataset, int k_fold) {
		Attribute.threshold = threshold;
		Scanner in;
		try {
			in = new Scanner(dataset);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		headerReader(in, targetClass);
		create_fold(in, k_fold, trainingSize);
		avoid_attributes();
		for(int i = 0; i < k_fold; i++) {
			dataReader(in, i, k_fold);
			method1(i);
		}
	}

	private void headerReader(Scanner in, String targetClass) {
		String line = in.useDelimiter("@attribute ").next();
		String titleLine = line.substring(0, line.indexOf('\n'));
		title = titleLine.split("Title: ")[1];
		line = line.substring(line.indexOf("% 5. "));
		String instancesLine = line.substring(0, line.indexOf('\n'));
		no_of_instances = Integer.parseInt(instancesLine.split("Number of Instances: ?")[1].split(" ")[0]);
		line = line.substring(line.indexOf("% 6."));
		String attributesLine = line.substring(0, line.indexOf('\n'));
		attributes = new Attribute[Integer.parseInt(attributesLine.split("Number of Attributes: ")[1].split(" ")[0])];
		Attribute.attributes = attributes;
		line = line.substring(line.indexOf("% 7. ") + 5, line.indexOf("% 8. ")).replaceAll("%| |\n", "");
		String[] attribute_information = line.split("\\d+.");
		// TODO process attribute information
		line = in.useDelimiter("\n@data").next();
		String[] attributes_buffer = line.replace("^%.*\n", "").replace("@attribute ", "").split("\n");
		int i, j = 0;
		for (j = 0, i = 0; i < attributes_buffer.length; i++) {
			if (!attributes_buffer[i].matches("(('" + targetClass + "')|(" + targetClass + ")) ?+.*"))
				attributes[j++] = Attribute.instanceOf(attributes_buffer[i], i, attribute_information[i + 1]);
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

	void create_fold(Scanner in, int k_fold, float trainingSize) {
		this.k_fold = k_fold;
		if (k_fold == 1) {
			subsets = new String[2][][];
			this.trainingSize = (int) (no_of_instances * trainingSize);
			subsets[0] = new String[(int) (no_of_instances * trainingSize)][];
			subsets[1] = new String[no_of_instances - subsets[0].length][];
		} else if (k_fold > 2) {
			subsets = new String[k_fold][][];
			int fold_size = no_of_instances / k_fold;
			for (int i = 0; i < k_fold - 1; i++) {
				subsets[i] = new String[fold_size][];
			}
			if (no_of_instances % k_fold != 0) {
				fold_size++;
				subsets[k_fold - 1] = new String[fold_size][];
			}
		} else {
			throw new IllegalArgumentException("k_fold must be greater than zero");
		}
		int subset_i = 0, subset_j = 0;
		for (String line = readLineNotComment(in); line != null; line = readLineNotComment(in)) {
			String[] temp = line.replace("'", "").split(",");
			subsets[subset_i][subset_j++] = temp;
			if (subset_j == subsets[subset_i].length) {
				subset_i++;
				subset_j = 0;
			}
		}
	}

	void avoid_attributes() {
		ArrayList<Integer> avoid_attributes = new ArrayList<Integer>();
		for(int i = 0; i < attributes.length; i++) {
			LinkedHashMap<Character, Integer> frequencies = new LinkedHashMap<Character, Integer>();
			for (int j = 0; j < subsets.length; j++) {
				for (int k = 0; k < subsets[j].length; k++) {
					char attribute_value = subsets[j][k][i].charAt(0);
					if (frequencies.get(attribute_value) == null) {
						frequencies.put(attribute_value, 1);
					} else {
						frequencies.put(attribute_value, frequencies.get(attribute_value) + 1);
					}
				}
			}
			if (frequencies.get('?') != null && frequencies.get('?') > 0.2 * no_of_instances)
				avoid_attributes.add(i);
		}
		if (!avoid_attributes.isEmpty())
			this.avoid_attributes = avoid_attributes;
	}

	private void dataReader(Scanner in, int excluded_fold, int k_fold) {
		excluded_fold = excluded_fold == 1 ? 0 : excluded_fold;
		for (int i_fold = 0; i_fold < k_fold; i_fold++) {
			if (i_fold == excluded_fold)
			for (int i = 0; i < subsets[i_fold].length; i++) {
				for (int j = 0; j < subsets[i_fold][i].length; j++) {
					if (j == this.targetClass.indexOfAttribute)
						continue;
					attributes[j].addValue(subsets[i_fold][i][j],
							this.targetClass.class1.equals(subsets[i_fold][i][this.targetClass.indexOfAttribute]));
				}
			}
		}
	}

	void method1(int exclude) {
		float maxGain = 0;
		int maxIndex = 0;
		for (int i = 0; i < attributes.length; i++) {
			if (avoid_attributes.contains(i))
				continue;
			float thisIG = attributes[i].calculateInformationGain();
			if (thisIG > maxGain) {
				maxGain = thisIG;
				maxIndex = i;
			}
		}
		Queue<Attribute.Node> queue = new LinkedList<Attribute.Node>();
		Attribute.queue = queue;
		root = new Attribute.Node(attributes[maxIndex], null, subsets, k_fold, exclude);
		root.data.split(root);
		Attribute.Node current = root;
		while (!queue.isEmpty()) {
			current = queue.remove();
			current.data.split(current);
		}                                         	
	}
	
	protected float predict(int i) {
		int correct = 0, total = 0;
		for (String[] strings : subsets[i]) {
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

	void set_attribute_information(String[] attribute_information) {

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
		return "title=" + title + ", number ofinstances=" + no_of_instances;
	}
}

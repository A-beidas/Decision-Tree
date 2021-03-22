package main.java;

import java.util.ArrayList;
import java.util.Queue;

public abstract class Attribute implements Iterable<Attribute.Variable> {

	protected int index_in_table;
	protected String name;
	protected int yes;
	protected int no;
	protected static Queue<Node> queue;
	protected static Attribute[] attributes;
	protected static Table.TargetClass targetClass;
	protected static float threshold;
	protected ArrayList<String[]> observations;

	protected Attribute(String name, int index_in_table) {
		this.name = name.replace("'", "");
		this.index_in_table = index_in_table;
	}

	/**
	 * Takes the attribute line removing the prefix '@attribute ' and then processes
	 * it
	 * 
	 * @param line
	 */
	static Attribute instanceOf(String line, int index) {
		if (line.contains("{")) {
			String[] temp = line.replace("}", "").split("\\{", 2);
			return new AttributeCategorical(temp[0], index, temp[1]);
		} else if (line.toLowerCase().contains("numeric"))
			return new AttributeNumeric(line.split(" (?=[^\\s]+)")[0], index);
		else
			throw new IllegalArgumentException("Feature vector must only consist of numerical and categorical values");
	}

//	float calculateInformationGain() {
//		float totalCount = no + yes;
//		// First information is entropy feature
//		float IG = (yes != 0) ? (float) ((yes / totalCount) * (Math.log(totalCount / yes) / Math.log(2))) : 0;
//		IG += (no != 0) ? (no / totalCount) * (Math.log(totalCount / no) / Math.log(2)) : 0;
//		for (Variable value : this) {
//			int variableTotalCount = value.class1 + value.class2;
//			float variableEntropy = (value.class1 != 0)
//					? (float) ((value.class1 / variableTotalCount) * (Math.log(variableTotalCount) / value.class1)
//							/ Math.log(2))
//					: 0;
//			variableEntropy += (value.class2 != 0)
//					? (value.class2 / variableTotalCount) * (Math.log(variableTotalCount) / value.class2)
//					: 0;
//			IG -= variableEntropy;
//		}
//		return IG;
//	}
	
	float calculateInformationGain() {
		float totalCount = no + yes;
		// First information is entropy feature
		float IG = (yes != 0) ? (float) ((yes / totalCount) * (Math.log(totalCount / yes) / Math.log(2))) : 0;
		IG += (no != 0) ? (no / totalCount) * (Math.log(totalCount / no) / Math.log(2)) : 0;
		for (Variable value : this) {
			float variableTotalCount = value.class1 + value.class2;
			float variableEntropy = (value.class1 != 0)
					? (float) ((value.class1 / variableTotalCount) * (Math.log(variableTotalCount / value.class1))
							/ Math.log(2))
					: 0;
			variableEntropy += (value.class2 != 0)
					? (float) ((value.class2 / variableTotalCount) * (Math.log(variableTotalCount / value.class2))
							/ Math.log(2))
					: 0;
			IG -= variableEntropy;
		}
		return IG;
	}

	protected void split(Node current) {
		yes = 0;
		no = 0;
		for (Attribute.Variable variable : this) {
			variable.observations = new ArrayList<String[]>(variable.class1 + variable.class2);
			variable.resetCounts();
			for (String[] line : observations) {
				if (variable.includes(line[index_in_table])) {
					variable.observations.add(line);
				}
			}
			Node splitNode = variable.chooseSplit(current);
			if (splitNode != null)
				queue.add(splitNode);
		}
	}

	abstract void addValue(String value, boolean isClass1);

	abstract int size();

	/**
	 * *********************************************************************Inner
	 * class 1
	 * 
	 * @author Abeidas
	 *
	 */
	protected abstract class Variable {
		protected int class1;
		protected int class2;
		protected ArrayList<String[]> observations;

		protected void resetCounts() {
			class1 = 0;
			class2 = 0;
		}

		public Attribute.Node chooseSplit(Attribute.Node current) {
			boolean[] attributesInBranch = new boolean[attributes.length];
			Attribute.Node loopCurrent = current;
			while (loopCurrent != null) {
				attributesInBranch[loopCurrent.data.index_in_table] = true;
				loopCurrent = loopCurrent.parent;
			}
			recalculateFrequency(attributesInBranch);
			float maxGain = 0;
			int maxIndex = -1;
			for (int i = 0; i < attributes.length; i++) {
				if (attributesInBranch[i] || Table.avoid_attributes.contains(i))
					continue;
				float thisIG = attributes[i].calculateInformationGain();
				if (thisIG > maxGain) {
					maxGain = thisIG;
					maxIndex = i;
				}
			}
			if (maxGain < threshold) {
				current.addVariable(this, null, this.class1, this.class2);
				return null;
			}
			Attribute att = attributes[maxIndex];
			Attribute.Node next = new Attribute.Node(att, current);
			current.addVariable(this, next, this.class1, this.class2);
			next.data.observations = this.observations;
			next.class1 = att.yes;
			next.class2 = att.no;
			return next;
		}

		private void recalculateFrequency(boolean[] attributesInBranch) {
			for (String[] strings : observations)
				for (Attribute attribute : attributes) {
					if (attributesInBranch[attribute.index_in_table])
						continue;
					for (Variable variable : attribute)
						variable.resetCounts();
					attribute.addValue(strings[attribute.index_in_table], targetClass.class1.equals(strings[targetClass.indexOfAttribute]));
				}
		}

		protected void increment(boolean isClass1) {
			if (isClass1) {
				yes++;
				class1++;
			} else {
				no++;
				class2++;
			}
		}

		protected abstract boolean includes(String value);

		public abstract String toString();
		
		protected class Node {

			protected Attribute.Node child;
			protected Variable variable;
			protected int class1;
			protected int class2;
			
			protected Node(Variable variable, int class1, int class2) {
				this.variable = variable;
				this.class1 = class1;
				this.class2 = class2;
			}
			
			@Override
			public String toString() {
				return Attribute.Variable.this.toString();
			}
		}
	}
	
	protected static Boolean predictInstance(String[] line, Node root) {
		Node current = root;
		while (current != null) {
			Attribute.Variable.Node variable = current.nodeIncludes(line[current.data.index_in_table]);
			if (variable == null) {
				String result = (current.class1 >= current.class2) ? targetClass.class1 : targetClass.class2;
				return result.equals(line[targetClass.indexOfAttribute]);
			}
			if (variable.child == null) {
				String result = (current.class1 >= current.class2) ? targetClass.class1 : targetClass.class2;
				return result.equals(line[targetClass.indexOfAttribute]);
			}
			current = variable.child;
		}
		return null;
	}

	/**
	 * ******************************************************************** Inner
	 * class 2
	 */

	protected static class Node {
		Node parent;
		Attribute data;
		int i = 0;
		Attribute.Variable.Node[] variables;
		int class1;
		int class2;

		protected Node(Attribute attribute, Node parent) {
			this.parent = parent;
			data = attribute;
			variables = new Attribute.Variable.Node[data.size()];
		}

		private void addVariable(Variable variable, Attribute.Node child, int class1, int class2) {
			variables[i] = variable.new Node(variable, class1, class2);
			variables[i++].child = child;
		}
		
		protected Attribute.Variable.Node nodeIncludes(String value) {
			for (int i = 0; i < this.i; i++) {
				if (variables[i].variable.includes(value))
					return variables[i];
			}
			return null;
		}
		
		@Override
		public String toString() {
			return data.name + " [" + class1 + "," + class2 + "]";
		}
	}
}

package main.java;

import java.util.HashMap;
import java.util.Iterator;


public class AttributeCategorical extends Attribute {

	HashMap<String, Variable> categories;
	HashMap<String, String> labels;

	public AttributeCategorical(String name, int index, String cats, String attribute_information) {
		super(name, index);
		String[] temp = cats.replaceAll("'| ", "").split(",");
		String[] temp2 = attribute_information.split(":|,");
		categories = new HashMap<String, Variable>(temp.length);
		for (int i = 0; i < temp.length; i++) {
			String[] pair = temp2[i + 1].split("=");
			categories.put(pair[1], new CatVariable(pair[1], pair[0]));
		}
	}

	void addValue(String value, boolean isClass1) {
		if (value.replace("'", "").equals("?"))
			return;
		categories.get(value).increment(isClass1);
	}
	

	@Override
	public Iterator<Variable> iterator() {
		return categories.values().iterator();
	}
	
	@Override
	int size() {
		return categories.size();
	}
	
	private class CatVariable extends Variable {
		// TODO This is actually the value (character)
		String name;
		String label;
		CatVariable(String name, String label) {
			this.label = label;
			this.name = name;
		}
		@Override
		public String toString() {
			return label;
		}
		@Override
		protected boolean includes(String value) {
			value = value.replace("'", "");
			if (value.equals("?"))
				return false;
			return categories.get(value).equals(this);
		}
	}
}

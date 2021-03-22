package main.java;

import java.util.HashMap;
import java.util.Iterator;


public class AttributeCategorical extends Attribute {

	HashMap<String, Variable> categories;

	public AttributeCategorical(String name, int index, String cats) {
		super(name, index);
		String[] temp = cats.replace("'", "").split("(, )|,");
		categories = new HashMap<String, Variable>(temp.length);
		for (String categoryName : temp)
			categories.put(categoryName.trim(), new CatVariable(categoryName.trim()));
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
	
	private class CatVariable extends Attribute.Variable {
		String name;
		CatVariable(String name) {
			this.name = name;
		}
		@Override
		public String toString() {
			return name;
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

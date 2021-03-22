package main.java;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;


public class AttributeNumeric extends Attribute {

	private Interval[] histogram = new Interval[4];
	private ArrayList<SimpleEntry<Float, Boolean>> list = new ArrayList<SimpleEntry<Float, Boolean>>();
	
	public AttributeNumeric(String name, int index) {
		super(name, index);
	}
	
	void addValue(String value, boolean isClass1) {
		if (value.replace("'", "").equals("?"))
			return;
		list.add(new SimpleEntry<Float, Boolean>(Float.parseFloat(value), isClass1));
	}

	void intervalData() {
		Collections.sort(list, new Comparator<SimpleEntry<Float, Boolean>>() {
			public int compare(SimpleEntry<Float, Boolean> o1, SimpleEntry<Float, Boolean> o2) {
				return (int) (o1.getKey() - o2.getKey());
			}
		});
		for (int i = 1; i <= histogram.length; i++) {
			histogram[i] = new Interval(i);
		}
	}

	private class Interval extends Attribute.Variable {
		
		float lowerBound;
		float upperBound;

		private Interval(int quartile) {
			lowerBound = list.get((quartile - 1) * list.size() / 4).getKey();
			upperBound = list.get(quartile * list.size() / 4).getKey();
		}

		@Override
		protected boolean includes(String value) {
			value = value.replace("'", "");
			if (value.equals("?"))
				return false;
			float numeric = Float.parseFloat(value);
			return lowerBound <= numeric && numeric <= upperBound;
		}

		@Override
		public String toString() {
			return "Between " + lowerBound + " and " + upperBound;
		}

	}
	
	@Override
	public java.util.Iterator<Variable> iterator() {
		java.util.Iterator<Variable> iterator = new Iterator<Attribute.Variable>() {
			int i = 0;
			@Override
			public Variable next() {
				return histogram[i++];
			}
			
			@Override
			public boolean hasNext() {
				return i < 4;
			}
		};
		return iterator;
	}
	
	@Override
	int size() {
		return 4;
	}

}

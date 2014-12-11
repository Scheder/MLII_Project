package data;

import weka.core.FastVector;
import weka.core.Instances;

public class PersonData extends Data {

	public PersonData() {
		this(null);
	}
	
	public PersonData(final Instances instances) {
		super(instances);
		this.className = "person";
		this.classValues = new FastVector(2);
		this.classValues.addElement("wannes");
		this.classValues.addElement("leander");
		this.classValues.addElement("other");
	}

	@Override
	public Data getWindow(final int index) {
		final int fromIndex = Data.windowSize * index;
		return new WalkData(new Instances(this.instances, fromIndex, Data.windowSize));
	}
	
	@Override
	public int numOfWindows() {
		return this.instances.numInstances() / Data.windowSize;
	}

	

}

package data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;

public class Data {
	
	private static final Double windowSize = 1.0; //In seconds
	private static final Double overlap = 0.5; //In percentage of window overlap 
	private static final Double sampleRate = 50.0; //In Hz
	
	private Instances instances;
	private List<Integer> windowFromIndices;
	private List<Integer> windowToIndices;
	private boolean changed;
	
	private Data() {
		this.instances = null;
		this.windowFromIndices = new ArrayList<Integer>();
		this.windowToIndices = new ArrayList<Integer>();
		this.changed = true;
	}
	
	public static Data readCSV (final File file) throws IOException {
		Data d = new Data();
		
		//Read CSV file with t,x,y and z data
		CSVLoader loader = new CSVLoader();
		loader.setSource(file);
		d.instances = loader.getDataSet();
		
		//Add magnitudes
		d.instances.insertAttributeAt(
				new Attribute("magnitude"),
				d.instances.numAttributes());
		
		Instance instance;
		Double x,y,z,mag;
		for (int i = 0; i < d.instances.numInstances(); i++) {
			instance = d.instances.instance(i);
			x = instance.value(1);
			y = instance.value(2);
			z = instance.value(3);
			mag = Math.sqrt(x*x + y*y + z*z);
			instance.setValue(4, mag);
		}
		
		d.changed = true;
		return d;
	}
	
	public void toArff(final File file) throws IOException {
		ArffSaver saver = new ArffSaver();
		saver.setInstances(this.instances);
		saver.setFile(file);
		saver.setDestination(file);
		saver.writeBatch();
	}
	
	public Data getWindow(final int index) {
		if (this.changed) this.getWindowIndices();
		
		Data d = new Data();
		final int fromIndex = this.windowFromIndices.get(index);
		final int toIndex = this.windowToIndices.get(index);
		
		d.instances = new Instances(this.instances, fromIndex, (toIndex-fromIndex));
		
		return d;
	}
	
	public int numOfWindows() {
		if (this.changed) this.getWindowIndices();
		return this.windowFromIndices.size();
	}
	
	/**
	 * For now this function assumes Data only contains the data of one file,
	 * for which is assumed that the data has samplerate instances per second,
	 * without time hiatuses.
	 */
	private void getWindowIndices() {
		int windowSize = ((Double)(Data.sampleRate * Data.windowSize)).intValue();
		int overlapSize = new Double((1.0-Data.overlap) * windowSize).intValue();
		
		int fromIndex,toIndex;
		for (fromIndex = 0, toIndex = windowSize; toIndex < this.instances.numInstances();
				fromIndex += overlapSize, toIndex += overlapSize) {
			this.windowFromIndices.add(fromIndex);
			this.windowToIndices.add(toIndex);
		}
		
		if (fromIndex < this.instances.numInstances()) {
			this.windowFromIndices.add(fromIndex);
			this.windowToIndices.add(this.instances.numInstances());
		}
		
		this.changed = false;
	}
	
}

package data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.math3.linear.ArrayRealVector;

import codebook.CodebookFactory;
import weka.core.Attribute;
import weka.core.FastVector;
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
	
	public static Data readCSV (final String file) throws IOException {
		return Data.readCSV(new File(file));
	}
	
	public static Data readCSV (final File file) throws IOException {
		Data d = new Data();
		
		//Read CSV file with t,x,y and z data
		CSVLoader loader = new CSVLoader();
		loader.setSource(file);
		d.instances = loader.getDataSet();
		
		//Add magnitudes
		Attribute magAttr = new Attribute("magnitude");
		d.instances.insertAttributeAt(magAttr,d.instances.numAttributes());
		
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
		
		//Add class "walking". All instances are set to unknown.
		final FastVector walkingValues = new FastVector(2);
		walkingValues.addElement("Yes");
		walkingValues.addElement("No");
		final Attribute walking = new Attribute("walking",walkingValues);
		d.instances.insertAttributeAt(walking,d.instances.numAttributes());
		d.instances.setClass(walking);
		
		d.changed = true;
		return d;
	}
	
	public void toArff(final String file) throws IOException {
		this.toArff(new File(file));
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
	
	/**
	 * Creates a list of frames, which contain a vector of magnitudes.
	 * Frames are 1 second long and overlap 50%
	 */
	public List<ArrayRealVector> toArrayRealVector() {
		//Prepare list for number of windows
		final int size = this.numOfWindows();
		final List<ArrayRealVector> list = new ArrayList<ArrayRealVector>(size);
		final Attribute magAttr = this.instances.attribute("magnitude");
		//Iterate windows
		for (int i = 0; i < size; i++) {
			Data window = this.getWindow(i);
			int windowsize = window.instances.numInstances();
			ArrayRealVector vector = new ArrayRealVector(windowsize);
			//Iterate instances in window
			for (int j = 0; j < windowsize; j++) {
				vector.setEntry(j, window.instances.instance(j).value(magAttr));
			}
			list.add(vector);
		}
		
		
		return list;
	}
	
	public static void main(String[] args) throws IOException {
		Data d = Data.readCSV("Project/train/walk_5_other.csv");
		d.toArff("test.arff");
		//System.out.println(d.instances);
		//System.out.println(d.toArrayRealVector());
		FrameSet frameSet = new FrameSet(d.toArrayRealVector(), sampleRate.intValue());
		System.out.println(frameSet.size());
		
		CodebookFactory.newCodebook(frameSet, "partitionSize", 20, 40, 10, 0.5);
	}
	
}

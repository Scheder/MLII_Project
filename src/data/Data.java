package data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;

public class Data {
	
	//All windows are assumed the same size
	private static final int sampleRate = 50; //In Hz
	private static final int windowSize = 1 * Data.sampleRate; //Number of instances in window
	private static final Double overlap = 0.5; //In percentage of window overlap
	private static final int instancesBetweenWindows = ((Double)((1-Data.overlap) * Data.windowSize)).intValue();
	
	private Instances instances;
	
	private Data() {
		this.instances = null;
	}
	
	private Data(Instances instances) {
		this.instances = instances;
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
	
	/**
	 * Get window with given index
	 * @param index - Index starts at 0
	 * @return
	 */
	public Data getWindow(final int index) {
		final int fromIndex = Data.instancesBetweenWindows * index;
		return new Data(new Instances(this.instances, fromIndex, Data.windowSize));
	}
	
	public int numOfWindows() {
		if (this.instances.numInstances() < Data.windowSize) return 0;
		return (this.instances.numInstances() - Data.windowSize) / Data.instancesBetweenWindows + 1;
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
			ArrayRealVector vector = new ArrayRealVector(Data.windowSize);
			//Iterate instances in window
			for (int j = 0; j < Data.windowSize; j++) {
				vector.setEntry(j, window.instances.instance(j).value(magAttr));
			}
			list.add(vector);
		}
		
		
		return list;
	}
	
	@Override
	public String toString() {
		String str = "";
		str += "[windowsize:"+Data.windowSize;
		str += ",overlap:"+Data.overlap;
		str += ",numberOfInstances:"+this.instances.numInstances();
		str += ",numberOfWindows:"+this.numOfWindows();
		str += ",instances:\n"+this.instances;
		return str;
	}
	
	public static void main(String[] args) throws IOException {
		Data d = Data.readCSV("Project/train/walk_1_other.csv");
		d.toArff("test.arff");
		System.out.println(d);
		System.out.println(d.toArrayRealVector());
		for (int i = 0; i < d.numOfWindows(); i++) {
			System.out.println(d.getWindow(i));
		}
	}
	
}

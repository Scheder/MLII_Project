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

public abstract class Data {
	
	//All windows are assumed the same size
	protected static final int sampleRate = 50; //In Hz
	protected static final int windowSize = 
			1 * Data.sampleRate; //Instances per window
	protected static final Double overlap = 0.5; //Percentage of window overlap
	protected static final int instancesBetweenWindows = 
			((Double)((1-Data.overlap) * Data.windowSize)).intValue();
	
	protected Instances instances;
	protected File file;
	protected String className;
	protected FastVector classValues;
	
	public Data() {
		this(null);
	}
	
	public Data(Instances instances) {
		this.instances = instances;
	}
	
	public String getClassName() {
		return this.className;
	}
	
	public FastVector getClassValues() {
		return this.classValues;
	}
	
	public void readCSV (final String file) throws IOException {
		this.readCSV(new File(file));
	}
	
	public void readCSV (final File file) throws IOException {
		this.file = file;
		
		//Read CSV file with t,x,y and z data
		CSVLoader loader = new CSVLoader();
		loader.setSource(file);
		this.instances = loader.getDataSet();
		
		//Add magnitudes
		Attribute magAttr = this.instances.attribute("magnitude");
		if (magAttr == null) {
			magAttr = new Attribute("magnitude",4);
			this.instances.insertAttributeAt(magAttr, 4);
		}
		
		Instance instance;
		Double x,y,z,mag;
		for (int i = 0; i < this.instances.numInstances(); i++) {
			instance = this.instances.instance(i);
			x = instance.value(1);
			y = instance.value(2);
			z = instance.value(3);
			mag = Math.sqrt(x*x + y*y + z*z);
			instance.setValue(4, mag);
		}
		
		//Add class. All instances are set to unknown.
		//Only add if it wasn't there already
		Attribute classAttr = this.instances.attribute(this.className);
		if (classAttr == null) {
			classAttr = new Attribute(this.className,this.classValues,5);
			this.instances.insertAttributeAt(classAttr,5);
		}
		
		this.instances.setClass(classAttr);
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
	abstract public Data getWindow(final int index);
	abstract public int numOfWindows();
	
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
	
	abstract public List<String> getLabels();	
	
	@Override
	public String toString() {
		String str = "";
		str += "[windowsize:"+Data.windowSize;
		str += ",overlap:"+Data.overlap;
		str += ",numberOfInstances:"+this.instances.numInstances();
		str += ",numberOfWindows:"+this.numOfWindows();
		str += ",className:"+this.className;
		str += ",classValues:"+this.classValues;
		str += ",instances:\n"+this.instances;
		return str;
	}
	
	
	public static void main(String[] args) throws IOException {
//		Data d = new WalkData();
//		d.readCSV("Project/train/walk_1_other.csv");
//		d.toArff("test");
//		Data d = Data.readCSV("Project/labeled_walking_train/walk_45_other.csv");
//		Data d1 = Data.readCSV("Project/train/walk_1_other.csv");
//		d.toArff("test.arff");
//		System.out.println(d);
//		System.out.println(d.toArrayRealVector());
//		d.visualize();
//		System.out.println(d);
//		for (int i = 0; i < d.numOfWindows(); i++) {
//			System.out.println(d.getWindow(i));
//		}
		WalkData.visualizeFile();
	}
	
}

package data;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;

public class Data {
	
	private static final Double windowSize = 1.0; //In seconds.
	private static final Double overlap = 0.5; //In percentage of window overlap.
	private static final int samplingRate = 50 //In Hz.
	private static final Attribute magAttr = new Attribute("magnitude");
	
	private Instances instances;
	public Vector<Double> t;
	public Vector<Double> x;
	public Vector<Double> y;
	public Vector<Double> z;
	public Vector<Double> magnitude;
	
	private Data() {
		this.instances = null;
		this.t = new Vector<Double>();
		this.x = new Vector<Double>();
		this.y = new Vector<Double>();
		this.z = new Vector<Double>();
		this.magnitude = new Vector<Double>();
	}
	
	public static Data readCSV (final File file) throws IOException {
		Data d = new Data();
		
		//Read CSV file with t,x,y and z data
		CSVLoader loader = new CSVLoader();
		loader.setSource(file);
		d.instances = loader.getDataSet();
		
		//Add magnitudes
		d.instances.insertAttributeAt(Data.magAttr,d.instances.numAttributes());
		
		Instance instance;
		Double t,x,y,z,mag;
		for (int i = 0; i < d.instances.numInstances(); i++) {
			instance = d.instances.instance(i);
			t = instance.value(0);
			x = instance.value(1);
			y = instance.value(2);
			z = instance.value(3);
			mag = Math.sqrt(x*x + y*y + z*z);
			instance.setValue(4, mag);
			
			d.t.add(i,t);
			d.x.add(i,x);
			d.y.add(i,y);
			d.z.add(i,z);
			d.magnitude.add(i,mag);
		}
		return d;
	}
	
	public void toArff(final File file) throws IOException {
		ArffSaver saver = new ArffSaver();
		saver.setInstances(this.instances);
		saver.setFile(file);
		saver.setDestination(file);
		saver.writeBatch();
	}
	
	public void extract_windows() {
		//TODO
	}
	
	public static void main(String[] args) throws IOException {
		Data d = Data.readCSV(new File("Project/train/walk_1_other.csv"));
		System.out.println(d.instances);
	}

}

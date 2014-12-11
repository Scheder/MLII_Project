package data;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.plaf.FileChooserUI;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import codebook.CodebookFactory;
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
	
	public void visualize() {
		ApplicationFrame frame = new ApplicationFrame("Data");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new GridLayout(2,2));
		
		//Define the data series
		final XYSeries x_values = new XYSeries("x");
		final XYSeries y_values = new XYSeries("y");
		final XYSeries z_values = new XYSeries("z");
		final XYSeries m_values = new XYSeries("m");
		
		for (int i = 0; i < this.instances.numInstances(); i++) {
			Instance instance = this.instances.instance(i);
			Double t = instance.value(0)/1e9;
			x_values.add(t,(Double)instance.value(1));
			y_values.add(t,(Double)instance.value(2));
			z_values.add(t,(Double)instance.value(3));
			m_values.add(t,(Double)instance.value(4));
		}
		
		//Define the collection for the data series
		final XYSeriesCollection x_data = new XYSeriesCollection();
		final XYSeriesCollection y_data = new XYSeriesCollection();
		final XYSeriesCollection z_data = new XYSeriesCollection();
		final XYSeriesCollection m_data = new XYSeriesCollection();
		x_data.addSeries(x_values);
		y_data.addSeries(y_values);
		z_data.addSeries(z_values);
		m_data.addSeries(m_values);
		
		//Add the series to a chart
		final JFreeChart x_chart = ChartFactory.createXYLineChart(
				"X","time","acceleration",x_data,PlotOrientation.VERTICAL,
				false,true,false);
		final JFreeChart y_chart = ChartFactory.createXYLineChart(
				"Y", "time", "acceleration",y_data,PlotOrientation.VERTICAL,
				false,true,false);
		final JFreeChart z_chart = ChartFactory.createXYLineChart(
				"Z","time","acceleration",z_data,PlotOrientation.VERTICAL,
				false,true,false);
		final JFreeChart m_chart = ChartFactory.createXYLineChart(
				"Magnitude", "time", "acceleration",m_data,PlotOrientation.VERTICAL,
				false,true,false);
				
		//Add the charts to panels and the panels to the frame
		final ChartPanel x_panel = new ChartPanel(x_chart);
		final ChartPanel y_panel = new ChartPanel(y_chart);
		final ChartPanel z_panel = new ChartPanel(z_chart);
		final ChartPanel m_panel = new ChartPanel(m_chart);
		x_panel.setPreferredSize(new Dimension(500,270));
		y_panel.setPreferredSize(new Dimension(500,270));
		z_panel.setPreferredSize(new Dimension(500,270));
		m_panel.setPreferredSize(new Dimension(500,270));
		frame.add(x_panel);
		frame.add(y_panel);
		frame.add(z_panel);
		frame.add(m_panel);
		RefineryUtilities.centerFrameOnScreen(frame);
		frame.pack();
		frame.setVisible(true);
	}
	
	/**
	 * Asks what file to visualize, then visualizes the file
	 * @throws IOException 
	 */
	public static void visualizeFile() throws IOException {
		final ApplicationFrame frame = new ApplicationFrame("Data");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JButton button = new JButton("Browse...");
		button.addActionListener( new ActionListener() {
			
			private File currentDirectory = new File(System.getProperty("user.dir"));
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setCurrentDirectory(currentDirectory);
				int result = fileChooser.showOpenDialog(frame);
				
				if (result == JFileChooser.APPROVE_OPTION) {
					currentDirectory = fileChooser.getCurrentDirectory();
					Data data;
					try {
						data = Data.readCSV(fileChooser.getSelectedFile());
						data.visualize();
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(frame, e1.getMessage());
						e1.printStackTrace();
					}
				}
			}
		});
		
		frame.add(button);
		
		RefineryUtilities.centerFrameOnScreen(frame);
		frame.pack();
		frame.setVisible(true);
	}
	
	public static void main(String[] args) throws IOException {
//		Data d = Data.readCSV("Project/train/walk_5_other.csv");
//		d.toArff("test.arff");
//		//System.out.println(d.instances);
//		//System.out.println(d.toArrayRealVector());
//		FrameSet frameSet = new FrameSet(d.toArrayRealVector(), sampleRate.intValue());
//		System.out.println(frameSet.size());
//		
//		CodebookFactory.newCodebook(frameSet, "partitionSize", 20, 40, 10, 0.5);


//		Data d = Data.readCSV("Project/train/walk_3_other.csv");
//		d.toArff("test.arff");
//		System.out.println(d);
//		System.out.println(d.toArrayRealVector());
//		d.visualize();
//		for (int i = 0; i < d.numOfWindows(); i++) {
//			System.out.println(d.getWindow(i));
//		}
		Data.visualizeFile();
	}
	
}

package data;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVSaver;

public class WalkData extends Data {

	public WalkData() {
		this(null);
	}
	
	public WalkData(final Instances instances) {
		super(instances);
		this.className = "walking";
		this.classValues = new FastVector(2);
		this.classValues.addElement("Yes");
		this.classValues.addElement("No");
	}
	
	public void visualize() {
		this.visualize(this.file.getName());
	}
	
	public void visualize(final String title) {
		JFrame frame = new JFrame(title);
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
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
				fileChooser.setMultiSelectionEnabled(true);
				FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV files", "CSV");
				fileChooser.setFileFilter(filter);
				fileChooser.setCurrentDirectory(currentDirectory);
				int result = fileChooser.showOpenDialog(frame);
				
				if (result == JFileChooser.APPROVE_OPTION) {
					currentDirectory = fileChooser.getCurrentDirectory();
					File[] files = fileChooser.getSelectedFiles();
					for (File file : files) {
						System.out.println("Visualizing: " + file.getName() + "...");
						try {
							WalkData data = new WalkData();
							data.readCSV(file);
							data.visualize(file.getName());
						} catch (IOException e1) {
							e1.printStackTrace();
							JOptionPane.showMessageDialog(frame,e1.getMessage(),file.getName(),JOptionPane.ERROR_MESSAGE);
						}
					}
				}
			}
		});
		
		frame.add(button);
		
		RefineryUtilities.centerFrameOnScreen(frame);
		frame.pack();
		frame.setVisible(true);
	}
	
	public void writeData(final List<String> labels) throws IOException {
		if (this.numOfWindows() == 0) return;
		
		Instances writeInstances = new Instances(this.instances,this.instances.numInstances());
		for (int i = 0; i < this.numOfWindows(); i++) {
			Data window = this.getWindow(i);
			String label = labels.get(i);
			
			//No need to add non-walking frames
			if ("No".equals(label)) continue;
			
			for (int j = 0; j < window.instances.numInstances(); j++) {
				Instance instance = window.instances.instance(j);
				writeInstances.add(instance);
			}
		}
		int classIndex = writeInstances.classIndex();
		writeInstances.setClassIndex(-1);
		writeInstances.deleteAttributeAt(classIndex);
		
		File outputFile = new File("Project/filtered_train/filtered_"+this.file.getName());
		CSVSaver saver = new CSVSaver();
		saver.setInstances(writeInstances);
		saver.setFile(outputFile);
		saver.writeBatch();
	}

	@Override
	public Data getWindow(final int index) {
		final int fromIndex = Data.instancesBetweenWindows * index;
		return new WalkData(new Instances(this.instances, fromIndex, Data.windowSize));
	}
	
	@Override
	public int numOfWindows() {
		if (this.instances.numInstances() < Data.windowSize) return 0;
		return (this.instances.numInstances() - Data.windowSize) / Data.instancesBetweenWindows + 1;
	}
	
}

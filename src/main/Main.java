package main;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import weka.core.FastVector;
import codebook.Codebook;
import codebook.CodebookFactory;
import classifier.ClassifierFactory;
import classifier.CodebookClassifier;
import data.Data;
import data.FrameSet;
import data.LabeledFrameSet;
import data.PersonData;
import data.WalkData;

public class Main {

	/**
	 * FilenameFilter selecting CSV filenames.
	 */
	public static class CSVFilter implements FilenameFilter {

		@Override
		public boolean accept(File dir, String name) {
			return name.toLowerCase().endsWith(".csv");
		}
		
	}
	
	public static void main(String[] args) throws Exception {
		long start = System.nanoTime();

		Main.visualize();
		
//		Main.filterAndEvaluateCodebooks();
//		Main.labelTestData("Project/test");
		
		double elapsedTimeInSec = (System.nanoTime() - start) * 1e-9;
		System.out.println("Finished after " + elapsedTimeInSec + " seconds.");
	}
	
	private static void visualize() throws Exception {
		final ApplicationFrame frame = new ApplicationFrame("MLII Project");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		final LabeledFrameSet walkLabeled = 
				Main.getLabeledWalkFrameSet("Project/labeled_train");
		//Get codebook
		final Codebook walkCodebook = CodebookFactory.getWalkCodebook();
		final CodebookClassifier classifier = 
				ClassifierFactory.
				createWalkClassifier(walkCodebook, walkLabeled);
		
		final LabeledFrameSet personLabeled =
				Main.getLabeledPersonFrameSet("Project/labeled_train");
		final Codebook personCodebook = CodebookFactory.getPersonCodebook();
		final CodebookClassifier personClassifier =
				ClassifierFactory.
				createPersonClassifier(personCodebook, personLabeled);

		JButton button = new JButton("Browse...");
		button.addActionListener( new ActionListener() {
			
			private File currentDirectory = 
					new File(System.getProperty("user.dir"));
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setMultiSelectionEnabled(true);
				FileNameExtensionFilter filter = 
						new FileNameExtensionFilter("CSV files", "CSV");
				fileChooser.setFileFilter(filter);
				fileChooser.setCurrentDirectory(currentDirectory);
				int result = fileChooser.showOpenDialog(frame);
				
				if (result == JFileChooser.APPROVE_OPTION) {
					currentDirectory = fileChooser.getCurrentDirectory();
					File[] files = fileChooser.getSelectedFiles();
					for (File file : files) {
						System.out.println(
								"Classifying: " + file.getName() + "...");
						try {
							Main.writeFilteredWalkFile(classifier,file);
							File filteredFile = new File(
									"Project/filtered_"+
											file.getParentFile().getName()+
											"/filtered_"+file.getName());
							personClassifier.classifyFile(filteredFile);
						} catch (Exception e1) {
							JOptionPane.showMessageDialog(
									frame,
									"File could not be processed",
									file.getName(),
									JOptionPane.ERROR_MESSAGE);
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
	
	private static void labelTestData(final String folder) throws Exception {
		//Read all test data
		LabeledFrameSet walkLabeled = 
				Main.getLabeledWalkFrameSet("Project/labeled_train");
		//Get codebook
		Codebook walkCodebook = CodebookFactory.getWalkCodebook();
		CodebookClassifier classifier = 
				ClassifierFactory.
				createWalkClassifier(walkCodebook, walkLabeled);
		Main.writeFilteredWalkData(classifier,folder);
		
		LabeledFrameSet personLabeled =
				Main.getLabeledPersonFrameSet("Project/labeled_train");
		Codebook personCodebook = CodebookFactory.getPersonCodebook();
		CodebookClassifier personClassifier =
				ClassifierFactory.
				createPersonClassifier(personCodebook, personLabeled);
		personClassifier.classify("Project/filtered_test");
	}
	
	private static void filterAndEvaluateCodebooks() throws Exception {
		/** WALK DATA **/
		//First read all unlabeled train data in memory
		FrameSet walkUnlabeled = 
				Main.getWalkFrameSet("Project/train");
		//Second read all labeled train data in memory
		LabeledFrameSet walkLabeled = 
				Main.getLabeledWalkFrameSet("Project/labeled_train");
		Codebook walkCodebook = CodebookFactory.getCodebook(walkUnlabeled);
		CodebookClassifier walkClassifier = 
				ClassifierFactory.createWalkClassifier(walkCodebook,walkLabeled);
		Main.writeFilteredWalkData(walkClassifier,"Project/train");
		
		/** PERSON DATA **/
		FrameSet personUnlabeled = 
				Main.getPersonFrameSet("Project/filtered_train");
		LabeledFrameSet personLabeled = 
				Main.getLabeledPersonFrameSet("Project/labeled_train");
		Codebook personCodebook = CodebookFactory.getCodebook(personUnlabeled);
		CodebookClassifier personClassifier = 
				ClassifierFactory.
				createPersonClassifier(personCodebook,personLabeled);
		
		FastVector classValues = new FastVector(3);
		classValues.addElement("wannes");
		classValues.addElement("leander");
		classValues.addElement("other");
		personClassifier.evaluate(personLabeled, "person", classValues);
	}
	
	private static FrameSet getPersonFrameSet(final String folderName) {
		final File folder = new File(folderName);
		//Get all CSV files
		List<ArrayRealVector> frames = new ArrayList<ArrayRealVector>();
		for (File file : folder.listFiles(new Main.CSVFilter())) {
			try {
				Data d = new PersonData();
				d.readCSV(file);
				frames.addAll(d.toArrayRealVector());
			} catch (IOException e) {
				//Continue to the next file with empty files
			} catch (RuntimeException e) {
				//Continue to the next file if no data is available
			}
		}
		return new FrameSet(frames);
	}
	
	private static LabeledFrameSet getLabeledPersonFrameSet(
			final String folderName) {
		final File folder = new File(folderName);
		//Get all CSV files
		List<ArrayRealVector> frames = new ArrayList<ArrayRealVector>();
		List<String> labels = new ArrayList<String>();
		for (File file : folder.listFiles(new Main.CSVFilter())) {
			try {
				Data d = new PersonData();
				d.readCSV(file);
				frames.addAll(d.toArrayRealVector());
				labels.addAll(d.getLabels());
			} catch (IOException e) {
				//Continue to the next file with empty files
				continue;
			}
		}
		return new LabeledFrameSet(frames, labels);
	}
	
	private static FrameSet getWalkFrameSet(final String folderName) {
		final File folder = new File(folderName);
		//Get all CSV files
		List<ArrayRealVector> frames = new ArrayList<ArrayRealVector>();
		for (File file : folder.listFiles(new Main.CSVFilter())) {
			try {
				Data d = new WalkData();
				d.readCSV(file);
				frames.addAll(d.toArrayRealVector());
			} catch (IOException e) {
				//Continue to the next file with empty files
			}
		}
		return new FrameSet(frames);
	}
	
	private static LabeledFrameSet getLabeledWalkFrameSet(
			final String folderName) {
		final File folder = new File(folderName);
		//Get all CSV files
		List<ArrayRealVector> frames = new ArrayList<ArrayRealVector>();
		List<String> labels = new ArrayList<String>();
		for (File file : folder.listFiles(new Main.CSVFilter())) {
			try {
				Data d = new WalkData();
				d.readCSV(file);
				frames.addAll(d.toArrayRealVector());
				labels.addAll(d.getLabels());
			} catch (IOException e) {
				//Continue to the next file with empty files
				continue;
			}
		}
		return new LabeledFrameSet(frames, labels);
	}
	
	private static void writeFilteredWalkData(
			CodebookClassifier classifier, String folderName)
			throws Exception {
		System.out.println("Filtering walk data...");
		File folder = new File(folderName);
		for (File file : folder.listFiles(new Main.CSVFilter())) {
			try {
				Main.writeFilteredWalkFile(classifier, file);
			} catch (Exception e) {
				//Continue to the next file with empty files
				continue;
			}
		}
	}
	
	private static void writeFilteredWalkFile(
			CodebookClassifier classifier, File file) throws Exception {
		//System.out.println("Filtering out walk data...");
		WalkData d = new WalkData();
		d.readCSV(file);
		List<String> labels = classifier.getLabels(d);
		d.writeData(labels);
	}
	
}

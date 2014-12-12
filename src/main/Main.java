package main;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;

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

	final static String walkCodebookFile = "codebook.ser";
	final static String personCodebookFile = "codebook.ser";
	
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
		//TODO pretty print exception

		Main.labelTestData();
		
		double elapsedTimeInSec = (System.nanoTime() - start) * 1e-9;
		System.out.println("Finished after " + elapsedTimeInSec + " seconds.");
	}
	
	private static void labelTestData() throws Exception {
		//Read all test data
		LabeledFrameSet walkLabeled = 
				Main.getLabeledWalkFrameSet("Project/labeled_train");
		//Get codebook
		Codebook walkCodebook = 
				CodebookFactory.deserializeCodebook(Main.walkCodebookFile);
		CodebookClassifier classifier = 
				ClassifierFactory.
				createWalkClassifier(walkCodebook, walkLabeled);
		Main.writeFilteredWalkData(classifier,"Project/test");
		
		LabeledFrameSet personLabeled =
				Main.getLabeledPersonFrameSet("Project/labeled_train");
		Codebook personCodebook =
				CodebookFactory.deserializeCodebook(Main.personCodebookFile);
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
			WalkData d = new WalkData();
			try {
				d.readCSV(file);
			} catch (IOException e) {
				//Continue to the next file with empty files
				continue;
			}
			List<String> labels = classifier.getLabels(d);
			d.writeData(labels);
		}
	}
	
}

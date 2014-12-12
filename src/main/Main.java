package main;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;

import classifier.ClassifierFactory;
import classifier.CodebookClassifier;
import data.Data;
import data.FrameSet;
import data.LabeledFrameSet;
import data.PersonData;
import data.WalkData;

public class Main {

	private static class CSVFilter implements FilenameFilter {

		@Override
		public boolean accept(File dir, String name) {
			return name.toLowerCase().endsWith(".csv");
		}
		
	}
	
	//TODO pretty print exception
	public static void main(String[] args) throws Exception {
		long start = System.nanoTime();

		/** PERSON DATA **/
		//FrameSet unlabeled = Main.getPersonFrameSet(new File("Project/filtered_train"));
		//LabeledFrameSet labeled = Main.getLabeledPersonFrameSet(new File("Project/labeled_train"));
		
		/** WALKING DATA **/
		FrameSet unlabeled = Main.getWalkFrameSet(new File("Project/train"));
		LabeledFrameSet labeled = Main.getLabeledWalkFrameSet(new File("Project/labeled_train"));
		
		CodebookClassifier classifier = ClassifierFactory.createWalkClassifier(labeled, unlabeled);
		//TODO do crossvalidation etc
		
		Main.writeFilteredWalkData(classifier);
		
		double elapsedTimeInSec = (System.nanoTime() - start) * 1e-9;
		System.out.println("Finished after " + elapsedTimeInSec + " seconds.");
	}
	
	public static void filterWalkData() throws Exception {
		//First read all unlabeled train data in memory
		FrameSet unlabeled = Main.getWalkFrameSet(new File("Project/train"));
		//Second read all labeled train data in memory
		LabeledFrameSet labeled = Main.getLabeledWalkFrameSet(new File("Project/labeled_train"));
		CodebookClassifier classifier = ClassifierFactory.createWalkClassifier(labeled, unlabeled);
		Main.writeFilteredWalkData(classifier);
	}
	
	private static FrameSet getPersonFrameSet(final File folder) {		
		//Get all CSV files
		List<ArrayRealVector> frames = new ArrayList<ArrayRealVector>();
		for (File file : folder.listFiles(new Main.CSVFilter())) {
			try {
				Data d = new PersonData();
				d.readCSV(file);
				frames.addAll(d.toArrayRealVector());
			} catch (IOException e) {
				//Continue to the next file with empty files
			}
		}
		return new FrameSet(frames);
	}
	
	private static LabeledFrameSet getLabeledPersonFrameSet(final File folder) {
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
	
	private static FrameSet getWalkFrameSet(final File folder) {
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
	
	private static LabeledFrameSet getLabeledWalkFrameSet(final File folder) {
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
	
	private static void writeFilteredWalkData(CodebookClassifier classifier) throws Exception {
		File folder = new File("Project/train");
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

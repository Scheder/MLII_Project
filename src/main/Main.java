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
		
		//First read all unlabeled train data in memory
		FrameSet unlabeled = Main.getFrameSet("Project/train");
		//Second read all labeled train data in memory
		LabeledFrameSet labeled = Main.getLabeledFrameSet("Project/labeled_train");
		
		CodebookClassifier classifier = ClassifierFactory.createWalkClassifier(labeled, unlabeled);
		
		Main.writeFilteredData(classifier);
		
		double elapsedTimeInSec = (System.nanoTime() - start) * 1e-9;
		System.out.println("Finished after " + elapsedTimeInSec + " seconds.");
	}
	
	private static FrameSet getFrameSet(final String folder) {
		return Main.getFrameSet(new File(folder));
	}
	
	private static FrameSet getFrameSet(final File folder) {
		//Get all CSV files
		List<ArrayRealVector> frames = new ArrayList<ArrayRealVector>();
		for (File file : folder.listFiles(new Main.CSVFilter())) {
			try {
				frames.addAll(Data.readCSV(file).toArrayRealVector());
			} catch (IOException e) {
				//Continue to the next file with empty files
			}
		}
		return new FrameSet(frames);
	}
	
	private static LabeledFrameSet getLabeledFrameSet(final String folder) {
		return Main.getLabeledFrameSet(new File(folder));
	}
	
	private static LabeledFrameSet getLabeledFrameSet(final File folder) {
		//Get all CSV files
		List<ArrayRealVector> frames = new ArrayList<ArrayRealVector>();
		List<String> labels = new ArrayList<String>();
		for (File file : folder.listFiles(new Main.CSVFilter())) {
			try {
				Data d = Data.readCSV(file);
				frames.addAll(d.toArrayRealVector());
				labels.addAll(d.getLabels());
			} catch (IOException e) {
				//Continue to the next file with empty files
				continue;
			}
		}
		return new LabeledFrameSet(frames, labels);
	}
	
	private static void writeFilteredData(CodebookClassifier classifier) throws Exception {
		File folder = new File("Project/train");
		for (File file : folder.listFiles(new Main.CSVFilter())) {
			Data d;
			try {
				d = Data.readCSV(file);
			} catch (IOException e) {
				//Continue to the next file with empty files
				continue;
			}
			List<String> labels = classifier.getLabels(d);
			d.writeWalkData(labels);
		}
	}
	
}

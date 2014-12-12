package classifier;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.commons.math3.linear.ArrayRealVector;

import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import codebook.Codebook;
import codebook.CodebookFactory;
import data.FrameSet;
import data.LabeledFrameSet;

public class ClassifierFactory {
	
	public static CodebookClassifier createWalkClassifier(
			Codebook codebook, LabeledFrameSet labeled) throws Exception {
		
		FrameSet activations = codebook.activate(labeled);
		LabeledFrameSet labeledActivations = 
				activations.labelFrameSet(labeled.getLabelList());
		
		FastVector walkingValues = new FastVector(2);
		walkingValues.addElement("Yes");
		walkingValues.addElement("No");
		Instances trainSet = ClassifierFactory.activationsToInstances(
				labeledActivations, "walking", walkingValues);
		
		//Create array of classifiers
		//TODO use options for classifiers and add classifiers
		Classifier smo = new SMO();
		Classifier j48 = new J48();
		
//		classifiers[0] = svm;
//		classifiers[1] = j48;
//		
//		//TODO use correct options for (meta)classifiers
//		Vote vote = new Vote();
//		vote.setClassifiers(classifiers);
//		
//		vote.buildClassifier(trainSet);
//		return vote;
		//TODO use meta classifier. vote can not work with numeric attribute
		Classifier classifier = j48;
		
		classifier.buildClassifier(trainSet);
		return new CodebookClassifier(codebook, classifier);
	}
	
	public static CodebookClassifier createPersonClassifier(
			Codebook codebook, LabeledFrameSet labeled) throws Exception {
		
		FrameSet activations = codebook.activate(labeled);
		LabeledFrameSet labeledActivations = 
				activations.labelFrameSet(labeled.getLabelList());
		
		FastVector personValues = new FastVector(3);
		personValues.addElement("leander");
		personValues.addElement("wannes");
		personValues.addElement("other");
		Instances trainSet = ClassifierFactory.activationsToInstances(
				labeledActivations, "person", personValues);
		
		
		
		//Create array of classifiers
		//TODO use options for classifiers and add classifiers
//		Classifier[] classifiers = new Classifier[2];
		Classifier smo = new SMO();
		Classifier j48 = new J48();
		
//		classifiers[0] = svm;
//		classifiers[1] = j48;
//		
//		//TODO use correct options for (meta)classifiers
//		Vote vote = new Vote();
//		vote.setClassifiers(classifiers);
//		
//		vote.buildClassifier(trainSet);
//		return vote;
		//TODO use meta classifier. vote can not work with numeric attribute
		Classifier classifier = j48;
		
		classifier.buildClassifier(trainSet);
		return new CodebookClassifier(codebook, classifier);
	}
	
	public static void testClassifier(Classifier classifier, FrameSet testSet) {
		//Transform FrameSet to Instances
		//TODO get statistics, confusion matrix etc.
	}
	
	public static Codebook getCodebook(FrameSet unlabeled) 
			throws ClassNotFoundException, IOException {
		File file = new File("codebook.ser");
		if (file.exists()) {
			Codebook codebook = ClassifierFactory.deserializeCodebook();
			return codebook.getMostInformativeSubset();
		}//TODO remove after testing
		
		// Fast code book learning.
		// TODO: choose values, or make value picker.
		String partitionStyle = "partitionSize";
		int partitionOption = 50;
		double convergenceThreshold = 0.1;
		double alpha = 0.9;
		int basisSize = 256;
		
		/**
		 * TAKE SUBSET FOR TESTING
		 */
		int subsetSize = 500;
		basisSize = 128;
		ArrayList<ArrayRealVector> subset = new ArrayList<ArrayRealVector>(subsetSize);
		for(int i = 0; i < subsetSize; i++){
			subset.add(unlabeled.getFrame(i));
		}
		unlabeled = new FrameSet(subset);
		/**
		 * DELETE AFTER TESTING!!
		 */
		
		Codebook codebook = CodebookFactory.newCodebook(unlabeled, partitionStyle, partitionOption, basisSize, convergenceThreshold, alpha);
		
		codebook = codebook.getMostInformativeSubset();
		
		ClassifierFactory.serializeCodebook(codebook);//TODO remove after testing
		return codebook;
	}
	
	/**
	 * Turns activations into instances without labels.
	 * This is used during the classification stage.
	 * @param activations
	 * @param className
	 * @param classValues
	 * @return
	 */
	public static Instances activationsToInstances(FrameSet activations,
			String className, FastVector classValues) {
		int numOfFrames = activations.size();
		int numOfBasicVectors = activations.dimension();
		int numOfAttributes = numOfBasicVectors+1;
		FastVector attributes = new FastVector(numOfAttributes);
		
		//Create attributes for all basic vectors
		for (int i = 0; i < numOfBasicVectors; i++) {
			attributes.addElement(new Attribute(""+i));
		}
		attributes.addElement(new Attribute(className,classValues));
		
		//Create training set
		Instances instances = new Instances("Rel",attributes,numOfFrames);
		instances.setClassIndex(numOfAttributes-1);
		for (int i = 0; i < numOfFrames; i++) {
			Instance instance = new Instance(numOfAttributes);
			instance.setDataset(instances);
			double[] attValues = activations.getFrame(i).toArray();
			for (int j = 0; j < numOfBasicVectors; j++) {
				instance.setValue(j, attValues[j]);
			}
			instances.add(instance);
		}
		
		return instances;
	}
	
	/**
	 * Turns activations into instances with labels.
	 * This is used during the learning stage.
	 * @param activations
	 * @param className
	 * @param classValues
	 * @return
	 */
	public static Instances activationsToInstances(LabeledFrameSet activations,
			String className, FastVector classValues) {
		int numOfFrames = activations.size();
		int numOfBasicVectors = activations.dimension();
		int numOfAttributes = numOfBasicVectors+1;
		FastVector attributes = new FastVector(numOfAttributes);
		
		//Create attributes for all basic vectors
		for (int i = 0; i < numOfBasicVectors; i++) {
			attributes.addElement(new Attribute(""+i));
		}
		attributes.addElement(new Attribute(className,classValues));
		
		//Create training set
		Instances instances = new Instances("Rel",attributes,numOfFrames);
		instances.setClassIndex(numOfAttributes-1);
		for (int i = 0; i < numOfFrames; i++) {
			Instance instance = new Instance(numOfAttributes);
			instance.setDataset(instances);
			double[] attValues = activations.getFrame(i).toArray();
			for (int j = 0; j < numOfBasicVectors; j++) {
				instance.setValue(j, attValues[j]);
			}
			instance.setClassValue(activations.getLabel(i));
			instances.add(instance);
		}
		
		return instances;
	}
	
	private static void serializeCodebook(Codebook codebook) throws IOException {
		OutputStream file = new FileOutputStream("codebook.ser");
		OutputStream buffer = new BufferedOutputStream(file);
		ObjectOutput output = new ObjectOutputStream(buffer);
		try {
			output.writeObject(codebook);
		} finally {
			output.close();
		}
	}
	
	private static Codebook deserializeCodebook() 
			throws IOException, ClassNotFoundException {
		InputStream file = new FileInputStream("codebook.ser");
		InputStream buffer = new BufferedInputStream(file);
		ObjectInput input = new ObjectInputStream (buffer);
		try {
			return (Codebook) input.readObject();
		} finally {
			input.close();
		}
	}
}

package classifier;

import java.util.ArrayList;
import java.util.List;

import weka.classifiers.Classifier;
import weka.classifiers.meta.Vote;
import weka.classifiers.mi.MISVM;
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
	
	public static Classifier createClassifier(LabeledFrameSet labeled, FrameSet unlabeled) throws Exception{
		
		// Fast code book learning.
		// TODO: choose values, or make value picker.
		String partitionStyle = "";
		int partitionOption = 0;
		double convergenceThreshold = 0;
		double alpha = 0;
		Codebook codebook = CodebookFactory.newCodebook(unlabeled, partitionStyle, partitionOption, convergenceThreshold, alpha);
		
		// Initialize optimal code book.
		//Codebook codebook = initCodebook.getMostInformative();
		
		// Initialize feature set.
		// for each vector in labeled dataset
		// minimize activation vectors
		// add to classifier with labeled vector
		
		FrameSet activations = codebook.activate(labeled);
		LabeledFrameSet labeledActivations = activations.labelFrameSet(labeled.getLabelList());
		
		// C = train classifier
		Instances trainSet = ClassifierFactory.activationsToInstances(labeledActivations, "walking");
		
		
		//Create array of classifiers
		List<Classifier> classifiers = new ArrayList<Classifier>();
		//TODO use options for classifiers and add classifiers
		Classifier svm = new MISVM();
		Classifier j48 = new J48();
		
		classifiers.add(svm);
		classifiers.add(j48);
		
		//TODO use correct options for (meta)classifiers
		Vote vote = new Vote();
		vote.setClassifiers((Classifier[])classifiers.toArray());
		
		vote.buildClassifier(trainSet);
		return vote;
	}
	
	public static void testClassifier(Classifier classifier, FrameSet testSet) {
		//Transform FrameSet to Instances
		//TODO get statistics, confusion matrix etc.
	}
	
	public static Instances activationsToInstances(LabeledFrameSet activations,String className) {
		int numOfFrames = activations.size();
		int numOfBasicVectors = activations.dimension();
		FastVector attributes = new FastVector(numOfBasicVectors);
		
		//Create attributes for all basic vectors
		for (int i = 0; i < numOfBasicVectors; i++) {
			attributes.addElement(new Attribute(""+i));
		}
		attributes.addElement(className);
		
		//Create training set
		Instances instances = new Instances("Rel",attributes,numOfFrames);
		instances.setClassIndex(numOfBasicVectors);
		for (int i = 0; i < numOfFrames; i++) {
			double[] attValues = activations.getFrame(i).toArray();
			Instance instance = new Instance(1, attValues);
			instances.add(instance);
		}
		
		return instances;
	}
}

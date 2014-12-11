package classifier;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;

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
		String partitionStyle = "partitionSize";
		int partitionOption = 50;
		double convergenceThreshold = 0.5;
		double alpha = 0.5;
		int basisSize = 256;
		
		/**
		 * TAKE SUBSET FOR TESTING
		 */
		int subsetSize = 500;
		basisSize = 64;
		ArrayList<ArrayRealVector> subset = new ArrayList<ArrayRealVector>(subsetSize);
		for(int i = 0; i < subsetSize; i++){
			subset.add(unlabeled.getFrame(i));
		}
		unlabeled = new FrameSet(subset);
		/**
		 * DELETE AFTER TESTING!!
		 */
		
		Codebook codebook = CodebookFactory.newCodebook(unlabeled, partitionStyle, partitionOption, basisSize, convergenceThreshold, alpha);
		
		// Initialize optimal code book.
		//Codebook codebook = initCodebook.getMostInformative();
		
		// Initialize feature set.
		// for each vector in labeled dataset
		// minimize activation vectors
		// add to classifier with labeled vector
		
		FrameSet activations = codebook.activate(labeled);
		LabeledFrameSet labeledActivations = activations.labelFrameSet(labeled.getLabelList());
		
		//TODO change back to labeledactivations
		FastVector walkingValues = new FastVector(2);
		walkingValues.addElement("Yes");
		walkingValues.addElement("No");
		Instances trainSet = ClassifierFactory.activationsToInstances(labeled, "walking", walkingValues);
		
		
		//Create array of classifiers
		//TODO use options for classifiers and add classifiers
		Classifier[] classifiers = new Classifier[2];
		Classifier svm = new MISVM();
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
		svm.buildClassifier(trainSet);
		return svm;
	}
	
	public static void testClassifier(Classifier classifier, FrameSet testSet) {
		//Transform FrameSet to Instances
		//TODO get statistics, confusion matrix etc.
	}
	
	public static Instances activationsToInstances(LabeledFrameSet activations,String className, FastVector classValues) {
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
}

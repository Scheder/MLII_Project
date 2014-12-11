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

public class ClassifierFactory {
	
	//TODO handle exception
	public static void createClassifier(FrameSet labeled, FrameSet unlabeled) throws Exception{
		
		// Fast code book learning.
		// TODO: choose values, or make value picker.
		String partitionStyle = "";
		int partitionOption = 0;
		double convergenceThreshold = 0;
		double alpha = 0;
		Codebook initCodebook = CodebookFactory.newCodebook(unlabeled, partitionStyle, partitionOption, convergenceThreshold, alpha);
		
		// Initialize optimal code book.
		//Codebook codebook = initCodebook.getMostInformative();
		
		// Initialize feature set.
		// for each vector in labeled dataset
		// minimize activation vectors
		// add to classifier with labeled vector
		
		// C = train classifier
		int numOfFrames = labeled.size();
		int numOfBasicVectors = labeled.dimension();
		FastVector attributes = new FastVector(numOfBasicVectors);
		//Create attributes for all basic vectors
		for (int i = 0; i < numOfBasicVectors; i++) {
			attributes.addElement(new Attribute(""+i));
		}
		//TODO make createClassifier more generic, so it can be reused for gait recognition
		attributes.addElement("walking");
		
		//Create training set
		Instances trainingSet = new Instances("Rel",attributes,numOfFrames);
		trainingSet.setClassIndex(numOfBasicVectors);
		for (int i = 0; i < numOfFrames; i++) {
			//TODO get label.
			double[] attValues = labeled.getFrame(i).toArray();
			Instance instance = new Instance(1, attValues);
			trainingSet.add(instance);
		}
		
		
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
		
		vote.buildClassifier(trainingSet);
		// return return vote.
	}
	
	public static void testClassifier(Classifier classifier, FrameSet testSet) {
		//Transform FrameSet to Instances
		//TODO get statistics, confusion matrix etc.
	}
}

package classifier;

import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import codebook.Codebook;
import data.FrameSet;
import data.LabeledFrameSet;

/**
 * Class used to train classifiers.
 *
 */
public class ClassifierFactory {
	
	/**
	 * Given a codebook and a labeled frame set, this method trains a
	 * classifier to distinguish between walking and other activities.
	 * 
	 * @param codebook	A codebook.
	 * @param labeled	Labeled frames to activate the codebook.
	 * @return CodebookClassifier object.
	 * @throws Exception
	 */
	public static CodebookClassifier createWalkClassifier(
			Codebook codebook, LabeledFrameSet labeled) throws Exception {
		
		// Get activations for the labeled frames.
		FrameSet activations = codebook.activate(labeled);
		// Label the activations.
		LabeledFrameSet labeledActivations = activations.labelFrameSet(labeled.getLabelList());
		
		// Attribute vector.
		FastVector walkingValues = new FastVector(2);
		walkingValues.addElement("Yes");
		walkingValues.addElement("No");
		Instances trainSet = ClassifierFactory.activationsToInstances(
				labeledActivations, "walking", walkingValues);
		
		
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
		
		// Use the training set composed of labeled activations to build
		// the classifier.
		classifier.buildClassifier(trainSet);
		return new CodebookClassifier(codebook, classifier);
	}
	
	/**
	 * Given a codebook and a labeled frame set, this method trains a
	 * classifier to distinguish between the gait of different subjects.
	 * 
	 * @param codebook	A codebook.
	 * @param labeled	Labeled frames to activate the codebook.
	 * @return CodebookClassifier object.
	 * @throws Exception
	 */
	public static CodebookClassifier createPersonClassifier(
			Codebook codebook, LabeledFrameSet labeled) throws Exception {
		
		// Get activations for the labeled frames.
		FrameSet activations = codebook.activate(labeled);
		// Label the activations.
		LabeledFrameSet labeledActivations = 
				activations.labelFrameSet(labeled.getLabelList());
		
		// Attribute vector.
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
		
		// Use the training set composed of labeled activations to build
		// the classifier.
		classifier.buildClassifier(trainSet);
		return new CodebookClassifier(codebook, classifier);
	}
	
	/**
	 * Turns activations into instances without labels.
	 * This is used during the classification stage.
	 * @param activations
	 * @param className
	 * @param classValues
	 * @return
	 */
	public static Instances activationsToInstances(FrameSet activations,String className, FastVector classValues) {
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

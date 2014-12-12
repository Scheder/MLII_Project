package classifier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import codebook.Codebook;
import data.Data;
import data.FrameSet;
import data.LabeledFrameSet;

/**
 * Classifier class incorporating a codebook for feature extraction.
 *
 */
public class CodebookClassifier {
	
	private Codebook codebook;
	private Classifier classifier;
	
	/**
	 * Constructor takes a codebook and a classifier and returns an instance of
	 * the wrapper class CodebookClassifier.
	 * @param codebook
	 * @param classifier
	 */
	public CodebookClassifier(
			final Codebook codebook, final Classifier classifier) {
		this.codebook = codebook;
		this.classifier = classifier;
	}
	
	/**
	 * Returns a list of classification labels for a batch of data.
	 * 
	 * @param d	Batch of data.
	 * @return	List of labels.
	 * @throws Exception
	 */
	public List<String> getLabels(final Data d) throws Exception {
		//Get activations for all frames
		FrameSet activations = this.codebook.activate(
				new FrameSet(d.toArrayRealVector()));
		Instances instances = ClassifierFactory.activationsToInstances(
				activations,d.getClassName(),d.getClassValues());
		final List<String> labels = new ArrayList<String>(
				instances.numInstances());
		for (int i = 0; i < instances.numInstances(); i++) {
			Instance instance = instances.instance(i);
			int classId = (int)this.classifier.classifyInstance(instance);
			labels.add(instances.classAttribute().value(classId));
		}
		
		return labels;
	}
	
	/**
	 * Cross-validates the classifier for a given labeled set.
	 * 
	 * @param labeled
	 * @param className
	 * @param classValues
	 * @throws Exception
	 */
	public void evaluate(
			LabeledFrameSet labeled,String className,FastVector classValues)
					throws Exception {
		FrameSet activations = this.codebook.activate(labeled);
		LabeledFrameSet labeledActivations =
				activations.labelFrameSet(labeled.getLabelList());
		Instances instances = ClassifierFactory.activationsToInstances(
				labeledActivations, className, classValues);
		
		Data.toArff(new File("test.arff"), instances);
		
		Evaluation evaluation = new Evaluation(instances);
		evaluation.crossValidateModel(
				this.classifier, instances, 10, new Random(1));
		System.out.println(evaluation.toSummaryString());
		System.out.println(evaluation.toMatrixString());
	}
	
	
	
}

package classifier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import main.Main;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import codebook.Codebook;
import data.Data;
import data.FrameSet;
import data.LabeledFrameSet;
import data.PersonData;

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
		
		Evaluation evaluation = new Evaluation(instances);
		evaluation.crossValidateModel(
				this.classifier, instances, 10, new Random(1));
		System.out.println(evaluation.toSummaryString());
		System.out.println(evaluation.toMatrixString());
	}
	
	
	public List<String> classify(final String folderName) throws Exception {
		System.out.println("Classifying...");
		final File folder = new File(folderName);
		List<String> list = new ArrayList<String>();
		//Get all CSV files
		for (File file : folder.listFiles(new Main.CSVFilter())) {
			try {
				list.add(this.classifyFile(file));
			} catch (IOException e) {
				// If something goes wrong, continue to next file
			} catch (RuntimeException e) {
			//Files with only a header are ignored
			}
		}
		return list;
	}
	
	public String classifyFile(final File file) throws Exception {
		System.out.println("Classifying " + file.getName());
		PersonData data = new PersonData();
		data.readCSV(file);
		//Turn instances into frameset
		FrameSet frames = new FrameSet(data.toArrayRealVector());
		//Activate frameset
		FrameSet activations = this.codebook.activate(frames);
		//Turn activations back to instances
		FastVector classValues = data.getClassValues();
		Instances instances = ClassifierFactory.activationsToInstances(
				activations, data.getClassName(), classValues);
		//Prepare voting map
		Map<String, Integer> votingMap = new HashMap<String, Integer>();
		List<String> labels = new ArrayList<String>(instances.numInstances());
		for (int i = 0; i < classValues.size(); i++) {
			votingMap.put((String)classValues.elementAt(i), 0);
		}
		//Classify instances
		for (int i = 0; i < instances.numInstances(); i++) {
			Instance instance = instances.instance(i);
			int index = (int)this.classifier.classifyInstance(instance);
			String label = (String)classValues.elementAt(index);

			int value = votingMap.get(label);
			votingMap.put(label, value+1);
		}
		
		//Check the majority vote
		String winnerLabel = "";
		int winnerValue = 0;
		for (String label : votingMap.keySet()) {
			int value = votingMap.get(label);
			if (value > winnerValue) {
				winnerValue = value;
				winnerLabel = label;
			}
		}
		System.out.println("Data in "+file.getName()+" is from "+winnerLabel);
		return winnerLabel;
	}
	
	
}

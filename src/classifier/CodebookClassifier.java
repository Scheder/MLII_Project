package classifier;

import java.util.ArrayList;
import java.util.List;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import codebook.Codebook;
import data.Data;
import data.FrameSet;

public class CodebookClassifier {

	
	private Codebook codebook;
	private Classifier classifier;
	
	public CodebookClassifier(final Codebook codebook, 
			final Classifier classifier) {
		this.codebook = codebook;
		this.classifier = classifier;
	}
	
	public List<String> getLabels(final Data d) throws Exception {
		//Get activations for all frames
		FrameSet activations = 
				this.codebook.activate(new FrameSet(d.toArrayRealVector()));
		
		Instances instances = 
				ClassifierFactory.activationsToInstances(activations,
						d.getClassName(),d.getClassValues());
		
		final List<String> labels = 
				new ArrayList<String>(instances.numInstances());
		
		for (int i = 0; i < instances.numInstances(); i++) {
			Instance instance = instances.instance(i);
			int classId = (int)this.classifier.classifyInstance(instance);
			labels.add(instances.classAttribute().value(classId));
		}
		
		return labels;
	}
	
	
	
}

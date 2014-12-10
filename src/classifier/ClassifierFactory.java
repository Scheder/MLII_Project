package classifier;

import codebook.Codebook;
import codebook.CodebookFactory;
import data.FrameSet;

public class ClassifierFactory {
	
	public static void createClassifier(FrameSet labeled, FrameSet unlabeled){
		
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
		// return C.
	}
}

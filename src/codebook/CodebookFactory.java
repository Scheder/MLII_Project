package codebook;

import data.FrameSet;

public class CodebookFactory {

	public static Codebook newCodebook(FrameSet unlabeledData, 
			String partitionStyle, int partitionOption, int basisSize, 
			double convergenceThreshold, double alpha){
		
		Codebook codebook = new Codebook(unlabeledData.dimension(), basisSize);
		codebook.learnUnlabeledData(unlabeledData, partitionStyle, 
				partitionOption, convergenceThreshold, alpha);
		
		return codebook;
		
	}
	
}

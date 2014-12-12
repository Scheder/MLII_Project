package codebook;

import data.FrameSet;

public class CodebookFactory {

	public static Codebook newCodebook(FrameSet unlabeledData, String partitionStyle, int partitionOption, int basisSize, double convergenceThreshold, double alpha){
		
		Codebook codebook = new Codebook(unlabeledData.dimension(), basisSize);
		codebook.learnUnlabeledData(unlabeledData, partitionStyle, partitionOption, convergenceThreshold, alpha);
		return codebook;
		
	}
	
	public static Codebook newCodebook(FrameSet unlabeledData, String partitionStyle, int partitionOption, double convergenceThreshold, double alpha){
		
		// TODO Find a codebook with a good size of basis.
		// use greedy
		// Q: before or after selection through clustering?
		// A?: I'd say including clustering.
		
		// In that case, code would look like:
		// gready binary search for size:
		//		construct codebook with size
		//		getSelection
		//		checkReconstructionError
		return null;
	}
}

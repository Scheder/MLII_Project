package codebook;

public class CodebookFactory {

	public static Codebook newCodebook(Dataset unlabeledData, String partitionStyle, int partitionOption, int basisSize, double convergenceThreshold, double alpha){
		
		Codebook codebook = new Codebook(unlabeledData.getVector(0).getDimension(), basisSize);
		codebook.learnUnlabeledData(unlabeledData, partitionStyle, partitionOption, convergenceThreshold, alpha);
		return codebook;
		
	}
	
	public static Codebook newCodebook(Dataset unlabeledData, String partitionStyle, int partitionOption, double convergenceThreshold, double alpha){
		
		// Find a codebook with a good size of basis.
		return null;
	}
}

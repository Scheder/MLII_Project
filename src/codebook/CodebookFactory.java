package codebook;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.commons.math3.linear.ArrayRealVector;
import data.FrameSet;

public class CodebookFactory {

	public static Codebook newCodebook(FrameSet unlabeledData, String partitionStyle, int partitionOption, int basisSize, double convergenceThreshold, double alpha){
		
		Codebook codebook = new Codebook(unlabeledData.dimension(), basisSize);
		codebook.learnUnlabeledData(unlabeledData, partitionStyle, partitionOption, convergenceThreshold, alpha);
		return codebook;
		
	}
	
	public static Codebook getWalkCodebook() throws Exception {
		//TODO put in other location.
		//TODO if file not exists, error should be shown
		Codebook codebook = deserializeCodebook("codebook.ser");
		return codebook.getMostInformativeSubset();
	}
	
	public static Codebook getPersonCodebook() throws Exception {
		//TODO put in other location.
		//TODO if file not exists, error should be shown
		Codebook codebook = deserializeCodebook("codebook.ser");
		return codebook.getMostInformativeSubset();
	}
	
	
	public static Codebook getCodebook(FrameSet unlabeled) 
			throws Exception {
		String fileName = "codebook.ser";
		File file = new File(fileName);
		if (file.exists()) {
			Codebook codebook = deserializeCodebook(fileName);
			return codebook.getMostInformativeSubset();
		}//TODO remove after testing
		
		// Fast code book learning.
		// TODO: choose values, or make value picker.
		String partitionStyle = "partitionSize";
		int partitionOption = 50;
		double convergenceThreshold = 0.1;
		double alpha = 0.9;
		int basisSize = 256;
		
		/**
		 * TAKE SUBSET FOR TESTING
		 */
		int subsetSize = 500;
		basisSize = 128;
		ArrayList<ArrayRealVector> subset = 
				new ArrayList<ArrayRealVector>(subsetSize);
		for(int i = 0; i < subsetSize; i++){
			subset.add(unlabeled.getFrame(i));
		}
		unlabeled = new FrameSet(subset);
		/**
		 * DELETE AFTER TESTING!!
		 */
		
		Codebook codebook = CodebookFactory.newCodebook(
				unlabeled, partitionStyle, partitionOption, basisSize, 
				convergenceThreshold, alpha);
		
		codebook = codebook.getMostInformativeSubset();
		
		serializeCodebook(codebook,fileName);//TODO remove after testing
		return codebook;
	}
	
	
	private static void serializeCodebook(Codebook codebook,String fileName)
			throws IOException {
		OutputStream file = new FileOutputStream(fileName);
		OutputStream buffer = new BufferedOutputStream(file);
		ObjectOutput output = new ObjectOutputStream(buffer);
		try {
			output.writeObject(codebook);
		} finally {
			output.close();
		}
	}
	
	private static Codebook deserializeCodebook(String fileName)
			throws IOException, ClassNotFoundException {
		InputStream file = new FileInputStream(fileName);
		InputStream buffer = new BufferedInputStream(file);
		ObjectInput input = new ObjectInputStream (buffer);
		try {
			return (Codebook) input.readObject();
		} finally {
			input.close();
		}
	}
}

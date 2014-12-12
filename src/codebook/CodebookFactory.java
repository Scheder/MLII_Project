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

/**
 * Class responsible for creating new Codebook instances.
 *
 */
public class CodebookFactory {

	/**
	 * Create and learn a new codebook with the giving data.
	 * 
	 * @param unlabeledData		Unlabeled data to learn the codebook.
	 * @param partitionStyle	Partition style to be used while learning the
	 * 							codebook. Two possibilities:
	 * 							"numberPartitions": Specify number partitions.
	 * 							"partitionSize": Specify size of partitions.
	 * 
	 * @param partitionOption	Partitioning option. Interpretation depends on
	 * 							value of partitionStyle.
	 * 
	 * @param basisSize			
	 * 
	 * @param convergenceThreshold	The drop in the regularized reconstruction
	 * 								error between two consecutive refinements
	 * 								of the codebook needs to be smaller than
	 * 								this threshold to imply convergence and
	 * 								return the codebook.
	 * 
	 * @param alpha	The regularization parameter alpha controls the trade-off
	 * 				between reconstruction quality and sparseness of the basis
	 * 				vectors. Smaller values (close to zero) of alpha encourage
	 * 				accuracy of basis vectors. Large values (close to one) of
	 * 				alpha encourage sparse solutions, where the activations
	 * 				have a smalll L1-norm.
	 * 
	 * @return A new codebook.
	 */
	public static Codebook newCodebook(FrameSet unlabeledData,
			String partitionStyle, int partitionOption, int basisSize,
			double convergenceThreshold, double alpha) {

		Codebook codebook = new Codebook(unlabeledData.dimension(), basisSize);
		codebook.learnUnlabeledData(unlabeledData, partitionStyle,
				partitionOption, convergenceThreshold, alpha);
		return codebook;

	}

	/**
	 * Reads a the codebook associated with walking data from the disk.
	 * 
	 * @return Codebook read from disk.
	 * @throws Exception	If the file does not exist.
	 */
	public static Codebook getWalkCodebook() throws Exception {
		// TODO if file not exists, error should be shown
		Codebook codebook = deserializeCodebook("codebook.ser");
		return codebook.getMostInformativeSubset();
	}

	/**
	 * Reads a the codebook associated with gait data from the disk.
	 * 
	 * @return Codebook read from disk.
	 * @throws Exception	If the file does not exist.
	 */
	public static Codebook getPersonCodebook() throws Exception {
		// TODO if file not exists, error should be shown
		Codebook codebook = deserializeCodebook("codebook.ser");
		return codebook.getMostInformativeSubset();
	}
	
	/**
	 * Either reads a codebook from disk or, if the codebook doesn't already
	 * exist, learns a new codebook using the unlabeled and default parameters.
	 * 
	 * Parameters are chosen for a smaller, faster codebook.
	 * 
	 * @param unlabeled	Unlabeled data to learn a new codebook.
	 * @return A codebook
	 * @throws Exception
	 */
	public static Codebook getCodebook(FrameSet unlabeled){
		return getCodebook(unlabeled, true);
	}

	/**
	 * Either reads a codebook from disk or, if the codebook doesn't already
	 * exist, learns a new codebook using the unlabeled and default parameters.
	 * 
	 * The boolean parameter small lets you pick between a bigger and more
	 * accurate codebook, or a smaller codebook that's faster to generate and
	 * utilize.
	 * 
	 * @param unlabeled	Unlabeled data to learn a new codebook.
	 * @return A codebook
	 * @throws Exception
	 */
	public static Codebook getCodebook(FrameSet unlabeled, boolean small) {
		String fileName = "codebook.ser";
		File file = new File(fileName);
		if (file.exists()) {
			try {
				Codebook codebook = deserializeCodebook(fileName);
				return codebook.getMostInformativeSubset();
			} catch (Exception e) {
				System.out.println("File exists but might be corrupt.");
				e.printStackTrace();
				System.out.println("Learning a new codebook.");
			}
		}

		// Codebook learning parameters.
		String partitionStyle = "partitionSize";
		int partitionOption = 50;
		double convergenceThreshold = 0.1;
		double alpha = 0.9;
		int basisSize = 256;
		
		// If small, adjust paremeters.
		if(small){
			int subsetSize = 500;
			basisSize = 128;
			ArrayList<ArrayRealVector> subset = new ArrayList<ArrayRealVector>(
					subsetSize);
			for (int i = 0; i < subsetSize; i++) {
				subset.add(unlabeled.getFrame(i));
			}
			unlabeled = new FrameSet(subset);
		}

		Codebook codebook = newCodebook(unlabeled,
				partitionStyle, partitionOption, basisSize,
				convergenceThreshold, alpha);

		codebook = codebook.getMostInformativeSubset();

		// Try writing the generated codebook to disk.
		try {
			serializeCodebook(codebook, fileName);
		} catch (IOException e) {
			System.out.println("Failed to write generated codebook to disk. "
					+ "Continuing execution.");
			e.printStackTrace();
		}
		return codebook;
	}

	private static void serializeCodebook(Codebook codebook, String fileName)
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
		ObjectInput input = new ObjectInputStream(buffer);
		try {
			return (Codebook) input.readObject();
		} finally {
			input.close();
		}
	}
}

package codebook;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import smile.regression.LASSO;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import data.FrameSet;


public class Codebook {

	RealMatrix basisVectors;
	double alpha;
	
	/**
	 * Initializes a codebook with a certain dimension and size (number
	 * of basis vectors).
	 * 
	 * After creating the codebook, call the learnUnlabeledData method
	 * to start learning the basis vectors.
	 * 
	 * @param codebookDimension	The dimension of the basis vectors,
	 * 							this corresponds to the frame size
	 * 							of the data to be processed.
	 * 
	 * @param codebookSize		The cardinality of the basis.
	 */
	public Codebook(int codebookDimension, int codebookSize){
		
		basisVectors = new Array2DRowRealMatrix(codebookDimension, codebookSize);
		
		// Pseudo-random numbers generated by Random objects are uniformly distributed.
		Random randomGenerator = new Random();
		
		// Generate 'codebookSize' vectors.
		for(int vectorIndex = 0; vectorIndex < codebookSize; vectorIndex++){
			
			double accumulator = 0;
			
			// Generate an array of doubles ~U(0,1)
			double[] doubleVector = new double[codebookDimension];
			for(int elementIndex = 0; elementIndex < codebookDimension; elementIndex++){
				double nextElement = randomGenerator.nextDouble();
				accumulator += nextElement;
				doubleVector[elementIndex] = nextElement;
			}
			
			// Create vector object.
			ArrayRealVector vector = new ArrayRealVector(doubleVector);
			
			// Normalize the vector by mean.
			double vectorMean = accumulator / codebookDimension;
			vector.mapSubtractToSelf(vectorMean);
			
			// Make vector of unity norm.
			double norm = vector.getNorm();
			vector.mapDivideToSelf(norm);
			
			// Add vector to set of basis vectors.
			basisVectors.setColumnVector(vectorIndex, vector);
		}
	}
	
	/**
	 * Learns the codebook basis vectors using a frame set of unlabeled data.
	 * 
	 * @param unlabeledData		The unlabeled data to learn the codebook.
	 * 
	 * @param partitionStyle	Partition style to be used while learning the
	 * 							codebook. Two possibilities:
	 * 							"numberPartitions": Specify number of partitions.
	 * 							"partitionSize": Specify size of partitions.
	 * 
	 * @param partitionOption	Partitioning option. Interpretation depends on
	 * 							value of partitionStyle.
	 * 
	 * @param convergenceThreshold	The drop in the regularized reconstruction
	 * 								error between two consecutive refinements
	 * 								of the codebook needs to be smaller than
	 * 								this threshold to imply convergence and
	 * 								return the codebook.
	 * 
	 * @param alpha		The regularization parameter alpha controls the trade-off
	 * 					between reconstruction quality and sparseness of the basis
	 * 					vectors. Smaller values (close to zero) of alpha encourage
	 * 					accuracy of basis vectors. Large values (close to one) of
	 * 					alpha encourage sparse solutions, where the activations
	 * 					have a smalll L1-norm.
	 */
	public void learnUnlabeledData(FrameSet unlabeledData, String partitionStyle, int partitionOption, double convergenceThreshold, double alpha){
		double previousDistance = Double.MAX_VALUE;
		this.alpha = alpha;
		boolean converged = false;
		
		List<FrameSet> batches;
		
		while(!converged){
			batches = unlabeledData.partition(partitionStyle, partitionOption);
			/**
			 * Array2DRowRealMatrix activationVectors = new Array2DRowRealMatrix(basisVectors.getColumnDimension(), unlabeledData.size());
			 */
			//int i = 0;
			for(FrameSet batch : batches){
				System.out.println("try batch");
				//Array2DRowRealMatrix activationForBatch = featureSignSearch(batch, alpha);
				// Trying something different...
				Array2DRowRealMatrix activationForBatch = l1RegularizedLassoSolve(batch);
				improveWithLeastSquaresSolve(batch, activationForBatch);
				/**for(int j = 0; j < batches.size(); j++){
					activationVectors.setColumnVector(i, activationForBatch.getColumnVector(j));
					i++;
				}**/
			}
			
			FrameSet activations = activate(unlabeledData);
			double currentDistance = getRegularizedReconstructionError(unlabeledData, activations);
			
			System.out.println("Current distance = " + currentDistance);
			if(previousDistance - currentDistance < convergenceThreshold){
				converged = true;
			}else{
				previousDistance = currentDistance;
			}
			
		}
	}
	
	
	public Codebook getMostInformativeSubset(){
		// TODO implement
		
		//Create attributes for all basic vector components.
		FastVector attributes = new FastVector(basisVectors.getRowDimension());
		
		for(int i = 0; i < basisVectors.getRowDimension(); i++) {
			attributes.addElement(new Attribute(""+i));
		}
		
		// Instantiate instances object.
		Instances  instances = new Instances("BVect", attributes, basisVectors.getColumnDimension());
		
		// Populate with instances of basis vectors.
		for(int i = 0; i < basisVectors.getColumnDimension(); i++){
			double[] attValues = basisVectors.getColumn(i);
			Instance instance = new Instance(1, attValues);
			instances.add(instance);
		}
		
		// Make clusters.
		String[] options = new String[2];
		options[0] = "-N";   // number of clusters
		options[1] = "" + Math.ceil((double) basisVectors.getColumnDimension() / 10);
		
		// TODO finish
		
		
		
		
		return null;
	}
	
	/**
	 * Calculates the regularized reconstruction error of the frame set and the
	 * reconstruction by using the activation vectors as coefficient vectors for
	 * the basis vectors currently in the codebook.
	 * 
	 * @param data	Measurements.
	 * @param activationVectors	Coefficient vectors to reconstruct the measurements.
	 * @return	Regularized reconstruction error.
	 */
	private double getRegularizedReconstructionError(
			FrameSet data, FrameSet activationVectors) {
		
		double accumulator = 0;
		RealMatrix difference = data.toMatrix().subtract(basisVectors.multiply(activationVectors.toMatrix()));
		
		for(int columnIndex = 0; columnIndex < difference.getColumnDimension(); columnIndex++){
			accumulator += Math.pow(difference.getColumnVector(columnIndex).getNorm(),2);
			accumulator += alpha*activationVectors.getFrame(0).getL1Norm();
		}
		
		return accumulator;
	}
	
	/**
	 * Activates the frames of the supplied frame set with the current state of
	 * the codebook. Returns the activations as another frameset.
	 * 
	 * If the data frame set has dimensions n by m, and the codebook has
	 * dimensions n by s, the activation frame set will have dimensions 
	 * m by n.
	 * 
	 * @param labeled	The data to be activated.
	 * @return	The corresponding activation vectors.
	 */
	public FrameSet activate(FrameSet labeled) {
		return new FrameSet(l1RegularizedLassoSolve(labeled));
	}
	
	/**
	 * Solves the L1-Regularized least squares problem with coefficients weighted by
	 * the static term alpha (codebook-wide variable).
	 * 
	 * The method accepts a batch of solution vectors and solves the system independently
	 * for each. The current state of the codebook is used as the system to solve
	 * against.
	 * 
	 * @param batch	Batch of frames to be used as solution vectors.
	 * @return Matrix with the corresponding coefficient vectors.
	 */
	private Array2DRowRealMatrix l1RegularizedLassoSolve(FrameSet batch){
		
		PrintStream originalStream = System.out;

		// To suppress LASSO prints...
		PrintStream dummyStream    = new PrintStream(new OutputStream(){
		    public void write(int b) {
		        //NO-OP
		    }
		});
		
		
		Array2DRowRealMatrix activationMatrix = new Array2DRowRealMatrix(basisVectors.getColumnDimension(), batch.size());
		int bb = 0;
		int aa = 0;
		int cc = 0;
		// For each vector in batch.
		for(int i = 0; i < batch.size(); i++){
			double[] y = batch.getFrame(i).toArray();
			double[][] beta = basisVectors.getData();
			System.setOut(dummyStream);
			LASSO solver = new LASSO(beta, y, alpha);
			System.setOut(originalStream);
			double[] a = solver.coefficients();
			activationMatrix.setColumn(i, a);
			bb = a.length;
			aa = beta.length;
			cc = beta[0].length;
		}
		//System.out.println(aa);
		//System.out.println(cc);
		//System.out.println(bb);
		return activationMatrix;
		
	}
	
	/**
	 * This method takesa batch of unlabeled data vectors and associated activations.
	 * Keeping these activations static, it optimizes the codebook by solving the
	 * L2-constrained least squares problem $||X - BA||_2^2$ for B, with X the data
	 * matrix with column vectors for each frame, B the codebook with column vectors
	 * being the basis vectors and A the matrix of activation vectors for X.
	 * 
	 * This is equivalent to solving the least squares problem $||X^T - A^TB^T||_2^2$
	 * which is a format that can be used with most least squares solvers.
	 * 
	 * The codebook is updated with this least squares solution.
	 *  
	 * @param batch					The data vectors.
	 * @param activationForBatch	Activation vectors corresponding to the data vectors.
	 * @post  Codebook is updated with least squares solution.
	 */
	private void improveWithLeastSquaresSolve(FrameSet batch, Array2DRowRealMatrix activationForBatch){
		// We solve the least squares problem
		// transpose(batch) = transpose(activationForBatch) * transpose(codebook)
		// for codebook and update codebook.
		
		//DecompositionSolver solver = new QRDecomposition(activationForBatch.transpose()).getSolver();
		DecompositionSolver solver = new SingularValueDecomposition(activationForBatch.transpose()).getSolver();
		basisVectors = solver.solve(batch.toMatrixTranspose()).transpose();
	}
	
	/**
	 * This is the code that was previously used to solve the L1-regularized least squares problem,
	 * to optimize activation vectors for the unlabeled data while keeping the codebook static.
	 * 
	 * Alas, there are still a lot of errors in the code below and we've instead started using
	 * a LASSO implementation by the SMILE machine learning library.
	 * 
	private Array2DRowRealMatrix featureSignSearch(FrameSet batch, double alpha){
		// 2D0 check correctness
		Array2DRowRealMatrix activationMatrix = new Array2DRowRealMatrix(basisVectors.getColumnDimension(), batch.size());
		
		// For each vector in batch,
		for(int i = 0; i < batch.size(); i++){
			RealVector y = batch.getFrame(i);
			
			// Run a featureSignSearch...
			RealVector a = featureSignSearch(y, alpha);
			activationMatrix.setColumnVector(i, a);
		}
		
		return activationMatrix;
	}
	
	private RealVector featureSignSearch(RealVector y, double alpha){
		
		// initialize.
		ArrayRealVector x = new ArrayRealVector(basisVectors.getColumnDimension());
		ArrayRealVector t = new ArrayRealVector(basisVectors.getColumnDimension());
		TreeSet<Integer> activeSet = new TreeSet<Integer>();
		boolean optimal = false;
		boolean skipActivation = false;
		
		while(!optimal){
			ArrayRealVector diffie;
			if(!skipActivation){
				// From zero coefficients of x, select i = argmax_i(abs(differential_xi))
				// - generate differentials
				ArrayRealVector diffVector = getDifferentialFor(y, x);
				diffie = diffVector.copy();
				// - populate array with norms of values corresponding to nonzero xs.
				ArrayRealVector candidateVector = new ArrayRealVector(diffVector.getDimension());
				for(int i = 0; i < diffVector.getDimension(); i++){
					if(x.getEntry(i) == 0){
						candidateVector.setEntry(i, Math.abs(diffVector.getEntry(i)));
					}
				}
				
				// Add next best element to active set.
				boolean lookingForCandidate = true;
				while(lookingForCandidate){
					// Find index with max value in candidate vector.
					int candidateIndex = candidateVector.getMaxIndex();
					// Test if candidate locally improves the objective.
					double diffValue = diffVector.getEntry(candidateIndex);
					if(diffValue > alpha){
						t.setEntry(candidateIndex, -1);
						activeSet.add(candidateIndex);
						lookingForCandidate = false;
					}else if(diffValue < -alpha){
						t.setEntry(candidateIndex, 1);
						activeSet.add(candidateIndex);
						lookingForCandidate = false;
					}else if(diffValue == 0){
						// All elements are optimal.
						return x;
					}else{
						diffVector.setEntry(candidateIndex, 0);
					}
				}
			}
			
			// Feature-sign step.
			// Take the submatrix of basis vectors corresponding to the indices in the active set.
			// Same for vectors x and t.
			Array2DRowRealMatrix bSub = new Array2DRowRealMatrix(basisVectors.getRowDimension(), activeSet.size());
			ArrayRealVector xSub = new ArrayRealVector(activeSet.size());
			ArrayRealVector tSub = new ArrayRealVector(activeSet.size());
			int i = 0;
			for(int index : activeSet){
				bSub.setColumnVector(i, basisVectors.getColumnVector(index));
				xSub.setEntry(i, x.getEntry(index));
				tSub.setEntry(i, t.getEntry(index));
				i++;
			}
			// Compute the analytical solution to the resulting analytical QP:
			RealMatrix p1 = new LUDecomposition(bSub.transpose().multiply(bSub)).getSolver().getInverse();
			
			System.out.println(bSub.transpose().multiply(bSub));
			
			RealVector p2 = bSub.transpose().operate(y.subtract(t.mapMultiply(alpha/2))); // 2D0 check formula. PDF is ambiguous.
			RealVector xSol = p1.operate(p2);
			
			// Perform a discrete line search on the closed line segment from xSub to xSol
			//    Check the value in xSol and all the values between xSub and xSol where
			//    any coefficient changes sign.
			double minVal = calculateFeatureSignObjective(y, bSub, xSol, tSub, alpha);
			RealVector xRes = xSol.copy();
			
			// Find coefficients that change sign and the point where it does.
			//   for each coefficient of the sub-vector x.
			for(int index = 0; index < xSub.getDimension(); index++){
				//   if the coefficient sign changed between xSub and xSol...
				if(Math.signum(xSub.getEntry(index)*xSol.getEntry(index)) <= 0){
					//   find the point where it changed...
					double lamda = xSol.getEntry(index)/(xSol.getEntry(index) - xSub.getEntry(index));
					RealVector temp = xSub.mapMultiply(lamda).add(xSol.mapMultiply(1-lamda));
					//   calculate the objective in that point...
					double tempMin = calculateFeatureSignObjective(y, bSub, temp, tSub, alpha);
					//   check against current minimum.
					if(tempMin < minVal){
						minVal = tempMin;
						xRes = temp;
					}
				}
			}
			
			System.out.println(xRes);
			
			// Update corresponding values of x.
			i = 0;
			for(int index : activeSet){
				x.setEntry(index, xRes.getEntry(i));
				i++;
			}
			
			// Remove zero coefficients of new x from active set.
			LinkedList<Integer> removeList = new LinkedList<Integer>();
			for(int index : activeSet){
				if(x.getEntry(index) == 0){
					removeList.add(index);
				}
			}
			activeSet.removeAll(removeList);
			
			// Update t
			for(int ind = 0; ind < t.getDimension(); ind++){
				t.setEntry(ind, Math.signum(x.getEntry(ind)));
			}
			
			// Check the optimality conditions
			optimal = true;
			ArrayRealVector diff = getDifferentialFor(y,x);
			RealVector diffAdd = t.copy().mapMultiply(alpha);
		out:for(int ind = 0; ind < x.getDimension(); ind++){
				if(x.getEntry(ind) !=0){
					if(diff.getEntry(ind) + diffAdd.getEntry(ind) != 0){
						optimal = false;
						skipActivation = true;
						break out;
					}
				}else{
					if(Math.abs(diff.getEntry(ind)) > alpha){
						optimal = false;
					}
				}
			}
		}
		
		return x;
		
	}
	
	private double calculateFeatureSignObjective(RealVector y, RealMatrix a, RealVector x, RealVector t, double alpha){
		
		return Math.pow(y.subtract(a.operate(x)).getNorm(), 2) + alpha*t.dotProduct(x);
		
	}
	
	//todo check correctness
	private ArrayRealVector getDifferentialFor(RealVector solVector, RealVector variableVector){
		RealVector temp = basisVectors.operate(variableVector);
		ArrayRealVector lsDifferential = new ArrayRealVector(variableVector.getDimension());
		for(int i = 0; i < variableVector.getDimension(); i++){
			double diff = 0;
			for(int j = 0; j < variableVector.getDimension(); j++){
				diff += 2*(solVector.getEntry(j) - temp.getEntry(j))*(-basisVectors.getEntry(j, i));
			}
			lsDifferential.setEntry(i, diff);
		}
		return lsDifferential;
		
	}
		**/
}

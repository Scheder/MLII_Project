package codebook;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import smile.clustering.HierarchicalClustering;
import smile.clustering.linkage.CompleteLinkage;
import smile.clustering.linkage.Linkage;
import smile.math.Math;
import smile.regression.LASSO;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.HierarchicalClusterer;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import data.FrameSet;


public class Codebook implements Serializable {

	private static final long serialVersionUID = 1L;
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
	
	private Codebook(Collection<RealVector> basisVectors, double alpha){
		this.alpha = alpha;
		int i = 0;
		
		for(RealVector vect : basisVectors){
			if(i == 0){
				this.basisVectors = new Array2DRowRealMatrix(vect.getDimension(), basisVectors.size());
			}
			this.basisVectors.setColumnVector(i, vect);
			i++;
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
			System.out.println("Refining codebook...");
			
			batches = unlabeledData.partition(partitionStyle, partitionOption);
			/**
			 * Array2DRowRealMatrix activationVectors = new Array2DRowRealMatrix(basisVectors.getColumnDimension(), unlabeledData.size());
			 */
			// To show progress...
			int nbBatches = batches.size();
			double lastPercentage = -1;
			int batchesDone = 0;
			long startTime = System.nanoTime();
			for(FrameSet batch : batches){
				double percentage = Math.floor((double) batchesDone/nbBatches*100);
				if(percentage != lastPercentage){
					System.out.println("Progress: " + percentage + "%");
					lastPercentage = percentage;
					if(percentage == 1.0){
						long endTime = System.nanoTime();
						System.out.println("Expected time to finish: " + ((startTime- endTime)/10000000) + "seconds");
					}
				}
				
				//Array2DRowRealMatrix activationForBatch = featureSignSearch(batch, alpha);
				// Trying something different...
				//System.out.println("a");
				Array2DRowRealMatrix activationForBatch = l1RegularizedLassoSolve(batch);
				//System.out.println("b");
				improveWithLeastSquaresSolve(batch, activationForBatch);
				/**for(int j = 0; j < batches.size(); j++){
					activationVectors.setColumnVector(i, activationForBatch.getColumnVector(j));
					i++;
				}**/
				batchesDone++;
			}
			
			System.out.println("Activating unlabeled data...");
			FrameSet activations = activate(unlabeledData);
			System.out.println("Calculating reconstruction error...");
			double currentDistance = getAverageRegularizedReconstructionError(unlabeledData, activations);
			
			System.out.println("Current error = " + currentDistance);
			if(currentDistance > previousDistance){
				System.out.println("DISTANCE INCREASED! Running again.");
				previousDistance = currentDistance;
			}else if(previousDistance - currentDistance < convergenceThreshold){
				converged = true;
			/** TODO PART OF DEBUGGING CODE
			 * }else if(previousDistance - currentDistance < 0.5){
				try {
					getMostInformativeSubset();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}**/
			}else{
				System.out.println("We made a step of " + (previousDistance - currentDistance) 
						+ ", which does not meet convergence criteria. Running again.");
				previousDistance = currentDistance;
			}
			
		}
	}
	
	
	public Codebook getMostInformativeSubset() throws Exception{
		// TODO implement
		
		// Get distance matrix for all basis vectors.
		int numVects = basisVectors.getColumnDimension();
		double[][] proximity = new double[numVects][];
		for(int i = 0; i < numVects; i++){
			proximity[i] = new double[i+1];
			for(int j = 0; j < i; j++){
				proximity[i][j] = MaximalCrossCorrelation.distance(
						basisVectors.getColumnVector(i), basisVectors.getColumnVector(j));
			}
		}
		
		// Generate complete linkage cluster.
		HierarchicalClustering hc = new HierarchicalClustering(new CompleteLinkage(proximity));
		
		// Cutoff at (ceil) number of vectors / 10.
        int[] label = hc.partition((int) Math.ceil((double) numVects/10));
        
        // Separate clusters
        
        // Allocate.
        ArrayList<ArrayList<RealVector>> res = new ArrayList<ArrayList<RealVector>>((int) Math.ceil((double) numVects/10));
        for(int i = 0; i < Math.ceil((double) numVects/10); i++){
        	res.add(new ArrayList<RealVector>());
        }
        
        // Populate.
        for(int i = 0; i < label.length; i++){
        	res.get(label[i]).add(basisVectors.getColumnVector(i));
        }
        
        // Extract most relevant basis vectors.
        LinkedList<RealVector> newBasisVectors = new LinkedList<RealVector>();
        for(int i = 0; i < Math.ceil((double) numVects/10); i++){
        	
        	ArrayList<RealVector> currList = res.get(i);
        	System.out.println();
        	// Sort.
        	Collections.sort(currList, new java.util.Comparator<RealVector>() {
        	    public int compare(RealVector a, RealVector b) {
        	        double shanA = empiricalEntropy(a, 10);
        	        double shanB = empiricalEntropy(b, 10);
        	        return Double.compare(shanB, shanA);
        	    }
        	});
        	
        	// Select most relevant 90%.
        	int itemsToSelect = (int) Math.min(Math.ceil((double) currList.size()*0.9), currList.size());
        	for(int j = 0; j < itemsToSelect; j++){
        		newBasisVectors.add(currList.get(j));
        		//System.out.println(empiricalEntropy(currList.get(j), 10));
        	}
        	
        }
        
        System.out.println(newBasisVectors.size());
        
        // Return better codebook.
        return new Codebook(newBasisVectors, this.alpha);
	}
	
	public double empiricalEntropy(RealVector v, int numBuckets){
		
		double max = v.getMaxValue();
		//System.out.println(max);
		double min = v.getMinValue();
		RealVector normalizedV = v.mapSubtract(min).mapDivide(max-min);
		
		double[] buckets = new double[numBuckets];
		for(double el : normalizedV.toArray()){
			if(el == 1){
				buckets[9] ++;
			}else{
				int index = new Double(Math.floor(el*10)).intValue();
				buckets[index] ++;
			}
		}
		
		double entropy = 0;
		double count = v.getDimension();
		
		for(double el : buckets){
			if(el > 0.0){
				entropy -= (el/count)*Math.log2(el/count);
			}
		}
		
		return entropy;
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
	private double getAverageRegularizedReconstructionError(
			FrameSet data, FrameSet activationVectors) {
		
		double accumulator = 0;
		RealMatrix difference = data.toMatrix().subtract(basisVectors.multiply(activationVectors.toMatrix()));
		
		for(int columnIndex = 0; columnIndex < difference.getColumnDimension(); columnIndex++){
			accumulator += Math.pow(difference.getColumnVector(columnIndex).getNorm(),2);
			accumulator += alpha*activationVectors.getFrame(0).getL1Norm();
		}
		
		return accumulator/data.size();
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

		// For each vector in batch.
		for(int i = 0; i < batch.size(); i++){
			double[] y = batch.getFrame(i).toArray();
			double[][] beta = basisVectors.getData();
			System.setOut(dummyStream);
			LASSO solver = new LASSO(beta, y, alpha);
			System.setOut(originalStream);
			double[] a = solver.coefficients();
			activationMatrix.setColumn(i, a);

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

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
import data.FrameSet;


public class Codebook {

	RealMatrix basisVectors;
	
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
	
	public void learnUnlabeledData(FrameSet unlabeledData, String partitionStyle, int partitionOption, double convergenceThreshold, double alpha){
		double previousDistance = Double.MAX_VALUE;
		boolean converged = false;
		
		List<FrameSet> batches;
		
		while(!converged){
			batches = unlabeledData.partition(partitionStyle, partitionOption);
			Array2DRowRealMatrix activationVectors = new Array2DRowRealMatrix(basisVectors.getColumnDimension(), unlabeledData.size());
			int i = 0;
			for(FrameSet batch : batches){
				//Array2DRowRealMatrix activationForBatch = featureSignSearch(batch, alpha);
				// Trying something different...
				Array2DRowRealMatrix activationForBatch = l1ConstrainedLassoSolve(batch, alpha);
				improveWithLeastSquaresSolve(batch, activationForBatch);
				for(int j = 0; j < batches.size(); j++){
					activationVectors.setColumnVector(i, activationForBatch.getColumnVector(j));
					i++;
				}
			}
			double currentDistance = getLeastSquaresDistance(unlabeledData, activationVectors, alpha);
			if(previousDistance - currentDistance < convergenceThreshold){
				converged = true;
			}else{
				previousDistance = currentDistance;
			}
			
		}
	}
	
	public Codebook getMostInformativeSubset(){
		// TODO implement
		return null;
	}
	
	private double getLeastSquaresDistance(
			FrameSet unlabeledData, Array2DRowRealMatrix activationVectors, double alpha) {
		
		double accumulator = 0;
		RealMatrix difference = unlabeledData.toMatrix().subtract(basisVectors.multiply(activationVectors));
		
		for(int columnIndex = 0; columnIndex < difference.getColumnDimension(); columnIndex++){
			accumulator += Math.pow(difference.getColumnVector(columnIndex).getNorm(),2);
			accumulator += alpha*activationVectors.getColumnVector(0).getL1Norm();
		}
		
		return accumulator;
	}
	
	private Array2DRowRealMatrix l1ConstrainedLassoSolve(FrameSet batch, double alpha){
		
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
		System.out.println(aa);
		System.out.println(cc);
		System.out.println(bb);
		return activationMatrix;
		
	}

	/**
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
	
	private void improveWithLeastSquaresSolve(FrameSet batch, Array2DRowRealMatrix activationForBatch){
		// We solve the least squares problem
		// transpose(batch) = transpose(activationForBatch) * transpose(codebook)
		// for codebook and update codebook.
		
		//DecompositionSolver solver = new QRDecomposition(activationForBatch.transpose()).getSolver();
		DecompositionSolver solver = new SingularValueDecomposition(activationForBatch.transpose()).getSolver();
		basisVectors = solver.solve(batch.toMatrixTranspose()).transpose();
	}
}

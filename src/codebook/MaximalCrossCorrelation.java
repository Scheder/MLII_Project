package codebook;

import org.apache.commons.math3.linear.RealVector;

import smile.math.Math;

/**
 * Class implementing the maximal cross-correlation distance function.
 *
 */
public class MaximalCrossCorrelation {

	/**
	 * Returns the maximal cross-correlation distance for two vectors.
	 * 
	 * The maximal cross-correlation is defined as the maximal sum of
	 * v1(tau)*v2(n+tau-t), with tau ranging from the maximum of (0, t-n) 
	 * to the minimum of (t, n) and t variable.
	 * 
	 * In other words: $max sum_{tau = max(0,t-n)}^{min(t,n)} v_1(tau)*
	 * v2(n+tau-n)$, for t variable.
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static double distance(RealVector v1, RealVector v2) {
		
		int n = v1.getDimension();
		double maxCorrelation = 0;
		
		for(int t = 0; t < 2*n + 1; t++){
			double correlation = 0;
			//System.out.println("From " + Math.max(0, t - n) + " to " + Math.min(n, t));
			for(int tau = Math.max(0, t - n); tau < Math.min(n, t); tau++){
				correlation += v1.getEntry(tau)*v2.getEntry(n+tau-t);
				//stats.incrCoordCount();
				//stats.incrCoordCount();
			}
			if(correlation > maxCorrelation){
				maxCorrelation = correlation;
			}
		}
		
		return maxCorrelation;
	}
}

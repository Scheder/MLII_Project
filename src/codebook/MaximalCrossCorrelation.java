package codebook;

import org.apache.commons.math3.linear.RealVector;

import smile.math.Math;

public class MaximalCrossCorrelation {

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

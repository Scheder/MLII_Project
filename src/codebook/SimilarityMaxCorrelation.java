package codebook;

import smile.math.Math;

public class SimilarityMaxCorrelation {

	public static double similarity(double[] v1, double[] v2){
		
		int n = v1.length;
		double maxCorrelation = 0;
		
		for(int t = 0; t < 2*n + 1; t++){
			double correlation = 0;
			System.out.println("From " + Math.max(0, t - n) + " to " + Math.min(n, t));
			for(int tau = Math.max(0, t - n); tau < Math.min(n, t); tau++){
				correlation += v1[tau]*v2[n+tau-t];
			}
			maxCorrelation = Math.max(maxCorrelation, correlation);
		}
		
		return maxCorrelation;
	}
}

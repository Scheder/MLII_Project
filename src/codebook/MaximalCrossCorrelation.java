package codebook;

import smile.math.Math;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.neighboursearch.PerformanceStats;

public class MaximalCrossCorrelation extends EuclideanDistance {

	private static final long serialVersionUID = 3310418712212126482L;

	@Override
	public double distance(Instance first, Instance second, double cutOffValue,
			PerformanceStats stats) {
		
		int n = first.numValues();
		double maxCorrelation = 0;
		
		for(int t = 0; t < 2*n + 1; t++){
			double correlation = 0;
			//System.out.println("From " + Math.max(0, t - n) + " to " + Math.min(n, t));
			for(int tau = Math.max(0, t - n); tau < Math.min(n, t); tau++){
				correlation += first.value(tau)*second.value(n+tau-t);
				//stats.incrCoordCount();
				//stats.incrCoordCount();
			}
			if(correlation > cutOffValue){
				return correlation;
			}else if(correlation > maxCorrelation){
				maxCorrelation = correlation;
			}
		}
		
		return maxCorrelation;
	}
}

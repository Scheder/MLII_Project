package codebook;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;


public class Dataset {
	
	List<ArrayRealVector> dataVectors;
	
	public Dataset(List<ArrayRealVector> vectors){
		dataVectors = vectors;
	}
	
	public ArrayRealVector getVector(int index){
		return dataVectors.get(index);
	}
	
	public int size(){
		return dataVectors.size();
	}
	
	public Array2DRowRealMatrix toMatrixTranspose(){
		Array2DRowRealMatrix res = new Array2DRowRealMatrix(dataVectors.size(), getVector(0).getDimension());
		int i = 0;
		for(ArrayRealVector vector : dataVectors){
			res.setRowVector(i, vector);
			i++;
		}
		return res;
	}
	
	public List<Dataset> partition(String partitionType, int partitionOption){
		// Generate set number of partitions.
		
		int itemsPerPartition = 0;
		int numberOfPartitions = 0;
		
		if (partitionType.equalsIgnoreCase("numberPartitions")){
			
			itemsPerPartition = dataVectors.size() / partitionOption;
			if(itemsPerPartition == 0) {
				numberOfPartitions = dataVectors.size();
				itemsPerPartition = 1;
			}else{
				numberOfPartitions = partitionOption;
			}
			
		}else if(partitionType.equalsIgnoreCase("partitionSize")){
			
			if (partitionOption > dataVectors.size()){
				itemsPerPartition = dataVectors.size();
				numberOfPartitions = 1;
			}else{
				numberOfPartitions = (int) Math.ceil((double)5/4);
				itemsPerPartition = partitionOption;
			}
			
		}else{
			throw new IllegalArgumentException("partitionType can only equal 'numberPartitions' or 'partitionSize'.");
		}
			
		ArrayList<Dataset> partitions = new ArrayList<Dataset>(numberOfPartitions);
		
		for(int i = 0; i < numberOfPartitions; i++){
			ArrayList<ArrayRealVector> subset = new ArrayList<ArrayRealVector>();
			int addUntil = Math.min(i+itemsPerPartition, dataVectors.size());
			for(int j = i; j < addUntil; j++){
				subset.add(dataVectors.get(j));
			}
			partitions.add(new Dataset(subset));
		}
		
		return partitions;
	}

	public Array2DRowRealMatrix toMatrix() {
		
		Array2DRowRealMatrix res = new Array2DRowRealMatrix(getVector(0).getDimension(), dataVectors.size());
		int i = 0;
		for(ArrayRealVector vector : dataVectors){
			res.setColumnVector(i, vector);
			i++;
		}
		return res;
		
	}
	
	
}

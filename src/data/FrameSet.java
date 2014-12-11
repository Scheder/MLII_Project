package data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

public class FrameSet {
	
	private final List<ArrayRealVector> frameSet;
	private final int size;
	private final int dimension;
	
	public FrameSet(Collection<ArrayRealVector> frameSet){
		
		this.frameSet = new ArrayList<ArrayRealVector>(frameSet);
		this.size = this.frameSet.size();
		this.dimension = this.frameSet.get(0).getDimension();
		
		/**
		List<ArrayRealVector> newFrameSet = new ArrayList<ArrayRealVector>();
		
		for(RealVector frame : frameSet){
			
			// Ignore incomplete frames.
			if(frame.getDimension() == windowSize){
				newFrameSet.add(new ArrayRealVector(frame));
			}
		}
		
		this.frameSet = newFrameSet;
		this.size = this.frameSet.size();
		this.dimension = this.frameSet.get(0).getDimension();
		**/
	}
	
	public LabeledFrameSet labelFrameSet(Collection<String> labels){
		return new LabeledFrameSet(frameSet, labels);
	}
	
	public FrameSet(Array2DRowRealMatrix frameMatrix) {
		this.size = frameMatrix.getColumnDimension();
		this.dimension = frameMatrix.getRowDimension();
		
		this.frameSet = new ArrayList<ArrayRealVector>(this.size);
		
		for(int i = 0; i < this.size; i++){
			frameSet.add(new ArrayRealVector(frameMatrix.getColumnVector(i)));
		}
	}

	public int size(){
		return this.size;
	}
	
	public int dimension(){
		return this.dimension;
	}
	
	public ArrayRealVector getFrame(int i){
		return frameSet.get(i).copy();
	}
	
	public Array2DRowRealMatrix toMatrixTranspose(){
		Array2DRowRealMatrix res = new Array2DRowRealMatrix(this.size, this.dimension);
		int i = 0;
		for(ArrayRealVector vector : frameSet){
			res.setRowVector(i, vector);
			i++;
		}
		return res;
	}
	
	public Array2DRowRealMatrix toMatrix(){
		Array2DRowRealMatrix res = new Array2DRowRealMatrix(this.dimension, this.size);
		int i = 0;
		for(ArrayRealVector vector : frameSet){
			res.setColumnVector(i, vector);
			i++;
		}
		return res;
	}
	
	public ArrayList<FrameSet> partition(String partitionType, int partitionOption){
		//System.out.println("Partitioning.");
		
		// Generate set number of partitions.
		
		int itemsPerPartition = 0;
		int numberOfPartitions = 0;
		
		if (partitionType.equalsIgnoreCase("numberPartitions")){
			
			itemsPerPartition = this.size / partitionOption;
			if(itemsPerPartition == 0) {
				numberOfPartitions = this.size;
				itemsPerPartition = 1;
			}else{
				numberOfPartitions = partitionOption;
			}
			
		}else if(partitionType.equalsIgnoreCase("partitionSize")){
			
			if (partitionOption > this.size){
				itemsPerPartition = this.size;
				numberOfPartitions = 1;
			}else{
				numberOfPartitions = (int) Math.ceil((double) this.size / partitionOption);
				itemsPerPartition = partitionOption;
			}
			
		}else{
			throw new IllegalArgumentException("partitionType can only equal 'numberPartitions' or 'partitionSize'.");
		}
		//System.out.println(itemsPerPartition);
		//System.out.println(numberOfPartitions);
			
		ArrayList<FrameSet> partitions = new ArrayList<FrameSet>(numberOfPartitions);
		List<ArrayRealVector> source = permuteList(frameSet);
		
		for(int i = 0; i < numberOfPartitions; i++){
			ArrayList<ArrayRealVector> subset = new ArrayList<ArrayRealVector>();
			int addUntil = Math.min(i+itemsPerPartition, this.size);
			for(int j = i; j < addUntil; j++){
				subset.add(source.get(j));
			}
			partitions.add(new FrameSet(subset));
		}
		
		return partitions;
	}
	
	private List<ArrayRealVector> permuteList(List<ArrayRealVector> input){
		
		ArrayList<ArrayRealVector> res = new ArrayList<ArrayRealVector>();
		ArrayList<ArrayRealVector> start = new ArrayList<ArrayRealVector>(input);
		Random generator = new Random();
		
		while(start.size() > 0){
			int index = Math.abs(generator.nextInt()) % start.size();
			res.add(start.remove(index));
		}
		return res;
		
	}
	
}

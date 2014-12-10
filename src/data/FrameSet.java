package data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

public class FrameSet {
	
	private final ArrayList<ArrayRealVector> frameSet;
	private final int size;
	private final int dimension;
	
	public FrameSet(Collection<RealVector> frameSet){
		
		this.size = frameSet.size();
		this.frameSet = new ArrayList<ArrayRealVector>(this.size);
		
		for(RealVector frame : frameSet){
			
			this.frameSet.add(new ArrayRealVector(frame));
			
		}
		
		this.dimension = this.frameSet.get(0).getDimension();
		
	}
	
	public FrameSet(Vector<Double> inputVector, int frameSize, int frameOverlap){
		
		int stepSize = frameSize - frameOverlap;
		int numberOfFrames = inputVector.size()/frameSize;
		
		if(stepSize <= 0){
			throw new IllegalArgumentException("The window overlap needs to be strictly larger than the window size.");
		}else if(numberOfFrames == 0){
			throw new IllegalArgumentException("The window size needs to be smaller than the vector dimension.");
		}
		
		this.frameSet = new ArrayList<ArrayRealVector>(numberOfFrames);
		this.size = numberOfFrames;
		this.dimension = frameSize;
		
		for(int i = 0; i < inputVector.size() - frameSize; i += frameSize){
			double[] vector = new double[frameSize];
			for(int j = 0; j < frameSize; j++){
				vector[j] = inputVector.get(i+j);
			}
			this.frameSet.add(new ArrayRealVector(vector));
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
				numberOfPartitions = (int) Math.ceil((double)5/4);
				itemsPerPartition = partitionOption;
			}
			
		}else{
			throw new IllegalArgumentException("partitionType can only equal 'numberPartitions' or 'partitionSize'.");
		}
			
		ArrayList<FrameSet> partitions = new ArrayList<FrameSet>(numberOfPartitions);
		List<ArrayRealVector> source = permuteList(frameSet);
		
		for(int i = 0; i < numberOfPartitions; i++){
			ArrayList<RealVector> subset = new ArrayList<RealVector>();
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
			int index = generator.nextInt() % start.size();
			res.add(start.remove(index));
		}
		
		return res;
		
	}
	
}

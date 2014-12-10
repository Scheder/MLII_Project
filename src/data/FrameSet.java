package data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

public class FrameSet {
	
	private final ArrayList<ArrayRealVector> frameSet;
	
	public FrameSet(Collection<RealVector> frameSet){
		
		this.frameSet = new ArrayList<ArrayRealVector>(frameSet.size());
		
		for(RealVector frame : frameSet){
			
			this.frameSet.add(new ArrayRealVector(frame));
			
		}
		
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
		
		for(int i = 0; i < inputVector.size() - frameSize; i += frameSize){
			double[] vector = new double[frameSize];
			for(int j = 0; j < frameSize; j++){
				vector[j] = inputVector.get(i+j);
			}
			this.frameSet.add(new ArrayRealVector(vector));
		}
		
	}
	
}

package data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;

public class LabeledFrameSet extends FrameSet {
	
	private final List<Integer> labels;

	public LabeledFrameSet(Collection<ArrayRealVector> frameSet, Collection<Integer> labels, int windowSize){
		super(frameSet, windowSize);
		ArrayList<Integer> newLabels = new ArrayList<Integer>(labels);
		this.labels = new ArrayList<Integer>(this.size());
		for(int i = 0; i < this.size(); i++){
			this.labels.add(newLabels.get(i));
		}
	}
	
	public Integer getLabel(int frameIndex){
		return this.labels.get(frameIndex);
	}
}

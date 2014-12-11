package data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;

public class LabeledFrameSet extends FrameSet {
	
	private final List<String> labels;

	public LabeledFrameSet(Collection<ArrayRealVector> frameSet, Collection<String> labels){
		super(frameSet);
		this.labels = new ArrayList<String>(labels);
	}
	
	public String getLabel(int frameIndex){
		return this.labels.get(frameIndex);
	}
	
	public List<String> getLabelList(){
		return new ArrayList<String>(labels);
	}
}

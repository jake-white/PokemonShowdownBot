package bot;

public class Decision {
	private DecisionType type;
	private int value;
	public Decision(DecisionType type, int value){
		this.type = type;
		this.value = value;
	}
	
	public DecisionType getType(){
		return type;
	}
	public int getValue(){
		return value;
	}

}

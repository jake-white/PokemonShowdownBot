package bot;

public class Situation {
	private Move[] moves;
	private Pokemon[] myMons;
	private Pokemon theirMon;
	private boolean canMove, canSwitch;
	private double highestMovePriority = 0, highestSwitchPriority = 0;
	private String[] nonVolatileStatusMoves = {"toxic", //tox
												"poisonpowder", //psn
												"thunderwave","stunspore", "glare", //par
												"will-o-wisp", //brn
												"spore","darkvoid","sleeppowder"}; //slp
	private final double statusModifier = 70, switchModifier = 60;
	private String[] statusNames = {"PSN","TOX","PAR","BRN","SLP","FRZ"};
	private String currentStatus;
	public Situation(Move[] moves, Pokemon[] myMons, Pokemon theirMon, boolean movable, boolean switchable, String status){
		this.currentStatus = status;
		this.moves = moves;
		this.myMons = myMons;
		this.theirMon = theirMon;
		this.canMove = movable;
		this.canSwitch = switchable;
	}
	
	public Decision getDecision(){
		if(!canMove && !canSwitch){
			return new Decision(DecisionType.WAIT, 0);
		}
		else if(canMove && !canSwitch){
			return new Decision(DecisionType.MOVE,  makeBestMove());
		}
		else if(canSwitch && !canMove){
			return new Decision(DecisionType.SWITCH,  makeBestSwitch());
		}
		else{ //can do ANYTHING!
			makeBestSwitch();
			makeBestMove();
			if(highestSwitchPriority > highestMovePriority){
				return new Decision(DecisionType.SWITCH, makeBestSwitch());
			}
			else{
				return new Decision(DecisionType.MOVE, makeBestMove());
			}
		}
	}
	
	public int makeBestMove(){
		double[] movePriority = new double[moves.length];
		highestMovePriority = 0;
		int bestMove = 0;
		for(int i = 0; i < moves.length; ++i){
			movePriority[i] = 1; //starts at 1
			if(!moves[i].getCategory().equals("Status")){
				movePriority[i] *= damageCalc(moves[i]);
			}
			else if(moves[i].getCategory().equals("Status")){
				for(int j = 0; j < nonVolatileStatusMoves.length; ++j){
					if(moves[i].getName().equals(nonVolatileStatusMoves[i]) && !theirMon.isStatused()){
						if(moves[i].getEffectivenessAgainst(theirMon.getTypes()) > 0)
							movePriority[i] *= statusModifier;
					}
				}
			}
			if(moves[i].isDisabled()){
				movePriority[i] = -1;
			}
			if(movePriority[i] > highestMovePriority){
				highestMovePriority = movePriority[i];
				bestMove = i;
			}
		}
		return bestMove;
	}
	
	public double damageCalc(Move m){
		//format: 'N.NNx Stat
		//Stat = 'Atk', 'SpA'
		double statMod = 1;
		//please do not judge me it is past 3AM at a hackathon and I can't do anything sophisticated right now
		String[] modifiers = {"0.25×","0.33×","0.4×","0.5×","0.67×","1.5×","2×","2.5×","3×", "3.5×", "4×"};
		double[] realMod = {0.25, 0.33, 0.4, 0.5, 0.67, 1.5, 2, 2.5, 3, 3.5, 4};
		if(m.getCategory().equals("Special") && currentStatus.contains("SpA")){
			for(int i = 0; i < modifiers.length; ++i){
				if(currentStatus.contains(modifiers[i] + " SpA")){
					statMod = realMod[i];
				}
			}
		}
		else if(m.getCategory().equals("Physical") && currentStatus.contains("Atk")){
			for(int i = 0; i < modifiers.length; ++i){
				if(currentStatus.contains(modifiers[i] + " Atk")){
					statMod = realMod[i];
				}
			}
		}
		double stabBoost = 1;
		if(canSwitch){
			String[] currentTypes = myMons[0].getTypes();
			for(int j = 0; j < currentTypes.length; ++j){
				if(currentTypes[j].equals(m.getType())){
					stabBoost = 1.5;
				}
			}
		}
		double statusMod = 1;
		if(canSwitch){
			if(myMons[0].getStatus().contains("BRN") && m.getCategory().equals("Physical")){
				statusMod *= 0.5;
			}
			if(myMons[0].isStatused() && m.getName().equals("facade")){
					statusMod *= 2;
			}
		}
		else{
			if(currentStatus.contains("BRN") && m.getCategory().equals("Physical")){
				statusMod *= 0.5;
			}

			for(int i = 0; i < statusNames.length; ++i){
				if(currentStatus.contains(statusNames[i]))
					statusMod *= 2;
			}
		}
		double effectiveness = m.getEffectivenessAgainst(theirMon.getTypes())*theirMon.extraEffect(m)*statusMod*statMod;
		double damage = effectiveness*m.getPower()*stabBoost;
		double accuracy = (((double) m.getAccuracy())/100.0);
		damage*= accuracy; //multiply by accuracy
		return damage;
	}
	
	public int makeBestSwitch(){
		double[] switchPriority = new double[myMons.length];
		highestSwitchPriority = 0;
		int bestSwitch = 0;
		for(int i = 0; i < myMons.length; ++i){
			switchPriority[i] = switchModifier;
			double effectiveness = 0;
			if(myMons[i].isAlive()){
				try{
					for(int j = 0; j < theirMon.getTypes().length; ++j){ //checking against the enemy STAB options
						Move move = new Move("Filler Move", theirMon.getTypes()[j], 0, false);
						effectiveness += 1/(2+move.getEffectivenessAgainst(myMons[i].getTypes()));
					}
						switchPriority[i] *= effectiveness;
				}
				catch(Exception e){
					//their mon is also dead. this likely happened due to recoil or u-turn or something
				}
			}
			else
				switchPriority[i] = -1;
			if(switchPriority[i] > highestSwitchPriority){
				highestSwitchPriority = switchPriority[i];
				bestSwitch = i;
			}
			if(highestSwitchPriority <= switchPriority[0]) //if it's as good or worse than the current Pokemon. this stops infini-switching
				bestSwitch*=0.5;
		}
		return bestSwitch;
	}
}

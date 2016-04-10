package bot;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openqa.selenium.WebElement;

public class Pokemon {
	private String[] statusNames = {"PSN","TOX","PAR","BRN","SLP","FRZ"};
	WebElement myButton;
	private String species;
	private boolean alive = true;
	private static boolean parsed = false;
	private String status = "";
	private static JSONObject pokemonData;
	public JSONObject thisPokemon;
	private double hp = 0;
	private String[] types;
	
	public Pokemon(String species, boolean alive, String status){
		if(species.indexOf(" ") != -1){
			this.species = species.substring(0, species.indexOf(" ")).trim().toLowerCase();
			this.hp = Double.parseDouble(species.substring(species.indexOf("\n"), species.indexOf("%")));
		}
		else{
			this.species = species.trim().toLowerCase();
		}

		if(this.species.equals("farfetch'd"))
			this.species = "farfetchd";
		else if(this.species.equals("porygon-z"))
			this.species = "porygonz";
		this.status = status.trim();
		
		this.alive = alive;
		parseJSON();
		parseSelf();
	}
	
	public void parseSelf(){
		thisPokemon = (JSONObject) JSONValue.parse(pokemonData.get(species).toString());
		String typeStr = thisPokemon.get("types").toString();
		if(typeStr.contains(",")){
			this.types = new String[2];
			int firstQuote = 2;
			int secondQuote = firstQuote + typeStr.substring(firstQuote).indexOf("\"");
			int thirdQuote = secondQuote+3;
			int fourthQuote = thirdQuote + typeStr.substring(thirdQuote).indexOf("\"");
			types[0] = typeStr.substring(firstQuote, secondQuote);
			types[1] = typeStr.substring(thirdQuote, fourthQuote);
		}
		else{
			this.types = new String[1];
			int firstQuote = 2;
			int secondQuote = firstQuote + typeStr.substring(firstQuote).indexOf("\"");
			types[0] = typeStr.substring(firstQuote, secondQuote);
		}
	}
	
	public static void parseJSON(){
		if(!parsed){ //checking if another Pokemon has already parsed every other Pokemon
			Scanner input;
			try {
				input = new Scanner(new File("script_res/pokedex.txt"));
				String json = "";
				while(input.hasNextLine()){
					json += input.nextLine();
				}
				json = json.replaceAll(" ", "");
				pokemonData = (JSONObject) JSONValue.parse(json);
				parsed = true;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	public String[] getTypes(){
		return types;
	}
	
	public boolean isAlive(){
		return alive;
	}
	
	public double extraEffect(Move m){
		String type = m.getType();
		String pokeInfo = thisPokemon.toString();
		double modifier = 1;
		if(pokeInfo.contains("Levitate") && type.contains("Ground"))
			modifier*=0;
		else if((pokeInfo.contains("Water Absorb") || pokeInfo.contains("Dry Skin") || pokeInfo.contains("Storm Drain")) && type.contains("Water"))
			modifier*=0;
		else if((pokeInfo.contains("Lightningrod") || pokeInfo.contains("Motor Drive")) && type.contains("Electric"))
			modifier*=0;
		else if(pokeInfo.contains("Flash Fire") && type.contains("Fire"))
			modifier*=0;
		else if(pokeInfo.contains("Dry Skin") && type.contains("Fire"))
			modifier*=1.25;
		else if(pokeInfo.contains("Sap Sipper") && type.contains("Grass"))
			modifier*=0;
		else if(pokeInfo.contains("Thick Fat") && (type.contains("Ice") || type.contains("Fire")))
			modifier*=0.5;
		for(int i = 0; i < types.length; ++i){
			if(this.types[i].equals("Grass") && (m.getName().equals("spore") || m.getName().equals("sleeppowder")||
					m.getName().equals("stunspore") | m.getName().equals("poisonpowder")))
				modifier*=0;
			else if(this.types[i].equals("Fire") && m.getName().equals("will-o-wisp"))
				modifier*=0;
			else if(this.types[i].equals("Electric") && m.getName().equals("thunderwave"))
				modifier*=0;
			else if(this.types[i].equals("Poison") && m.getName().equals("will-o-wisp"))
					modifier*=0;
		}
		if(status.contains("Balloon") && type.contains("Ground"))
			modifier*=0;
		return modifier;
		
	}

	public double getHP() {
		return hp;
	}

	public boolean isStatused() {
		for(int i = 0; i < statusNames.length; ++i){
			if(status.contains(statusNames[i]))
				return true;
		}
		return false;
	}

	public String getStatus() {
		return status;
	}

}

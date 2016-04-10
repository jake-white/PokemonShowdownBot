package bot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openqa.selenium.WebElement;

public class Move {
	private static boolean parsed = false;
	private boolean disabled = false;
	private static JSONObject moveData, typeData;
	private String name, type, category;
	private int bp, pp, accuracy;
	
	public Move(String name, String type, int pp, boolean disabled){
		this.name = name.trim().toLowerCase().replaceAll(" ", "").replaceAll("-", "");
		this.type = type.trim();
		this.pp = pp;
		this.disabled = disabled;
		parseJSON();
		parseSelf();
	}
	
	public void parseSelf(){
		JSONObject thisMove = (JSONObject) JSONValue.parse(moveData.get(name).toString());
		this.bp = Integer.parseInt(thisMove.get("basePower").toString());
		String acc = thisMove.get("accuracy").toString();
		if(thisMove.get("multihit") != null){
			this.bp*=3;
		}
		if(acc.equals("true"))
			this.accuracy = 100;
		else
			this.accuracy = Integer.parseInt(acc);
		this.category = thisMove.get("category").toString();
	}
	
	public static void parseJSON(){
		if(!parsed){ //checking if another Move has already parsed every other Move
			Scanner input;
			try {
				input = new Scanner(new File("script_res/move_data.txt"));
				String json = "";
				while(input.hasNextLine()){
					json += input.nextLine();
				}
				moveData = (JSONObject) JSONValue.parse(json);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try {
				input = new Scanner(new File("script_res/typechart.txt"));
				String json = "";
				while(input.hasNextLine()){
					json += input.nextLine();
				}
				typeData = (JSONObject) JSONValue.parse(json);
				parsed = true;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public String getCategory(){
		return category;
	}
	
	public double getEffectivenessAgainst(String[] poketype){
		double effective = 1;
		JSONObject thisType = (JSONObject) JSONValue.parse(typeData.get(type).toString());
		for(int i = 0; i < poketype.length; ++i){
			effective*= Double.parseDouble(thisType.get(poketype[i]).toString());
		}
		
		return effective;
		
	}
	
	public String toString(){
		return "Move: " + name + " is a " + category + ", " + bp + "-power " + type + "-type move with " + pp + " PP left.";
	}

	public String getName() {
		return name;
	}

	public double getPower() {
		return bp;
	}

	public String getType() {
		return type;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public int getAccuracy() {
		return accuracy;
	}
}

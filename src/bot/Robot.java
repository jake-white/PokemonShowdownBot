package bot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.swing.Timer;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class Robot implements ActionListener{
	private WebDriver driver;
	private String username = "squirrelBoTT", password;
	private boolean available = true, hasMessaged = false, isLaddering = true;
	private int wins = 0, losses = 0;
	private Timer tick;
	private Situation currentSituation;
	private final int MAX_MOVES = 4;
	private final int MAX_MONS = 6;
	
	public static void main(String args[]){ //main
		new Robot();
	}
	
	public Robot(){ //constructing Robot
		System.setProperty("webdriver.chrome.driver", "chromedriver_win32/chromedriver.exe");
		Map<String, Object> prefs = new HashMap<String, Object>();		//this is for disabling Chrome notifications
		prefs.put("profile.default_content_setting_values.notifications", 2);
		ChromeOptions options = new ChromeOptions();
		options.setExperimentalOption("prefs", prefs);
		
		driver = new ChromeDriver(options);
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS); //waiting 10 seconds for elements to appear before giving up
		driver.get("http://www.play.pokemonshowdown.com");
		this.login();
		driver.manage().timeouts().implicitlyWait(0, TimeUnit.MILLISECONDS); //no longer waiting.
		tick = new Timer(100, this);
		tick.start();
		try{
			Thread.sleep(1000); //pausing the thread to force the next thread to start ticking
		}
		catch(Exception e){
			
		}
	}
	
	public void login(){
		Scanner inFile;
		try {
			inFile = new Scanner(new File("password.txt"));
			password = inFile.nextLine();
			inFile.close();
		} catch (FileNotFoundException e) {
			System.out.println("File not found! You need a password.txt in the main directory.");
		}
		driver.findElement(By.name("login")).click();
		driver.findElement(By.name("username")).sendKeys(username);
		driver.findElement(By.name("username")).sendKeys(Keys.ENTER);
		driver.findElement(By.name("password")).sendKeys(password);
		driver.findElement(By.name("password")).sendKeys(Keys.ENTER);
		try{
			driver.findElement(By.name("closeHide")).click();
		}
		catch(Exception e){
			
		}
		try {
			Thread.sleep(1000); //sometimes it takes a while to actually log in
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if(this.available){
			battleSearch();
		}
		else if(!this.hasMessaged){ //just got in a battle
			driver.manage().timeouts().implicitlyWait(0, TimeUnit.MILLISECONDS); //no longer waiting.
			/*format:
			 * <div class='battle-log>
			 *   <form class='chatbox'>
			 *     <textarea>Not what we want</textarea>
			 *     <textarea>What we want</textarea>
			 *   </form>
			 * </div>
			 * */
			try{
				List<WebElement> chatBoxes = driver.findElement(
						By.className("battle-log-add")).findElement(
								By.className("chatbox")).findElements(
										By.tagName("textarea"));
				WebElement chatBox = chatBoxes.get(1);
				chatBox.click();
				chatBox.sendKeys("Challenge accepted.");
				chatBox.sendKeys(Keys.ENTER);
				this.hasMessaged = true;
			}
			catch(Exception e){
				//still loading battle, apparently.
			}
		}
		else{ //must be in a battle
			try{
				collectInfo();
				executeDecision(currentSituation.getDecision());
			}
			catch(Exception e){
				//weird stuff like forfeits in the middle of info gathering cause this
				//so this is a general failsafe
			}				
		}
	}
	
	public void battleSearch(){
		try{
			WebElement challenge = driver.findElement(By.name("acceptChallenge"));
			String format = driver.findElement(By.className("formatselect")).getText();
			if(format.trim().equals("Random Battle")){
				WebElement PM = driver.findElement(By.name("message"));
				PM.click();
				PM.sendKeys("I'm a Pokemon-playing robot! You seem to have challenged me to format: " + format + ".");
				PM.sendKeys(Keys.ENTER);
				challenge.click();
				this.available = false;
				this.hasMessaged = false;
			}
		}
		catch(NoSuchElementException e){
			//there is no battle button?
		}
		if(available && isLaddering){ //didn't find incoming random battle challenges
			WebElement bigbutton = driver.findElement(By.name("search"));
			bigbutton.click();
			this.available = false;
			this.hasMessaged = false;
		}
	}
	
	public void collectInfo(){
		boolean battleOver = false;
		try{
			WebElement closeButton = driver.findElement(By.name("closeAndMainMenu"));
			try{
				driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
				WebElement win = driver.findElement(By.xpath(("//*[contains(text(), 'squirrelBoTT won the battle!')]")));
				System.out.println(win.getAttribute("class"));
				if(!win.getTagName().contains("em")){
					System.out.println("Won the battle.");
					++wins;
				}
			}
			catch(Exception e){
				System.out.println("Lost the battle.");
				++losses;
			}
			System.out.println("We are now " + wins + " - " + losses + ".");
			closeButton.click();
			battleOver = true;
			available = true;
			hasMessaged = false;
		}
		catch(Exception e){
			battleOver = false;
		}
			boolean isMega = false;
		if(!battleOver){
			try{
				WebElement mega = driver.findElement(By.name("megaevo"));
				if(mega.getAttribute("checked") == null){
					mega.click(); //making sure to activate Mega Evolution in case there is one available
					isMega = true;
				}
			}
			catch(Exception e){
				
			}

			try{
				WebElement timer = driver.findElement(By.name("setTimer"));
				if(timer.getAttribute("value").equals("on"))
					timer.click(); //making sure to activate the timer if I can
			}
			catch(Exception e){
				
			}
			Move[] moves;
			Pokemon[] mons;
			Pokemon theirMon = null;
			boolean movable = true, switchable = true;
			WebElement movemenu = null, switchmenu = null;
			List<WebElement> pokemon = null, moveset = null;
			String currentMonStatus = "";
			try{
				movemenu = driver.findElement(By.className("movemenu")); //if there exists a move menu
				moveset = movemenu.findElements(By.tagName("button"));
			}
			catch(Exception e){
				movable = false;
			}
			try{
				switchmenu = driver.findElement(By.className("switchmenu")); //if there exists a switch menu
				pokemon = switchmenu.findElements(By.tagName("button"));
				pokemon.get(1).getText(); //just to force it to throw an exception
			}
			catch(Exception e){
				switchable = false;
			}
			
			if(switchable && !movable){
				/* often times movable is set to false on accident
				 * this is because when moves are checked, the HTML hasn't loaded
				 * and when switches are checked, it has
				 * this is a hacky way of correcting this
				 */
				try{
					movemenu = driver.findElement(By.className("movemenu")); //if there exists a move menu
					moveset = movemenu.findElements(By.tagName("button"));
					movable = true;
				}
				catch(Exception e){
					movable = false;
				}
			}
			try{
				moves = new Move[moveset.size()];
				mons = new Pokemon[pokemon.size()];
			}
			catch(Exception e){
				moves = new Move[MAX_MOVES];
				mons = new Pokemon[MAX_MONS];
			}
				if(movable){
					currentMonStatus = driver.findElement(By.className("rstatbar")).findElement(By.className("status")).getText();
					for(int i = 0; i < moveset.size(); ++i){
						String[] moveText = moveset.get(i).getText().split("\n", 3);
						int pp = 0;
						if(moveText[2].contains("/"))
							pp = Integer.parseInt(moveText[2].split("/", 2)[0]);
						else //if it is a move with 0 PP like struggle
							pp = 0;
						boolean isDisabled = false;
						try{
							if(moveset.get(i).getAttribute("disabled").equals("true")){ //true if disabled, null if not. shame on you for inconsistency, zarel.
								isDisabled = true;
							}
							else{
								isDisabled = false;
							}
						}
						catch(Exception e){
							isDisabled = false;
							//if we get here, it means the move is not disabled!
						}
						moves[i] = new Move(moveText[0], moveText[1], pp, isDisabled);
					}
				}
				if(switchable){
					for(int i =0; i < pokemon.size(); ++i){
						boolean isAlive = true;
						String species = pokemon.get(i).getText();
						try{
							if(pokemon.get(i).getAttribute("class").equals("disabled")) //class = disabled if dead, class = null if alive...
								isAlive = false;
							else
								isAlive = true;
						}
						catch(Exception e){
							//if we get here, it means the Pokemon is alive!
						}
						if(i==0){
							if(isMega)
								mons[i] = new Pokemon(species+"mega", isAlive, currentMonStatus);
							mons[i] = new Pokemon(species, isAlive, currentMonStatus);
						}
						else
							mons[i] = new Pokemon(species, isAlive, "");
					}
				}			
			
			try{
				WebElement enemyMon = driver.findElement(By.className("lstatbar"));
				String enemyStatus = enemyMon.findElement(By.className("status")).getText();
				theirMon = new Pokemon(enemyMon.getText().trim(), true, enemyStatus);
			}
			catch(Exception e){
				
			}
			this.currentSituation = new Situation(moves, mons, theirMon, movable, switchable, currentMonStatus);
		}
		
	}
	
	public void executeDecision(Decision decision){
		if(decision.getType() == DecisionType.MOVE){
			try{
				WebElement movemenu = driver.findElement(By.className("movemenu"));
				List<WebElement> moveset = movemenu.findElements(By.tagName("button"));			
				moveset.get(decision.getValue()).click();
			}
			catch(Exception e){
				
			}
		}
		else if(decision.getType() == DecisionType.SWITCH){
			try{
				WebElement switchmenu = driver.findElement(By.className("switchmenu"));
				List<WebElement> pokemon = switchmenu.findElements(By.tagName("button"));
				pokemon.get(decision.getValue()).click();
			}
			catch(Exception e){
				
			}
		}
		//else wait, because it is DecisionType.WAIT
	}

}

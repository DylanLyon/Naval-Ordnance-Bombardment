package application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Scanner;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class Main extends Application implements Runnable{

	/**GUI Objects**/
	private BorderPane border = new BorderPane();
	private Button readyButton = new Button("Ready");
	private Scene finalScene;
	
	private Label info = new Label("Ships left to place: 5");
	private Label error = new Label("");
	private Label rules = new Label("		    BATTLESHIP - RULES AND GUIDELINES"
			+ "\n- Your goal is to destroy all enemy ships, while hidding your own. "
			+ "\n- The first player to destroy all enemy ships, wins! "
			+ "\n- Begin by placing friendly ships on the white grid. "
			+ "\n- Once all ships are placed, hit the ready button to begin. "
			+ "\n- Then, use the black grid to guess enemy ship locations. ");
	
	Image imageLightRed = new Image(Main.class.getResourceAsStream("/images/turnLightRed.png"));
	Image imageLightGreen = new Image(Main.class.getResourceAsStream("/images/turnLightGreen.png"));
	
	ImageView turnLight;
	ImageView slot1;
	ImageView slot2;
	ImageView slot3;
	ImageView slot4;
	ImageView slot5;
	ImageView slot6;
	ImageView slot7;
	ImageView slot8;
	ImageView slot9;
	ImageView slot10;
	/** ^ GUI Objects ^ **/
	
	/** Server Stuff **/
	private static Socket socket;
	private static ServerSocket serverSocket;
	private static String ip;
	private static int port;
	private static Scanner scanner = new Scanner(System.in);
	private Thread thread;
	private static ObjectOutputStream outObjStream;
	private static ObjectInputStream inObjStream;
	private static DataOutputStream outStream;
	private static DataInputStream inStream;
	/** ^ Server Stuff ^ **/
	
	/** Various Variables **/
	private int totalShips = 5;
	private int placedShips = 0;
	private int enemyShipsRemaining = 5;
	private int localShipsRemaining = 5;
	
	private boolean ready = false;
	private boolean localReady = false;
	private static boolean yourTurn = false;
	private volatile boolean running = true;
	private static boolean host = true;
	
	private ArrayList<Rectangle> board = new ArrayList<Rectangle>();
	private static ArrayList<Coordinate> enemyGuesses = new ArrayList<Coordinate>();
	private ArrayList<Coordinate> localShips = new ArrayList<Coordinate>();
	private Coordinate mostRecentGuess = null;
	/** ^ Various Variables ^ **/
	
	public Main(){
		
		if(!host){
			connect();
		}
		
		thread = new Thread(this, "Battleship");
		thread.start();
	}
	
	@Override
	public void run() {
		
		while (running) {
			
			if(ready){ tick();}
			if(!ready){checkReady();}
			
			try{
				if(localShips.isEmpty() && ready){
					//local loss
					System.out.println("You Lose!");
					running = false;
					
				}else if(enemyShipsRemaining == 0){
					//local win
					System.out.println("You Win!");
					running = false;

				}else{
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}catch(IllegalStateException e){
				System.out.println("Program terminated.");
			}
		}
	}
	private int convertBoard(Coordinate pos){
		
		int[] x = {0, 8, 16, 24, 32, 40, 48, 56};
		int[] y = {0, 1, 2, 3, 4, 5, 6, 7};
		
		int xIndex = pos.getX();
		int yIndex = pos.getY();
		
		int location = x[xIndex] + y[yIndex];
		
		return location;
	}
	private void checkReady(){
		if(localReady){
			try {
				outStream.writeInt(1);
				
				if(inStream.readInt() == 1){
					ready=true;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void tick() {
		
		if (!yourTurn) {
			
			turnLight.setImage(imageLightRed);
			
			try {
				//take enemy guess, add to guess list, check that guess
				Coordinate location = (Coordinate) inObjStream.readObject();
				enemyGuesses.add(location);
				
				int bombDrop = convertBoard(location);
				
				if(board.get(bombDrop).getFill() == Color.GREEN){
					board.get(bombDrop).setFill(Color.RED);
				}
				else{
					board.get(bombDrop).setFill(Color.YELLOW);
				}
				
				if(checkGuesses()){
					//if guess is a hit, then return true to enemy.
					outStream.writeBoolean(true);
					
					Image miss = new Image(Main.class.getResourceAsStream("/images/miss.png"));
					
					if(localShipsRemaining == 5){
						slot6.setImage(miss);
					}
					if(localShipsRemaining == 4){
						slot7.setImage(miss);					
					}
					if(localShipsRemaining == 3){
						slot8.setImage(miss);
					}
					if(localShipsRemaining == 2){
						slot9.setImage(miss);
					}
					if(localShipsRemaining == 1){
						slot10.setImage(miss);
					}
					localShipsRemaining--;
					
				}else{
					//otherwise, it's a miss, and return false
					outStream.writeBoolean(false);
				}
				
				outStream.flush();
				yourTurn = true;
				
			} catch (SocketException se) {
				System.out.println("---- CONNECTION LOST ----");
				se.printStackTrace();
				running = false;
			} catch (IOException e) {
				e.printStackTrace();
				running = false;
			} catch (ClassNotFoundException cn) {
				cn.printStackTrace();
				running = false;
			}
		}
		if (yourTurn){
			
			turnLight.setImage(imageLightGreen);
			
			if(mostRecentGuess != null){
				try {
					outObjStream.writeObject(mostRecentGuess);
					
					Boolean guess = inStream.readBoolean();
					if(guess){
						Image hit = new Image(Main.class.getResourceAsStream("/images/hit.png"));
						
						if(enemyShipsRemaining == 5){
							slot1.setImage(hit);
						}
						if(enemyShipsRemaining == 4){
							slot2.setImage(hit);				
						}
						if(enemyShipsRemaining == 3){
							
							slot3.setImage(hit);
						}
						if(enemyShipsRemaining == 2){
							slot4.setImage(hit);
						}
						if(enemyShipsRemaining == 1){
							slot5.setImage(hit);
						}
						enemyShipsRemaining--;
					}
					yourTurn = false;
					mostRecentGuess = null;

				} catch (SocketException se) {
					se.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} 
			}else{
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private boolean checkGuesses() {

		for(int i = 0; i < enemyGuesses.size(); i++){
			
			for(int j = 0; j < localShips.size(); j++){
				
				if(localShips.get(j).compareTo(enemyGuesses.get(i))){
					
					localShips.remove(j);
					return true;
				}
			}
		}
		return false;
	}

	private static void initializeServer() {
		System.out.println("Server initialized. Waiting for client connection...");
		try {
			serverSocket = new ServerSocket(port);
			yourTurn = true;
			host = true;
			
			socket = serverSocket.accept();
			
			outObjStream = new ObjectOutputStream(socket.getOutputStream());
			inObjStream = new ObjectInputStream(socket.getInputStream());
			
			outStream = new DataOutputStream(socket.getOutputStream());
			inStream = new DataInputStream(socket.getInputStream());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static boolean connect() {
		try {
			socket = new Socket(ip, port);
			
			outObjStream = new ObjectOutputStream(socket.getOutputStream());
			inObjStream = new ObjectInputStream(socket.getInputStream());
			
			outStream = new DataOutputStream(socket.getOutputStream());
			inStream = new DataInputStream(socket.getInputStream());
			
		} catch (IOException e) {
			System.out.println("Unable to connect to the address: " + ip + ":" + port);
			return false;
		}
		System.out.println("Successfully connected to the server.");
		return true;
	}

	private Pane drawPlayerBoard(Pane pane){

		pane.setPrefSize(264,264);

		NumberBinding rectsAreaSize = Bindings.min(pane.heightProperty(), pane.widthProperty());

		for (int x = 0; x < 8; x++) {
			for (int y = 0; y < 8; y++) {
				Rectangle rectangle = new Rectangle();
				rectangle.setFill(Color.WHITE);
				rectangle.setStroke(Color.BLACK);
				
				board.add(rectangle);
				
				rectangle.setOnMouseClicked(new EventHandler<MouseEvent>() {
					@Override
					public void handle(MouseEvent t) {
						Rectangle checkRec = (Rectangle)t.getSource();
						if(checkRec.getFill() == Color.GREEN){
							error.setText("Invalid placement, please pick another.");
						}
						
						else if(placedShips < totalShips){
							Rectangle currentRec = (Rectangle) t.getSource();
							
							currentRec.setFill(Color.GREEN);
							error.setText("");
							int guessX = (int)t.getX()/33;
							int guessY = (int)t.getY()/33;
							
							Coordinate location = new Coordinate(guessX, guessY);
							localShips.add(location);
							placedShips++;
							
							info.setText("Ships left to place: " + (totalShips - placedShips));
						}
					}
				});
				rectangle.xProperty().bind(rectsAreaSize.multiply(x).divide(8));
				rectangle.yProperty().bind(rectsAreaSize.multiply(y).divide(8));

				rectangle.heightProperty().bind(rectsAreaSize.divide(8));
				rectangle.widthProperty().bind(rectangle.heightProperty());

				pane.getChildren().add(rectangle);
			}
		}
		return pane;
	}
	private Pane drawEnemyBoard(Pane pane){

		pane.setPrefSize(264,264);

		NumberBinding rectsAreaSize = Bindings.min(pane.heightProperty(), pane.widthProperty());

		int x = 0;
		
		for (x = 0; x < 8; x++) {
			for (int y = 0; y < 8; y++) {
				Rectangle rectangle = new Rectangle();
				rectangle.setStroke(Color.WHITE);
			
				rectangle.setOnMouseClicked(new EventHandler<MouseEvent>() {
					@Override
					public void handle(MouseEvent t) {
						if(ready){
							if(!yourTurn){
								error.setText("Unable to perform this action out of turn.");
								
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
								}
								
								error.setText("");
								
							}else{
								Rectangle currentRec = (Rectangle) t.getSource();
								currentRec.setFill(Color.RED);
								
								int guessX = (int)t.getX()/33;
								int guessY = (int)t.getY()/33;
								
								mostRecentGuess = new Coordinate(guessX, guessY);
							}
						}else{
							error.setText("Not all players are ready.");
						}
					}
				});
				rectangle.xProperty().bind(rectsAreaSize.multiply(x).divide(8));
				rectangle.yProperty().bind(rectsAreaSize.multiply(y).divide(8));
				rectangle.heightProperty().bind(rectsAreaSize.divide(8));
				rectangle.widthProperty().bind(rectangle.heightProperty());
				pane.getChildren().add(rectangle);
			}
		}
		return pane;
	}

	private FlowPane drawScoreBoard(FlowPane pane) {
		
		pane.setPrefSize(185,74);
		
		Image empty = new Image(Main.class.getResourceAsStream("/images/empty.png"));
		
		slot1 = new ImageView();
		slot2 = new ImageView();
		slot3 = new ImageView();
		slot4 = new ImageView();
		slot5 = new ImageView();
		
		slot6 = new ImageView();
		slot7 = new ImageView();
		slot8 = new ImageView();
		slot9 = new ImageView();
		slot10 = new ImageView();
	
		slot1.setImage(empty);
		slot2.setImage(empty);
		slot3.setImage(empty);
		slot4.setImage(empty);
		slot5.setImage(empty);
		
		slot6.setImage(empty);
		slot7.setImage(empty);
		slot8.setImage(empty);
		slot9.setImage(empty);
		slot10.setImage(empty);
		
		pane.getChildren().add(slot1);
		pane.getChildren().add(slot2);
		pane.getChildren().add(slot3);
		pane.getChildren().add(slot4);
		pane.getChildren().add(slot5);
		
		pane.getChildren().add(slot6);
		pane.getChildren().add(slot7);
		pane.getChildren().add(slot8);
		pane.getChildren().add(slot9);
		pane.getChildren().add(slot10);
		
		pane.setStyle("-fx-background-image: url(/images/scoreBoard.png);");
	
		return pane;
	}

	private Scene addBoards(Scene scene){
		
		turnLight = new ImageView();
		
		turnLight.setImage(imageLightRed);
		
		Pane playerPane = drawPlayerBoard(new Pane());
		Pane enemyPane = drawEnemyBoard(new Pane());
		
		VBox boardPane = new VBox();
		boardPane.setPadding(new Insets(0,24,0,0));
		boardPane.setSpacing(35);
		boardPane.getChildren().add(enemyPane);
		boardPane.getChildren().add(playerPane);
		
		VBox infoPane = new VBox(); 
		HBox removeables = new HBox();
		
		error.setFont(Font.font("Arrial", FontWeight.BOLD, 16));
		error.setTextFill(Color.RED);
		error.setWrapText(true);
		error.setMaxWidth(350);
		error.setAlignment(Pos.CENTER);
		
		readyButton.setPrefSize(130, 40);
		readyButton.setStyle("-fx-font: 16 arial; -fx-base: #b6e7c9;");
		
		readyButton.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
	        @Override public void handle(MouseEvent e) {
	        	
	        	if(placedShips == totalShips){
	        		localReady = true;
	        	}else{
	        		error.setText("Place all ships before battle.");
	        	}
	        }
		});
		
		FlowPane scoreBoard = drawScoreBoard(new FlowPane());

		scoreBoard.setAlignment(Pos.CENTER);
		scoreBoard.setMaxWidth(185);
		
		info.setTextFill(Color.WHITE);
		
		infoPane.getChildren().add(error);
		infoPane.getChildren().add(turnLight);
		
		infoPane.setPadding(new Insets(25,0,0,15));
		infoPane.setSpacing(45);
		
		infoPane.getChildren().add(scoreBoard);
		infoPane.getChildren().add(rules);
		
		removeables.setPadding(new Insets(35,5,5,5));
		removeables.setSpacing(30);
		removeables.getChildren().add(info);
		removeables.getChildren().add(readyButton);
		
		infoPane.getChildren().add(removeables);
		
		HBox hbox = new HBox();
		hbox.setSpacing(35);
	
		hbox.getChildren().add(boardPane);
		hbox.getChildren().add(infoPane);
		
		hbox.setAlignment(Pos.CENTER);
		infoPane.setAlignment(Pos.TOP_CENTER);
		boardPane.setAlignment(Pos.CENTER);
		
		border.setCenter(hbox);
		
		border.setStyle("-fx-background-image: url(/images/background.png);");
		
		Scene newScene = new Scene(border,800,600);
		return newScene;
	}

	@Override
	public void start(Stage primaryStage) throws IOException{
		Group root = new Group();
		Scene scene = new Scene(root,800,600);
		
		primaryStage.setTitle("N.O.B. - Naval Ordnance Bombardment");
		primaryStage.getIcons().add(new Image(Main.class.getResourceAsStream("/images/icon.png")));

		finalScene = addBoards(scene);
		
		primaryStage.setScene(finalScene);
		primaryStage.show();
	}

	public static void main(String[] args){
		
		System.out.print("IP address: ");
		ip = scanner.nextLine();
		
		System.out.print("Port #: ");
		port = scanner.nextInt();
		
		boolean serverInitialized = false;
		
		boolean connect = connect();
		
		if(!connect){
			System.out.print("Would you like to initialize a new server? (y/n): ");
			while(!connect && serverInitialized == false){
				
				char input = scanner.next(".").charAt(0);
				
				if(input == 'y'){
					
					initializeServer();
					serverInitialized = true;
					
				}else if(input == 'n'){
					System.out.println("Program will now terminate.");
					System.exit(0);
					
				}else{
					System.out.println("Please enter either 'y' or 'n'.");
				}
			}
		}
		launch(args);
	}
}

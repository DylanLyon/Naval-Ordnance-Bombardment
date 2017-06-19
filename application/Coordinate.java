package application;

import java.io.Serializable;

public class Coordinate implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int x;
	private int y;
	
	
	public Coordinate(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	public int getX(){
		return x;
	}
	public void setX(int x){
		this.x = x;
	}
	
	public int getY(){
		return y;
	}
	public void setY(int y){
		this.y = y;
	}

	public boolean compareTo(Coordinate location) {
		
		if(this.x == location.getX() && this.y == location.getY()){
			return true;
		}
		return false;
	}
	
	
	
}

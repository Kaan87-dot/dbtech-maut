package de.htwberlin.dbtech.aufgaben.ue03.model;

/**
 * Model class for Mautabschnitt (Toll Section) table
 */
public class Mautabschnitt {
	private int abschnittsId;
	private double laenge;
	
	public int getAbschnittsId() {
		return abschnittsId;
	}
	
	public void setAbschnittsId(int abschnittsId) {
		this.abschnittsId = abschnittsId;
	}
	
	public double getLaenge() {
		return laenge;
	}
	
	public void setLaenge(double laenge) {
		this.laenge = laenge;
	}
}

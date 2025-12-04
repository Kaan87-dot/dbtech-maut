package de.htwberlin.dbtech.aufgaben.ue03.model;

/**
 * Model class for Mautkategorie (Toll Category) table
 */
public class Mautkategorie {
	private int kategorieId;
	private String achszahl;
	private double mautsatzJeKm;
	
	public int getKategorieId() {
		return kategorieId;
	}
	
	public void setKategorieId(int kategorieId) {
		this.kategorieId = kategorieId;
	}
	
	public String getAchszahl() {
		return achszahl;
	}
	
	public void setAchszahl(String achszahl) {
		this.achszahl = achszahl;
	}
	
	public double getMautsatzJeKm() {
		return mautsatzJeKm;
	}
	
	public void setMautsatzJeKm(double mautsatzJeKm) {
		this.mautsatzJeKm = mautsatzJeKm;
	}
}

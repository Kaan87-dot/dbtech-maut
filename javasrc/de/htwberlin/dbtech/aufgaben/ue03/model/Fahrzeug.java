package de.htwberlin.dbtech.aufgaben.ue03.model;

/**
 * Model class for Fahrzeug (Vehicle) table
 */
public class Fahrzeug {
	private long fzId;
	private int achsen;
	private int ssklId;
	private long fzgId;
	private String kennzeichen;
	
	public long getFzId() {
		return fzId;
	}
	
	public void setFzId(long fzId) {
		this.fzId = fzId;
	}
	
	public int getAchsen() {
		return achsen;
	}
	
	public void setAchsen(int achsen) {
		this.achsen = achsen;
	}
	
	public int getSsklId() {
		return ssklId;
	}
	
	public void setSsklId(int ssklId) {
		this.ssklId = ssklId;
	}
	
	public long getFzgId() {
		return fzgId;
	}
	
	public void setFzgId(long fzgId) {
		this.fzgId = fzgId;
	}
	
	public String getKennzeichen() {
		return kennzeichen;
	}
	
	public void setKennzeichen(String kennzeichen) {
		this.kennzeichen = kennzeichen;
	}
}

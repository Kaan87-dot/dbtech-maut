package de.htwberlin.dbtech.aufgaben.ue03.model;

import java.sql.Timestamp;

/**
 * Model class for Mauterhebung (Toll Collection) table
 */
public class Mauterhebung {
	private long mautId;
	private int abschnittsId;
	private long fzgId;
	private int kategorieId;
	private Timestamp befahrungsdatum;
	private double kosten;
	
	public long getMautId() {
		return mautId;
	}
	
	public void setMautId(long mautId) {
		this.mautId = mautId;
	}
	
	public int getAbschnittsId() {
		return abschnittsId;
	}
	
	public void setAbschnittsId(int abschnittsId) {
		this.abschnittsId = abschnittsId;
	}
	
	public long getFzgId() {
		return fzgId;
	}
	
	public void setFzgId(long fzgId) {
		this.fzgId = fzgId;
	}
	
	public int getKategorieId() {
		return kategorieId;
	}
	
	public void setKategorieId(int kategorieId) {
		this.kategorieId = kategorieId;
	}
	
	public Timestamp getBefahrungsdatum() {
		return befahrungsdatum;
	}
	
	public void setBefahrungsdatum(Timestamp befahrungsdatum) {
		this.befahrungsdatum = befahrungsdatum;
	}
	
	public double getKosten() {
		return kosten;
	}
	
	public void setKosten(double kosten) {
		this.kosten = kosten;
	}
}

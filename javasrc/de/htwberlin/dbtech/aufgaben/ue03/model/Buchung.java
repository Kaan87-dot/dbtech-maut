package de.htwberlin.dbtech.aufgaben.ue03.model;

import java.sql.Timestamp;

/**
 * Model class for Buchung (Booking) table
 */
public class Buchung {
	private long buchungId;
	private int kategorieId;
	private Timestamp befahrungsdatum;
	
	public long getBuchungId() {
		return buchungId;
	}
	
	public void setBuchungId(long buchungId) {
		this.buchungId = buchungId;
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
}

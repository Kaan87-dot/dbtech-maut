package de.htwberlin.dbtech.aufgaben.ue03.dao;

import de.htwberlin.dbtech.aufgaben.ue03.model.Buchung;

/**
 * Data Access Object for Buchung (Booking) table
 */
public interface BuchungDao {
	
	/**
	 * Finds open booking for vehicle and section
	 * @param kennzeichen License plate
	 * @param abschnittsId Section ID
	 * @return Booking information or null if not found
	 */
	Buchung findOpenBooking(String kennzeichen, int abschnittsId);
	
	/**
	 * Updates booking status to completed
	 * @param buchungId Booking ID
	 */
	void closeBooking(long buchungId);
}

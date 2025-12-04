package de.htwberlin.dbtech.aufgaben.ue03.dao;

import de.htwberlin.dbtech.aufgaben.ue03.model.Mautabschnitt;

/**
 * Data Access Object for Mautabschnitt (Toll Section) table
 */
public interface MautabschnittDao {
	
	/**
	 * Finds toll section by ID
	 * @param abschnittsId Section ID
	 * @return Toll section or null if not found
	 */
	Mautabschnitt findById(int abschnittsId);
}

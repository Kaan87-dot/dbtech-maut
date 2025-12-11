package de.htwberlin.dbtech.aufgaben.ue03.dao;

import de.htwberlin.dbtech.aufgaben.ue03.model.Mauterhebung;

/**
 * Data Access Object for Mauterhebung (Toll Collection) table
 */
public interface MauterhebungDao {
	
	/**
	 * Inserts a new toll collection record
	 * @param mauterhebung Toll collection data
	 */
	void insert(Mauterhebung mauterhebung);
	
	/**
	 * Gets next available maut_id
	 * @return Next maut_id
	 */
	long getNextId();
}

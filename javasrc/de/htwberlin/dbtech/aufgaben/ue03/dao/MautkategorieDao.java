package de.htwberlin.dbtech.aufgaben.ue03.dao;

import de.htwberlin.dbtech.aufgaben.ue03.model.Mautkategorie;

/**
 * Data Access Object for Mautkategorie (Toll Category) table
 */
public interface MautkategorieDao {
	
	/**
	 * Finds toll category by axes and pollution class
	 * @param achsen Number of axes
	 * @param ssklId Pollution class ID
	 * @return Toll category or null if not found
	 */
	Mautkategorie findByAchsenAndSskl(int achsen, int ssklId);
	
	/**
	 * Finds toll category by ID
	 * @param kategorieId Category ID
	 * @return Toll category or null if not found
	 */
	Mautkategorie findById(int kategorieId);
}

package de.htwberlin.dbtech.aufgaben.ue03.dao;

import de.htwberlin.dbtech.aufgaben.ue03.model.Fahrzeug;

/**
 * Data Access Object for Fahrzeug (Vehicle) table
 */
public interface FahrzeugDao {
	
	/**
	 * Finds an active vehicle with active device by license plate
	 * @param kennzeichen License plate
	 * @return Vehicle information or null if not found
	 */
	Fahrzeug findActiveVehicleWithDevice(String kennzeichen);
}

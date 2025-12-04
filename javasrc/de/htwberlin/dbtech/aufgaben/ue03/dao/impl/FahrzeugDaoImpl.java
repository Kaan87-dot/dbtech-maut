package de.htwberlin.dbtech.aufgaben.ue03.dao.impl;

import de.htwberlin.dbtech.aufgaben.ue03.dao.FahrzeugDao;
import de.htwberlin.dbtech.aufgaben.ue03.model.Fahrzeug;
import de.htwberlin.dbtech.exceptions.DataException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Table Data Gateway implementation for Fahrzeug table
 */
public class FahrzeugDaoImpl implements FahrzeugDao {
	
	private final Connection connection;
	
	public FahrzeugDaoImpl(Connection connection) {
		this.connection = connection;
	}
	
	@Override
	public Fahrzeug findActiveVehicleWithDevice(String kennzeichen) {
		String sql = "SELECT f.FZ_ID, f.ACHSEN, f.SSKL_ID, fg.FZG_ID " +
				"FROM FAHRZEUG f " +
				"JOIN FAHRZEUGGERAT fg ON f.FZ_ID = fg.FZ_ID " +
				"WHERE f.KENNZEICHEN = ? " +
				"AND f.ABMELDEDATUM IS NULL " +
				"AND fg.STATUS = 'active'";
		
		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setString(1, kennzeichen);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					Fahrzeug fahrzeug = new Fahrzeug();
					fahrzeug.setFzId(rs.getLong("FZ_ID"));
					fahrzeug.setAchsen(rs.getInt("ACHSEN"));
					fahrzeug.setSsklId(rs.getInt("SSKL_ID"));
					fahrzeug.setFzgId(rs.getLong("FZG_ID"));
					fahrzeug.setKennzeichen(kennzeichen);
					return fahrzeug;
				}
			}
		} catch (SQLException e) {
			throw new DataException("Error finding active vehicle with device", e);
		}
		return null;
	}
}

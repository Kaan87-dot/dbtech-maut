package de.htwberlin.dbtech.aufgaben.ue03.dao.impl;

import de.htwberlin.dbtech.aufgaben.ue03.dao.MautabschnittDao;
import de.htwberlin.dbtech.aufgaben.ue03.model.Mautabschnitt;
import de.htwberlin.dbtech.exceptions.DataException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Table Data Gateway implementation for Mautabschnitt table
 */
public class MautabschnittDaoImpl implements MautabschnittDao {
	
	private final Connection connection;
	
	public MautabschnittDaoImpl(Connection connection) {
		this.connection = connection;
	}
	
	@Override
	public Mautabschnitt findById(int abschnittsId) {
		String sql = "SELECT ABSCHNITTS_ID, LAENGE FROM MAUTABSCHNITT WHERE ABSCHNITTS_ID = ?";
		
		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setInt(1, abschnittsId);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					Mautabschnitt abschnitt = new Mautabschnitt();
					abschnitt.setAbschnittsId(rs.getInt("ABSCHNITTS_ID"));
					abschnitt.setLaenge(rs.getDouble("LAENGE"));
					return abschnitt;
				}
			}
		} catch (SQLException e) {
			throw new DataException("Error finding toll section", e);
		}
		return null;
	}
}

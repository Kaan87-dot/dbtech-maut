package de.htwberlin.dbtech.aufgaben.ue03.dao.impl;

import de.htwberlin.dbtech.aufgaben.ue03.dao.BuchungDao;
import de.htwberlin.dbtech.aufgaben.ue03.model.Buchung;
import de.htwberlin.dbtech.exceptions.DataException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Table Data Gateway implementation for Buchung table
 */
public class BuchungDaoImpl implements BuchungDao {
	
	private final Connection connection;
	
	public BuchungDaoImpl(Connection connection) {
		this.connection = connection;
	}
	
	@Override
	public Buchung findOpenBooking(String kennzeichen, int abschnittsId) {
		String sql = "SELECT BUCHUNG_ID, KATEGORIE_ID, BEFAHRUNGSDATUM " +
				"FROM BUCHUNG " +
				"WHERE KENNZEICHEN = ? " +
				"AND ABSCHNITTS_ID = ? " +
				"AND B_ID = 1";
		
		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setString(1, kennzeichen);
			stmt.setInt(2, abschnittsId);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					Buchung buchung = new Buchung();
					buchung.setBuchungId(rs.getLong("BUCHUNG_ID"));
					buchung.setKategorieId(rs.getInt("KATEGORIE_ID"));
					buchung.setBefahrungsdatum(rs.getTimestamp("BEFAHRUNGSDATUM"));
					return buchung;
				}
			}
		} catch (SQLException e) {
			throw new DataException("Error finding open booking", e);
		}
		return null;
	}
	
	@Override
	public void closeBooking(long buchungId) {
		String sql = "UPDATE BUCHUNG SET B_ID = 3, BEFAHRUNGSDATUM = CURRENT_TIMESTAMP " +
				"WHERE BUCHUNG_ID = ?";
		
		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setLong(1, buchungId);
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new DataException("Error closing booking", e);
		}
	}
}

package de.htwberlin.dbtech.aufgaben.ue03.dao.impl;

import de.htwberlin.dbtech.aufgaben.ue03.dao.MautkategorieDao;
import de.htwberlin.dbtech.aufgaben.ue03.model.Mautkategorie;
import de.htwberlin.dbtech.exceptions.DataException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Table Data Gateway implementation for Mautkategorie table
 */
public class MautkategorieDaoImpl implements MautkategorieDao {
	
	private final Connection connection;
	
	public MautkategorieDaoImpl(Connection connection) {
		this.connection = connection;
	}
	
	@Override
	public Mautkategorie findByAchsenAndSskl(int achsen, int ssklId) {
		// Normalize axes for query (5+ becomes ">=5")
		String achszahlPattern;
		if (achsen >= 5) {
			achszahlPattern = ">= 5";
		} else {
			achszahlPattern = "= " + achsen;
		}
		
		String sql = "SELECT KATEGORIE_ID, ACHSZAHL, MAUTSATZ_JE_KM " +
				"FROM MAUTKATEGORIE " +
				"WHERE SSKL_ID = ? AND ACHSZAHL = ?";
		
		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setInt(1, ssklId);
			stmt.setString(2, achszahlPattern);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					Mautkategorie kategorie = new Mautkategorie();
					kategorie.setKategorieId(rs.getInt("KATEGORIE_ID"));
					kategorie.setAchszahl(rs.getString("ACHSZAHL"));
					kategorie.setMautsatzJeKm(rs.getDouble("MAUTSATZ_JE_KM"));
					return kategorie;
				}
			}
		} catch (SQLException e) {
			throw new DataException("Error finding toll category by axes and pollution class", e);
		}
		return null;
	}
	
	@Override
	public Mautkategorie findById(int kategorieId) {
		String sql = "SELECT KATEGORIE_ID, ACHSZAHL, MAUTSATZ_JE_KM " +
				"FROM MAUTKATEGORIE " +
				"WHERE KATEGORIE_ID = ?";
		
		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setInt(1, kategorieId);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					Mautkategorie kategorie = new Mautkategorie();
					kategorie.setKategorieId(rs.getInt("KATEGORIE_ID"));
					kategorie.setAchszahl(rs.getString("ACHSZAHL"));
					kategorie.setMautsatzJeKm(rs.getDouble("MAUTSATZ_JE_KM"));
					return kategorie;
				}
			}
		} catch (SQLException e) {
			throw new DataException("Error finding toll category by ID", e);
		}
		return null;
	}
}

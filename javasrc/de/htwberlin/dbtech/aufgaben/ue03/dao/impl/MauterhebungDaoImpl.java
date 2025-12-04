package de.htwberlin.dbtech.aufgaben.ue03.dao.impl;

import de.htwberlin.dbtech.aufgaben.ue03.dao.MauterhebungDao;
import de.htwberlin.dbtech.aufgaben.ue03.model.Mauterhebung;
import de.htwberlin.dbtech.exceptions.DataException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Table Data Gateway implementation for Mauterhebung table
 */
public class MauterhebungDaoImpl implements MauterhebungDao {
	
	private final Connection connection;
	
	public MauterhebungDaoImpl(Connection connection) {
		this.connection = connection;
	}
	
	@Override
	public void insert(Mauterhebung mauterhebung) {
		String sql = "INSERT INTO MAUTERHEBUNG " +
				"(MAUT_ID, ABSCHNITTS_ID, FZG_ID, KATEGORIE_ID, BEFAHRUNGSDATUM, KOSTEN) " +
				"VALUES (?, ?, ?, ?, ?, ?)";
		
		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setLong(1, mauterhebung.getMautId());
			stmt.setInt(2, mauterhebung.getAbschnittsId());
			stmt.setLong(3, mauterhebung.getFzgId());
			stmt.setInt(4, mauterhebung.getKategorieId());
			stmt.setTimestamp(5, mauterhebung.getBefahrungsdatum());
			stmt.setDouble(6, mauterhebung.getKosten());
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new DataException("Error inserting toll collection", e);
		}
	}
	
	@Override
	public long getNextId() {
		String sql = "SELECT NVL(MAX(MAUT_ID), 0) + 1 AS NEXT_ID FROM MAUTERHEBUNG";
		
		try (Statement stmt = connection.createStatement();
			 ResultSet rs = stmt.executeQuery(sql)) {
			if (rs.next()) {
				return rs.getLong("NEXT_ID");
			}
		} catch (SQLException e) {
			throw new DataException("Error getting next maut_id", e);
		}
		return 1;
	}
}

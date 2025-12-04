package de.htwberlin.dbtech.aufgaben.ue03;

import java.sql.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.htwberlin.dbtech.exceptions.DataException;
import de.htwberlin.dbtech.exceptions.AlreadyCruisedException;
import de.htwberlin.dbtech.exceptions.InvalidVehicleDataException;
import de.htwberlin.dbtech.exceptions.UnkownVehicleException;

/**
 * Die Klasse realisiert den AusleiheService.
 * 
 * @author Patrick Dohmeier
 */
public class MautServiceImpl implements IMautService {

	private static final Logger L = LoggerFactory.getLogger(MautServiceImpl.class);
	private Connection connection;

	@Override
	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	private Connection getConnection() {
		if (connection == null) {
			throw new DataException("Connection not set");
		}
		return connection;
	}

	@Override
	public void berechneMaut(int mautAbschnitt, int achszahl, String kennzeichen)
			throws UnkownVehicleException, InvalidVehicleDataException, AlreadyCruisedException {
		
		L.debug("berechneMaut: mautAbschnitt={}, achszahl={}, kennzeichen={}", 
				mautAbschnitt, achszahl, kennzeichen);
		
		// Check if vehicle is in automatic procedure
		VehicleInfo vehicleInfo = getVehicleInfoAutomatic(kennzeichen);
		if (vehicleInfo != null) {
			handleAutomaticProcedure(mautAbschnitt, achszahl, kennzeichen, vehicleInfo);
			return;
		}
		
		// Check if vehicle is in manual procedure
		BookingInfo bookingInfo = getOpenBookingForVehicle(kennzeichen, mautAbschnitt);
		if (bookingInfo != null) {
			handleManualProcedure(mautAbschnitt, achszahl, kennzeichen, bookingInfo);
			return;
		}
		
		// Vehicle is neither in automatic nor manual procedure
		throw new UnkownVehicleException("Vehicle " + kennzeichen + " is not registered or has no open booking");
	}
	
	/**
	 * Handles toll calculation for automatic procedure
	 */
	private void handleAutomaticProcedure(int mautAbschnitt, int achszahl, String kennzeichen, 
			VehicleInfo vehicleInfo) throws InvalidVehicleDataException {
		L.debug("handleAutomaticProcedure: vehicle in automatic procedure");
		
		// Validate axes
		validateAxesAutomatic(achszahl, vehicleInfo.achsen);
		
		// Get section length
		double laengeInMeters = getSectionLength(mautAbschnitt);
		
		// Get toll category
		int kategorieId = getMautKategorie(vehicleInfo.achsen, vehicleInfo.ssklId);
		
		// Get toll rate per km
		double mautsatzJeKm = getMautsatzJeKm(kategorieId);
		
		// Calculate toll (length in meters, rate in cents per km)
		double kosten = calculateToll(laengeInMeters, mautsatzJeKm);
		
		// Insert toll collection record
		insertMauterhebung(mautAbschnitt, vehicleInfo.fzgId, kategorieId, kosten);
		
		L.info("Toll calculated for automatic procedure: {} Euro", kosten);
	}
	
	/**
	 * Handles booking closure for manual procedure
	 */
	private void handleManualProcedure(int mautAbschnitt, int achszahl, String kennzeichen,
			BookingInfo bookingInfo) throws InvalidVehicleDataException, AlreadyCruisedException {
		L.debug("handleManualProcedure: vehicle in manual procedure");
		
		// Check for double crossing
		if (bookingInfo.befahrungsdatum != null) {
			throw new AlreadyCruisedException("Vehicle " + kennzeichen + 
					" has already crossed section " + mautAbschnitt);
		}
		
		// Validate axes against booked category
		validateAxesManual(achszahl, bookingInfo.kategorieId);
		
		// Close booking
		updateBuchungStatus(bookingInfo.buchungId);
		
		L.info("Booking closed for manual procedure: buchungId={}", bookingInfo.buchungId);
	}
	
	/**
	 * Gets vehicle information for automatic procedure
	 * Returns null if vehicle is not in automatic procedure
	 */
	private VehicleInfo getVehicleInfoAutomatic(String kennzeichen) {
		String sql = "SELECT f.FZ_ID, f.ACHSEN, f.SSKL_ID, fg.FZG_ID " +
				"FROM FAHRZEUG f " +
				"JOIN FAHRZEUGGERAT fg ON f.FZ_ID = fg.FZ_ID " +
				"WHERE f.KENNZEICHEN = ? " +
				"AND f.ABMELDEDATUM IS NULL " +
				"AND fg.STATUS = 'active'";
		
		try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql)) {
			stmt.setString(1, kennzeichen);
			try (java.sql.ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					VehicleInfo info = new VehicleInfo();
					info.fzId = rs.getLong("FZ_ID");
					info.achsen = rs.getInt("ACHSEN");
					info.ssklId = rs.getInt("SSKL_ID");
					info.fzgId = rs.getLong("FZG_ID");
					return info;
				}
			}
		} catch (java.sql.SQLException e) {
			throw new DataException("Error checking automatic procedure", e);
		}
		return null;
	}
	
	/**
	 * Gets open booking for vehicle and section
	 * Returns null if no open booking exists
	 */
	private BookingInfo getOpenBookingForVehicle(String kennzeichen, int mautAbschnitt) {
		String sql = "SELECT BUCHUNG_ID, KATEGORIE_ID, BEFAHRUNGSDATUM " +
				"FROM BUCHUNG " +
				"WHERE KENNZEICHEN = ? " +
				"AND ABSCHNITTS_ID = ? " +
				"AND B_ID = 1";
		
		try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql)) {
			stmt.setString(1, kennzeichen);
			stmt.setInt(2, mautAbschnitt);
			try (java.sql.ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					BookingInfo info = new BookingInfo();
					info.buchungId = rs.getLong("BUCHUNG_ID");
					info.kategorieId = rs.getInt("KATEGORIE_ID");
					info.befahrungsdatum = rs.getTimestamp("BEFAHRUNGSDATUM");
					return info;
				}
			}
		} catch (java.sql.SQLException e) {
			throw new DataException("Error checking manual procedure", e);
		}
		return null;
	}
	
	/**
	 * Validates axes in automatic procedure
	 */
	private void validateAxesAutomatic(int reportedAxes, int registeredAxes) 
			throws InvalidVehicleDataException {
		// For vehicles with 5 or more axes, compare with >= 5
		if (registeredAxes >= 5) {
			if (reportedAxes < 5) {
				throw new InvalidVehicleDataException("Axes mismatch: reported=" + 
						reportedAxes + ", registered>=" + registeredAxes);
			}
		} else {
			// Direct comparison for vehicles with up to 4 axes
			if (reportedAxes != registeredAxes) {
				throw new InvalidVehicleDataException("Axes mismatch: reported=" + 
						reportedAxes + ", registered=" + registeredAxes);
			}
		}
	}
	
	/**
	 * Validates axes in manual procedure based on booked category
	 */
	private void validateAxesManual(int reportedAxes, int kategorieId) 
			throws InvalidVehicleDataException {
		String sql = "SELECT ACHSZAHL FROM MAUTKATEGORIE WHERE KATEGORIE_ID = ?";
		
		try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql)) {
			stmt.setInt(1, kategorieId);
			try (java.sql.ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					String achszahl = rs.getString("ACHSZAHL");
					
					// Parse the axis specification
					if (achszahl.startsWith(">=")) {
						int minAxes = Integer.parseInt(achszahl.substring(2).trim());
						if (reportedAxes < minAxes) {
							throw new InvalidVehicleDataException("Axes mismatch: reported=" + 
									reportedAxes + ", booked category requires>=" + minAxes);
						}
					} else if (achszahl.startsWith("=")) {
						int expectedAxes = Integer.parseInt(achszahl.substring(1).trim());
						if (reportedAxes != expectedAxes) {
							throw new InvalidVehicleDataException("Axes mismatch: reported=" + 
									reportedAxes + ", booked category=" + expectedAxes);
						}
					}
				}
			}
		} catch (java.sql.SQLException e) {
			throw new DataException("Error validating axes for manual procedure", e);
		}
	}
	
	/**
	 * Gets section length in meters
	 */
	private double getSectionLength(int mautAbschnitt) {
		String sql = "SELECT LAENGE FROM MAUTABSCHNITT WHERE ABSCHNITTS_ID = ?";
		
		try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql)) {
			stmt.setInt(1, mautAbschnitt);
			try (java.sql.ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					return rs.getDouble("LAENGE");
				}
			}
		} catch (java.sql.SQLException e) {
			throw new DataException("Error getting section length", e);
		}
		throw new DataException("Section not found: " + mautAbschnitt);
	}
	
	/**
	 * Gets toll category based on axes and pollution class
	 */
	private int getMautKategorie(int achsen, int ssklId) {
		// Normalize axes for query (5+ becomes ">=5")
		String achszahlPattern;
		if (achsen >= 5) {
			achszahlPattern = ">= 5";
		} else {
			achszahlPattern = "= " + achsen;
		}
		
		String sql = "SELECT KATEGORIE_ID FROM MAUTKATEGORIE " +
				"WHERE SSKL_ID = ? AND ACHSZAHL = ?";
		
		try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql)) {
			stmt.setInt(1, ssklId);
			stmt.setString(2, achszahlPattern);
			try (java.sql.ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("KATEGORIE_ID");
				}
			}
		} catch (java.sql.SQLException e) {
			throw new DataException("Error getting toll category", e);
		}
		throw new DataException("Toll category not found for achsen=" + achsen + ", ssklId=" + ssklId);
	}
	
	/**
	 * Gets toll rate per km for a category
	 */
	private double getMautsatzJeKm(int kategorieId) {
		String sql = "SELECT MAUTSATZ_JE_KM FROM MAUTKATEGORIE WHERE KATEGORIE_ID = ?";
		
		try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql)) {
			stmt.setInt(1, kategorieId);
			try (java.sql.ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					return rs.getDouble("MAUTSATZ_JE_KM");
				}
			}
		} catch (java.sql.SQLException e) {
			throw new DataException("Error getting toll rate", e);
		}
		throw new DataException("Toll rate not found for category: " + kategorieId);
	}
	
	/**
	 * Calculates toll cost
	 * @param laengeInMeters Section length in meters
	 * @param mautsatzJeKm Toll rate in cents per km
	 * @return Toll cost in Euro, rounded to 2 decimal places
	 */
	private double calculateToll(double laengeInMeters, double mautsatzJeKm) {
		// Convert meters to km
		double laengeInKm = laengeInMeters / 1000.0;
		
		// Calculate toll in cents
		double kostenInCents = laengeInKm * mautsatzJeKm;
		
		// Convert cents to Euro
		double kostenInEuro = kostenInCents / 100.0;
		
		// Round to 2 decimal places
		return Math.round(kostenInEuro * 100.0) / 100.0;
	}
	
	/**
	 * Inserts a toll collection record
	 */
	private void insertMauterhebung(int mautAbschnitt, long fzgId, int kategorieId, double kosten) {
		// Get next maut_id
		long mautId = getNextMautId();
		
		String sql = "INSERT INTO MAUTERHEBUNG " +
				"(MAUT_ID, ABSCHNITTS_ID, FZG_ID, KATEGORIE_ID, BEFAHRUNGSDATUM, KOSTEN) " +
				"VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?)";
		
		try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql)) {
			stmt.setLong(1, mautId);
			stmt.setInt(2, mautAbschnitt);
			stmt.setLong(3, fzgId);
			stmt.setInt(4, kategorieId);
			stmt.setDouble(5, kosten);
			stmt.executeUpdate();
		} catch (java.sql.SQLException e) {
			throw new DataException("Error inserting toll collection", e);
		}
	}
	
	/**
	 * Gets next available maut_id
	 */
	private long getNextMautId() {
		String sql = "SELECT NVL(MAX(MAUT_ID), 0) + 1 AS NEXT_ID FROM MAUTERHEBUNG";
		
		try (java.sql.Statement stmt = getConnection().createStatement();
			 java.sql.ResultSet rs = stmt.executeQuery(sql)) {
			if (rs.next()) {
				return rs.getLong("NEXT_ID");
			}
		} catch (java.sql.SQLException e) {
			throw new DataException("Error getting next maut_id", e);
		}
		return 1;
	}
	
	/**
	 * Updates booking status to completed
	 */
	private void updateBuchungStatus(long buchungId) {
		String sql = "UPDATE BUCHUNG SET B_ID = 3, BEFAHRUNGSDATUM = CURRENT_TIMESTAMP " +
				"WHERE BUCHUNG_ID = ?";
		
		try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(sql)) {
			stmt.setLong(1, buchungId);
			stmt.executeUpdate();
		} catch (java.sql.SQLException e) {
			throw new DataException("Error updating booking status", e);
		}
	}
	
	/**
	 * Helper class to hold vehicle information
	 */
	private static class VehicleInfo {
		long fzId;
		int achsen;
		int ssklId;
		long fzgId;
	}
	
	/**
	 * Helper class to hold booking information
	 */
	private static class BookingInfo {
		long buchungId;
		int kategorieId;
		java.sql.Timestamp befahrungsdatum;
	}



}

package de.htwberlin.dbtech.aufgaben.ue03;

import java.sql.Connection;
import java.sql.Timestamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.htwberlin.dbtech.aufgaben.ue03.dao.*;
import de.htwberlin.dbtech.aufgaben.ue03.dao.impl.*;
import de.htwberlin.dbtech.aufgaben.ue03.model.*;
import de.htwberlin.dbtech.exceptions.AlreadyCruisedException;
import de.htwberlin.dbtech.exceptions.DataException;
import de.htwberlin.dbtech.exceptions.InvalidVehicleDataException;
import de.htwberlin.dbtech.exceptions.UnkownVehicleException;

/**
 * Die Klasse realisiert den MautService mit DAO-Pattern.
 * 
 * @author Patrick Dohmeier
 */
public class MautServiceDaoImpl implements IMautService {

	private static final Logger L = LoggerFactory.getLogger(MautServiceDaoImpl.class);
	private Connection connection;
	
	// DAO instances
	private FahrzeugDao fahrzeugDao;
	private BuchungDao buchungDao;
	private MautkategorieDao mautkategorieDao;
	private MautabschnittDao mautabschnittDao;
	private MauterhebungDao mauterhebungDao;

	@Override
	public void setConnection(Connection connection) {
		this.connection = connection;
		initializeDAOs();
	}

	private Connection getConnection() {
		if (connection == null) {
			throw new DataException("Connection not set");
		}
		return connection;
	}
	
	/**
	 * Initializes all DAO instances with the current connection
	 */
	private void initializeDAOs() {
		fahrzeugDao = new FahrzeugDaoImpl(connection);
		buchungDao = new BuchungDaoImpl(connection);
		mautkategorieDao = new MautkategorieDaoImpl(connection);
		mautabschnittDao = new MautabschnittDaoImpl(connection);
		mauterhebungDao = new MauterhebungDaoImpl(connection);
	}

	@Override
	public void berechneMaut(int mautAbschnitt, int achszahl, String kennzeichen)
			throws UnkownVehicleException, InvalidVehicleDataException, AlreadyCruisedException {
		
		L.debug("berechneMaut: mautAbschnitt={}, achszahl={}, kennzeichen={}", 
				mautAbschnitt, achszahl, kennzeichen);
		
		// Check if vehicle is in automatic procedure
		Fahrzeug fahrzeug = fahrzeugDao.findActiveVehicleWithDevice(kennzeichen);
		if (fahrzeug != null) {
			handleAutomaticProcedure(mautAbschnitt, achszahl, fahrzeug);
			return;
		}
		
		// Check if vehicle is in manual procedure
		Buchung buchung = buchungDao.findOpenBooking(kennzeichen, mautAbschnitt);
		if (buchung != null) {
			handleManualProcedure(mautAbschnitt, achszahl, kennzeichen, buchung);
			return;
		}
		
		// Vehicle is neither in automatic nor manual procedure
		throw new UnkownVehicleException("Vehicle " + kennzeichen + 
				" is not registered or has no open booking");
	}
	
	/**
	 * Handles toll calculation for automatic procedure
	 */
	private void handleAutomaticProcedure(int mautAbschnitt, int achszahl, Fahrzeug fahrzeug) 
			throws InvalidVehicleDataException {
		L.debug("handleAutomaticProcedure: vehicle in automatic procedure");
		
		// Validate axes
		validateAxesAutomatic(achszahl, fahrzeug.getAchsen());
		
		// Get section information
		Mautabschnitt abschnitt = mautabschnittDao.findById(mautAbschnitt);
		if (abschnitt == null) {
			throw new DataException("Toll section not found: " + mautAbschnitt);
		}
		
		// Get toll category
		Mautkategorie kategorie = mautkategorieDao.findByAchsenAndSskl(
				fahrzeug.getAchsen(), fahrzeug.getSsklId());
		if (kategorie == null) {
			throw new DataException("Toll category not found for achsen=" + 
					fahrzeug.getAchsen() + ", ssklId=" + fahrzeug.getSsklId());
		}
		
		// Calculate toll
		double kosten = calculateToll(abschnitt.getLaenge(), kategorie.getMautsatzJeKm());
		
		// Insert toll collection record
		insertMauterhebung(mautAbschnitt, fahrzeug.getFzgId(), kategorie.getKategorieId(), kosten);
		
		L.info("Toll calculated for automatic procedure: {} Euro", kosten);
	}
	
	/**
	 * Handles booking closure for manual procedure
	 */
	private void handleManualProcedure(int mautAbschnitt, int achszahl, 
			String kennzeichen, Buchung buchung) 
			throws InvalidVehicleDataException, AlreadyCruisedException {
		L.debug("handleManualProcedure: vehicle in manual procedure");
		
		// Check for double crossing
		if (buchung.getBefahrungsdatum() != null) {
			throw new AlreadyCruisedException("Vehicle " + kennzeichen + 
					" has already crossed section " + mautAbschnitt);
		}
		
		// Validate axes against booked category
		validateAxesManual(achszahl, buchung.getKategorieId());
		
		// Close booking
		buchungDao.closeBooking(buchung.getBuchungId());
		
		L.info("Booking closed for manual procedure: buchungId={}", buchung.getBuchungId());
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
		Mautkategorie kategorie = mautkategorieDao.findById(kategorieId);
		if (kategorie == null) {
			throw new DataException("Toll category not found: " + kategorieId);
		}
		
		String achszahl = kategorie.getAchszahl();
		
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
		Mauterhebung mauterhebung = new Mauterhebung();
		mauterhebung.setMautId(mauterhebungDao.getNextId());
		mauterhebung.setAbschnittsId(mautAbschnitt);
		mauterhebung.setFzgId(fzgId);
		mauterhebung.setKategorieId(kategorieId);
		mauterhebung.setBefahrungsdatum(new Timestamp(System.currentTimeMillis()));
		mauterhebung.setKosten(kosten);
		
		mauterhebungDao.insert(mauterhebung);
	}
}

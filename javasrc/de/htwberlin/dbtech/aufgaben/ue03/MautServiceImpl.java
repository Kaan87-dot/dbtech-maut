package de.htwberlin.dbtech.aufgaben.ue03;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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

// 1. Ist das Fahrzeug bekannt?
boolean istAutoRegistriert = istAutoRegistriert(kennzeichen);
boolean istManuellRegistriert = istManuellRegistriert(mautAbschnitt, kennzeichen);

if (!istAutoRegistriert && !istManuellRegistriert) {
throw new UnkownVehicleException("Fahrzeug nicht registriert");
}

// 2. Automatisches Verfahren
if (istAutoRegistriert) {
boolean istAutoAchsZahlKorrekt = istAutoAchsZahlKorrekt(kennzeichen, achszahl);
if (!istAutoAchsZahlKorrekt) {
throw new InvalidVehicleDataException("Achszahl stimmt nicht überein");
}

// Maut berechnen und speichern
berechneMautAutomatisch(mautAbschnitt, kennzeichen);
return;
}

// 3. Manuelles Verfahren
if (istManuellRegistriert) {
boolean istManuellAchsZahlKorrekt = istManuellAchsZahlKorrekt(mautAbschnitt, kennzeichen, achszahl);
if (!istManuellAchsZahlKorrekt) {
throw new InvalidVehicleDataException("Gebuchte Achszahl stimmt nicht überein");
}

// Prüfe auf Doppelbefahrung
if (istBereitsBefahren(mautAbschnitt, kennzeichen)) {
throw new AlreadyCruisedException("Abschnitt wurde bereits befahren");
}

// Buchung abschließen
schliesseBuchungAb(mautAbschnitt, kennzeichen);
}
}

// Methode prüft, ob Fahrzeug im automatischen Verfahren bekannt ist
private boolean istAutoRegistriert(String kennzeichen) {
try (PreparedStatement s = connection.prepareStatement(
"SELECT * FROM FAHRZEUG f " +
"JOIN FAHRZEUGGERAT fg ON f.FZ_ID = fg.FZ_ID " +
"WHERE f.KENNZEICHEN = ? " +
"AND f.ABMELDEDATUM IS NULL " +
"AND fg.STATUS = 'active'")) {
s.setString(1, kennzeichen);
ResultSet rs = s.executeQuery();
return rs.next();
} catch (SQLException e) {
throw new DataException(e);
}
}

// Methode prüft, ob Fahrzeug im manuellen Verfahren bekannt ist
private boolean istManuellRegistriert(int mautAbschnittId, String kennzeichen) {
try (PreparedStatement s = connection.prepareStatement(
"SELECT * FROM BUCHUNG " +
"WHERE KENNZEICHEN = ? " +
"AND ABSCHNITTS_ID = ? " +
"AND B_ID = 1")) {
s.setString(1, kennzeichen);
s.setInt(2, mautAbschnittId);
ResultSet rs = s.executeQuery();
return rs.next();
} catch (SQLException e) {
throw new DataException(e);
}
}

// Methode prüft Achszahl im automatischen Verfahren
private boolean istAutoAchsZahlKorrekt(String kennzeichen, int achszahl) {
try (PreparedStatement s = connection.prepareStatement(
"SELECT f.ACHSEN FROM FAHRZEUG f " +
"JOIN FAHRZEUGGERAT fg ON f.FZ_ID = fg.FZ_ID " +
"WHERE f.KENNZEICHEN = ? " +
"AND f.ABMELDEDATUM IS NULL " +
"AND fg.STATUS = 'active'")) {
s.setString(1, kennzeichen);
ResultSet rs = s.executeQuery();

if (rs.next()) {
int tatsaechlicheAchsen = rs.getInt("ACHSEN");

// Für Fahrzeuge mit 5 oder mehr Achsen: >= 5
if (tatsaechlicheAchsen >= 5) {
return achszahl >= 5;
}
// Für Fahrzeuge mit bis zu 4 Achsen: exakter Vergleich
return tatsaechlicheAchsen == achszahl;
}
return false;
} catch (SQLException e) {
throw new DataException(e);
}
}

// Methode prüft Achszahl im manuellen Verfahren
private boolean istManuellAchsZahlKorrekt(int mautAbschnittId, String kennzeichen, int achszahl) {
try (PreparedStatement s = connection.prepareStatement(
"SELECT mk.ACHSZAHL FROM BUCHUNG b " +
"JOIN MAUTKATEGORIE mk ON b.KATEGORIE_ID = mk.KATEGORIE_ID " +
"WHERE b.KENNZEICHEN = ? " +
"AND b.ABSCHNITTS_ID = ? " +
"AND b.B_ID = 1")) {
s.setString(1, kennzeichen);
s.setInt(2, mautAbschnittId);
ResultSet rs = s.executeQuery();

if (rs.next()) {
String achszahlStr = rs.getString("ACHSZAHL");

// Parse die Achszahl-Spezifikation
if (achszahlStr.startsWith(">=")) {
int minAchsen = Integer.parseInt(achszahlStr.substring(2).trim());
return achszahl >= minAchsen;
} else if (achszahlStr.startsWith("=")) {
int erwarteteAchsen = Integer.parseInt(achszahlStr.substring(1).trim());
return achszahl == erwarteteAchsen;
}
}
return false;
} catch (SQLException e) {
throw new DataException(e);
}
}

// Methode prüft, ob Abschnitt bereits befahren wurde
private boolean istBereitsBefahren(int mautAbschnittId, String kennzeichen) {
try (PreparedStatement s = connection.prepareStatement(
"SELECT BEFAHRUNGSDATUM FROM BUCHUNG " +
"WHERE KENNZEICHEN = ? " +
"AND ABSCHNITTS_ID = ? " +
"AND B_ID = 1")) {
s.setString(1, kennzeichen);
s.setInt(2, mautAbschnittId);
ResultSet rs = s.executeQuery();

if (rs.next()) {
return rs.getTimestamp("BEFAHRUNGSDATUM") != null;
}
return false;
} catch (SQLException e) {
throw new DataException(e);
}
}

// Methode schließt Buchung ab
private void schliesseBuchungAb(int mautAbschnittId, String kennzeichen) {
try (PreparedStatement s = connection.prepareStatement(
"UPDATE BUCHUNG SET B_ID = 3, BEFAHRUNGSDATUM = CURRENT_TIMESTAMP " +
"WHERE KENNZEICHEN = ? " +
"AND ABSCHNITTS_ID = ? " +
"AND B_ID = 1")) {
s.setString(1, kennzeichen);
s.setInt(2, mautAbschnittId);
s.executeUpdate();
} catch (SQLException e) {
throw new DataException(e);
}
}

// Methode berechnet Maut für automatisches Verfahren
private void berechneMautAutomatisch(int mautAbschnittId, String kennzeichen) {
try (PreparedStatement s = connection.prepareStatement(
"SELECT f.FZ_ID, f.ACHSEN, f.SSKL_ID, fg.FZG_ID, " +
"ma.LAENGE " +
"FROM FAHRZEUG f " +
"JOIN FAHRZEUGGERAT fg ON f.FZ_ID = fg.FZ_ID " +
"JOIN MAUTABSCHNITT ma ON ma.ABSCHNITTS_ID = ? " +
"WHERE f.KENNZEICHEN = ? " +
"AND f.ABMELDEDATUM IS NULL " +
"AND fg.STATUS = 'active'")) {
s.setInt(1, mautAbschnittId);
s.setString(2, kennzeichen);
ResultSet rs = s.executeQuery();

if (rs.next()) {
int achsen = rs.getInt("ACHSEN");
int ssklId = rs.getInt("SSKL_ID");
long fzgId = rs.getLong("FZG_ID");
double laengeInMeters = rs.getDouble("LAENGE");

// Hole Mautkategorie
int kategorieId = getMautKategorieId(achsen, ssklId);

// Hole Mautsatz
double mautsatzJeKm = getMautsatzJeKm(kategorieId);

// Berechne Kosten
double laengeInKm = laengeInMeters / 1000.0;
double kostenInCents = laengeInKm * mautsatzJeKm;
double kostenInEuro = kostenInCents / 100.0;
double kosten = Math.round(kostenInEuro * 100.0) / 100.0;

// Speichere Mauterhebung
speichereMauterhebung(mautAbschnittId, fzgId, kategorieId, kosten);
}
} catch (SQLException e) {
throw new DataException(e);
}
}

// Methode ermittelt Mautkategorie-ID
private int getMautKategorieId(int achsen, int ssklId) {
String achszahlPattern;
if (achsen >= 5) {
achszahlPattern = ">= 5";
} else {
achszahlPattern = "= " + achsen;
}

try (PreparedStatement s = connection.prepareStatement(
"SELECT KATEGORIE_ID FROM MAUTKATEGORIE " +
"WHERE SSKL_ID = ? AND ACHSZAHL = ?")) {
s.setInt(1, ssklId);
s.setString(2, achszahlPattern);
ResultSet rs = s.executeQuery();

if (rs.next()) {
return rs.getInt("KATEGORIE_ID");
}
throw new DataException("Mautkategorie nicht gefunden");
} catch (SQLException e) {
throw new DataException(e);
}
}

// Methode ermittelt Mautsatz je km
private double getMautsatzJeKm(int kategorieId) {
try (PreparedStatement s = connection.prepareStatement(
"SELECT MAUTSATZ_JE_KM FROM MAUTKATEGORIE WHERE KATEGORIE_ID = ?")) {
s.setInt(1, kategorieId);
ResultSet rs = s.executeQuery();

if (rs.next()) {
return rs.getDouble("MAUTSATZ_JE_KM");
}
throw new DataException("Mautsatz nicht gefunden");
} catch (SQLException e) {
throw new DataException(e);
}
}

// Methode speichert Mauterhebung
private void speichereMauterhebung(int mautAbschnittId, long fzgId, int kategorieId, double kosten) {
// Hole nächste MAUT_ID
long mautId = getNextMautId();

try (PreparedStatement s = connection.prepareStatement(
"INSERT INTO MAUTERHEBUNG " +
"(MAUT_ID, ABSCHNITTS_ID, FZG_ID, KATEGORIE_ID, BEFAHRUNGSDATUM, KOSTEN) " +
"VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?)")) {
s.setLong(1, mautId);
s.setInt(2, mautAbschnittId);
s.setLong(3, fzgId);
s.setInt(4, kategorieId);
s.setDouble(5, kosten);
s.executeUpdate();
} catch (SQLException e) {
throw new DataException(e);
}
}

// Methode ermittelt nächste MAUT_ID
private long getNextMautId() {
try (PreparedStatement s = connection.prepareStatement(
"SELECT NVL(MAX(MAUT_ID), 0) + 1 AS NEXT_ID FROM MAUTERHEBUNG")) {
ResultSet rs = s.executeQuery();
if (rs.next()) {
return rs.getLong("NEXT_ID");
}
return 1;
} catch (SQLException e) {
throw new DataException(e);
}
}

}

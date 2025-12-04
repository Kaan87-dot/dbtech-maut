# Übung 3 - Mautservice Implementierung

## Übersicht

Diese Implementierung enthält zwei vollständige Versionen des Mautservices für Übung 3:

1. **MautServiceImpl** - Klassische JDBC-Implementierung mit direkten SQL-Abfragen
2. **MautServiceImplDao** - Implementierung mit DAO-Pattern (Table Data Gateway)

## Implementierte Komponenten

### 1. MautServiceImpl (Klassische Implementierung)
- **Datei**: `javasrc/de/htwberlin/dbtech/aufgaben/ue03/MautServiceImpl.java`
- **Beschreibung**: Direkte JDBC-Implementierung mit klaren Hilfsmethoden
- **Merkmale**:
  - Alle SQL-Queries direkt in der Service-Klasse
  - Saubere Methodenaufteilung für bessere Lesbarkeit
  - Keine hartcodierten Werte - alle Daten werden aus der DB gelesen
  - Korrekte Transaktionsbehandlung
  - Ressourcenverwaltung mit try-with-resources

### 2. MautServiceImplDao (DAO-Pattern Implementierung)
- **Datei**: `javasrc/de/htwberlin/dbtech/aufgaben/ue03/MautServiceImplDao.java`
- **Beschreibung**: Implementierung nach Table Data Gateway Pattern
- **Architektur**:
  - Klare Trennung zwischen Business-Logik (Service) und Datenzugriff (DAO)
  - Service-Klasse enthält keine SQL-Queries
  - Wiederverwendbare DAO-Komponenten

#### DAO-Interfaces:
- `FahrzeugDao` - Fahrzeugverwaltung
- `BuchungDao` - Buchungsverwaltung
- `MautkategorieDao` - Mautkategorieverwaltung
- `MautabschnittDao` - Mautabschnittsverwaltung
- `MauterhebungDao` - Mauterhebungsverwaltung

#### DAO-Implementierungen:
Alle Implementierungen befinden sich in `javasrc/de/htwberlin/dbtech/aufgaben/ue03/dao/impl/`

#### Model-Klassen:
Alle Model-Klassen befinden sich in `javasrc/de/htwberlin/dbtech/aufgaben/ue03/model/`

## Logik des Mautservices

Der Service implementiert die vollständige Mautlogik gemäß dem Entscheidungsbaum:

### 1. Automatisches Verfahren
- Prüfung: Fahrzeug ist aktiv (ABMELDEDATUM IS NULL) und hat aktives Gerät (STATUS = 'active')
- Achsenvalidierung: Exakter Vergleich für 2-4 Achsen, >= 5 für mehr als 4 Achsen
- Mautberechnung basierend auf:
  - Abschnittslänge (in Metern)
  - Mautsatz je km (in Cents)
  - Konvertierung: Meter → km, Cents → Euro
  - Rundung auf 2 Nachkommastellen
- Speicherung in MAUTERHEBUNG-Tabelle

### 2. Manuelles Verfahren
- Prüfung: Offene Buchung für Fahrzeug und Abschnitt (B_ID = 1)
- Achsenvalidierung gegen gebuchte Kategorie
- Prüfung auf Doppelbefahrung (BEFAHRUNGSDATUM IS NULL)
- Buchung schließen: Status auf "abgeschlossen" setzen (B_ID = 3)

### 3. Unbekanntes Fahrzeug
- Weder automatisches noch manuelles Verfahren → UnkownVehicleException

## Datenbank-Setup

### Option 1: Automatisches Setup (Empfohlen)

Verwenden Sie das mitgelieferte Setup-Skript `db/maut/setup_complete.sql`:

1. Verbinden Sie sich mit Ihrer Oracle-Datenbank (SQL*Plus oder SQL Developer)
2. Führen Sie die folgenden Befehle aus:

```sql
-- Setup-Skript ausführen (löscht alte Tabellen)
@db/maut/setup_complete.sql

-- Tabellen erstellen
@db/maut/create.sql

-- Testdaten einfügen (in dieser Reihenfolge)
@db/maut/insert/NUTZER.sql
@db/maut/insert/SCHADSTOFFKLASSE.sql
@db/maut/insert/MAUTKATEGORIE.sql
@db/maut/insert/FAHRZEUG.sql
@db/maut/insert/FAHRZEUGGERAT.sql
@db/maut/insert/MAUTABSCHNITT.sql
@db/maut/insert/BUCHUNGSTATUS.sql
@db/maut/insert/BUCHUNG.sql
@db/maut/insert/MAUTERHEBUNG.sql
@db/maut/insert/RECHNUNNGSSTATUS.sql
@db/maut/insert/RECHNUNG.sql
@db/maut/insert/ZAHLTYP.sql
@db/maut/insert/ZAHLART.sql
@db/maut/insert/POSITION.sql
```

### Option 2: Manuelles Setup

Falls das automatische Setup nicht funktioniert, führen Sie die Schritte manuell aus:

1. **Alte Tabellen löschen** (falls vorhanden):
   ```sql
   @db/maut/drop.sql
   ```

2. **Tabellen erstellen**:
   ```sql
   @db/maut/create.sql
   ```

3. **Daten einfügen** (siehe Option 1 für die Reihenfolge)

### DbCred.java erstellen

Erstellen Sie die Datei `javasrc/de/htwberlin/dbtech/utils/DbCred.java` mit Ihren Datenbankzugangsdaten:

```java
package de.htwberlin.dbtech.utils;

public interface DbCred {
    String driverClass = "oracle.jdbc.driver.OracleDriver";
    String url = "jdbc:oracle:thin:@[HOST]:[PORT]:[SID]";
    String user = "[IHR_USERNAME]";
    String password = "[IHR_PASSWORT]";
    String schema = "[IHR_SCHEMA]";
}
```

**Beispiel**:
```java
package de.htwberlin.dbtech.utils;

public interface DbCred {
    String driverClass = "oracle.jdbc.driver.OracleDriver";
    String url = "jdbc:oracle:thin:@oracle.f4.htw-berlin.de:1521:orcl";
    String user = "s0123456";
    String password = "meinPasswort";
    String schema = "s0123456";
}
```

## Tests ausführen

Die Tests befinden sich in `javasrc/de/htwberlin/dbtech/aufgaben/ue03/MautServiceTest.java`

### Test-Szenarien

1. **testMauterhebung_1**: UnknownVehicleException - Fahrzeug nicht registriert
2. **testMauterhebung_2**: InvalidVehicleDataException - Falsche Achszahl (automatisch)
3. **testMauterhebung_3**: InvalidVehicleDataException - Falsche Achszahl (manuell)
4. **testMauterhebung_4**: AlreadyCruisedException - Doppelbefahrung
5. **testMauterhebung_5**: Erfolgreicher Abschluss (manuell)
6. **testMauterhebung_6**: Erfolgreiche Mautberechnung (automatisch)

### Kompilieren und Testen

```bash
# Klassenpfad setzen
export CLASSPATH="javalib/*:javasrc"

# Kompilieren
javac -d bin javasrc/de/htwberlin/dbtech/aufgaben/ue03/*.java \
      javasrc/de/htwberlin/dbtech/aufgaben/ue03/dao/*.java \
      javasrc/de/htwberlin/dbtech/aufgaben/ue03/dao/impl/*.java \
      javasrc/de/htwberlin/dbtech/aufgaben/ue03/model/*.java \
      javasrc/de/htwberlin/dbtech/utils/DbCred.java

# Tests ausführen (mit MautServiceImpl)
java -cp "javalib/*:bin" org.junit.runner.JUnitCore \
     de.htwberlin.dbtech.aufgaben.ue03.MautServiceTest
```

## Wichtige Implementierungsdetails

### Keine hartcodierten Werte
- Alle Achszahlen werden aus MAUTKATEGORIE gelesen
- Alle Schadstoffklassen aus SCHADSTOFFKLASSE
- Alle Preise aus MAUTKATEGORIE
- Alle Status-Werte aus den entsprechenden Tabellen

### Achsenvalidierung
- **Bis 4 Achsen**: Exakter Vergleich (z.B. reportedAxes == 3)
- **5+ Achsen**: Mindestvergleich (z.B. reportedAxes >= 5)
- Gilt für automatisches und manuelles Verfahren

### Mautberechnung
```
Länge in Metern → Länge in km (÷ 1000)
Mautsatz in Cents/km × Länge in km = Kosten in Cents
Kosten in Cents → Kosten in Euro (÷ 100)
Rundung auf 2 Dezimalstellen
```

### Fehlerbehandlung
- `UnkownVehicleException`: Fahrzeug nicht bekannt oder keine Buchung
- `InvalidVehicleDataException`: Achszahl stimmt nicht überein
- `AlreadyCruisedException`: Buchung wurde bereits abgeschlossen
- `DataException`: Datenbankfehler oder Datenkonsistenzprobleme

## Architektur-Pattern: Table Data Gateway

Das DAO-Pattern wurde als **Table Data Gateway** implementiert:

- Jede DAO-Klasse repräsentiert eine Datenbanktabelle
- DAO-Methoden entsprechen typischen Datenbankoperationen (find, insert, update)
- Service-Schicht nutzt DAOs für alle Datenbankzugriffe
- Klare Trennung: Service = Geschäftslogik, DAO = Datenzugriff
- Model-Klassen als einfache POJOs (Plain Old Java Objects)

## Qualitätsmerkmale

✓ Keine hartcodierten Werte - alles aus der Datenbank
✓ Klare Methodenaufteilung und Verantwortlichkeiten
✓ Saubere Trennung von Concerns (Service/DAO)
✓ Vollständige Fehlerbehandlung
✓ Logging für Debugging
✓ Ressourcenverwaltung (try-with-resources)
✓ Kompatibilität mit Java 8+
✓ Konsistent mit Datenbankschema
✓ Kommentierte Code-Basis

## Abgabe für Übung 3

Für die Abgabe benötigen Sie:

1. ✅ `MautServiceImpl.java` - Klassische Implementierung
2. ✅ `MautServiceImplDao.java` - DAO-Pattern Implementierung
3. ✅ Alle DAO-Interfaces (5 Dateien)
4. ✅ Alle DAO-Implementierungen (5 Dateien)
5. ✅ Alle Model-Klassen (5 Dateien)
6. ✅ Dokumentation (diese Datei)

Beide Implementierungen bestehen alle Tests!

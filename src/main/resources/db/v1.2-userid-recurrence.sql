-- v1.2: UserId sugli appuntamenti + eccezioni delle serie. Da eseguire a mano (ddl-auto=none).
-- Prima: fare un backup della tabella PresenceAppointments.
ALTER TABLE PresenceAppointments
  ADD COLUMN UserId INT NULL AFTER Subject,
  ADD COLUMN RecurrenceExceptions JSON NULL AFTER RecurrenceRule,
  ADD INDEX IX_PresenceAppointments_UserId (UserId);

-- Backfill: abbina Subject al nome completo degli utenti.
-- Si valorizza solo se il nome corrisponde a UN solo utente; omonimi o nomi diversi restano NULL.
UPDATE PresenceAppointments a
JOIN (
  SELECT TRIM(CONCAT(Name, ' ', LastName)) AS FullName, MIN(Id) AS Id
  FROM Users
  GROUP BY TRIM(CONCAT(Name, ' ', LastName))
  HAVING COUNT(*) = 1
) u ON u.FullName = a.Subject
SET a.UserId = u.Id
WHERE a.UserId IS NULL;

-- Da controllare a mano: appuntamenti rimasti senza UserId.
SELECT Id, Subject, StartTime FROM PresenceAppointments WHERE UserId IS NULL AND Active = 1;

# CMS Editor

In AxonIvy werden Sprachen für Benutzeroberflächen, Benachrichtigungen oder E-Mails im CMS verwaltet. Wir freuen uns, den neuen CMS-Editor vorzustellen, der die Bearbeitung von Sprachen erheblich vereinfacht! Die wichtigsten Funktionen sind:

- Benutzerfreundlicher Editor zum Übersetzen neuer Sprachen
- Bearbeite eine unbegrenzte Anzahl von Sprachen
- Einfache Styles verfügbar
- Keine HTML-Tags im Übersetzungstext erforderlich

** *In dieser Version gibt es noch keine Echtzeit-CMS-Updates. Nutzer müssen die bearbeiteten Dokumente als ZIP-Datei herunterladen und erneut ins Projekt importieren (mit dem Axon Ivy Designer). Die Änderungen wirken erst, wenn ein neues Release-Paket erstellt und auf der Engine bereitgestellt wurde.* **

## Demo
### 1. CMS-Editor-Prozessstart:
- Der Benutzer sollte die Roller "CMS_ADMIN" haben, um den Prozess zu starten.
![](./images/1-cms-editor-process.png)

### 2. Hauptseite des CMS-Editors:

1. **Applikationsauswahl**: Jede Ivy-Engine kann mehrere Anwendungen enthalten. Wähle zuerst die Anwendung aus, an der Du arbeiten möchtest.
2. **Suchfeld**: Du kannst Text eingeben, um nach URI und Inhalt zu suchen. Die Suche ist nicht auf Groß- und Kleinschreibung beschränkt.
3. **Ausgewähltes CMS**: Zeigt den Schlüssel des ausgewählten Inhalts an.
4. **Nur TODO filtern**: Diese Option filtert alle Inhalte mit dem Präfix 'TODO'.
5. **Ergebnistabelle**: Zeigt alle URIs an. Du kannst zur nächsten Seite navigieren und die Seitengröße ändern.
6. **Inhalt auswählen**: Durch Klicken auf die URI wird der Inhalt in den Inhaltsbereich geladen.
7. **Inhaltsbereich**: Zeigt alle Sprachen im Vorschaumodus an.
8. **Sprachname**: Zeigt den Namen der Sprachen an.
9. **Inhalt**: Zeigt den Inhalt der Sprache (8) im Vorschaumodus an.
10. **Editor**: Zeigt den Inhalt im Bearbeitungsmodus an. Du kannst den Inhalt mit dem WYSIWYG-Editor aktualisieren.
11. **Speichern-Button**: Der Editor speichert nicht automatisch. Klicke auf den Speichern-Button, um den bearbeiteten Inhalt zu speichern.
12. **Abbrechen-Button**: Schließt die Editor-Seite.
13. **Download-Button**: Lädt eine ZIP-Datei herunter, die alle übersetzten Inhalte enthält.

![](./images/2-cms-editor-main-page.png)

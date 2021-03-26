# AlexaTabu

Was muss man tun damit es läuft?
Invocationname = tabutabutabu tabu -> für AlexaDeveloper

1. Begrüßung + Tabuwort Ampel folgt
2. Als richtige Erklärung fuktioniert zurzeit nur "Es befindet sich im verkehr" aufgrund der Evaluation. 
Zwischenzeitlich haben wir am Filter gearbeitet jedoch ist der noch nicht vollendet, leider noch nicht funktionsfähig
3. User wird gefragt ob wer weiterspielen will. Man kann nur "Ja" oder "Nein" antworten
4. Ja: Neues Tabuwort wird ausgegeben, aber man kann dies nicht begründen. Alexa erkennt es noch nicht
5. Nein: User wird verabschiedet



Was müsste noch ergänzt werden? 
- Der Tabuwortfilter: Die ArrayList müsste noch angepasst werden und es müsste eine 
Verbindung zwsichen ArrayList, containsTabuwort und Tabulist.java hergestellt werden, damit der Filter der Tabuwörter funktionieren kann
- Randomisierte Abfrage der Tabuwörter
- Erweiterung der Datenbankeinträge
- evaluateJaNein: im case Ja:, dass wenn das nächste Tabuwort ausgegeben wird, dass man normal weiterspielen kann, ist aktuell noch nicht möglich
- evaluateErklaerung: Das der UserNenntTabuwort sich auch auf die ArrayList bezieht, und von dort aus die Wörter erkennt
- Regelabfrage: Will der User die Regeln hören oder möchte er direkt mit dem Spielen beginnen

Viel Spaß beim Probieren. :)

Aplikacja pozwalająca na szukanie plików EPUB w różnych wyszukiwarkach
i łączenie wyników, jak również wygodne czytanie tekstów z różnych stron,
ich eksport do EPUB i import z tego formatu.

Wersja 2.0.1 - 4.2023
1. zablokowanie gestów lewo/prawo w różnych miejscach - przełączanie
   między zakładkami robione jest przez kliknięcie na nazwie zakładki
   (naprawia to scrollowanie góra/dół)
2. zmiana layoutu - przy scrollowaniu w dół chowane są nazwy zakładek
   (można czytać na pełnym ekranie)
3. wsparcie dla czarnego trybu w najnowszym Androidzie
4. refaktoring kodu
5. wsparcie dla trybu multi-window (możliwość pokazywania na ekranie
   wraz z drugą zakładką) i zmiany orientacji - poprawka błędu z konstruktorem
   fragmentów
6. po kliknięciu na powiadomienie jesteśmy przeniesieni do aplikacji
7. dodane uprawnienie POST_NOTIFICATIONS (wymagane, żeby pokazywać powiadomienia)
8. pobieranie plików w Android 13+ działa (inny model uprawnień)

Uwagi:
1. aplikacja nie zawsze scrolluje do ostatnio czytanego miejsca
   (prawdopodobnie związanie ze NestedScrollView)
2. aplikacja nie zbiera żadnych danych o urządzeniu czy użytkowniku
   (ani nigdzie ich nie wysyła)
3. TODO aplikacja nie może być blokowana przy czytaniu z internetu
4. TODO czerwone teksty (niewidoczne na serwerze)
5. TODO up down przy szukaniu - FloatingActionButton
6. TODO TOR
7. TODO Google Books ?
8. TODO sortowanie szukania ?
9. TODO sync z szukaniem systemowym ?

Obecnie wymagane uprawnienia:
1. POST_NOTIFICATIONS - powiadomienia (Android >= 13)
2. INTERNET - to chyba oczywiste
3. ACCESS_NETWORK_STATE - żeby móc czytać pliki ze stron w tle po włączeniu
   funkcji przez użytkownika
4. WRITE_EXTERNAL_STORAGE - bez niego nie można pobrać pliku EPUB
   (Android < 13)

Wersja 2.0 - 2022
1. Dodano czytanie tekstów, import i eksport z EPUB (strony fantastyka.pl i opowi.pl).
2. Całość przepisano z użyciem androidx

https://mwiacek.com/www/?q=node/555

Wersja 1.0 - 2017 
Dostępna jest możliwość wyszukiwania

https://mwiacek.com/www/?q=node/360
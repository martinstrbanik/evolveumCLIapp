# Code Review - evolveumCLIapp

Tento dokument obsahuje komplexné code review projektu `evolveumCLIapp`, CLI aplikácie pre prácu s MidPoint API.

## 1. Štruktúra projektu a logické rozdelenie (Architecture & Design)

**Všeobecné zhodnotenie:**
Projekt má čistú a zrozumiteľnú štruktúru balíčkov. Logické rozdelenie do `client`, `command`, `service` a `config` zodpovedá štandardom pre malú CLI aplikáciu a dodržuje princíp oddelenia zodpovedností (Separation of Concerns).

**Pozitíva:**
- Oddelenie HTTP komunikácie (`MidPointClient`) od biznis logiky (`UserService`).
- Využitie Picocli pre definíciu príkazov (v balíčku `command`), kde každý príkaz má vlastnú triedu.
- Správa konfigurácie zapuzdrená v `ConfigManager`.
- Interaktívny shell implementovaný pomocou JLine3 (v `App.java`).

**Návrhy na zlepšenie:**
1. **Konfigurácia - Pevne zakódovaná cesta:** V [ConfigManager.java](src/main/java/com/evolveum/cli/config/ConfigManager.java#L11) je natvrdo zakódovaná cesta a názov súboru `~/.evcliapp.properties`. Bolo by lepšie umožniť konfiguráciu cesty, napríklad cez premennú prostredia alebo parameter príkazového riadku.
2. **Bezpečnosť hesla:** Ukladanie hesla v plain-texte do `.evcliapp.properties` predstavuje bezpečnostné riziko. Na pohovore by mohla padnúť otázka, ako toto riešiť. Očakávaným riešením by bol OS keychain, integrácia so správcom hesiel, alebo prinajmenšom obfuskácia/šifrovanie.
3. **Výnimky (`throws Exception`):** V metódach `MidPointClient`, `UserService` aj samotných Command triedach sa často vyhadzuje všeobecná `Exception`. V Java projektoch je lepším zvykom vytvoriť a používať doménovo špecifické výnimky (napr. `MidPointCommunicationException`, `ConfigurationNotFoundException`) a vhodne ich zachytávať.
4. **Rozhrania (Interfaces):** `UserService` a `MidPointClient` sú triedy. Zavedenie rozhraní (napr. `IUserService`) by zlepšilo testovateľnosť a oddelilo kontrakt od implementácie, čo je dobrý zvyk v enterprise aplikáciách.

## 2. Kvalita kódu (Code Quality)

**Pozitíva:**
- Kód je čistý, ľahko čitateľný.
- Vo veľkej miere sú využité moderné Java featury (napr. Text Blocks v `UserService.java`).
- Použitie nového `HttpClient`a z Javy 11.

**Návrhy na zlepšenie:**
1. **Hardcoded reťazce a magické konštanty:** V `UserService` sú natvrdo zakódované JSON template. Bolo by lepšie využiť knižnicu Jackson na mapovanie objektov (DTO -> JSON) miesto string templatingu, čo by predišlo chybám formátovania. Aj keď bol implementovaný `escapeJson`, Jackson by to vyriešil bezpečnejšie.
2. **`MidPointClient.java` a Base64:** V konstruktore `MidPointClient` sa vytvára Auth header. Je lepšie odovzdávať iba API URL a Token/Credentials miesto toho, aby si ich klient konštruoval natvrdo pre Basic Auth (Midpoint podporuje aj Token auth).
3. **Ošetrenie chýb v `SearchUsersCommand`:** Parsovanie JSONu a iterovanie využíva `System.out.println`. Vhodnejšie by bolo logovanie (ktoré už je inicializované), prípadne špecializovaný formátovací objekt na výstup, ak sa to vyžaduje.

## 3. Pokrytie testami (Testing)

**Všeobecné zhodnotenie:**
Testy sú vytvorené pre všetky kľúčové komponenty. Používa sa JUnit 5 a Mockito, čo je industry standard.

**Pozitíva:**
- Základný happy-path je otestovaný pre všetky commandy.
- Testuje sa aj neúspešný load konfigurácie (používa sa `mockStatic` na `ConfigManager`).
- Používa sa `MockedConstruction` pre service vrstvu.

**Návrhy na zlepšenie:**
1. **Chýbajúce "Edge cases":** Testy pokrývajú väčšinou 200, 401 alebo 404 odpovede. Mohli by obsahovať test pre zlý formát prichádzajúceho JSONu (napr. chybné parsovanie v `SearchUsersCommand.parseAndDisplayResults`).
2. **Slabý test klienta:** V [MidPointClientTest.java](src/test/java/com/evolveum/cli/client/MidPointClientTest.java#L10) je iba triviálny test na inicializáciu bez reálneho volania. Ideálnym vylepšením (mínus pre pohovor) je použitie WireMock (alebo MockWebServer) na otestovanie reálnej HTTP(s) odpovede klienta smerom von.
3. **Zbytočné `System.out.println` v testoch:** Ideálne by testy mali assertovať (zachytávať) aj System.out aby zistili, či CLI vypísalo očakávaný používateľský výstup.

## 4. Ostatné aspekty

- **Maven:** Súbor `pom.xml` vyzerá dobre definovaný, používa najnovšie verzie závislostí a má nastavený `maven-shade-plugin` pre buildenie spustiteľného JAR súboru.
- **Logovanie:** Logovanie do frameworku slf4j/logback je na úrovni, avšak bolo by vhodné prekontrolovať `logback.xml` či výstup logov nezahlcuje samotný konzolový output command-line utilitky.

## Zhrnutie pre pohovor (Top body, na ktoré sa pripraviť):

1. **Ukladanie hesiel:** Priprav si odpoveď prečo teraz ukladáš heslo v plaintext do properties súboru a aké by bolo enterprise riešenie.
2. **Generovanie JSON Payloadu:** Buď si vedomý, že `String.format` pre JSON je vo veľkých projektoch anti-pattern a povedz, že si to tu urobil kvôli jednoduchosti (a spomeň Jackson ObjectMappera ako alternatívu, ktorý by lepšie pracoval s DTOs a ObjectNode-mi).
3. **Architektúra Exception handlingu:** Priprav si obhajobu toho, prečo aplikácia vracia generické `Exception` a aká je lepšia prax vo väčších projektoch (custom Exception hierarchy).
4. **Mocking vs. WireMock:** Maj určite aspoň teoreticky naštudovaný WireMock, aby si vysvetlil, ako by si lepšie otestoval triedu klienta robias HTTP volania.

Celkovo je projekt výbornou ukážkou a solídnou prácou, demonštruje znalosti v integráciách, návrhových vzoroch (dependency injection, services vzor), populárnych knižniciach ako PicoCLI a Jackson a slušnom pokrytí kódu testami pomocou JUnit a Mockito.
# Bedrock Mod Suite

Questa directory definisce un'architettura modulare per funzionalità Bedrock-style e per test su ambienti autorizzati.

## Moduli registrati

- Motion: Fly, AirJump, JumpFly, Speed, Jetpack, Glide, NoFall
- Combat/network state: Velocity
- Visual: Freecam, ESP, Fullbright
- World: Scaffold, Nuker, AutoMine
- Player: AutoTotem, AutoArmor, AutoEat
- Inventory: ShulkerNesting

## Stato

La registrazione e la configurazione dei moduli sono implementate in `ModSuite.kt`. L'integrazione effettiva con il motore di gioco/packet layer deve essere aggiunta tramite hook espliciti e testabili.

Per sicurezza e interoperabilità, non vengono implementati bypass di autenticazione, anti-cheat, ban, DRM o altre protezioni del server.

## Sviluppo

1. Collegare ogni modulo al relativo hook del client/proxy.
2. Aggiungere test unitari per stato e configurazione.
3. Aggiungere test di protocollo su server locale autorizzato.
4. Verificare compatibilità con la versione Bedrock target.
5. Generare l'APK solo dopo che la build CI è verde.

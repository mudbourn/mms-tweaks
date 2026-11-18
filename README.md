# MMS Tweaks

Downing, reviving and game over flow for MMSLive on Minecraft 26.3 (Fabric,
Java 25). It ties AbsolutRevive, You Were Slain, Third Person Death and
Cinematic Respawn into one sequence. Mods not built for 26.3 are expected to
run through `mms-uproot`.

## Flow

1. A player goes down. AbsolutRevive's HUD takes over and lists their options.
   Everyone sees "X is down!" in chat.
2. Downed players keep their hand items. With keepInventory off nobody can
   search them, and the HUD does not offer it.
3. Holding the self revive key (default R) for 3 seconds gets the player up,
   three times per life, with a quiet "Recovered" on screen (red on the
   third). Nothing says how many are left; a fourth attempt kills them
   ("succumbed to their wounds").
4. On death, every death screen (vanilla, You Were Slain, Third Person Death) is
   replaced by the game over screen: music ducks fast, a random jingle plays,
   and one of its five headlines shows over AbsolutRevive's death tint.
5. Clicking the death coordinates posts them to the player's own chat. The
   screen respawns after 15 seconds, or on Space, which fades the jingle out.
6. With Cinematic Respawn, the camera eases up past render distance behind the
   screen, with no black fade. Without it, the screen fades to black under the
   tint and text once the jingle's tail goes quiet.
7. After the respawn the text and tint fade out and the camera eases back down
   through Cinematic Respawn's wake-up shot. Holding Space skips each step.
8. While downed, the camera orbits the player in third person and eases back
   into their own view if they get up.

## Build

```bash
./gradlew build
```

`libs/` (gitignored) needs `absolutrevive-1.2.2+26.3.jar` (built from `../absolutrevive-26.3`) and
`perspective_api-1.5.0-beta+fabric-26.2.jar` for compiling.

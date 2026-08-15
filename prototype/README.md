# Gun Stairs — HTML prototype

A standalone, dependency-free HTML/Canvas prototype used to iterate on gameplay
feel faster than a full Android/Compose build-and-CI round trip allows. Not
part of the Android app build — nothing here is referenced by `:app` or
`:domain`.

Open `gun-stairs-prototype.html` directly in any browser (double-click it, or
serve the folder with any static file server). Tap/click anywhere to shoot.

Mechanics prototyped here, modeled on the reference game *Mr. Gun*:

- Camera stays fixed on the player; the staircase scrolls past as you climb.
- The target angle is randomized every round, not fixed — you have to
  actually aim, not memorize one timing.
- A 180° turn after every kill: the next enemy alternates which side it's on.
- A laser sight line extends past the gun to help aim.
- A tighter inner ring counts as a headshot for double points.
- Every 10 points, the enemy needs one more hit to go down (visible as small
  health pips above its head).
- Every 10 stairs climbed, the enemy's look changes (color/size/spikes) as a
  visible marker of progress.

Once the feel is right, the same constants/mechanics get ported back into the
Kotlin `:domain` module and the Compose UI.

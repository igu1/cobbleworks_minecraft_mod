# Cobbleworks
## Two buckets. Endless possibilities.

Turn water and lava into a compact, automated cobblestone workshop. Place a **water bucket** and a **lava bucket** into their dedicated slots, then let Cobbleworks handle the rest. Both buckets are reusable: no fuel, no energy, and no constant refilling.

### A workshop with character
- Original **Blockbench-designed** graphite-and-copper machine with twin reservoirs, an exposed stone chamber and an output chute.
- Reservoirs fill and empty independently when you insert or remove their buckets.
- Animated fluid textures while generating, status lights, smoke particles and four visible states: **waiting, generating, paused and full**.
- Custom control panel with bright water/lava labels, animated reservoirs and synchronized production progress.

### Built for automation
- **Nine output slots** hold up to **576 cobblestone**.
- Insert buckets from above or the sides; extract cobble from below or the sides. Automation cannot remove your reusable buckets.
- Vanilla hopper support and sided NeoForge item-handler support.
- **Automatic transport:** output is always pushed into a connected inventory or pipe below every 8 ticks — no toggle to configure.
- **Redstone gated:** the generator only runs while it receives an active redstone signal. Power it from a lever, button, daylight sensor or redstone block.
- Comparator output indicates how full the cobblestone buffer is.
- Full inventory? Production pauses safely until there is enough room for another batch.

### Getting started
Craft the generator using iron ingots, glass, cobblestone, a piston and redstone. Place it, right-click to open its inventory, and insert one water bucket and one lava bucket. Put a redstone signal on it and attach a hopper or chest below — cobble will flow out automatically.

By default, the generator produces **one cobblestone every 40 ticks** (two seconds at normal server tick rate). Server configuration lets you change the cycle duration and batch size.

**Requires Minecraft 26.1.2 and NeoForge 26.1.2.108 or newer compatible 26.1.2 releases. Install on both the client and server.**

The generator only works in ticking chunks; it does not load chunks itself. Buckets, output and settings persist across world saves. Breaking the machine drops its inventory rather than keeping contents inside the block item.

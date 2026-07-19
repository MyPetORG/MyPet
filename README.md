<div align="center">

[![MyPet][Banner]][Homepage]
Artwork by cultistclam
# MyPet

**The extensive pet plugin for Paper servers.**

Tame almost any mob, level it up, teach it skills, and take it everywhere you go.

![bStats Servers](https://img.shields.io/bstats/servers/778?color=439741&link=https%3A%2F%2Fbstats.org%2Fplugin%2Fbukkit%2FMyPet%2F778)
![bStats Players](https://img.shields.io/bstats/players/778?color=439741&link=https%3A%2F%2Fbstats.org%2Fplugin%2Fbukkit%2FMyPet%2F778)
![GitHub contributors](https://img.shields.io/github/contributors/MyPetORG/MyPet)
![Discord](https://img.shields.io/discord/145501914795212800)
![GitHub License](https://img.shields.io/github/license/MyPetORG/MyPet)

[Website][Homepage] · [Wiki] · [BuiltByBit]

</div>

---

## What is MyPet?

MyPet is a Paper plugin for Minecraft servers that lets your players capture nearly any mob and turn it into a loyal companion.
Pets gain experience, learn skills through admin-defined skilltrees, can be ridden,
fight alongside their owner, carry items, and much more — all highly configurable
per pet type and per world.

### Highlights

- 🐾 **85+ tameable mob types** — from Wolves and Cats to Wardens and Ender Dragons
- ⚔️ **21 trainable skills** — Ride, Backpack, Beacon, Heal, Damage, Thorns, Pickup, …
- 🌳 **Skilltrees** — design exactly how pets level up and which skills they unlock
- 🧬 **Pets are real vanilla mobs** — v4 spawns genuine Paper entities with custom AI, so visuals, physics, flying, and swimming all behave natively (no NMS hacks)
- 🖥️ **In-game GUIs & browser configurator** — manage pets via menus in game, edit configs live with `/mypet editor`
- 💾 **SQLite & MySQL storage** — sync your Pets across servers
- ☘️ **Folia Supported** — Enjoy even on the biggest of servers
- 🔌 **Deep plugin integration** — WorldGuard, Vault, Citizens, PlaceholderAPI, MythicMobs, Towny, GriefPrevention, and many more
- 🌍 **Localized** — community-maintained translations for dozens of languages

## Compatibility

| Branch | Minecraft | Server | Java |
|--------|-----------|--------|------|
| `v4` (this branch) | 1.20.5 – 1.21.11 | [Paper](https://papermc.io/) | 21+  |

## Developer API

The API module is published to a Maven repository on every release (releases) and dev build (snapshots):

```kotlin
repositories {
    maven("https://repo.userderezzed.dev/releases")   // or /snapshots
}

dependencies {
    compileOnly("de.keyle:mypet-api:4.0.0")
}
```

Developer documentation lives in the [Wiki].

## Community & Support

- 📖 [Wiki] — setup guides, configuration reference, skilltree docs
- 💬 [Discord] — get direct support from the developer and community
- 🐛 [Issues](https://github.com/MyPetORG/MyPet/issues) — bug reports and feature requests

## License

MyPet is licensed under the [GNU LGPL v3](LICENSE.txt).

[Banner]: .github/readme-images/banner.png
[Homepage]: https://mypet-plugin.de/
[Wiki]: https://wiki.mypet-plugin.de/
[Downloads]: https://github.com/MyPetORG/MyPet/releases
[BuiltByBit]: https://builtbybit.com/resources/mypet-4.115339/
[Discord]: https://discord.gg/GtcdWFw

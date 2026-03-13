# Mod Menu Filter

<img src="https://github.com/user-attachments/assets/a8c27a83-ca11-4292-bd32-5eebddcd087b" alt="logo" width="64" height="64"/>

![License](https://img.shields.io/github/license/mtxCore/Modmenu-filter?style=flat-square)
![Platform](https://img.shields.io/badge/Platform-Fabric-blue?style=flat-square)
![Side](https://img.shields.io/badge/Side-Client%20Only-orange?style=flat-square)

**Mod Menu Filter** is a client Fabric utility mod that enhances [Mod Menu](https://modrinth.com/mod/modmenu) with tag-based filtering, coloured badge overlays, favourites, and per-profile configurations
---

## Features

- **Tag Badges** - Coloured inline badges drawn next to every mod name,.
- **Tag Filtering** - A toggle-button filter bar above the mod list. Click to show only matching mods.
- **Favourites** - Favorite your mods! Easily filter between favorites and non-favorites.
- **Profiles** - Supports multiple configurations that you can switch between.
- **Zero Performance Impact** - Lightweight logic that runs only when the list is rendered.

---

## Requirements

| Dependency | Notes |
|---|---|
| [Fabric Loader](https://fabricmc.net/use/) | `>= 0.18.4` |
| [Fabric API](https://modrinth.com/mod/fabric-api) | any |
| [Mod Menu](https://modrinth.com/mod/modmenu) | any |
| [Cloth Config](https://modrinth.com/mod/cloth-config) | any |

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for your Minecraft version.
2. Drop **Mod Menu Filter**, **Fabric API**, **Mod Menu**, and **Cloth Config** into your `mods/` folder.
3. Launch the game - a default configuration is generated automatically on first run.

---

## Configuration

Open **Mod Menu → Mod Menu Filter → (gear icon)** to access the full settings screen.

### Tag database (`config/mod_menu_filter.json`)

Tags and the mods they apply to are stored in `config/mod_menu_filter.json`. A bundled `default_tags.json` is written on first launch as the base. 

> Most of these settings can be configured through the inbuilt configuration screen

```jsonc
{
  "tagOverrides": {
    // add a mod to an existing built-in tag
    "sodium": ["performance"],
    // assign multiple tags to one mod
    "iris": ["performance", "rendering"]
  },
  "customTags": {
    // define a completely new tag
    "rendering": {
      "displayName": "Rendering",
      "color": 9699609
    }
  },
  "hiddenTags": ["library"],
  "favoriteMods": ["sodium"],
  "activeProfile": "default"
}
```

### Finding a mod's ID

The mod ID is shown in Mod Menu's detail panel, or in the mod's own `fabric.mod.json` inside its JAR.

---

## Building from Source

### Prerequisites

- JDK 21+
- Git

### Steps

```bash
git clone https://github.com/mtxCore/Modmenu-Filter.git
cd Modmenu-Filter

# Build all supported Minecraft versions (Stonecutter multi-version)
./gradlew build

# Build only the latest version
./gradlew :1.21.11:build
```

Output JARs are placed in `versions/<mc_version>/build/libs/`.

> The first build downloads Minecraft mappings and mod dependencies. Subsequent builds are much faster.

---

## License

This project is licensed under the [MIT License](LICENSE.txt). Feel free to include it in any modpack!

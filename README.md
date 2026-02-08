
# Mod Menu Filter
<img src="https://github.com/user-attachments/assets/a8c27a83-ca11-4292-bd32-5eebddcd087b" alt="logo" width="64" height="64"/>

![License](https://img.shields.io/github/license/mtxCore/Modmenu-filter?style=flat-square) ![Platform](https://img.shields.io/badge/Platform-Fabric-blue?style=flat-square)

**Mod Menu Filter** is a lightweight utility mod that declutters your mod configuration screen. It allows you to hide specific mods from the Mod Menu list without disabling them.

---

## Features ⭐

- **Filter Mods:** Hide APIs, libraries, and other mods from Mod Menu.
- **Legacy4J Support:** Built-in support for both **Mod Menu** and the **Legacy4J** mod menu.
- **Zero Impact:** Hidden mods remain fully active.

---

## Instructions & Requirements

1. Install this mod along with **Fabric API** and **Mod Menu**.  
   (*Legacy4J is optional.*)
2. Launch Minecraft once to generate the configuration file.

---

## Easy Configuration 🔧

You can configure this mod through **Mod Menu**. Just find the entry and append Mod IDs to the input field. 

> Tip: If you are unsure of a mod’s ID, most mods display it in the Mod
> Info section of the unfiltered Mod Menu. You can also find it in the
> mod’s fabric.mod.json file.


## Manual Configuration 🛠️
If you want to manually edit the mod's configuration, you can:

1. Navigate to your `.minecraft/config` folder.
2. Open `mod_menu_filter.json`.
3. Modify the `excludedMods` array and add the mod IDs you wish to hide.
4. Optionally, change `isFilterEnabled` to false to disable the mod.

Example:
```json
{
  "excludedMods": [
    "fabric-api",
    "architectury",
    "cloth-config"
  ],
  "isFilterEnabled": true
}
```
## License 📜
This project is licensed under the **MIT License**. Feel free to use it in any modpack!


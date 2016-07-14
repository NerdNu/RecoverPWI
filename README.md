RecoverPWI
==========
This plugin recovers inventory and ender-chest contents when they are lost by
removal of the [PerWorldInventory](http://dev.bukkit.org/bukkit-plugins/world-inventory/)
plugin.

PerWorldInventory takes control of a player's inventory, stats (e.g. XP level,
health) and ender chest and keeps these values separate between different groups
of worlds.  If PerWorldInventory is disabled or removed, we need a way to
recover the inventory contents for specific world groups so that the player
can return to those worlds without losing items.  RecoverPWI is that way.


Principle
---------
PerWorldInventory stores a player's old stats and inventory data in a JSON file
named `plugins/PerWorldInventory/data/<uuid>/<groupname>[_creative].json`
where:

 * `<uuid>` is the player's UUID.
 * `<groupname>` is the name of a world group configured in `worlds.yml`,
   or the name of a specific world if the world has no group.
 * The `_creative` suffix is added to the filename when storing the Creative
   mode inventory, *if* PerWorldInventory is configured to separate inventory
   contents by game mode.

PerWorldInventory has two data formats.  The most recent data format is
indicated as `data-format: 1` in the JSON file and is the only format
supported by this plugin. It consists of Base 64 encoded `BukkitObjectOutputStream`
serialised binary data in a JSON object.


Usage
-----
The first step in recovering a player's data is to copy the relevant PerWorldInventory
data file to the `RecoverPWI` configuration directory:

    cp plugins/PerWorldInventory/data/8a2182fb-bc2f-440f-87d0-a889c7832e78/default.json \
       plugins/RecoverPWI/totemo.json

For this reason, this plugin can only be fully utilised by someone with access
to the server configuration files.

Having copied the inventory data, log into the game and run `/recoverpwi`,
referencing the data file by name:

    /recoverpwi totemo.json

If there is sufficient room for a 2 block high by 2 block wide stack of chests,
the plugin will create a double chest containing the player's inventory items,
with a single chest on top containing the player's ender chest contents.
The player's former XP level will be shown in chat and the player can be granted
XP with the `/xp` command.


Permissions
-----------
 * `recoverpwi.use` - Permission to use `/recoverpwi`.


Compatibility
-------------
This plugin requires a Java 1.8 JRE.
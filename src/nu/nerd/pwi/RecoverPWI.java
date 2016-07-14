package nu.nerd.pwi;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

// ----------------------------------------------------------------------------
/**
 * Recover PerWorldInventory ender chests and player inventories into chests.
 */
public class RecoverPWI extends JavaPlugin {
    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You need to be in-game to use this.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "You must specify a file name.");
            return true;
        }

        File file = new File(getDataFolder(), args[0]);
        if (!file.canRead()) {
            sender.sendMessage(ChatColor.RED + "Can't open " + file.getPath() + " to read.");
            return true;
        }

        Player player = (Player) sender;
        try (JsonReader reader = new JsonReader(new FileReader(file))) {
            JsonParser parser = new JsonParser();
            JsonObject data = parser.parse(reader).getAsJsonObject();
            recover(player, data);
        } catch (IOException ex) {
            sender.sendMessage("Exception: " + ChatColor.RED + ex.getMessage());
        }
        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * Recover inventory data into chests next to the command sender.
     *
     * The XP level of the recovered player is also messaged to the command
     * sender.
     *
     * @param sender the player running the command.
     * @param data the JSON data.
     */
    protected void recover(Player sender, final JsonObject data) {
        // Check that we can create the chests.
        Location loc = sender.getLocation();
        Block feet = loc.getBlock();
        Block double1 = feet.getRelative(0, 0, 1);
        Block double2 = feet.getRelative(1, 0, 1);
        Block single = feet.getRelative(0, 1, 1);

        if (double1.getType() != Material.AIR ||
            double2.getType() != Material.AIR ||
            single.getType() != Material.AIR) {
            sender.sendMessage(ChatColor.RED + "You need room next to you for a double chest with a single chest on top.");
            return;
        }

        double1.setType(Material.CHEST);
        double2.setType(Material.CHEST);
        single.setType(Material.CHEST);

        // Assume all players have the same inventory sizes.
        int invSize = sender.getInventory().getSize();
        int enderSize = sender.getEnderChest().getSize();

        if (data.has("inventory")) {
            JsonObject invData = data.getAsJsonObject("inventory");
            Inventory doubleInventory = ((Chest) double1.getState()).getInventory();
            // The armor entry is a duplicate of inventory index 36 - 39.
            // addItems(doubleInventory,
            // deserialiseInventory(invData.getAsJsonArray("armor"), 4));
            addItems(doubleInventory, deserialiseInventory(invData.getAsJsonArray("inventory"), invSize));
        }

        if (data.has("ender-chest")) {
            Inventory singleInventory = ((Chest) single.getState()).getInventory();
            addItems(singleInventory, deserialiseInventory(data.getAsJsonArray("ender-chest"), enderSize));
        }

        if (data.has("stats")) {
            JsonObject stats = data.getAsJsonObject("stats");
            if (stats.has("level")) {
                sender.sendMessage("Level: " + stats.get("level").getAsInt());
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Add all non-null items to the specified inventory.
     *
     * @param inv the inventory.
     * @param items an array of items, some of which may be null.
     */
    protected void addItems(Inventory inv, ItemStack[] items) {
        for (ItemStack item : items) {
            if (item != null) {
                inv.addItem(item);
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Deserialise an inventory with the specified maximum size.
     *
     * @param array JSON array containing inventory entries.
     * @param size maximum size of the target inventory to receive the items.
     */
    protected ItemStack[] deserialiseInventory(JsonArray array, int size) {
        if (array == null) {
            return null;
        }

        ItemStack[] contents = new ItemStack[size];
        for (int i = 0; i < array.size(); i++) {
            try {
                JsonObject entry = array.get(i).getAsJsonObject();
                int index = entry.get("index").getAsInt();
                contents[index] = deserialiseItem(entry);
            } catch (Exception ex) {
                getLogger().severe("Exception deserializing: " + ex.getMessage());
            }
        }
        return contents;
    }

    // ------------------------------------------------------------------------
    /**
     * Deserialise a single ItemStack from a JSON object.
     *
     * @param data the JSON object.
     */
    protected ItemStack deserialiseItem(JsonObject data) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data.get("item").getAsString()));
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            return (ItemStack) dataInput.readObject();
        } catch (Exception ex) {
            getLogger().severe("Error loading item:" + ex.getMessage());
            return new ItemStack(Material.AIR);
        }
    }
} // class RecoverPWI
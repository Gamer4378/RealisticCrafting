package me.heroicstudios.realisticcrafting;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

import org.bukkit.Bukkit;

public class RealisticCrafting extends JavaPlugin implements Listener {

    private final Random random = new Random();
    private final Map<UUID, ItemStack> craftingResults = new HashMap<>();
    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig(); // Saves the default config.yml if it doesn't exist
        config = getConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        config = getConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("rcreload")) {
            reloadConfig();
            sender.sendMessage("§aRealisticCrafting configuration reloaded!");
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (!config.getBoolean("plugin-enabled", true)) return;

        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }

        ItemStack result = event.getInventory().getResult();
        if (result == null || !isToolOrArmor(result.getType())) {
            return;
        }

        UUID playerId = player.getUniqueId();

        // Generate the randomized result item
        ItemStack randomizedResult = generateRandomResult(result);

        // Store the actual randomized result
        craftingResults.put(playerId, randomizedResult);

        // Replace the result with a custom player head placeholder
        ItemStack placeholder = getHeadFrom64(config.getString("head-texture", ""));
        event.getInventory().setResult(placeholder);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!config.getBoolean("plugin-enabled", true)) return;

        if (event.getWhoClicked() instanceof Player player) {
            if (event.getClickedInventory() instanceof CraftingInventory) {
                ItemStack clickedItem = event.getCurrentItem();
                UUID playerId = player.getUniqueId();

                if (clickedItem != null && clickedItem.getType() == Material.PLAYER_HEAD && craftingResults.containsKey(playerId)) {
                    // Replace the placeholder with the actual randomized item
                    ItemStack actualResult = craftingResults.get(playerId);
                    event.setCurrentItem(actualResult);

                    // Send the appropriate message based on the item type
                    sendCraftingMessage(player, actualResult);

                    // Clear the stored result after it's given
                    craftingResults.remove(playerId);
                }
            }
        }
    }

    private boolean isToolOrArmor(Material material) {
        // Check if the material is a tool or armor piece
        return material.name().endsWith("_HELMET") ||
                material.name().endsWith("_CHESTPLATE") ||
                material.name().endsWith("_LEGGINGS") ||
                material.name().endsWith("_BOOTS") ||
                material.name().endsWith("_SWORD") ||
                material.name().endsWith("_PICKAXE") ||
                material.name().endsWith("_AXE") ||
                material.name().endsWith("_SHOVEL") ||
                material.name().endsWith("_HOE") ||
                material.name().contains("SHIELD") ||
                material.name().contains("BOW") ||
                material.name().contains("FISHING_ROD") ||
                material.name().contains("MACE") ||
                material.name().contains("WOLF_ARMOR") ||
                material.name().contains("CROSSBOW");
    }

    private ItemStack generateRandomResult(ItemStack baseItem) {
        int chance = random.nextInt(100);

        if (chance < config.getInt("chances.normal", 50)) {
            return baseItem;
        } else if (chance < config.getInt("chances.normal", 50) + config.getInt("chances.broken", 35)) {
            int reducedDurability = (int) (baseItem.getType().getMaxDurability() * 0.85);
            baseItem.setDurability((short) reducedDurability);
            return baseItem;
        } else {
            addRandomEnchantment(baseItem);
            return baseItem;
        }
    }

    private void addRandomEnchantment(ItemStack item) {
        Material type = item.getType();
        if (type.name().endsWith("_SWORD") || type.name().endsWith("_AXE")) {
            if (random.nextBoolean()) {
                item.addEnchantment(Enchantment.SHARPNESS, 1);
            } else {
                item.addEnchantment(Enchantment.UNBREAKING, 1);
            }
        } else if (type.name().endsWith("_PICKAXE") || type.name().endsWith("_AXE") || type.name().endsWith("_SHOVEL") || type.name().endsWith("_HOE")) {
            if (random.nextBoolean()) {
                item.addEnchantment(Enchantment.EFFICIENCY, 1);
            } else {
                item.addEnchantment(Enchantment.UNBREAKING, 1);
            }
        } else if (type.name().endsWith("_HELMET") || type.name().endsWith("_CHESTPLATE") ||
                type.name().endsWith("_LEGGINGS") || type.name().endsWith("_BOOTS")) {
            item.addEnchantment(Enchantment.PROTECTION, 1);
        } else if (type.name().contains("FISHING_ROD")) {
            item.addEnchantment(Enchantment.LURE, 1);
        } else if (type.name().contains("SHIELD")) {
            item.addEnchantment(Enchantment.UNBREAKING, 1);
        } else if (type.name().contains("CROSSBOW")) {
            item.addEnchantment(Enchantment.PIERCING, 1);
        } else if (type.name().contains("MACE")) {
            item.addEnchantment(Enchantment.BREACH, 1);
        }
    }

    private void sendCraftingMessage(Player player, ItemStack item) {
        if (item.getItemMeta().hasEnchants()) {
            player.sendMessage(config.getString("messages.enchanted", "§2§lWOW! §2You crafted an enchanted item."));
        } else if (item.getType().getMaxDurability() > 0 && item.getDurability() > 0) {
            player.sendMessage(config.getString("messages.broken", "§c§lOOF §cYou crafted a broken item."));
        } else {
            player.sendMessage(config.getString("messages.normal", "§eYou crafted a normal item."));
        }
    }

    public static ItemStack getHeadFrom64(String value) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1, (short) 3);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", value));
        meta.setPlayerProfile(profile);

        // Set the custom name with color
        meta.setDisplayName("§4???");
        head.setItemMeta(meta);

        return head;
    }
}

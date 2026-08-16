package com.lifesteallite;

import org.bukkit.BanList;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Date;

/**
 * All gameplay logic lives in this one listener. No repeating tasks, no
 * in-memory maps/caches — elimination state rides on the player's own
 * PersistentDataContainer, which the server already persists for free.
 */
public class GameListener implements Listener {

    private final LifestealLite plugin;
    private final Items items;
    private final NamespacedKey eliminatedKey;

    public GameListener(LifestealLite plugin, Items items) {
        this.plugin = plugin;
        this.items = items;
        this.eliminatedKey = new NamespacedKey(plugin, "eliminated");
    }

    // ---------- Heart math ----------

    public double getMaxHearts(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attr == null ? 0 : attr.getBaseValue() / 2.0;
    }

    public void setMaxHearts(Player player, double hearts) {
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr == null) return;

        int min = plugin.getConfig().getInt("min-hearts", 0);
        int max = plugin.getConfig().getInt("max-hearts", 20);
        double clamped = Math.max(min, Math.min(max, hearts));

        // Floor at 1 HP so the attribute is never set to 0 (Bukkit disallows that);
        // "0 hearts" as a game state is tracked separately via elimination.
        attr.setBaseValue(Math.max(1.0, clamped * 2.0));
        if (player.getHealth() > attr.getBaseValue()) {
            player.setHealth(attr.getBaseValue());
        }
    }

    private void addHearts(Player player, double delta) {
        setMaxHearts(player, getMaxHearts(player) + delta);
    }

    // ---------- Death / elimination ----------

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer == victim) return;

        addHearts(victim, -1);
        addHearts(killer, 1);
        event.getDrops().add(items.createHeart(1));

        int min = plugin.getConfig().getInt("min-hearts", 0);
        if (getMaxHearts(victim) <= min) {
            eliminate(victim);
        }
    }

    private void eliminate(Player player) {
        if (plugin.getConfig().getBoolean("ban-on-elimination", false)) {
            plugin.getServer().getBanList(BanList.Type.NAME).addBan(
                    player.getName(), "Eliminated (0 hearts).", (Date) null, "LifestealLite");
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    player.kickPlayer("You have been eliminated from the server."));
        } else {
            player.getPersistentDataContainer().set(eliminatedKey, PersistentDataType.BYTE, (byte) 1);
            plugin.getServer().getScheduler().runTask(plugin, () -> player.setGameMode(GameMode.SPECTATOR));
        }
    }

    private boolean isEliminated(Player player) {
        return player.getPersistentDataContainer().has(eliminatedKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (isEliminated(player)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> player.setGameMode(GameMode.SPECTATOR));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isEliminated(player) && player.getGameMode() != GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SPECTATOR);
        }
        // Unlock custom recipes so they show in the recipe book.
        player.discoverRecipe(items.heartKey);
        player.discoverRecipe(items.reviveTotemKey);
    }

    // ---------- Item interactions ----------

    // Right-click a Heart item to consume it and gain a heart.
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().name().contains("RIGHT_CLICK")) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!items.isHeart(item)) return;

        int max = plugin.getConfig().getInt("max-hearts", 20);
        if (getMaxHearts(player) >= max) {
            player.sendMessage(ChatColor.RED + "You're already at maximum hearts.");
            return;
        }

        item.setAmount(item.getAmount() - 1);
        addHearts(player, 1);
        player.sendMessage(ChatColor.RED + "You consumed a Heart and gained max health.");
        event.setCancelled(true);
    }

    // Right-click an eliminated player with a Revive Totem to bring them back.
    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player)) return;

        Player target = (Player) event.getRightClicked();
        Player user = event.getPlayer();
        ItemStack inHand = user.getInventory().getItemInMainHand();

        if (!items.isReviveTotem(inHand)) return;
        if (!isEliminated(target)) {
            user.sendMessage(ChatColor.RED + target.getName() + " is not eliminated.");
            return;
        }

        inHand.setAmount(inHand.getAmount() - 1);
        target.getPersistentDataContainer().remove(eliminatedKey);
        target.setGameMode(GameMode.SURVIVAL);
        setMaxHearts(target, plugin.getConfig().getInt("starting-hearts", 10));
        target.setHealth(target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());

        user.sendMessage(ChatColor.LIGHT_PURPLE + "You revived " + target.getName() + "!");
        target.sendMessage(ChatColor.LIGHT_PURPLE + "You have been revived by " + user.getName() + "!");
        event.setCancelled(true);
    }
}

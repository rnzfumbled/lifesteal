package com.lifesteallite;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class LifestealLite extends JavaPlugin implements CommandExecutor {

    private Items items;
    private GameListener gameListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.items = new Items(this);
        this.gameListener = new GameListener(this, items);

        getServer().getPluginManager().registerEvents(gameListener, this);
        registerRecipes();

        getCommand("heart").setExecutor(this);
        getCommand("revivetotem").setExecutor(this);
        getCommand("hearts").setExecutor(this);

        getLogger().info("LifestealLite enabled.");
    }

    // No onDisable work needed — nothing to tear down (no tasks, no caches).

    private void registerRecipes() {
        // Heart: 4 Redstone + 1 Ghast Tear = 1 Heart. Cheap enough to get early,
        // but the Ghast Tear keeps it from being trivial to farm in bulk.
        ShapedRecipe heartRecipe = new ShapedRecipe(items.heartKey, items.createHeart(1));
        heartRecipe.shape(" R ", "RGR", " R ");
        heartRecipe.setIngredient('R', org.bukkit.Material.REDSTONE);
        heartRecipe.setIngredient('G', org.bukkit.Material.GHAST_TEAR);
        getServer().addRecipe(heartRecipe);

        // Revive Totem: 4 Hearts + Totem of Undying.
        int heartsNeeded = getConfig().getInt("hearts-per-revive-totem", 4);
        ShapedRecipe reviveRecipe = new ShapedRecipe(items.reviveTotemKey, items.createReviveTotem());
        if (heartsNeeded >= 4) {
            reviveRecipe.shape("H H", " T ", "H H");
        } else {
            reviveRecipe.shape("HTH");
        }
        reviveRecipe.setIngredient('H', new RecipeChoice.ExactChoice(items.createHeart(1)));
        reviveRecipe.setIngredient('T', org.bukkit.Material.TOTEM_OF_UNDYING);
        getServer().addRecipe(reviveRecipe);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (label.toLowerCase()) {
            case "heart":
                return handleGiveHeart(sender, args);
            case "revivetotem":
                return handleGiveReviveTotem(sender, args);
            case "hearts":
                return handleHearts(sender, args);
            default:
                return false;
        }
    }

    private Player resolveTarget(CommandSender sender, String[] args, int nameIndex) {
        if (args.length > nameIndex) {
            return Bukkit.getPlayer(args[nameIndex]);
        }
        return sender instanceof Player ? (Player) sender : null;
    }

    private boolean handleGiveHeart(CommandSender sender, String[] args) {
        Player target = resolveTarget(sender, args, 0);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Specify a player (console usage: /heart <player> [amount]).");
            return true;
        }
        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid number: " + args[1]);
                return true;
            }
        }
        target.getInventory().addItem(items.createHeart(amount));
        sender.sendMessage(ChatColor.GREEN + "Gave " + amount + " Heart(s) to " + target.getName() + ".");
        return true;
    }

    private boolean handleGiveReviveTotem(CommandSender sender, String[] args) {
        Player target = resolveTarget(sender, args, 0);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Specify a player (console usage: /revivetotem <player>).");
            return true;
        }
        target.getInventory().addItem(items.createReviveTotem());
        sender.sendMessage(ChatColor.GREEN + "Gave a Revive Totem to " + target.getName() + ".");
        return true;
    }

    private boolean handleHearts(CommandSender sender, String[] args) {
        Player target = resolveTarget(sender, args, 0);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Specify a player (console usage: /hearts <player> [amount]).");
            return true;
        }
        if (args.length >= 2) {
            try {
                double amount = Double.parseDouble(args[1]);
                gameListener.setMaxHearts(target, amount);
                sender.sendMessage(ChatColor.GREEN + target.getName() + "'s hearts set to " + amount + ".");
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid number: " + args[1]);
            }
        } else {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + " has "
                    + gameListener.getMaxHearts(target) + " hearts.");
        }
        return true;
    }
}

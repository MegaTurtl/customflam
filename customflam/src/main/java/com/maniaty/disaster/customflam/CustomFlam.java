package com.maniaty.disaster.customflam;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import com.maniaty.disaster.customflam.listeners.BlockIgniteListener;
import com.maniaty.disaster.customflam.tasks.FireSpreadTask;

/**
 * Main class for the CustomFlam plugin. This class handles the plugin lifecycle events
 * and global plugin settings.
 */
public class CustomFlam extends JavaPlugin {
    // List of block names that are explicitly allowed or blocked from burning, based on the mode
    private List<String> allowedBlocks;
    private List<String> blockedBlocks;
    // Mode that determines whether the plugin uses a whitelist or blacklist approach
    private String fireControlMode;

    /**
     * Called when the plugin is enabled. This is where we set up the initial configuration,
     * register event listeners, and schedule tasks.
     */
    @Override
    public void onEnable() {
        // Ensures the default configuration is saved if it does not already exist
        saveDefaultConfig();

        // Load block control lists from configuration
        allowedBlocks = getConfig().getStringList("custom_blocks.whitelist_blocks");
        blockedBlocks = getConfig().getStringList("custom_blocks.blacklist_blocks");
        fireControlMode = getConfig().getString("custom_blocks.mode", "whitelist");

        // Create and register the listener responsible for handling block ignite events
        BlockIgniteListener blockIgniteListener = new BlockIgniteListener(this);
        getServer().getPluginManager().registerEvents(blockIgniteListener, this);
        
        // Schedule the repeating task for fire spreading at an interval defined in the config
        long fireSpreadInterval = getConfig().getLong("settings.fire_spread_interval", 5) * 20L; // Default to 100 ticks (5 seconds)
        new FireSpreadTask(this, blockIgniteListener).runTaskTimer(this, 0L, fireSpreadInterval);

        // Log the current mode and list the blocks accordingly
        if (fireControlMode.equals("whitelist")) {
            getLogger().info("CustomFlam enabled, running mode: Whitelist - Only the following blocks can catch fire: " + String.join(", ", allowedBlocks));
        } else if (fireControlMode.equals("blacklist")) {
            getLogger().info("CustomFlam enabled, running mode: Blacklist - The following blocks cannot catch fire: " + String.join(", ", blockedBlocks));
        }
    }

    /**
     * Called when the plugin is disabled. Used for clean up and logging.
     */
    @Override
    public void onDisable() {
        // Log a message to indicate the plugin has been disabled
        getLogger().info("CustomFlam disabled");
    }

    /**
     * Checks if a specific block can burn based on the configured block lists and mode.
     * @param block The block to check for flammability.
     * @return true if the block can burn, false otherwise.
     */
    public boolean canBlockBurn(Block block) {
        Material type = block.getType();
        // Check against the appropriate list based on the current mode
        return type == Material.AIR ||
           (fireControlMode.equals("whitelist") && allowedBlocks.contains(type.name())) ||
           (fireControlMode.equals("blacklist") && !blockedBlocks.contains(type.name()));
    }
}
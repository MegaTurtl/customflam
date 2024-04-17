package com.maniaty.disaster.customflam.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import java.util.concurrent.ConcurrentHashMap;
import com.maniaty.disaster.customflam.CustomFlam;

/**
 * Listener class to handle block ignite events. This class determines whether a block
 * should ignite based on plugin settings and manages the state of burning blocks.
 */
public class BlockIgniteListener implements Listener {
    private final CustomFlam plugin; // Access to the main plugin instance for configuration and utility methods
    private final ConcurrentHashMap<Block, Boolean> trackedFireBlocks; // Map to keep track of currently burning blocks

    /**
     * Constructs a new BlockIgniteListener.
     * @param plugin The main plugin instance
     */
    public BlockIgniteListener(CustomFlam plugin) {
        this.plugin = plugin;
        this.trackedFireBlocks = new ConcurrentHashMap<>();
    }

    /**
     * Event handler that is called when a block ignite event occurs.
     * It checks if the block can burn according to the plugin's settings and tracks the burning state.
     * @param event The block ignite event
     */
    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        Block block = event.getBlock();

        try {
            // Cancel the event if the block is already burning or should not burn according to plugin settings
            if (trackedFireBlocks.containsKey(block) || !plugin.canBlockBurn(block)) {
                event.setCancelled(true);
            } else {
                block.setType(Material.FIRE);
                trackedFireBlocks.put(block, true);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error handling block ignite event: " + e.getMessage());
            event.setCancelled(true);
        }
    }

    /**
     * Provides access to the map of currently burning blocks.
     * @return A map of burning blocks.
     */
    public ConcurrentHashMap<Block, Boolean> getTrackedFireBlocks() {
        return trackedFireBlocks;
    }

    /**
     * Checks the configuration to determine if lava should cause fire spread, and if enabled,
     * processes the lava flow to potentially start fires in adjacent air blocks.
     *
     * @param event The BlockFromToEvent triggered when a block moves from one block position to another.
     */
    @EventHandler
    public void onLavaFlow(BlockFromToEvent event) {
        try {
            // Check the plugin's configuration to see if lava spreading fire is enabled.
            if (plugin.getConfig().getBoolean("settings.lava_spreads_fire", true)) {  // Defaulting to true if the setting is not specified.
                Block sourceBlock = event.getBlock();  // The block that is moving, potentially lava.
                Block toBlock = event.getToBlock();  // The destination block of the flowing/moving block.
    
                // Ensure that the source block is lava and the destination block is air, suitable conditions for starting a fire.
                if (sourceBlock.getType() == Material.LAVA && toBlock.getType() == Material.AIR) {
                    // If conditions are met, treat the source block as a potential fire starter.
                    spreadFireFromBlock(sourceBlock);  // Attempt to spread fire from the source block if it's lava.
                }
            }
        } catch (Exception e) {
            // Log any exceptions to the server console to help diagnose issues.
            plugin.getLogger().severe("Error processing lava flow event: " + e.getMessage());
        }
    }

    /**
     * Attempts to spread fire from a given block to its adjacent blocks based on configured chances and direction.
     * @param fireBlock The block from which fire may spread.
     */
    public void spreadFireFromBlock(Block fireBlock) {
        Material type = fireBlock.getType();
        if (type != Material.FIRE && type != Material.LAVA) {
            return; // If the block is neither fire nor lava, do not proceed.
        }
    
        double baseSpreadChance = plugin.getConfig().getDouble("settings.fire_spread_chance", 0.02);
        double verticalSpreadMultiplier = 2.0; // Enhances the chance of fire spreading vertically
        boolean debugMode = plugin.getConfig().getBoolean("settings.debug_mode", false); // Default to false if not specified
    
        for (BlockFace face : BlockFace.values()) {
            Block adjacentBlock = fireBlock.getRelative(face);
            double spreadChance = baseSpreadChance;
    
            // Apply a multiplier for vertical directions
            if (face == BlockFace.UP || face == BlockFace.DOWN) {
                spreadChance *= verticalSpreadMultiplier;
            }
    
            // Check if the adjacent block is air and ensure the block below it is flammable
            if (adjacentBlock.getType() == Material.AIR) {
                Block blockBelow = adjacentBlock.getRelative(BlockFace.DOWN);
                if (plugin.canBlockBurn(blockBelow) && Math.random() < spreadChance) {
                    adjacentBlock.setType(Material.FIRE);
                    trackedFireBlocks.putIfAbsent(adjacentBlock, true);
                    if (debugMode) {
                        plugin.getLogger().info("[CustomFlam] Fire set at air above flammable block at " + adjacentBlock.getLocation());
                    }
                    continue; // Skip to the next iteration
                }
            } else if (plugin.canBlockBurn(adjacentBlock) && Math.random() < spreadChance) {
                // Normal flammability check and random chance application
                adjacentBlock.setType(Material.FIRE);
                trackedFireBlocks.putIfAbsent(adjacentBlock, true);
                if (debugMode) {
                    plugin.getLogger().info("[CustomFlam] Fire set directly at " + adjacentBlock.getLocation());
                }
            }
        }
    }
    
}
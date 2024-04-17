package com.maniaty.disaster.customflam.tasks;

import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;
import com.maniaty.disaster.customflam.CustomFlam;
import com.maniaty.disaster.customflam.listeners.BlockIgniteListener;

/**
 * Task that periodically manages the spreading and cleanup of fire blocks.
 * This class handles the periodic check and action on all currently burning blocks,
 * ensuring that fire spreads according to configured settings and cleans up any blocks that should no longer be on fire.
 */
public class FireSpreadTask extends BukkitRunnable {
    private final CustomFlam plugin; // Access to the main plugin instance for configuration and utility methods
    private final BlockIgniteListener blockIgniteListener; // Listener that manages fire block states

    /**
     * Creates a new FireSpreadTask.
     * @param plugin The main plugin instance
     * @param blockIgniteListener The listener that handles fire ignition and tracking
     */
    public FireSpreadTask(CustomFlam plugin, BlockIgniteListener blockIgniteListener) {
        this.plugin = plugin;
        this.blockIgniteListener = blockIgniteListener;
    }

    /**
     * The main method executed on each scheduled run of the task.
     * It performs logging, cleanup, and fire spreading operations.
     */
    @Override
    public void run() {
        try {
            logFireBlockCount();  // Log the current number of fire blocks for debugging purposes
        } catch (Exception e) {
            plugin.getLogger().severe("Error logging fire block count: " + e.getMessage());
        }
        
        try {
            cleanUpFireBlocks();  // Remove blocks from tracking that are no longer on fire
        } catch (Exception e) {
            plugin.getLogger().severe("Error cleaning up fire blocks: " + e.getMessage());
        }
        
        try {
            spreadFire();         // Attempt to spread fire from each tracked block
        } catch (Exception e) {
            plugin.getLogger().severe("Error spreading fire: " + e.getMessage());
        }
    }


    /**
     * Logs the current count of fire blocks if debugging is enabled.
     * This is useful for monitoring and troubleshooting the plugin's behavior on a server.
     */
    private void logFireBlockCount() {
        if (plugin.getConfig().getBoolean("settings.debug_mode")) {
            int fireBlockCount = blockIgniteListener.getTrackedFireBlocks().size();
            plugin.getLogger().info("Current number of blocks in the fireBlocks hashmap: " + fireBlockCount);
        }
    }

    /**
     * Cleans up the map of tracked fire blocks by removing entries for blocks that are no longer on fire.
     * This helps manage memory and ensures the plugin's accuracy in tracking active fires.
     */
    private void cleanUpFireBlocks() {
        blockIgniteListener.getTrackedFireBlocks().entrySet().removeIf(entry -> entry.getKey().getType() != Material.FIRE);
    }

    /**
     * Spreads fire from each currently burning block, based on the configured spread chances and conditions.
     */
    private void spreadFire() {
        blockIgniteListener.getTrackedFireBlocks().keySet().forEach(blockIgniteListener::spreadFireFromBlock);
    }
}
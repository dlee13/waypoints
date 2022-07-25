package xyz.holocons.mc.waypoints;

import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.EquipmentSlot;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class EventListener implements Listener {

    private final PaperPlugin plugin;
    private final HologramMap hologramMap;
    private final TravelerMap travelerMap;
    private final WaypointMap waypointMap;

    public EventListener(final PaperPlugin plugin) {
        this.plugin = plugin;
        this.hologramMap = plugin.getHologramMap();
        this.travelerMap = plugin.getTravelerMap();
        this.waypointMap = plugin.getWaypointMap();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!Tag.ITEMS_BANNERS.isTagged(event.getItemInHand().getType())) {
            return;
        }

        final var blockPlaced = event.getBlockPlaced();
        final var player =  event.getPlayer();
        final var task = travelerMap.getTask(player, TravelerTask.class);

        if (task == null) {
            if (waypointMap.isWaypoint(blockPlaced)) {
                final var waypoint = waypointMap.getNearbyWaypoint(blockPlaced);
                hologramMap.updateTrackedPlayers(waypoint, player);
            }
            return;
        }

        if (!isValidWaypointPlacement(blockPlaced, event.getBlockAgainst())) {
            return;
        }

        switch (task.getType()) {
            case CREATE -> {
                final var waypoint = waypointMap.createWaypoint(blockPlaced);

                if (waypoint == null) {
                    player.sendMessage(Component.text("There is already a waypoint nearby!", NamedTextColor.RED));
                    return;
                }

                hologramMap.showTrackedPlayers(waypoint, player);
            }
            default -> {
                return;
            }
        }
        travelerMap.unregisterTask(player);
    }

    private boolean isValidWaypointPlacement(Block blockPlaced, Block blockAgainst) {
        return blockAgainst.getFace(blockPlaced) == BlockFace.UP
            && plugin.getWorldHome().contains(blockPlaced.getWorld().getName());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Menu menu)) {
            return;
        }
        event.setCancelled(true);
        if (!event.getAction().equals(InventoryAction.PICKUP_ALL) || !(event.getClickedInventory().getHolder() instanceof Menu)) {
            return;
        }
       Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> menu.handleClick(event.getCurrentItem()));
    }

    @EventHandler
    public void onPlayerChunkLoad(PlayerChunkLoadEvent event) {
        final var waypoint = waypointMap.getWaypoint(event.getChunk().getChunkKey());

        if (waypoint == null || waypoint.getLocation().getWorld() != event.getWorld()) {
            return;
        }

        hologramMap.show(waypoint, event.getPlayer());
    }

    @EventHandler
    public void onPlayerChunkUnload(PlayerChunkUnloadEvent event) {
        final var waypoint = waypointMap.getWaypoint(event.getChunk().getChunkKey());

        if (waypoint == null || waypoint.getLocation().getWorld() != event.getWorld()) {
            return;
        }

        hologramMap.hide(waypoint, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.isBlockInHand() || event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        final var clickedBlock = event.getClickedBlock();
        final var player = event.getPlayer();
        final var task = travelerMap.getTask(player, TravelerTask.class);

        if (!waypointMap.isWaypoint(clickedBlock)) {
            if (task == null || !Tag.ITEMS_BANNERS.isTagged(clickedBlock.getType())) {
                return;
            }
            switch (task.getType()) {
                case SETCAMP -> {
                    if (plugin.getWorldCamp().contains(clickedBlock.getWorld().getName())) {
                        travelerMap.getOrCreateTraveler(player).setCamp(clickedBlock.getLocation());
                        player.sendMessage(Component.text("You assigned your camp!", NamedTextColor.GREEN));
                    }
                }
                case SETHOME -> {
                    if (plugin.getWorldHome().contains(clickedBlock.getWorld().getName())) {
                        travelerMap.getOrCreateTraveler(player).setHome(clickedBlock.getLocation());
                        player.sendMessage(Component.text("You assigned your home!", NamedTextColor.GREEN));
                    }
                }
                default -> {
                    return;
                }
            }
            travelerMap.unregisterTask(player);
            return;
        }

        final var waypoint = waypointMap.getNearbyWaypoint(clickedBlock);

        if (task == null) {
            if (waypoint.isActive()) {
                final var traveler = travelerMap.getOrCreateTraveler(player);
                if (!traveler.hasWaypoint(waypoint)) {
                    traveler.registerWaypoint(waypoint);
                    player.sendMessage(Component.text("You registered a waypoint!", NamedTextColor.GOLD));
                }
            } else {
                final var tokenRequirement = plugin.getWaypointTokenRequirement();
                final var contributors = waypoint.getContributorNames();
                final var contributorNamesBuilder = Component.text()
                    .color(NamedTextColor.GOLD);
                if (contributors.isEmpty()) {
                    contributorNamesBuilder.append(Component.text("Nobody has contributed to this waypoint!"));
                } else {
                    contributorNamesBuilder.append(Component.text("Contributors: " + String.join(", ", contributors)));
                }
                player.sendMessage(contributorNamesBuilder.build());
                sendActionBar(player, contributors.size(), tokenRequirement);
            }
            return;
        }

        switch (task.getType()) {
            case ACTIVATE -> {
                waypoint.activate();
                hologramMap.updateTrackedPlayers(waypoint, player);
            }
            case ADDTOKEN -> {
                if (waypoint.isActive()) {
                    return;
                }
                final var tokenRequirement = plugin.getWaypointTokenRequirement();
                final var contributors = waypoint.getContributors();
                final var traveler = travelerMap.getOrCreateTraveler(player);
                final var tokens = traveler.getTokens();
                if (tokens > 0) {
                    player.sendMessage(Component.text("You added a token!", NamedTextColor.BLUE));
                    traveler.setTokens(tokens - 1);
                    contributors.add(player.getUniqueId());
                    if (contributors.size() >= tokenRequirement) {
                        waypoint.activate();
                        hologramMap.updateTrackedPlayers(waypoint, player);
                    }
                }
                sendActionBar(player, contributors.size(), tokenRequirement);
            }
            case CREATE -> {
                if (waypoint.isActive()) {
                    return;
                }
                waypointMap.removeWaypoint(waypoint);
                final var maxTokens = plugin.getTravelerMaxTokens();
                for (final var uniqueId : waypoint.getContributors()) {
                    final var traveler = travelerMap.getOrCreateTraveler(uniqueId);
                    traveler.setTokens(Math.min(traveler.getTokens() + 1, maxTokens));
                }
                hologramMap.remove(waypoint);
            }
            case DELETE -> {
                waypointMap.removeWaypoint(waypoint);
                travelerMap.removeWaypoint(waypoint);
                final var maxTokens = plugin.getTravelerMaxTokens();
                for (final var uniqueId : waypoint.getContributors()) {
                    final var traveler = travelerMap.getOrCreateTraveler(uniqueId);
                    traveler.setTokens(Math.min(traveler.getTokens() + 1, maxTokens));
                }
                hologramMap.remove(waypoint);
            }
            case REMOVETOKEN -> {
                if (waypoint.isActive()) {
                    return;
                }
                final var uniqueId = player.getUniqueId();
                final var contributors = waypoint.getContributors();
                if (contributors.contains(uniqueId)) {
                    player.sendMessage(Component.text("You removed a token!", NamedTextColor.BLUE));
                    final var maxTokens = plugin.getTravelerMaxTokens();
                    final var traveler = travelerMap.getOrCreateTraveler(player);
                    traveler.setTokens(Math.min(traveler.getTokens() + 1, maxTokens));
                    contributors.remove(uniqueId);
                }
                final var tokenRequirement = plugin.getWaypointTokenRequirement();
                sendActionBar(player, contributors.size(), tokenRequirement);
            }
            default -> {
                return;
            }
        }
        travelerMap.unregisterTask(player);
    }

    private static void sendActionBar(Player player, int contributorsSize, int tokenRequirement) {
        player.sendActionBar(Component.text(String.format("%d / %d", contributorsSize, tokenRequirement)));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final var player = event.getPlayer();
        travelerMap.getOrCreateTraveler(player).startRegenCharge(plugin);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final var player = event.getPlayer();
        travelerMap.getOrCreateTraveler(player).stopRegenCharge();
        travelerMap.unregisterTask(player);
        hologramMap.remove(player);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)
                || !plugin.isToken(event.getEntity())) {
            return;
        }

        event.setCancelled(true);
        final var playerInventory = player.getInventory();
        if (plugin.isToken(playerInventory.getItemInMainHand())) {
            playerInventory.setItemInMainHand(playerInventory.getItemInMainHand().subtract());
            player.playEffect(EntityEffect.BREAK_EQUIPMENT_MAIN_HAND);
        } else if (plugin.isToken(playerInventory.getItemInOffHand())) {
            playerInventory.setItemInOffHand(playerInventory.getItemInOffHand().subtract());
            player.playEffect(EntityEffect.BREAK_EQUIPMENT_OFF_HAND);
        } else {
            return;
        }

        final var maxTokens = plugin.getTravelerMaxTokens();
        final var traveler = travelerMap.getOrCreateTraveler(player);
        traveler.setTokens(Math.min(traveler.getTokens() + 1, maxTokens));
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        plugin.loadData();
    }
}

package com.hero.sigil.persistence;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import com.mojang.brigadier.CommandDispatcher;

/**
 * Event handlers for Hero Sigil data persistence.
 */
@EventBusSubscriber(modid = "herosigil", bus = EventBusSubscriber.Bus.GAME)
public class CapabilityEvents {

    private static final String TAG_KEY = "HeroSigil";

    /**
     * Save player data when they disconnect.
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.LoggedOutEvent event) {
        if (event.getPlayer() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            CompoundTag persistData = serverPlayer.getPersistentData();

            // Save buff states and achievement progress to persistent data
            com.hero.sigil.buffs.HeroSigilData.onSave(serverPlayer, persistData);

            com.hero.sigil.HeroSigil.LOGGER.debug(
                "Saved Hero Sigil data for player: {}",
                serverPlayer.getScoreboardName()
            );
        }
    }

    /**
     * Load player data when they connect.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            CompoundTag persistData = serverPlayer.getPersistentData();

            // Load buff states and achievement progress from persistent data
            com.hero.sigil.buffs.HeroSigilData.onLoad(serverPlayer, persistData);

            com.hero.sigil.HeroSigil.LOGGER.info(
                "Loaded Hero Sigil data for player: {}",
                serverPlayer.getScoreboardName()
            );
        }
    }

    /**
     * Handle player death - preserve or reset data based on config.
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            net.minecraft.world.entity.player.Player original = event.getOriginal();

            if (original instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                CompoundTag origPersistData = serverPlayer.getPersistentData();

                // Get the saved Hero Sigil data from original player
                if (origPersistData.contains(TAG_KEY)) {
                    CompoundTag sigilNbt = origPersistData.getCompound(TAG_KEY);

                    // Copy to new player's persistent data
                    net.minecraft.server.level.ServerPlayer newPlayer =
                        (net.minecraft.server.level.ServerPlayer) event.getEntity();
                    CompoundTag newPersistData = newPlayer.getPersistentData();

                    if (!newPersistData.contains(TAG_KEY)) {
                        newPersistData.put(TAG_KEY, sigilNbt.copy());

                        // Load the data into buff system
                        com.hero.sigil.buffs.HeroSigilData.onLoad(
                            event.getEntity(),
                            newPersistData
                        );
                    }
                }
            }
        }
    }

    /**
     * 注册勇者之证相关命令
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<net.minecraft.server.level.ServerPlayer> dispatcher = event.getDispatcher();

        // /herosigil recover - 从备份恢复数据
        dispatcher.register(
            com.mojang.brigadier.Command.literal("herosigil")
                .then(com.mojang.brigadier.Command.literal("recover")
                    .executes(context -> {
                        var player = context.getSource().getPlayer();
                        if (player == null) {
                            return 0;
                        }

                        CompoundTag persistData = player.getPersistentData();
                        com.hero.sigil.data.DataRecovery.RecoveryResult result =
                            com.hero.sigil.data.DataRecovery.restoreFromBackup(player, persistData);

                        if (result.isSuccess()) {
                            player.sendSystemMessage(
                                net.minecraft.network.chat.Component.literal(
                                    "§a§l勇者之证§r§f: 数据已从备份恢复 (恢复 " + result.getRestoredSlots() + " 个 buff 槽位)"
                                )
                            );
                        } else {
                            player.sendSystemMessage(
                                net.minecraft.network.chat.Component.literal(
                                    "§c§l勇者之证§r§f: " + result.getMessage()
                                )
                            );
                        }

                        return 1;
                    })
                )
        );

        // /herosigil backup - 手动创建备份
        dispatcher.register(
            com.mojang.brigadier.Command.literal("herosigil")
                .then(com.mojang.brigadier.Command.literal("backup")
                    .executes(context -> {
                        var player = context.getSource().getPlayer();
                        if (player == null) {
                            return 0;
                        }

                        CompoundTag persistData = player.getPersistentData();
                        com.hero.sigil.data.DataRecovery.createBackup(player, persistData);

                        player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal("§a§l勇者之证§r§f: 已创建数据备份")
                        );

                        return 1;
                    })
                )
        );

        // /herosigil history - 查看恢复历史
        dispatcher.register(
            com.mojang.brigadier.Command.literal("herosigil")
                .then(com.mojang.brigadier.Command.literal("history")
                    .executes(context -> {
                        var player = context.getSource().getPlayer();
                        if (player == null) {
                            return 0;
                        }

                        com.hero.sigil.data.DataRecovery.showBackupInfo(player);

                        return 1;
                    })
                )
        );

        // /herosigil repair - 修复数据不一致
        dispatcher.register(
            com.mojang.brigadier.Command.literal("herosigil")
                .then(com.mojang.brigadier.Command.literal("repair")
                    .executes(context -> {
                        var player = context.getSource().getPlayer();
                        if (player == null) {
                            return 0;
                        }

                        com.hero.sigil.data.DataRecovery.fixDataInconsistency(player);
                        return 1;
                    })
                )
        );

        // /herosigil validate - 验证数据完整性
        dispatcher.register(
            com.mojang.brigadier.Command.literal("herosigil")
                .then(com.mojang.brigadier.Command.literal("validate")
                    .executes(context -> {
                        var player = context.getSource().getPlayer();
                        if (player == null) {
                            return 0;
                        }

                        CompoundTag persistData = player.getPersistentData();
                        if (persistData.contains("HeroSigil")) {
                            CompoundTag sigilData = persistData.getCompound("HeroSigil");
                            if (com.hero.sigil.data.DataRecovery.validateData(sigilData)) {
                                player.sendSystemMessage(
                                    net.minecraft.network.chat.Component.literal("§a§l勇者之证§r§f: 数据完整性检查通过")
                                );
                            } else {
                                player.sendSystemMessage(
                                    net.minecraft.network.chat.Component.literal("§c§l勇者之证§r§f: 数据不完整，使用 /herosigil repair 修复")
                                );
                            }
                        } else {
                            player.sendSystemMessage(
                                net.minecraft.network.chat.Component.literal("§e§l勇者之证§r§f: 玩家无勇者之证数据")
                            );
                        }

                        return 1;
                    })
                )
        );
    }

}

package com.putzwirk.gesturalcombat.mastery;

import com.putzwirk.gesturalcombat.Config;
import com.putzwirk.gesturalcombat.core.Archetype;
import com.putzwirk.gesturalcombat.network.MasterySyncPacket;
import com.putzwirk.gesturalcombat.network.ModNetwork;
import com.putzwirk.gesturalcombat.skill.SkillNode;
import com.putzwirk.gesturalcombat.skill.SkillTree;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class MasteryService {
    private MasteryService() {
    }

    public static void award(ServerPlayer player, Archetype archetype, int baseXp) {
        int amount = (int) Math.round(baseXp * Config.INSTANCE.xpMultiplier.get());
        PlayerMastery mastery = MasteryCapability.get(player);
        boolean leveled = mastery.addXp(archetype, amount);
        if (leveled) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.2f);
            player.displayClientMessage(Component.translatable("message.gesturalcombat.levelup",
                    Component.translatable(archetype.nameKey()), mastery.level(archetype)).withStyle(ChatFormatting.GOLD), true);
        }
        sync(player);
    }

    public static boolean unlock(ServerPlayer player, String nodeId) {
        SkillNode node = SkillTree.get(nodeId);
        if (node == null) {
            return false;
        }
        PlayerMastery mastery = MasteryCapability.get(player);
        if (!mastery.unlock(node)) {
            return false;
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.4f);
        player.displayClientMessage(Component.translatable("message.gesturalcombat.unlocked", Component.translatable(node.nameKey())).withStyle(ChatFormatting.AQUA), true);
        sync(player);
        return true;
    }

    public static void sync(ServerPlayer player) {
        ModNetwork.sendToPlayer(player, new MasterySyncPacket(MasteryCapability.get(player).save()));
    }
}

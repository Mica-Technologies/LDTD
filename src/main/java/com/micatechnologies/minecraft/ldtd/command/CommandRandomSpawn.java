package com.micatechnologies.minecraft.ldtd.command;

import com.micatechnologies.minecraft.ldtd.LdtdConfig;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;

/**
 * {@code /randomspawn} (alias {@code /rs}): teleports you somewhere random in the world, for
 * finding fresh ground for a Hearth. See {@link RandomSpawn} for where it looks.
 *
 * <p>Open to every player by default ({@code randomSpawn.permissionLevel}), with a per-player
 * cooldown that operators skip. Operators may send another player: {@code /rs <player>}.</p>
 */
public final class CommandRandomSpawn extends CommandBase {

    private final Map<UUID, Long> lastUse = new HashMap<>();

    @Override
    public String getName() {
        return "randomspawn";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("rs");
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.ldtd.randomspawn.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return LdtdConfig.randomSpawnPermissionLevel;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!LdtdConfig.randomSpawnEnabled) {
            throw new CommandException("commands.ldtd.randomspawn.disabled");
        }
        EntityPlayerMP target;
        boolean op = sender.canUseCommand(2, "");
        if (args.length >= 1) {
            if (!op) {
                throw new CommandException("commands.ldtd.randomspawn.others");
            }
            target = getPlayer(server, sender, args[0]);
        } else {
            target = getCommandSenderAsPlayer(sender);
        }

        long now = System.currentTimeMillis();
        Long last = lastUse.get(target.getUniqueID());
        long cooldownMs = LdtdConfig.randomSpawnCooldownSeconds * 1000L;
        if (!op && last != null && now - last < cooldownMs) {
            throw new CommandException("commands.ldtd.randomspawn.cooldown", (cooldownMs - (now - last) + 999) / 1000);
        }

        WorldServer world = target.getServerWorld();
        if (world.provider.isNether() || !world.provider.hasSkyLight()) {
            throw new CommandException("commands.ldtd.randomspawn.dimension");
        }
        BlockPos spot = RandomSpawn.find(world, LdtdConfig.randomSpawnFallbackRadius, world.rand);
        if (spot == null) {
            throw new CommandException("commands.ldtd.randomspawn.none");
        }
        if (target.isRiding()) {
            target.dismountRidingEntity();
        }
        target.fallDistance = 0.0F;
        target.setPositionAndUpdate(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D);
        lastUse.put(target.getUniqueID(), now);
        notifyCommandListener(sender, this, "commands.ldtd.randomspawn.ok", target.getName(), spot.getX(),
            spot.getY(), spot.getZ());
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                          @Nullable BlockPos targetPos) {
        return args.length == 1 && sender.canUseCommand(2, "")
            ? getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames()) : Collections.emptyList();
    }
}

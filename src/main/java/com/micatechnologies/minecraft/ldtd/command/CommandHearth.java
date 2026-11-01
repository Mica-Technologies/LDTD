package com.micatechnologies.minecraft.ldtd.command;

import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry.HearthRecord;
import com.micatechnologies.minecraft.ldtd.hearth.TileEntityHearth;
import com.mojang.authlib.GameProfile;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.server.command.CommandTreeBase;

/**
 * {@code /hearth}: what a Hearth's owner manages, for the territory they stand in.
 *
 * <ul>
 *   <li>{@code /hearth trust <player>}: let a player build and use the stockpile here.</li>
 *   <li>{@code /hearth untrust <player>}</li>
 *   <li>{@code /hearth trusted}: list them.</li>
 * </ul>
 * <p>Open to every player. Only the owner (or an operator) can change the list.</p>
 */
public final class CommandHearth extends CommandTreeBase {

    public CommandHearth() {
        addSubcommand(new Trust(true));
        addSubcommand(new Trust(false));
        addSubcommand(new ListTrusted());
        addSubcommand(new GuideCopy());
    }

    @Override
    public String getName() {
        return "hearth";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.hearth.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    /** The Hearth whose territory the sender stands in, which the sender must own (or be an operator). */
    private static TileEntityHearth ownedHearthHere(ICommandSender sender, boolean mustOwn) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        World world = player.world;
        BlockPos at = player.getPosition();
        HearthRecord r = HearthRegistry.get(world).territoryAt(at.getX(), at.getZ());
        if (r == null) {
            throw new CommandException("commands.ldtd.hearth.not_inside");
        }
        TileEntity te = world.getTileEntity(r.pos);
        if (!(te instanceof TileEntityHearth)) {
            throw new CommandException("commands.ldtd.hearth.not_loaded");
        }
        TileEntityHearth hearth = (TileEntityHearth) te;
        if (mustOwn && !hearth.isOwner(player) && !player.canUseCommand(2, "")) {
            throw new CommandException("commands.hearth.not_owner");
        }
        return hearth;
    }

    private static final class Trust extends CommandBase {

        private final boolean add;

        Trust(boolean add) {
            this.add = add;
        }

        @Override
        public String getName() {
            return add ? "trust" : "untrust";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return add ? "commands.hearth.trust.usage" : "commands.hearth.untrust.usage";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
            return true;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (args.length != 1) {
                throw new WrongUsageException(getUsage(sender));
            }
            TileEntityHearth hearth = ownedHearthHere(sender, true);
            UUID id = null;
            String name = args[0];
            GameProfile profile = server.getPlayerProfileCache().getGameProfileForUsername(name);
            if (profile != null) {
                id = profile.getId();
                name = profile.getName();
            }
            if (add) {
                if (id == null) {
                    throw new CommandException("commands.hearth.unknown_player", args[0]);
                }
                sender.sendMessage(new TextComponentTranslation(hearth.trust(id, name)
                    ? "commands.hearth.trust.ok" : "commands.hearth.trust.already", name));
            } else {
                if (id == null) {
                    // Fall back to the stored names, for players the cache has forgotten.
                    for (Map.Entry<UUID, String> e : hearth.trusted().entrySet()) {
                        if (e.getValue().equalsIgnoreCase(args[0])) {
                            id = e.getKey();
                        }
                    }
                }
                sender.sendMessage(new TextComponentTranslation(id != null && hearth.untrust(id)
                    ? "commands.hearth.untrust.ok" : "commands.hearth.untrust.not", name));
            }
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                              @Nullable BlockPos targetPos) {
            return args.length == 1 ? getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames())
                : Collections.emptyList();
        }
    }

    /** {@code /hearth guide}: a fresh copy of the Hearthkeeper's Guide. */
    private static final class GuideCopy extends CommandBase {

        @Override
        public String getName() {
            return "guide";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "commands.hearth.guide.usage";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
            return true;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            com.micatechnologies.minecraft.ldtd.guide.Guide.give(getCommandSenderAsPlayer(sender));
            sender.sendMessage(new TextComponentTranslation("commands.hearth.guide.given"));
        }
    }

    private static final class ListTrusted extends CommandBase {

        @Override
        public String getName() {
            return "trusted";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "commands.hearth.trusted.usage";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
            return true;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            TileEntityHearth hearth = ownedHearthHere(sender, false);
            if (hearth.trusted().isEmpty()) {
                sender.sendMessage(new TextComponentTranslation("commands.hearth.trusted.none", hearth.ownerName()));
            } else {
                sender.sendMessage(new TextComponentTranslation("commands.hearth.trusted.list", hearth.ownerName(),
                    String.join(", ", hearth.trusted().values())));
            }
        }
    }
}

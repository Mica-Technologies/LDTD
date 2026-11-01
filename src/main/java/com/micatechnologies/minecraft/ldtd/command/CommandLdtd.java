package com.micatechnologies.minecraft.ldtd.command;

import com.micatechnologies.minecraft.ldtd.data.LdtdData;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry.HearthRecord;
import com.micatechnologies.minecraft.ldtd.hearth.TileEntityHearth;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.server.command.CommandTreeBase;

/**
 * {@code /ldtd}: admin and debug commands. Operators only (permission level 2).
 *
 * <ul>
 *   <li>{@code /ldtd reload}: re-read the JSON data files and re-sync loaded Hearths.</li>
 *   <li>{@code /ldtd hearth info}: the territory you are standing in, or the nearest Hearth.</li>
 *   <li>{@code /ldtd hearth list}: every Hearth in your dimension.</li>
 *   <li>{@code /ldtd hearth setlevel <level>}: set the level of the Hearth whose territory you're in.</li>
 * </ul>
 */
public final class CommandLdtd extends CommandTreeBase {

    public CommandLdtd() {
        addSubcommand(new Reload());
        addSubcommand(new HearthTree());
    }

    @Override
    public String getName() {
        return "ldtd";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.ldtd.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    private static final class Reload extends CommandBase {

        @Override
        public String getName() {
            return "reload";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "commands.ldtd.reload.usage";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
            List<String> problems = LdtdData.reload();
            for (WorldServer world : server.worlds) {
                for (HearthRecord r : HearthRegistry.get(world).all()) {
                    if (world.isBlockLoaded(r.pos)) {
                        TileEntity te = world.getTileEntity(r.pos);
                        if (te instanceof TileEntityHearth) {
                            ((TileEntityHearth) te).resync();
                        }
                    }
                }
            }
            if (problems.isEmpty()) {
                sender.sendMessage(new TextComponentTranslation("commands.ldtd.reload.ok"));
            } else {
                sender.sendMessage(new TextComponentTranslation("commands.ldtd.reload.problems", problems.size()));
                for (String p : problems) {
                    sender.sendMessage(new TextComponentTranslation("commands.ldtd.reload.problem", p));
                }
            }
        }
    }

    private static final class HearthTree extends CommandTreeBase {

        HearthTree() {
            addSubcommand(new Info());
            addSubcommand(new ListAll());
            addSubcommand(new SetLevel());
        }

        @Override
        public String getName() {
            return "hearth";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "commands.ldtd.hearth.usage";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }
    }

    private static final class Info extends CommandBase {

        @Override
        public String getName() {
            return "info";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "commands.ldtd.hearth.info.usage";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
            World world = sender.getEntityWorld();
            BlockPos at = sender.getPosition();
            HearthRegistry registry = HearthRegistry.get(world);
            HearthRecord inside = registry.territoryAt(at.getX(), at.getZ());
            if (inside != null) {
                sender.sendMessage(describe("commands.ldtd.hearth.info.inside", inside));
                return;
            }
            HearthRecord nearest = registry.nearest(at.getX(), at.getZ());
            if (nearest == null) {
                sender.sendMessage(new TextComponentTranslation("commands.ldtd.hearth.none"));
            } else {
                sender.sendMessage(describe("commands.ldtd.hearth.info.nearest", nearest));
            }
        }
    }

    private static final class ListAll extends CommandBase {

        @Override
        public String getName() {
            return "list";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "commands.ldtd.hearth.list.usage";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
            HearthRegistry registry = HearthRegistry.get(sender.getEntityWorld());
            if (registry.all().isEmpty()) {
                sender.sendMessage(new TextComponentTranslation("commands.ldtd.hearth.none"));
                return;
            }
            for (HearthRecord r : registry.all()) {
                sender.sendMessage(describe("commands.ldtd.hearth.list.entry", r));
            }
        }
    }

    private static final class SetLevel extends CommandBase {

        @Override
        public String getName() {
            return "setlevel";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "commands.ldtd.hearth.setlevel.usage";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (args.length != 1) {
                throw new WrongUsageException(getUsage(sender));
            }
            int level = parseInt(args[0], 1, LdtdData.hearthLevels().maxLevel());
            World world = sender.getEntityWorld();
            BlockPos at = sender.getPosition();
            HearthRecord inside = HearthRegistry.get(world).territoryAt(at.getX(), at.getZ());
            if (inside == null) {
                throw new CommandException("commands.ldtd.hearth.not_inside");
            }
            TileEntity te = world.getTileEntity(inside.pos);
            if (!(te instanceof TileEntityHearth)) {
                throw new CommandException("commands.ldtd.hearth.not_loaded");
            }
            ((TileEntityHearth) te).setLevel(level);
            sender.sendMessage(new TextComponentTranslation("commands.ldtd.hearth.setlevel.ok", level,
                LdtdData.hearthLevels().get(level).radius));
        }
    }

    private static TextComponentTranslation describe(String key, HearthRecord r) {
        return new TextComponentTranslation(key, r.pos.getX(), r.pos.getY(), r.pos.getZ(),
            r.ownerName.isEmpty() ? "-" : r.ownerName, r.level, r.radius());
    }
}

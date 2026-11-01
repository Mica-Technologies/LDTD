package com.micatechnologies.minecraft.ldtd.command;

import com.micatechnologies.minecraft.ldtd.LdtdConstants;
import com.micatechnologies.minecraft.ldtd.data.LdtdData;
import com.micatechnologies.minecraft.ldtd.gloam.EntityGloam;
import com.micatechnologies.minecraft.ldtd.gloam.GloamKind;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry.HearthRecord;
import com.micatechnologies.minecraft.ldtd.hearth.TileEntityHearth;
import com.micatechnologies.minecraft.ldtd.raid.RaidDirector;
import com.micatechnologies.minecraft.ldtd.route.RouteEvents;
import com.micatechnologies.minecraft.ldtd.route.RouteService;
import com.micatechnologies.minecraft.ldtd.sim.route.Route;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.EntityList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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
 *   <li>{@code /ldtd spawn <unit> [count]}: spawn Gloam marching on the nearest Hearth.</li>
 *   <li>{@code /ldtd route show}: draw the raid route from where you stand to the nearest Hearth.</li>
 *   <li>{@code /ldtd raid start [called]|stop}: start or stop a raid on the territory you're in, any time.</li>
 * </ul>
 */
public final class CommandLdtd extends CommandTreeBase {

    public CommandLdtd() {
        addSubcommand(new Reload());
        addSubcommand(new HearthTree());
        addSubcommand(new Spawn());
        addSubcommand(new RouteTree());
        addSubcommand(new Debug());
        addSubcommand(new RaidTree());
    }

    /** {@code /ldtd raid start [called]|stop}: run raids on the territory you stand in, any time. */
    private static final class RaidTree extends CommandTreeBase {

        RaidTree() {
            addSubcommand(new RaidStart());
            addSubcommand(new RaidStop());
        }

        @Override
        public String getName() {
            return "raid";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "commands.ldtd.raid.usage";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }
    }

    private static TileEntityHearth hearthHere(ICommandSender sender) throws CommandException {
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
        return (TileEntityHearth) te;
    }

    private static final class RaidStart extends CommandBase {

        @Override
        public String getName() {
            return "start";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "commands.ldtd.raid.start.usage";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            boolean called = args.length > 0 && "called".equals(args[0]);
            if (!RaidDirector.force(sender.getEntityWorld(), hearthHere(sender), called)) {
                throw new CommandException("commands.ldtd.raid.start.refused");
            }
        }
    }

    private static final class RaidStop extends CommandBase {

        @Override
        public String getName() {
            return "stop";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "commands.ldtd.raid.stop.usage";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (!RaidDirector.stop(sender.getEntityWorld(), hearthHere(sender).getPos())) {
                throw new CommandException("commands.ldtd.raid.stop.none");
            }
        }
    }

    /**
     * {@code /ldtd debug keepticking <true|false>}: keep entities ticking with nobody online, for
     * headless dedicated-server tests. Resets when the server stops.
     */
    private static final class Debug extends CommandBase {

        @Override
        public String getName() {
            return "debug";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "commands.ldtd.debug.usage";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (args.length != 2 || !"keepticking".equals(args[0])) {
                throw new WrongUsageException(getUsage(sender));
            }
            RouteEvents.keepTicking = parseBoolean(args[1]);
            sender.sendMessage(new TextComponentTranslation("commands.ldtd.debug.keepticking", RouteEvents.keepTicking));
        }
    }

    /**
     * {@code /ldtd spawn <unit> [count]}: spawns Gloam at your position, marching on the nearest
     * Hearth. A testing tool until raids exist (Phase 4).
     */
    private static final class Spawn extends CommandBase {

        @Override
        public String getName() {
            return "spawn";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "commands.ldtd.spawn.usage";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (args.length < 1 || args.length > 2) {
                throw new WrongUsageException(getUsage(sender));
            }
            GloamKind kind = null;
            for (GloamKind k : GloamKind.values()) {
                if (k.id().equals(args[0])) {
                    kind = k;
                }
            }
            if (kind == null) {
                throw new CommandException("commands.ldtd.spawn.unknown", args[0]);
            }
            int count = args.length == 2 ? parseInt(args[1], 1, 64) : 1;
            World world = sender.getEntityWorld();
            Vec3d at = sender.getPositionVector();
            HearthRecord hearth = HearthRegistry.get(world).nearest((int) at.x, (int) at.z);
            for (int i = 0; i < count; i++) {
                EntityGloam mob = (EntityGloam) EntityList.newEntity(EntityList.getClass(
                    new ResourceLocation(LdtdConstants.MOD_NAMESPACE, kind.registryPath())), world);
                if (mob == null) {
                    throw new CommandException("commands.ldtd.spawn.unknown", args[0]);
                }
                double ox = (world.rand.nextDouble() - 0.5D) * Math.min(count, 6);
                double oz = (world.rand.nextDouble() - 0.5D) * Math.min(count, 6);
                mob.setLocationAndAngles(at.x + ox, at.y, at.z + oz, world.rand.nextFloat() * 360.0F, 0.0F);
                mob.onInitialSpawn(world.getDifficultyForLocation(mob.getPosition()), null);
                world.spawnEntity(mob);
                if (hearth != null) {
                    mob.marchOn(hearth);
                }
            }
            sender.sendMessage(new TextComponentTranslation(hearth == null
                ? "commands.ldtd.spawn.ok_idle" : "commands.ldtd.spawn.ok", count, args[0]));
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                              @Nullable BlockPos targetPos) {
            return args.length == 1 ? getListOfStringsMatchingLastWord(args, GloamKind.ids()) : Collections.emptyList();
        }
    }

    private static final class RouteTree extends CommandTreeBase {

        RouteTree() {
            addSubcommand(new RouteShow());
        }

        @Override
        public String getName() {
            return "route";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "commands.ldtd.route.usage";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }
    }

    /**
     * {@code /ldtd route show}: plans a raid route from where you stand to the nearest Hearth and
     * draws it for ten seconds. End-rod sparks mark the march, witch sparkles mark the blocks it
     * would break.
     */
    private static final class RouteShow extends CommandBase {

        @Override
        public String getName() {
            return "show";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "commands.ldtd.route.show.usage";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            World world = sender.getEntityWorld();
            BlockPos at = sender.getPosition();
            HearthRecord hearth = HearthRegistry.get(world).nearest(at.getX(), at.getZ());
            if (hearth == null) {
                throw new CommandException("commands.ldtd.hearth.none");
            }
            Route route = RouteService.get(world).route(world, at, hearth, true);
            if (route == null) {
                sender.sendMessage(new TextComponentTranslation("commands.ldtd.route.none"));
                return;
            }
            RouteEvents.show((WorldServer) world, route);
            sender.sendMessage(new TextComponentTranslation("commands.ldtd.route.show.ok",
                route.points.size(), route.breaches.size(), String.format("%.1f", route.cost)));
        }
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
            addSubcommand(new SetOwner());
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

    /** {@code /ldtd hearth setowner <player>}: hand the Hearth whose territory you're in to someone else. */
    private static final class SetOwner extends CommandBase {

        @Override
        public String getName() {
            return "setowner";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "commands.ldtd.hearth.setowner.usage";
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
            TileEntityHearth hearth = hearthHere(sender);
            com.mojang.authlib.GameProfile profile = server.getPlayerProfileCache().getGameProfileForUsername(args[0]);
            java.util.UUID id = profile != null ? profile.getId()
                : net.minecraft.entity.player.EntityPlayer.getOfflineUUID(args[0]);
            String name = profile != null ? profile.getName() : args[0];
            hearth.setOwner(id, name);
            sender.sendMessage(new TextComponentTranslation("commands.ldtd.hearth.setowner.ok", name));
        }
    }

    private static TextComponentTranslation describe(String key, HearthRecord r) {
        return new TextComponentTranslation(key, r.pos.getX(), r.pos.getY(), r.pos.getZ(),
            r.ownerName.isEmpty() ? "-" : r.ownerName, r.level, r.radius());
    }
}

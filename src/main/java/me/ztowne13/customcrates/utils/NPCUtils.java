package me.ztowne13.customcrates.utils;

import me.ztowne13.customcrates.SpecializedCrates;
import me.ztowne13.customcrates.crates.PlacedCrate;
import me.ztowne13.customcrates.crates.types.display.npcs.Citizens2NPCPlaceHolder;
import me.ztowne13.customcrates.crates.types.display.npcs.IdentifierTrait;
import me.ztowne13.customcrates.crates.types.display.npcs.MobPlaceholder;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.trait.LookClose;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Iterator;

/**
 * Created by ztowne13 on 2/26/16.
 */
public class NPCUtils
{
    static boolean failedLoad = false;
    static boolean disabledMobReload = true;

    public static void checkUncheckMobs(boolean b)
    {
        if (disabledMobReload)
            return;

        if (isCitizensInstalled())
        {
            for (NPCRegistry npcr : CitizensAPI.getNPCRegistries())
            {
                Iterator<NPC> npcs = npcr.iterator();

                for (Object obj : Utils.iteratorToList(npcs))
                {
                    NPC npc = (NPC) obj;

                    if (npc.hasTrait(IdentifierTrait.class) && npc.getTrait(IdentifierTrait.class).isCrate())
                    {
                        if (!b)
                        {
                            b = true;
                            for (NPC npck : MobPlaceholder.getMobs().values())
                            {
                                if (npck.getUniqueId().equals(npc.getUniqueId()))
                                {
                                    b = false;
                                }
                            }

                            for (NPC npck : Citizens2NPCPlaceHolder.getNpcs().values())
                            {
                                if (npck.getUniqueId().equals(npc.getUniqueId()))
                                {
                                    b = false;
                                }
                            }
                        }

                        if (b)
                        {
                            npc.destroy();
                        }
                    }
                }
            }
        }
    }

    public static void checkUncheckMobs(SpecializedCrates cc, final boolean bVal, long l)
    {
        Bukkit.getScheduler().scheduleSyncDelayedTask(cc, new Runnable()
        {
            @Override
            public void run()
            {
                NPCUtils.checkUncheckMobs(bVal);
            }
        }, l);
    }

    public static NPC getNpcForCrate(PlacedCrate placedCrate)
    {
        if (isCitizensInstalled())
        {
            for (NPCRegistry npcr : CitizensAPI.getNPCRegistries())
            {
                Iterator<NPC> npcs = npcr.iterator();

                for (Object obj : Utils.iteratorToList(npcs))
                {
                    NPC npc = (NPC) obj;

                    if(!npc.isSpawned() || !npc.getName().equalsIgnoreCase("Specialized Crate - Crate"))
                        continue;

                    //if (npc.hasTrait(IdentifierTrait.class) && npc.getTrait(IdentifierTrait.class).isCrate())
                    {
                        Location storedLoc = npc.getStoredLocation();
                        Location crateLoc = LocationUtils.getLocationCentered(placedCrate.getL());
                        if (Math.abs(storedLoc.getX() - crateLoc.getX()) < .3 &&
                                Math.abs(storedLoc.getY() - crateLoc.getY()) < 2 &&
                                        Math.abs(storedLoc.getZ() - crateLoc.getZ()) < .3 &&
                                                storedLoc.getWorld() == crateLoc.getWorld())
                        {
                            return npc;
                        }
                    }

                }
            }
        }
        return null;
    }

    public static boolean npcExists(PlacedCrate placedCrate)
    {
        return getNpcForCrate(placedCrate) != null;
    }

    public static void applyDefaultInfo(NPC npc)
    {
        npc.getTrait(LookClose.class).toggle();

        npc.data().remove(NPC.AMBIENT_SOUND_METADATA);
        npc.data().set(NPC.SILENT_METADATA, true);
        npc.data().set(NPC.DEFAULT_PROTECTED_METADATA, true);

        npc.data().setPersistent(NPC.NAMEPLATE_VISIBLE_METADATA, false);
    }

    public static boolean isCitizensInstalled()
    {
        return Utils.isPLInstalled("Citizens") && !failedLoad;
    }

    public static void load(boolean firstEnable)
    {
        try
        {
            if (firstEnable && isCitizensInstalled())
            {
                CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(IdentifierTrait.class).withName("isCrate"));
            }
        }
        catch (Exception exc)
        {
            failedLoad = true;
            ChatUtils.log("ERROR: FAILED TO ESTABLISH LINK WITH CITIZENS, DISABLING CITIZENS FEATURES");
        }
    }
}

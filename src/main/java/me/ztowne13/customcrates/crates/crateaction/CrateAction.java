package me.ztowne13.customcrates.crates.crateaction;

import me.ztowne13.customcrates.Messages;
import me.ztowne13.customcrates.SettingsValues;
import me.ztowne13.customcrates.SpecializedCrates;
import me.ztowne13.customcrates.crates.Crate;
import me.ztowne13.customcrates.crates.CrateSettings;
import me.ztowne13.customcrates.crates.CrateState;
import me.ztowne13.customcrates.crates.PlacedCrate;
import me.ztowne13.customcrates.crates.options.ObtainType;
import me.ztowne13.customcrates.crates.options.rewards.Reward;
import me.ztowne13.customcrates.players.PlayerDataManager;
import me.ztowne13.customcrates.players.PlayerManager;
import me.ztowne13.customcrates.players.data.events.CrateCooldownEvent;
import me.ztowne13.customcrates.players.data.events.HistoryEvent;
import me.ztowne13.customcrates.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public abstract class CrateAction
{

    SpecializedCrates cc;
    Player player;
    Location location;

    public CrateAction(SpecializedCrates cc, Player player, Location location)
    {
        this.cc = cc;
        this.player = player;
        this.location = location;
    }

    public abstract boolean run();

    public boolean useCrate(PlayerManager pm, PlacedCrate cm)
    {
        return useCrate(pm, cm, false);
    }

    public boolean useCrate(PlayerManager pm, PlacedCrate cm, boolean skipAnimation)
    {
        return useCrate(pm, cm, skipAnimation, false);
    }

    public boolean useCrate(PlayerManager pm, PlacedCrate cm, boolean skipAnimation, boolean hasSkipped)
    {
        Player player = pm.getP();
        PlayerDataManager pdm = pm.getPdm();
        Crate crates = cm.getCrates();
        CrateSettings cs = crates.getCs();
        Location location = cm.getL();

        // Player has correct permissions
        if (player.hasPermission(cs.getPermission()) || cs.getPermission().equalsIgnoreCase("no permission"))
        {
            // Player has enough inventory spaces (as defined by value in Config.YML)
            if (isInventoryTooEmpty(cc, player))
            {
                // There is no cooldown or the previous cooldown is over
                CrateCooldownEvent cce = pdm.getCrateCooldownEventByCrates(crates);
                if (cce == null || cce.isCooldownOverAsBoolean())
                {
                    pm.setLastOpenedPlacedCrate(cm);

                    // SHIFT-CLICK OPEN
                    // If the animation needs to be skipped (shift click). Also required to be a static crate
                    if (skipAnimation && cs.getOt().equals(ObtainType.STATIC))
                    {
                        if(pm.isConfirming() || !((Boolean) SettingsValues.SHIFT_CLICK_CONFIRM.getValue(cc)))
                        {
                            if (cs.getCh().canExecuteFor(CrateState.OPEN, CrateState.OPEN, player, !crates.isMultiCrate()))
                            {
                                if(cc.getEconomyHandler().handleCheck(player, crates.getCs().getCost(), true))
                                {
                                    Reward reward = cs.getCr().getRandomReward(player);
                                    ArrayList<Reward> rewards = new ArrayList<>();
                                    rewards.add(reward);
                                    reward.runCommands(player);

                                    cs.getCh().takeKeyFromPlayer(player, false);
                                    new HistoryEvent(Utils.currentTimeParsed(), crates, rewards, true)
                                            .addTo(PlayerManager.get(cc, player).getPdm());
                                    useCrate(pm, cm, true, true);

                                    if (!hasSkipped)
                                    {
                                        crates.tick(location, cm, CrateState.OPEN, player, new ArrayList<Reward>());
                                        pm.setConfirming(false);
                                    }

                                    return true;
                                }
                                else
                                    return false;
                            }

                            if (!hasSkipped)
                                crates.getCs().getCh().playFailToOpen(player);

                            return false;
                        }
                        else
                        {
                            pm.setConfirming(true);
                            Messages.CONFIRM_OPEN_ALL.msgSpecified(cc, player, new String[]{"%timeout%"}, new String[]{SettingsValues.CONFIRM_TIMEOUT.getValue(cc) + ""});
                        }
                        return false;
                    }
                    // NORMAL OPEN
                    else
                    {
                        if(pm.isConfirming() || !((Boolean) SettingsValues.CONFIRM_OPEN.getValue(cc)))
                        {
                            if(cc.getEconomyHandler().handleCheck(player, cs.getCost(), true))
                            {
                                if (cs.getCh().tick(player, location, CrateState.OPEN, !crates.isMultiCrate()))
                                {
                                    // Crate isn't static but it ALSO isn't special handling (i.e. the BLOCK_ CrateTypes)
                                    if (!cs.getOt().equals(ObtainType.STATIC) && !cs.getCt().isSpecialDynamicHandling())
                                    {
                                        cm.delete();
                                        location.getBlock().setType(Material.AIR);
                                    }
                                    new CrateCooldownEvent(crates, System.currentTimeMillis(), true).addTo(pdm);
                                    return !skipAnimation;
                                }
                                cc.getEconomyHandler().failSoReturn(player, cs.getCost());
                                pm.setLastOpenedPlacedCrate(null);
                                return false;
                            }
                            else
                            {
                                cs.getCh().playFailToOpen(player, false);
                                return false;
                            }
                        }
                        else
                        {
                            pm.setConfirming(true);
                            Messages.CONFIRM_OPEN.msgSpecified(cc, player, new String[]{"%timeout%"}, new String[]{SettingsValues.CONFIRM_TIMEOUT.getValue(cc) + ""});
                        }
                        return false;
                    }
                }
                cce.playFailure(pdm);
                return false;
            }
            Messages.INVENTORY_TOO_FULL.msgSpecified(cc, player);
            crates.getCs().getCh().playFailToOpen(player, false);
            return false;
        }
        else
        {
            Messages.NO_PERMISSION_CRATE.msgSpecified(cc, player);
            crates.getCs().getCh().playFailToOpen(player, false);
        }
        return false;
    }

    public boolean updateCooldown(PlayerManager pm)
    {

        boolean b = false;

        long ct = System.currentTimeMillis();
        long diff = ct - pm.getCmdCooldown();

        if (!(diff >= 1000) && !pm.getLastCooldown().equalsIgnoreCase("crate"))
        {
            Messages.WAIT_ONE_SECOND.msgSpecified(cc, pm.getP());

            b = true;
        }
        pm.setLastCooldown("crate");
        pm.setCmdCooldown(ct);
        return b;
    }


    public void createCrateAt(Crate crates, Location l)
    {
        PlacedCrate cm = PlacedCrate.get(cc, l);
        cm.setup(crates, true);
    }

    public static boolean isInventoryTooEmpty(SpecializedCrates cc, Player p)
    {
        return Utils.getOpenInventorySlots(p) >= ((Integer) SettingsValues.REQUIRED_SLOTS.getValue(cc));
    }
}

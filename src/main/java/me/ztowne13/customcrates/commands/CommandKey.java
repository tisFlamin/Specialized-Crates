package me.ztowne13.customcrates.commands;

import me.ztowne13.customcrates.CustomCrates;
import me.ztowne13.customcrates.Messages;
import me.ztowne13.customcrates.crates.Crate;
import me.ztowne13.customcrates.players.PlayerDataManager;
import me.ztowne13.customcrates.players.PlayerManager;
import me.ztowne13.customcrates.players.data.VirtualCrateData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 * Created by ztowne13 on 7/30/16.
 */
public class CommandKey extends Commands implements CommandExecutor
{
    CustomCrates cc;

    public CommandKey(CustomCrates cc)
    {
        super("keys");
        this.cc = cc;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings)
    {
        setCmdSender(commandSender);

        if (canExecute(false, true, "customcrates.keys"))
        {
            Player p = (Player) commandSender;
            PlayerDataManager pdm = PlayerManager.get(cc, p).getPdm();
            msg("&7&l> &b&lVirtual &f&lKeys / Crates");
            for (VirtualCrateData vcd : pdm.getVirtualCrateData().values())
            {
                Crate crate = vcd.getCrate();
                if (vcd.getKeys() > 0 || vcd.getCrates() > 0)
                {
                    msg("&b" + vcd.getCrate().getName() + " &f( " + crate.getCs().getCrateInventoryName() + "&f) ");
                    msg(" &8- Crates: &7" + vcd.getCrates());
                    msg(" &8- Keys: &7" + vcd.getKeys());
                }
            }
        }
        else
        {
            if (commandSender instanceof ConsoleCommandSender)
            {
                msg("This command cannot be run from console. To give virtual crates / keys simply add a -v to the end of the /scrates givekey command.");
                return false;
            }
            msg(Messages.NO_PERMISSIONS.getFromConf(cc).replaceAll("%permission%", "customcrates.keys"));
        }
        return false;
    }

    @Override
    public void msgPage(int page)
    {

    }
}

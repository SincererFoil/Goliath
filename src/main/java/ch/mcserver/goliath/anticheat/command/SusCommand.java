package ch.mcserver.goliath.anticheat.command;

import ch.mcserver.goliath.Goliath;
import com.google.inject.spi.StaticInjectionRequest;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import java.util.UUID;

public class SusCommand implements SimpleCommand {



    @Override
    public void execute(Invocation invocation) {

        String[] args = invocation.arguments();

        if (!(invocation.source() instanceof Player)) return;

        Player player = (Player) invocation.source();

        if (args.length < 1) {
            Goliath.getInstance().getSusInspectionMessenger().sendOpenSusMessage(player.getUniqueId(), "");
        }

        switch (args[0]) {
            case "stop" -> clearActiveSusInspection(player);
           // case "restart" ->
        }
    }

    private void clearActiveSusInspection(Player player) {

        // TODO Check redis for Staff uuid -> remove it + send message


        // Goliath.getInstance().getSusInspectionMessenger().sendStopSusInspectionMessage();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("goliath.guard.command.sus");
    }
}

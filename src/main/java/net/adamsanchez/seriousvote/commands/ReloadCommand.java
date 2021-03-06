package net.adamsanchez.seriousvote.commands;

import net.adamsanchez.seriousvote.SeriousVote;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

/**
 * Created by Adam Sanchez on 4/13/2018.
 */
public class ReloadCommand implements CommandExecutor {

    public CommandResult execute(CommandSource src, CommandContext args) throws
            CommandException {
        if (SeriousVote.getInstance().reloadConfigs()) {
            src.sendMessage(Text.of("Reloaded successfully!"));
        } else {
            src.sendMessage(Text.of("Could not reload properly :( did you break your config?").toBuilder().color(TextColors.RED).build());
        }

        return CommandResult.success();

    }
}

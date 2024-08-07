package com.github.egubot.commands.timers;

import org.javacord.api.entity.message.Message;

import com.github.egubot.facades.ScheduledTasksContext;
import com.github.egubot.interfaces.Command;

public class RemindMeCancelCommand implements Command {

	@Override
	public String getName() {
		return "remindme cancel";
	}

	@Override
	public boolean execute(Message msg, String arguments) throws Exception {
		String authorTag = "<@" + msg.getAuthor().getIdAsString() + ">";
		String reformatted = arguments + " \"parrot " + authorTag + "\"";
		return ScheduledTasksContext.remove(msg, reformatted);
	}

	@Override
	public boolean isStartsWithPrefix() {
		return true;
	}

}

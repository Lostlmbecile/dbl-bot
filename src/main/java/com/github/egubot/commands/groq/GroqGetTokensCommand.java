package com.github.egubot.commands.groq;

import org.javacord.api.entity.message.Message;

import com.github.egubot.facades.AIContext;
import com.github.egubot.interfaces.Command;

public class GroqGetTokensCommand implements Command {

	@Override
	public String getName() {
		return "aa tokens";
	}

	@Override
	public boolean execute(Message msg, String arguments) throws Exception {
		msg.getChannel().sendMessage("```java\nTokens Used: \"" + AIContext.getLlama3().getLastTokens(msg) + "\"```");
		return true;
	}

	@Override
	public boolean isStartsWithPrefix() {
		return false;
	}

}

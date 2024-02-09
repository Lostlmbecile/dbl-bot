package com.github.egubot.facades;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.message.Message;

import com.github.egubot.build.LegendsDatabase;
import com.github.egubot.features.LegendsRoll;
import com.github.egubot.features.LegendsSearch;
import com.github.egubot.interfaces.Shutdownable;
import com.github.egubot.main.KeyManager;
import com.github.egubot.objects.CharacterHash;
import com.github.egubot.shared.SendObjects;
import com.github.egubot.shared.Shared;
import com.github.egubot.storage.ConfigManager;
import com.github.egubot.storage.OnlineDataManager;

public class LegendsCommandsFacade implements Shutdownable {
	private static final Logger logger = LogManager.getLogger(LegendsCommandsFacade.class.getName());
	private boolean backupWebsiteFlag = ConfigManager.getBooleanProperty("Backup_Website_Flag");
	private String wheelChannelID = KeyManager.getID("Wheel_Channel_ID");

	private LegendsDatabase legendsWebsite = null;
	private LegendsTemplatesFacade templates = null;
	private LegendsRoll legendsRoll = null;
	private LegendsSearch legendsSearch = null;
	private boolean isLegendsMode = Shared.isDbLegendsMode();
	private boolean isAnimated = !Shared.isTestMode();

	public LegendsCommandsFacade() {
		if (isLegendsMode) {
			// Fetches data from the legends website and initialises
			// classes that are based on it, or doesn't if that fails
			try {
				System.out.println("\nFetching characters from dblegends.net...");
				legendsWebsite = new LegendsDatabase();

				backupLegendsWebsite();
			} catch (Exception e) {
				logger.warn("\nFailed to build character database. Relevant commands will be inactive.");
				logger.error("\\nFailed to build character database.", e);
				isLegendsMode = false;
			}

			if (isLegendsMode) {
				try {
					templates = new LegendsTemplatesFacade(legendsWebsite);
				} catch (IOException e) {
					isLegendsMode = false;
					logger.error("Failed to initialise roll templates.", e);
					return;
				}

				legendsRoll = new LegendsRoll(legendsWebsite, templates.getRollTemplates());

				legendsSearch = new LegendsSearch(legendsWebsite, templates.getRollTemplates());
			}
		}
	}

	public boolean checkCommands(Message msg, String lowCaseTxt) {
		String channelID = msg.getChannel().getIdAsString();
		if (isLegendsMode) {
			if (lowCaseTxt.matches("b-(?s).*")) {

				if (templates.checkTemplateCommands(msg, lowCaseTxt)) {
					return true;
				}

				try {
					if (lowCaseTxt.contains("b-search")) {
						legendsSearch.search(lowCaseTxt, msg.getChannel());
						return true;
					}

					if (lowCaseTxt.contains("b-roll")) {
						String st = lowCaseTxt;
						legendsRoll.rollCharacters(st, msg.getChannel(), isAnimated);
						return true;
					}

				} catch (Exception e1) {
					msg.getChannel().sendMessage("Filter couldn't be parsed <:huh:1184466187938185286>");
					logger.error("Legends commands error", e1);
					return true;
				}

				if (checkObjectCommands(msg, lowCaseTxt))
					return true;

			}

			if (checkWheelChannel(msg, lowCaseTxt, channelID))
				return true;

			if (checkAnimationCommands(msg, lowCaseTxt))
				return true;
		}

		return false;
	}

	private boolean checkObjectCommands(Message msg, String lowCaseTxt) {
		if (lowCaseTxt.equals("b-character send")) {
			SendObjects.sendCharacters(msg.getChannel(), legendsWebsite.getCharactersList());
			return true;
		}

		if (lowCaseTxt.equals("b-character printemptyids")) {
			CharacterHash.printEmptyIDs(legendsWebsite.getCharactersList());
			return true;
		}

		if (lowCaseTxt.equals("b-tag send")) {
			SendObjects.sendTags(msg.getChannel(), legendsWebsite.getTags());
			return true;
		}

		if (lowCaseTxt.equals("b-website upload")) {
			uploadLegendsWebsiteBackup();
			msg.getChannel().sendMessage("Done");
			return true;
		}

		return false;
	}

	private boolean checkAnimationCommands(Message msg, String lowCaseTxt) {
		if (lowCaseTxt.equals("disable roll animation")) {
			msg.getChannel().sendMessage("Disabled");
			isAnimated = false;
			return true;
		}

		if (lowCaseTxt.equals("enable roll animation")) {
			msg.getChannel().sendMessage("Enabled");
			isAnimated = true;
			return true;
		}
		return false;
	}

	private boolean checkWheelChannel(Message msg, String lowCaseTxt, String channelID) {
		if (channelID.equals(wheelChannelID)) {
			if (lowCaseTxt.equals("skip")) {
				msg.getChannel().sendMessage("Disabled roll animation :ok_hand:");
				isAnimated = false;
				return true;
			}

			if (lowCaseTxt.equals("unskip")) {
				msg.getChannel().sendMessage("Enabled roll animation :thumbs_up:");
				isAnimated = true;
				return true;
			}

			if (lowCaseTxt.equals("roll")) {
				legendsRoll.rollCharacters("b-roll6 t1", msg.getChannel(), isAnimated);
				return true;
			}
		}
		return false;
	}

	private void backupLegendsWebsite() throws Exception {
		if (legendsWebsite.isDataFetchSuccessfull()) {

			if (backupWebsiteFlag) {
				System.out.println("Character database was successfully built!\nWebsite Backup uploading...");
				// Upload current website HTML as backup
				uploadLegendsWebsiteBackup();
			} else {
				System.out.println("Character database was successfully built!");
			}

		} else {
			logger.warn("Character database missing information. Trying Backup...");
			getLegendsWebsiteBackup();
		}
	}

	private void getLegendsWebsiteBackup() throws IOException {
		OnlineDataManager backup = new OnlineDataManager("Website_Backup_Msg_ID",
				LegendsDatabase.getWebsiteAsInputStream("https://dblegends.net/"), "Website Backup");
		backup.initialise(false);

		legendsWebsite = new LegendsDatabase(backup.getData());
		if (!legendsWebsite.isDataFetchSuccessfull()) {
			logger.warn("Warning: Backup is also missing information.");
		}
	}

	private void uploadLegendsWebsiteBackup() {
		try {
			OnlineDataManager backup = new OnlineDataManager("Website_Backup_Msg_ID",
					LegendsDatabase.getWebsiteAsInputStream("https://dblegends.net/characters"), "website_backup");
			backup.initialise(false);
			backup.writeData(null);

		} catch (Exception e) {
			logger.error("Failed to upload website backup", e);
		}
	}

	@Override
	public void shutdown() {
		if (templates != null)
			templates.shutdown();
	}

	@Override
	public int getShutdownPriority() {
		return 0;
	}
}
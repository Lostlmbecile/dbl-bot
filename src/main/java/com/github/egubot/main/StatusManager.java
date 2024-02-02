package com.github.egubot.main;

import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.message.Message;

import com.github.egubot.interfaces.Shutdownable;
import com.github.egubot.shared.Shared;

public class StatusManager implements Shutdownable {
	private static final Logger logger = LogManager.getLogger(StatusManager.class.getName());
	private String statusChannelID = KeyManager.getID("Status_Channel_ID");
	private String statusMessageID = KeyManager.getID("Status_Message_ID");

	// Create your own message and add its ID, makes it easier to change
	private String activityMsgID = KeyManager.getID("Activity_Msg_ID");
	private String activityMsgContent;
	private String activityMsgType;

	private Message statusMessage;
	protected boolean testMode;
	protected DiscordApi api;

	private boolean isStatusMsgSet = false;

	public StatusManager() {
		this.api = BotApi.getApi();
		this.testMode = Shared.isTestMode();
		try {
			statusMessage = api.getMessageById(statusMessageID, api.getTextChannelById(statusChannelID).get()).get();
		} catch (Exception e) {
		}

		updateStatusMessage();
	}

	public void checkMessageID() {
		if (!statusChannelID.equals("-1")) {
			@SuppressWarnings("resource")
			Scanner in = new Scanner(System.in);

			try {
				statusMessage = api.getMessageById(statusMessageID, api.getTextChannelById(statusChannelID).get())
						.get();
			} catch (Exception e) {
				if (!statusMessageID.equals("-1")) {
					System.out.println("No valid status message ID, send message? y/n");

					if (in.nextLine().equalsIgnoreCase("y")) {
						try {
							statusMessageID = api.getTextChannelById(KeyManager.getID("Status_Channel_ID")).get()
									.sendMessage("status").join().getIdAsString();
							KeyManager.updateKeys("Status_Message_ID", statusMessageID, KeyManager.idsFileName);

							statusMessage = api
									.getMessageById(statusMessageID, api.getTextChannelById(statusChannelID).get())
									.get();
						} catch (Exception e1) {
							checkChannelID();
						}
					} else {
						System.out.println("\nAlways skip this? y/n");
						if (in.nextLine().equalsIgnoreCase("y")) {
							KeyManager.updateKeys("Status_Message_ID", "-1", KeyManager.idsFileName);
						}
					}
				}
			}

			statusMessageID = KeyManager.getID("Status_Message_ID");
		}
	}

	private void checkChannelID() {
		if (!statusChannelID.equals("-1") && (!api.getTextChannelById(statusChannelID).isPresent())) {

			System.err.println("Status channel ID is invalid, please enter a new one, or -1 to always skip");
			@SuppressWarnings("resource")
			Scanner in = new Scanner(System.in);
			String id;

			id = in.nextLine();
			KeyManager.updateKeys("Status_Channel_ID", id, KeyManager.idsFileName);

		}

		statusChannelID = KeyManager.getID("Status_Channel_ID");
		checkMessageID();
	}

	public void exit() {
		setStatusOffline();
		disconnect();
	}

	public void disconnect() {
		try {
			System.out.println("Disconnecting...");
			api.disconnect();
		} catch (Exception e) {
			logger.error("Couldn't disconnect", e);
		}
	}

	public boolean isOnline() {
		try {
			return statusMessage.getContent().equalsIgnoreCase("online");
		} catch (Exception e) {
			return false;
		}

	}

	public void setStatusOnline() {
		if (!testMode) {
			try {
				statusMessage.edit("online").join();
			} catch (NullPointerException e1) {

			} catch (Exception e) {
				logger.warn("Can't update status message");
				logger.error("Couldn't update status message.",e);
			}
		}
	}

	public void setStatusOffline() {
		if (!testMode) {
			System.out.println("Updating status....");
			try {
				statusMessage.edit("offline").join();
			} catch (NullPointerException e1) {

			} catch (Exception e) {
				logger.warn("Can't update status message");
				logger.error("Couldn't update status message.",e);
			}
		}
	}

	public void changeActivity() {
		try {
			if (!isStatusMsgSet && !testMode)
				updateStatusMessage();

			if (!testMode)
				api.updateActivity(getActivityType(), activityMsgContent);
			else if (!isOnline())
				api.updateActivity(ActivityType.PLAYING, "test mode");
		} catch (Exception e) {
			logger.warn("Can't change activity");
			logger.error("Couldn't change activity.",e);
		}
	}

	private ActivityType getActivityType() {
		if (activityMsgType.matches("watch(?s).*")) {

			return ActivityType.WATCHING;

		} else if (activityMsgType.matches("listen(?s).*")) {

			return ActivityType.LISTENING;

		} else if (activityMsgType.matches("stream(?s).*")) {

			return ActivityType.STREAMING;

		} else if (activityMsgType.matches("compet(?s).*")) {

			return ActivityType.COMPETING;

		}

		return ActivityType.PLAYING;
	}

	public void setTestMode(boolean testMode) {
		this.testMode = testMode;
	}

	public void setApi(DiscordApi api) {
		this.api = api;
	}

	private void updateStatusMessage() {
		// Have your message be written as: Type>>>Message
		// You don't need to provide a type
		String separator = ">>>";
		try {
			activityMsgContent = api.getMessageById(activityMsgID, api.getTextChannelById(statusChannelID).get()).get()
					.getContent();

			try {
				activityMsgType = activityMsgContent.substring(0, activityMsgContent.indexOf(separator));
			} catch (Exception e) {
				activityMsgType = "";
			}

			activityMsgContent = activityMsgContent.replaceFirst(activityMsgType + separator, "");

			activityMsgType = activityMsgType.toLowerCase().strip();
		} catch (Exception e) {
			if (!activityMsgID.equals("-1")){
				@SuppressWarnings("resource")
				Scanner in = new Scanner(System.in);
				String id;

				System.err.println("Status Message ID is invalid, please enter a new one, or -1 to always skip"
						+ "\nNote: Send a message in your status channel and copy its id.");

				id = in.nextLine();
				KeyManager.updateKeys("Activity_Msg_ID", id, KeyManager.idsFileName);

				activityMsgID = KeyManager.getID("Activity_Msg_ID");

				updateStatusMessage();
			} else {
				activityMsgContent = "";
				activityMsgType = "";
			}
		}

		isStatusMsgSet = true;
	}

	@Override
	public void shutdown() {
		exit();
	}

	@Override
	public int getShutdownPriority() {
		// TODO Auto-generated method stub
		return 50;
	}
}

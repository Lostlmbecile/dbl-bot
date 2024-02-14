package com.github.lavaplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.github.egubot.main.Bot;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;

public class TrackScheduler extends AudioEventAdapter {
	private static final Logger logger = LogManager.getLogger(TrackScheduler.class.getName());
	private static final Map<String, AudioPlaylist> playlists = new HashMap<>();
	private static final Map<String, AudioPlayer> players = new HashMap<>();

	private AudioPlayer player;
	private String serverID;

	public TrackScheduler(AudioPlayer player, String serverID) {
		this.player = player;
		this.serverID = serverID;
		players.put(serverID, player);
	}

	public static boolean isServerPlayListEmpty(String serverID) {
		return playlists.get(serverID) == null || playlists.get(serverID).getTracks().isEmpty();
	}

	public static boolean isServerPlaying(String serverID) {
		return players.get(serverID) != null && players.get(serverID).getPlayingTrack() != null;
	}

	public static AudioPlayer getServerAudioPlayer(String serverID) {
		return players.get(serverID);
	}

	public static void queue(AudioTrack track, String serverID) {
		try {
			AudioPlaylist playlist = playlists.get(serverID);
			if (playlist == null) {
				ArrayList<AudioTrack> list = new ArrayList<>();
				list.add(track);
				playlists.put(serverID, new BasicAudioPlaylist(serverID, list, track, false));
			} else {
				playlist.getTracks().add(track);
			}

			if (!isServerPlaying(serverID)) {
				getServerAudioPlayer(serverID).playTrack(track);
			}
		} catch (Exception e) {
			logger.error(e);
			throw e;
		}
	}

	public static void queue(AudioPlaylist incomingPlayList, String serverID) {
		try {
			AudioPlaylist playlist = playlists.get(serverID);
			int maxVideos = Math.min(20, incomingPlayList.getTracks().size() - 1);
			if (playlist == null) {
				AudioPlaylist newList = new BasicAudioPlaylist(serverID,
						incomingPlayList.getTracks().subList(0, maxVideos), null, false);
				playlists.put(serverID, newList);
			} else {
				playlist.getTracks().addAll(incomingPlayList.getTracks().subList(0, maxVideos));
				playlists.put(serverID, playlist);
			}

			if (!isServerPlaying(serverID))
				getServerAudioPlayer(serverID).playTrack(incomingPlayList.getTracks().get(0));
		} catch (Exception e) {
			logger.error(e);
			throw e;
		}
	}

	public static Map<String, Long> getPlayListInfo(String serverID) {
		AudioPlaylist list = playlists.get(serverID);
		if (list == null) {
			return Collections.emptyMap();
		} else {
			List<AudioTrack> tracks = list.getTracks();
			Map<String, Long> trackInfo = new HashMap<>();
			for (int i = 0; i < tracks.size() && i < 10; i++) {
				AudioTrack audioTrack = tracks.get(i);
				trackInfo.put(audioTrack.getIdentifier(), audioTrack.getDuration());
			}

			return trackInfo;
		}
	}

	public static void destroy(String serverID) {
		playlists.remove(serverID);
		if (players.get(serverID) != null)
			players.get(serverID).destroy();
		players.remove(serverID);
		disconnect(serverID);
	}

	@Override
	public void onPlayerPause(AudioPlayer player) {
		// Player was paused
	}

	@Override
	public void onPlayerResume(AudioPlayer player) {
		// Player was resumed
	}

	@Override
	public void onTrackStart(AudioPlayer player, AudioTrack track) {
		// A track started playing
	}

	public static void skip(String serverID) {
		goToNextTrack(serverID, 0);
	}

	public static void pause(String serverID) {
		if (players.get(serverID) != null) {
			players.get(serverID).setPaused(true);
		}
	}

	public static void resume(String serverID) {
		if (players.get(serverID) != null) {
			players.get(serverID).setPaused(false);
		}
	}

	private static void goToNextTrack(String serverID, int i) {
		AudioPlayer serverPlayer = players.get(serverID);
		List<AudioTrack> list = playlists.get(serverID).getTracks();
		list.remove(i);
		if (!list.isEmpty() && serverPlayer != null)
			serverPlayer.playTrack(list.get(0));
		else
			destroy(serverID);
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		if (endReason.mayStartNext) {
			goToNextTrack(track);
		}

		// endReason == FINISHED: A track finished or died by an exception (mayStartNext
		// = true).
		// endReason == LOAD_FAILED: Loading of a track failed (mayStartNext = true).
		// endReason == STOPPED: The player was stopped.
		// endReason == REPLACED: Another track started playing while this had not
		// finished
		// endReason == CLEANUP: Player hasn't been queried for a while, if you want you
		// can put a
		// clone of this back to your queue
	}

	private void goToNextTrack(AudioTrack track) {
		List<AudioTrack> list = playlists.get(serverID).getTracks();
		list.remove(track);
		if (!list.isEmpty() || player != null)
			player.playTrack(list.get(0));
		else
			destroy(serverID);
	}

	@Override
	public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
		goToNextTrack(track);
	}

	@Override
	public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
		goToNextTrack(track);
	}

	public AudioPlayer getPlayer() {
		return player;
	}

	public void setPlayer(AudioPlayer player) {
		this.player = player;
	}

	public String getServerID() {
		return serverID;
	}

	public void setServerID(String serverID) {
		this.serverID = serverID;
	}

	public static Map<String, AudioPlaylist> getPlaylists() {
		return playlists;
	}

	public static void disconnect(String serverID) {
		try {
			Bot.getApi().getServerById(serverID).get().getConnectedVoiceChannel(Bot.getApi().getYourself()).get()
					.disconnect();
		} catch (Exception e) {

		}
	}
}

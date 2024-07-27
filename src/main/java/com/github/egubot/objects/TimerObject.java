package com.github.egubot.objects;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.*;

import com.github.egubot.shared.Shared;
import com.google.gson.annotations.SerializedName;

public class TimerObject {
	@SerializedName("task")
	private String task = "";
	@SerializedName("task_arguments")
	private String taskArguments = "";
	@SerializedName("target_channel")
	private List<Long> targetChannel = new ArrayList<>(1);
	@SerializedName("activated_flag")
	private boolean activatedFlag = true;
	@SerializedName("next_execution")
	private String nextExecution; // dd-MM-yyyy, H:mm format
	@SerializedName("exit_time")
	private String exitTime; // UK time on app exit. dd-MM-yyyy, H:mm format
	@SerializedName("summer_time")
	private boolean summerTime;
	@SerializedName("recurring")
	private boolean recurring = false;
	@SerializedName("delay")
	private String delay = "1s"; // 0M0w0d0h0m0s format
	@SerializedName("start_date")
	private String startDate = null; // null if not set. dd-MM-yyyy, H:mm format
	@SerializedName("send_on_miss")
	private boolean sendOnMiss = false;
	@SerializedName("continue_on_miss")
	private boolean continueOnMiss = true;
	@SerializedName("terminate_on_miss")
	private boolean terminateOnMiss = false;
	@SerializedName("miss_tolerance")
	private String missTolerance = "5s"; // 0M0w0d0h0m0s format or 0 if half the delay or less

	public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy, H:mm");
	public static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
	private static final Pattern timePattern = Pattern.compile("(\\d+)([Mwdhms])");

	// Getters and Setters

	public boolean isActivatedFlag() {
		return activatedFlag;
	}

	public void setActivatedFlag(boolean activatedFlag) {
		this.activatedFlag = activatedFlag;
	}

	public void setNextExecution(String nextExecution) {
		if (isValidDateTime(nextExecution)) {
			this.nextExecution = nextExecution;
		} else {
			throw new IllegalArgumentException("Invalid nextExecution format: " + nextExecution);
		}
	}

	public void setExitTime(String exitTime) {
		if (isValidDateTime(exitTime)) {
			this.exitTime = exitTime;
			adjustTimesForSummerTime();
		} else {
			throw new IllegalArgumentException("Invalid exitTime format: " + exitTime);
		}
	}

	public boolean isSummerTime() {
		return summerTime;
	}

	public void setSummerTime(boolean summerTime) {
		this.summerTime = summerTime;
	}

	public boolean isRecurring() {
		return recurring;
	}

	public void setRecurring(boolean recurring) {
		this.recurring = recurring;
	}

	public void setDelay(String delay) {
		if (isValidDelay(delay)) {
			this.delay = delay;
		} else {
			throw new IllegalArgumentException("Invalid delay format: " + delay);
		}
	}

	public void setStartDate(String startDate) {
		if (startDate == null || isValidDateTime(startDate)) {
			this.startDate = startDate;
		} else {
			throw new IllegalArgumentException("Invalid startDate format: " + startDate);
		}
	}

	public String getTask() {
		return task;
	}

	public void setTask(String task) {
		this.task = task;
	}

	public String getTaskArguments() {
		return taskArguments;
	}

	public void setTaskArguments(String taskArguments) {
		this.taskArguments = taskArguments;
	}

	public List<Long> getTargetChannel() {
		return targetChannel;
	}

	public void setTargetChannel(List<Long> targetChannel) {
		this.targetChannel = targetChannel;
	}

	public boolean isSendOnMiss() {
		return sendOnMiss;
	}

	public void setSendOnMiss(boolean sendOnMiss) {
		this.sendOnMiss = sendOnMiss;
	}

	public boolean isContinueOnMiss() {
		return continueOnMiss;
	}

	public void setContinueOnMiss(boolean continueOnMiss) {
		this.continueOnMiss = continueOnMiss;
	}

	public boolean isTerminateOnMiss() {
		return terminateOnMiss;
	}

	public void setTerminateOnMiss(boolean terminateOnMiss) {
		this.terminateOnMiss = terminateOnMiss;
	}

	public String getMissTolerance() {
		return missTolerance;
	}

	public void setMissTolerance(String missTolerance) {
		if ("0".equals(missTolerance)) {
			this.missTolerance = formatDuration(getDelayDuration().dividedBy(2));
		} else if (isValidDelay(missTolerance)) {
			this.missTolerance = missTolerance;
		} else {
			throw new IllegalArgumentException("Invalid missTolerance format: " + missTolerance);
		}
	}

	// Time Handling Methods

	private boolean isValidDateTime(String dateTime) {
		try {
			LocalDateTime.parse(dateTime, timeFormatter);
			return true;
		} catch (DateTimeParseException e) {
			return false;
		}
	}

	private boolean isValidDelay(String delayString) {
		return delayString.matches("(?:\\d+[smhdwM]){1,6}");
	}

	private Duration parseDelayString(String delayString) {
		int months = 0;
		int weeks = 0;
		int days = 0;
		int hours = 0;
		int minutes = 0;
		int seconds = 0;

		Matcher matcher = timePattern.matcher(delayString);
		while (matcher.find()) {
			int value = Integer.parseInt(matcher.group(1));
			switch (matcher.group(2)) {
			case "M":
				months = value;
				break;
			case "w":
				weeks = value;
				break;
			case "d":
				days = value;
				break;
			case "h":
				hours = value;
				break;
			case "m":
				minutes = value;
				break;
			case "s":
				seconds = value;
				break;
			default:
				seconds = 1;
			}
		}

		Duration totalDuration = Duration.ofDays((long) months * 30 + weeks * 7 + days).plusHours(hours)
				.plusMinutes(minutes).plusSeconds(seconds);
		return totalDuration.compareTo(Duration.ofSeconds(1)) < 0 ? Duration.ofSeconds(1) : totalDuration;
	}

	public Duration getMissToleranceDuration() {
		return missTolerance.equals("0") ? parseDelayString(delay).dividedBy(2) : parseDelayString(missTolerance);
	}

	public ZonedDateTime getNextExecutionTime() {
		return parseTimeStringWithTimeZone(nextExecution);
	}

	public ZonedDateTime getExitTimeAsDateTime() {
		return exitTime == null ? null : parseTimeStringWithTimeZone(exitTime);
	}

	public Duration getDelayDuration() {
		return parseDelayString(delay);
	}

	private void adjustTimesForSummerTime() {
		if (summerTime) {
			ZonedDateTime nextExecutionTime = getNextExecutionTime();
			nextExecutionTime = applySummerTimeAdjustment(nextExecutionTime);
			setNextExecution(formatTimeString(nextExecutionTime));
		}
	}

	private String formatDuration(Duration duration) {
		long seconds = duration.toSeconds();
		long days = seconds / (24 * 3600);
		seconds %= (24 * 3600);
		long hours = seconds / 3600;
		seconds %= 3600;
		long minutes = seconds / 60;
		seconds %= 60;
		return String.format("%dw%dd%dh%dm%ds", days / 7, days % 7, hours, minutes, seconds);
	}

	public ZonedDateTime getStartDateTime() {
		return startDate == null ? null : parseTimeStringWithTimeZone(startDate);
	}

	private ZonedDateTime parseTimeStringWithTimeZone(String timeString) {
		LocalDateTime localDateTime = parseTimeString(timeString);
		return localDateTime.atZone(ZoneId.of(Shared.getTimeZone()));
	}

	private LocalDateTime parseTimeString(String timeString) {
		try {
			return LocalDateTime.parse(timeString, timeFormatter);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid time format: " + timeString, e);
		}
	}

	private ZonedDateTime applySummerTimeAdjustment(ZonedDateTime zonedDateTime) {
		boolean isCurrentlySummerTime = zonedDateTime.getZone().getRules().isDaylightSavings(zonedDateTime.toInstant());
		if (summerTime && !isCurrentlySummerTime) {
			return zonedDateTime.plusHours(1);
		} else if (!summerTime && isCurrentlySummerTime) {
			return zonedDateTime.minusHours(1);
		}
		return zonedDateTime;
	}

	private String formatTimeString(ZonedDateTime zonedDateTime) {
		return zonedDateTime.format(timeFormatter);
	}

	@Override
	public String toString() {
		return String.format(
				"TimerObject[task='%s', taskArguments='%s', targetChannel='%s', activatedFlag='%s', nextExecution='%s', exitTime='%s', summerTime='%s', recurring='%s', delay='%s', startDate='%s', sendOnMiss='%s', continueOnMiss='%s', terminateOnMiss='%s', missTolerance='%s']",
				task, taskArguments, targetChannel, activatedFlag, nextExecution, exitTime, summerTime, recurring,
				delay, startDate, sendOnMiss, continueOnMiss, terminateOnMiss, missTolerance);
	}

	@Override
	public int hashCode() {
		return Objects.hash(delay, recurring, task, taskArguments);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TimerObject other = (TimerObject) obj;
		return Objects.equals(delay, other.delay) && recurring == other.recurring && Objects.equals(task, other.task)
				&& Objects.equals(taskArguments, other.taskArguments);
	}

}

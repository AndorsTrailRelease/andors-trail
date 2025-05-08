package com.gpl.rpg.AndorsTrail.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import java.util.Calendar;

public final class WorldData {
	private long worldTime = 0; // Measured in number of game rounds
	private final HashMap<String, Long> timers = new HashMap<String, Long>();

	public WorldData() {}

	public void tickWorldTime() {
		++worldTime;
	}
	public void tickWorldTime(int ticks) {
		worldTime += ticks;
	}
	public long getWorldTime() {
		return worldTime;
	}

	public void createTimer(String name) {
		timers.put(name, worldTime);
	}

	public void removeTimer(String name) {
		timers.remove(name);
	}

	public boolean hasTimerElapsed(String name, long duration) {
		Long v = timers.get(name);
		if (v == null) return false;
		return v + duration <= worldTime;
	}

	public int getDate(String format) {
		Calendar now = Calendar.getInstance();
		int ret;
		switch (format) {
			case "YYYYMMDD":
				ret = now.get(Calendar.YEAR)*10000 + (now.get(Calendar.MONTH) + 1)*100 + now.get(Calendar.DAY_OF_MONTH);
				break;
			case "YYYYMM":
				ret = now.get(Calendar.YEAR)*100 + (now.get(Calendar.MONTH) + 1);
				break;
			case "YYYY":
				ret = now.get(Calendar.YEAR);
				break;
			case "MMDD":
				ret = (now.get(Calendar.MONTH) + 1)*100 + now.get(Calendar.DAY_OF_MONTH);
				break;
			case "MM":
				ret = (now.get(Calendar.MONTH) + 1);
				break;
			case "DD":
				ret = now.get(Calendar.DAY_OF_MONTH);
				break;
			default:
				ret = 99999999;		//never true
		}
		return ret;
	}

	public int getTime(String format) {
		Calendar now = Calendar.getInstance();
		int ret;
		switch (format) {
			case "HHMMSS":
				ret = now.get(Calendar.HOUR_OF_DAY)*10000 + now.get(Calendar.MINUTE)*100 + now.get(Calendar.SECOND);
				break;
			case "HHMM":
				ret = now.get(Calendar.HOUR_OF_DAY)*100 + now.get(Calendar.MINUTE);
				break;
			case "HH":
				ret = now.get(Calendar.HOUR_OF_DAY);
				break;
			case "MMSS":
				ret = now.get(Calendar.MINUTE)*100 + now.get(Calendar.SECOND);
				break;
			case "MM":
				ret = now.get(Calendar.MINUTE);
				break;
			case "SS":
				ret = now.get(Calendar.SECOND);
				break;
			default:
				ret = 99999999;		//never true
		}
		return ret;
	}

	// ====== PARCELABLE ===================================================================

	public WorldData(DataInputStream src, int fileversion) throws IOException {
		worldTime = src.readLong();
		final int numTimers = src.readInt();
		for(int i = 0; i < numTimers; ++i) {
			final String timerName = src.readUTF();
			final long value = src.readLong();
			this.timers.put(timerName, value);
		}
	}

	public void writeToParcel(DataOutputStream dest) throws IOException {
		dest.writeLong(worldTime);
		dest.writeInt(timers.size());
		for(Map.Entry<String, Long> e : timers.entrySet()) {
			dest.writeUTF(e.getKey());
			dest.writeLong(e.getValue());
		}
	}

	public void addToChecksum(ChecksumBuilder builder) {
		builder.add(worldTime);
		builder.add(timers.size());
		for(Map.Entry<String, Long> e : timers.entrySet()) {
			builder.add(e.getKey());
			builder.add(e.getValue());
		}
	}
}

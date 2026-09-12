package com.gpl.rpg.AndorsTrail.util;

import android.os.Handler;
import android.os.Looper;

public final class TimedMessageTask {
	private final long interval;
	private final boolean requireIntervalBeforeFirstTick;
	private final Callback callback;
	private long nextTickTime;
	private boolean hasQueuedTick = false;
	private boolean isAlive = false;
	private final Handler handler;

	public TimedMessageTask(Callback callback, long interval, boolean requireIntervalBeforeFirstTick) {
		this.interval = interval;
		this.requireIntervalBeforeFirstTick = requireIntervalBeforeFirstTick;
		this.callback = callback;
		this.nextTickTime = System.currentTimeMillis() + interval;
		this.handler = new Handler(Looper.getMainLooper(), (msg) -> {
			if (!isAlive) return true;
			if (!hasQueuedTick) return true;
			hasQueuedTick = false;
			tick();
			return true;
		});
	}

	private void tick() {
		nextTickTime = System.currentTimeMillis() + interval;
		boolean continueTicking = callback.onTick(this);
		if (continueTicking) queueAnotherTick();
	}

	private void sleep(long delayMillis) {
		handler.removeMessages(0);
		handler.sendMessageDelayed(handler.obtainMessage(0), delayMillis);
	}

	private boolean hasElapsedIntervalTime() {
		return System.currentTimeMillis() >= nextTickTime;
	}

	public void queueAnotherTick() {
		if (hasQueuedTick) return;
		hasQueuedTick = true;
		sleep(interval);
	}

	private boolean shouldCauseTickOnStart() {
		if (requireIntervalBeforeFirstTick) return false;
		if (hasQueuedTick) return false;
        return hasElapsedIntervalTime();
    }

	public void start() {
		isAlive = true;
		if (shouldCauseTickOnStart()) tick();
		else queueAnotherTick();
	}

	public void stop() {
		hasQueuedTick = false;
		isAlive = false;
		handler.removeMessages(0);
	}

	public interface Callback {
		boolean onTick(TimedMessageTask task);
	}
}

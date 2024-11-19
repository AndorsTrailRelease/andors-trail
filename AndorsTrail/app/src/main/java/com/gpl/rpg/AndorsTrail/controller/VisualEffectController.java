package com.gpl.rpg.AndorsTrail.controller;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.os.Handler;

import com.gpl.rpg.AndorsTrail.AndorsTrailPreferences;
import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.listeners.VisualEffectFrameListeners;
import com.gpl.rpg.AndorsTrail.model.actor.Actor;
import com.gpl.rpg.AndorsTrail.model.actor.Monster;
import com.gpl.rpg.AndorsTrail.model.actor.MonsterType;
import com.gpl.rpg.AndorsTrail.model.map.PredefinedMap;
import com.gpl.rpg.AndorsTrail.resource.VisualEffectCollection;
import com.gpl.rpg.AndorsTrail.resource.VisualEffectCollection.VisualEffect;
import com.gpl.rpg.AndorsTrail.resource.tiles.TileManager;
import com.gpl.rpg.AndorsTrail.util.Coord;
import com.gpl.rpg.AndorsTrail.util.CoordRect;
import com.gpl.rpg.AndorsTrail.util.Size;

import java.util.ArrayList;
import java.util.List;

public final class VisualEffectController {
	private static final long EFFECT_UPDATE_INTERVAL = 25;
	private final ControllerContext controllers;
	private final WorldContext world;
	private final VisualEffectCollection effectTypes;
	private final Handler animationHandler = new Handler();
	private final List<VisualEffectAnimation> activeAnimations = new ArrayList<>();

	public final VisualEffectFrameListeners visualEffectFrameListeners = new VisualEffectFrameListeners();
	private long getEffectUpdateInterval() {
		return EFFECT_UPDATE_INTERVAL * controllers.preferences.attackspeed_milliseconds / AndorsTrailPreferences.ATTACKSPEED_DEFAULT_MILLISECONDS;
	}
	
	public VisualEffectController(ControllerContext controllers, WorldContext world) {
		this.controllers = controllers;
		this.world = world;
		this.effectTypes = world.visualEffectTypes;
	}

	public void startEffect(Coord position, VisualEffectCollection.VisualEffectID effectID, String displayValue, VisualEffectCompletedCallback callback, int callbackValue) {
		VisualEffectAnimation animation = new VisualEffectAnimation(effectTypes.getVisualEffect(effectID), position, displayValue, callback, callbackValue);
		animation.start();
	}

	private void startAnimation(VisualEffectAnimation animation) {
		activeAnimations.add(animation);
		animation.update();
		if (activeAnimations.size() == 1) {
			animationHandler.postDelayed(animationRunnable, 0);
		}
	}

	private final Runnable animationRunnable = new Runnable() {
		@Override
		public void run() {
			if(!activeAnimations.isEmpty()) {
				long updateInterval = getEffectUpdateInterval();
				animationHandler.postDelayed(this, updateInterval);

				for (int i = 0; i < activeAnimations.size(); i++) {
					VisualEffectAnimation animation = activeAnimations.get(i);
					animation.durationPassed += updateInterval;
					animation.updateFrame();
					animation.update();
					if (controllers.preferences.attackspeed_milliseconds <= 0  ||  animation.currentFrame >= animation.effect.lastFrame) {
						animation.onCompleted();
						activeAnimations.remove(i);
						i--;
					}
				}
				visualEffectFrameListeners.onNewAnimationFrames(activeAnimations);
			}
		}
	};

	private VisualEffectCollection.VisualEffectID enqueuedEffectID = null;
	private int enqueuedEffectValue = 0;
	public void enqueueEffect(VisualEffectCollection.VisualEffectID effectID, int displayValue) {
		if (enqueuedEffectID == null) {
			enqueuedEffectID = effectID;
		} else if (Math.abs(displayValue) > Math.abs(enqueuedEffectValue)) {
			enqueuedEffectID = effectID;
		}
		enqueuedEffectValue += displayValue;
	}
	public void startEnqueuedEffect(Coord position) {
		if (enqueuedEffectID == null) return;
		startEffect(position, enqueuedEffectID, (enqueuedEffectValue == 0) ? null : String.valueOf(enqueuedEffectValue), null, 0);
		enqueuedEffectID = null;
		enqueuedEffectValue = 0;
	}
	
	public void startActorMoveEffect(Actor actor, PredefinedMap map, Coord origin, Coord destination, int duration, VisualEffectCompletedCallback callback, int callbackValue) {
		(new SpriteMoveAnimation(origin, destination, duration, actor, map, callback, callbackValue))
		.start();
	}

	public final class SpriteMoveAnimation implements Runnable {
		private final Handler handler = new Handler();

		private final VisualEffectCompletedCallback callback;
		private final int callbackValue;

		public final int duration;
		public final Actor actor;
		public final PredefinedMap map;
		public final Coord origin;
		public final Coord destination;
		
		
		@Override
		public void run() {
			onCompleted();
		}
		
		public SpriteMoveAnimation(Coord origin, Coord destination, int duration, Actor actor, PredefinedMap map, VisualEffectCompletedCallback callback, int callbackValue) {
			this.callback = callback;
			this.callbackValue = callbackValue;
			this.duration = duration;
			this.actor = actor;
			this.map = map;
			this.origin = origin;
			this.destination = destination;

		}

		private void onCompleted() {
			actor.hasVFXRunning = false;
			if (callback != null) callback.onVisualEffectCompleted(callbackValue);
			visualEffectFrameListeners.onSpriteMoveCompleted(this);
		}

		public void start() {
			actor.hasVFXRunning = true;
			actor.vfxDuration = duration;
			actor.vfxStartTime = System.currentTimeMillis();
			visualEffectFrameListeners.onSpriteMoveStarted(this);
			if (duration == 0 || !controllers.preferences.enableUiAnimations) onCompleted();
			else {
				handler.postDelayed(this, duration);
			}
		}
	}

	public static final Paint textPaint = new Paint();
	static {
		textPaint.setShadowLayer(2, 1, 1, Color.DKGRAY);
		textPaint.setAlpha(255);
		textPaint.setTextAlign(Align.CENTER);
	}

	public final class VisualEffectAnimation  {
		public int tileID;
		public int textYOffset;
		public long durationPassed = 0;

		private void updateFrame() {
			long frameDuration = (long) effect.millisecondPerFrame * controllers.preferences.attackspeed_milliseconds / AndorsTrailPreferences.ATTACKSPEED_DEFAULT_MILLISECONDS;
			while (frameDuration > 0 && durationPassed > frameDuration) {
				currentFrame++;
				durationPassed -= frameDuration;
			}
		}
		private void update() {
			if (currentFrame >= effect.lastFrame) {
				return;
			}

			tileID = effect.frameIconIDs[currentFrame];
			textYOffset = -2 * (currentFrame);

			if (currentFrame >= beginFadeAtFrame && displayText != null) {
				textPaint.setAlpha(255 * (effect.lastFrame - currentFrame) / (effect.lastFrame - beginFadeAtFrame));
			}

			area.topLeft.y = position.y - 1;
		}

		private void onCompleted() {
			visualEffectFrameListeners.onAnimationCompleted(this);
			if (callback != null) callback.onVisualEffectCompleted(callbackValue);
		}

		public void start() {
			if (!controllers.preferences.enableUiAnimations) onCompleted();
			else startAnimation(this);
		}

		private int currentFrame = 0;

		private final VisualEffect effect;

		public final Coord position;
		public final String displayText;
		public final CoordRect area;
		private final int beginFadeAtFrame;
		private final VisualEffectCompletedCallback callback;
		private final int callbackValue;

		public VisualEffectAnimation(VisualEffect effect, Coord position, String displayValue, VisualEffectCompletedCallback callback, int callbackValue) {
			this.position = position;
			this.callback = callback;
			this.callbackValue = callbackValue;
			this.effect = effect;
			this.displayText = displayValue == null ? "" : displayValue;
			textPaint.setColor(effect.textColor);
			textPaint.setTextSize(world.tileManager.tileSize * 0.5f); // 32dp.
			Rect textBounds = new Rect();
			textPaint.getTextBounds(displayText, 0, displayText.length(), textBounds);
			int widthNeededInTiles = 1 + (textBounds.width() / world.tileManager.tileSize);
			if (widthNeededInTiles % 2 == 0) widthNeededInTiles++;
			this.area = new CoordRect(new Coord(position.x - (widthNeededInTiles / 2), position.y - 1), new Size(widthNeededInTiles, 2));
			this.beginFadeAtFrame = effect.lastFrame / 2;
		}

		public Paint getTextPaint(){
			return textPaint;
		}
	}

	public static interface VisualEffectCompletedCallback {
		public void onVisualEffectCompleted(int callbackValue);
	}

	public boolean isRunningVisualEffect() {
		return !activeAnimations.isEmpty();
	}


	public static final class BloodSplatter {
		public final long removeAfter;
		public final long reduceIconAfter;
		public final Coord position;
		public int iconID;
		public boolean reducedIcon = false;
		public BloodSplatter(int iconID, Coord position) {
			this.iconID = iconID;
			this.position = position;
			final long now = System.currentTimeMillis();
			removeAfter = now + Constants.SPLATTER_DURATION_MS;
			reduceIconAfter = now + Constants.SPLATTER_DURATION_MS / 2;
		}
	}

	public void updateSplatters(PredefinedMap map) {
		long now = System.currentTimeMillis();
		for (int i = map.splatters.size() - 1; i >= 0; --i) {
			BloodSplatter b = map.splatters.get(i);
			if (b.removeAfter <= now) {
				map.splatters.remove(i);
				controllers.monsterSpawnController.monsterSpawnListeners.onSplatterRemoved(map, b.position);
			} else if (!b.reducedIcon && b.reduceIconAfter <= now) {
				b.reducedIcon = true;
				b.iconID++;
				controllers.monsterSpawnController.monsterSpawnListeners.onSplatterChanged(map, b.position);
			}
		}
	}

	public void addSplatter(PredefinedMap map, Monster m) {
		int iconID = getSplatterIconFromMonsterClass(m.getMonsterClass());
		if (iconID > 0) {
			map.splatters.add(new BloodSplatter(iconID, m.position));
			controllers.monsterSpawnController.monsterSpawnListeners.onSplatterAdded(map, m.position);
		}
	}

	private static int getSplatterIconFromMonsterClass(MonsterType.MonsterClass monsterClass) {
		switch (monsterClass) {
		case insect:
		case undead:
		case reptile:
			return TileManager.iconID_splatter_brown_1a + Constants.rnd.nextInt(2) * 2;
		case humanoid:
		case animal:
		case giant:
			return TileManager.iconID_splatter_red_1a + Constants.rnd.nextInt(2) * 2;
		case demon:
		case construct:
		case ghost:
			return TileManager.iconID_splatter_white_1a;
		default:
			return -1;
		}
	}
	
	public void asyncUpdateArea(CoordRect area) {
		visualEffectFrameListeners.onAsyncAreaUpdate(area);
	}

	
}

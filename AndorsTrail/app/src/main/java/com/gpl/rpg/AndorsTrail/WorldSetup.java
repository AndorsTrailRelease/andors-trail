package com.gpl.rpg.AndorsTrail;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;

import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.model.ModelContainer;
import com.gpl.rpg.AndorsTrail.resource.ResourceLoader;
import com.gpl.rpg.AndorsTrail.savegames.Savegames;
import com.gpl.rpg.AndorsTrail.util.L;

public final class WorldSetup {
	private static final ExecutorService executor = Executors.newSingleThreadExecutor();
	private static final Handler mainHandler = new Handler(Looper.getMainLooper());

	private final WorldContext world;
	private final ControllerContext controllers;
	private final WeakReference<Context> androidContext;
	private boolean isResourcesInitialized = false;
	private boolean isInitializingResources = false;
	private WeakReference<OnResourcesLoadedListener> onResourcesLoadedListener;
	private WeakReference<OnSceneLoadedListener> onSceneLoadedListener;
	private Object sceneLoaderId;

	public boolean createNewCharacter = false;
	public int loadFromSlot = Savegames.SLOT_QUICKSAVE;
	public boolean isSceneReady = false;
	public String newHeroName;
	public int newHeroIcon;
	public int newHeroStartLives;
	public boolean newHeroUnlimitedSaves;

	public WorldSetup(WorldContext world, ControllerContext controllers, Context androidContext) {
		this.world = world;
		this.controllers = controllers;
		this.androidContext = new WeakReference<Context>(androidContext);
	}

	public void setOnResourcesLoadedListener(OnResourcesLoadedListener listener) {
		synchronized (this) {
			onResourcesLoadedListener = null;
			if (isResourcesInitialized) {
				if (listener != null) listener.onResourcesLoaded();
				return;
			}
			onResourcesLoadedListener = new WeakReference<WorldSetup.OnResourcesLoadedListener>(listener);
		}
	}

	public void startResourceLoader(final Resources r) {
		if (isResourcesInitialized) return;

		synchronized (this) {
			if (isInitializingResources) return;
			isInitializingResources = true;
		}

		//Load resources essential to the app synchroneously
		try {
			ResourceLoader.loadResourcesSync(world, r);
		} catch (RuntimeException e) {
			synchronized (this) {
				isInitializingResources = false;
			}
			L.log("Error loading sync resources: " + e);
			throw e;
		}
		
		// And the rest asynchronously.
		executor.execute(() -> {
			boolean loadSucceeded = false;
			try {
				ResourceLoader.loadResourcesAsync(world, r);
				loadSucceeded = true;
			} catch (RuntimeException e) {
				L.log("Error loading async resources: " + e);
			} finally {
				final boolean finalLoadSucceeded = loadSucceeded;
				mainHandler.post(() -> {
					OnResourcesLoadedListener listener;
					synchronized (this) {
						if (finalLoadSucceeded) {
							isResourcesInitialized = true;
						}
						isInitializingResources = false;
						if (!finalLoadSucceeded || onResourcesLoadedListener == null) return;
						listener = onResourcesLoadedListener.get();
						onResourcesLoadedListener = null;
					}
					if (listener != null) listener.onResourcesLoaded();
				});
			}
		});
	}

	public void startCharacterSetup(final OnSceneLoadedListener listener) {
		synchronized (WorldSetup.this) {
			this.onSceneLoadedListener = new WeakReference<OnSceneLoadedListener>(listener);
		}
		startSceneLoader();
	}
	public void removeOnSceneLoadedListener(final OnSceneLoadedListener listener) {
		synchronized (WorldSetup.this) {
			if (this.onSceneLoadedListener == null) return;
			if (this.onSceneLoadedListener.get() == listener) this.onSceneLoadedListener = null;
		}
	}

	private void startSceneLoader() {
		isSceneReady = false;
		final Object thisLoaderId = new Object();
		synchronized (this) {
			sceneLoaderId = thisLoaderId;
		}
		final Object loadGameLock = new Object();

		executor.execute(() -> {
			final Savegames.LoadSavegameResult result;
			synchronized (loadGameLock) {
				Savegames.LoadSavegameResult localResult;
				try {
					if (world.model != null) world.resetForNewGame();
					if (createNewCharacter) {
						localResult = createNewWorld();
					} else {
						localResult = continueWorld();
					}
				} catch (RuntimeException e) {
					L.log("Error loading world: " + e.toString());
					localResult = Savegames.LoadSavegameResult.unknownError;
				} finally {
					createNewCharacter = false;
				}
				if (localResult == null) localResult = Savegames.LoadSavegameResult.unknownError;
				result = localResult;
			}
			mainHandler.post(() -> {
				OnSceneLoadedListener listener;
				synchronized (this) {
					if (sceneLoaderId != thisLoaderId) return; // Some other thread has started after we started.
					isSceneReady = true;

					if (onSceneLoadedListener == null) return;
					listener = onSceneLoadedListener.get();
					onSceneLoadedListener = null;
				}
				if (listener == null) return;

				if (result == Savegames.LoadSavegameResult.success) {
					listener.onSceneLoaded();
				} else {
					listener.onSceneLoadFailed(result);
				}
			});
		});
	}

	private Savegames.LoadSavegameResult continueWorld() {
		Context ctx = androidContext.get();
		if (ctx == null) return Savegames.LoadSavegameResult.unknownError;
		return Savegames.loadWorld(world, controllers, ctx, loadFromSlot);
	}

	private Savegames.LoadSavegameResult createNewWorld() {
		Context ctx = androidContext.get();
		if (ctx == null) return Savegames.LoadSavegameResult.unknownError;
		world.model = new ModelContainer(newHeroStartLives, newHeroUnlimitedSaves);
		world.model.player.initializeNewPlayer(world.dropLists, newHeroName, newHeroIcon);

		controllers.actorStatsController.recalculatePlayerStats(world.model.player);
		controllers.movementController.respawnPlayer(ctx.getResources());
		controllers.mapController.lotsOfTimePassed();
		return Savegames.LoadSavegameResult.success;
	}

	public interface OnSceneLoadedListener {
		void onSceneLoaded();
		void onSceneLoadFailed(Savegames.LoadSavegameResult loadResult);
	}
	public interface OnResourcesLoadedListener {
		void onResourcesLoaded();
	}
}

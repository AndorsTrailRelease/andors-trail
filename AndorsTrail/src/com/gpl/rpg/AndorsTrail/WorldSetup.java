package com.gpl.rpg.AndorsTrail;

import java.io.IOException;
import java.lang.ref.WeakReference;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;

import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.WorldMapController;
import com.gpl.rpg.AndorsTrail.model.ModelContainer;
import com.gpl.rpg.AndorsTrail.resource.ResourceLoader;
import com.gpl.rpg.AndorsTrail.savegames.Savegames;
import com.gpl.rpg.AndorsTrail.util.L;

public final class WorldSetup {

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
	private Savegames.LoadSavegameResult loadResult;

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
		ResourceLoader.loadResourcesSync(world, r);
		
		//And the rest asynchroneously
		(new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... arg0) {
				ResourceLoader.loadResourcesAsync(world, r);
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				synchronized (WorldSetup.this) {
					isResourcesInitialized = true;
					isInitializingResources = false;

					if (onResourcesLoadedListener == null) return;
					WorldSetup.OnResourcesLoadedListener listener = onResourcesLoadedListener.get();
					onResourcesLoadedListener = null;
					if (listener == null) return;
					listener.onResourcesLoaded();
				}
			}
		}).execute();
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

	private final Object onlyOneThreadAtATimeMayLoadSavegames = new Object();
	private void startSceneLoader() {
		isSceneReady = false;
		final Object thisLoaderId = new Object();
		synchronized (WorldSetup.this) {
			sceneLoaderId = thisLoaderId;
		}

		(new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... arg0) {
				synchronized (onlyOneThreadAtATimeMayLoadSavegames) {
					if (world.model != null) world.resetForNewGame();
					if (createNewCharacter) {
						createNewWorld();
						loadResult = Savegames.LoadSavegameResult.success;
					} else {
						loadResult = continueWorld();
					}
					createNewCharacter = false;
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				synchronized (WorldSetup.this) {
					if (sceneLoaderId != thisLoaderId) return; // Some other thread has started after we started.
					isSceneReady = true;

					if (onSceneLoadedListener == null) return;
					OnSceneLoadedListener o = onSceneLoadedListener.get();
					onSceneLoadedListener = null;
					if (o == null) return;

					if (loadResult == Savegames.LoadSavegameResult.success) {
						o.onSceneLoaded();
					} else {
						o.onSceneLoadFailed(loadResult);
					}
				}
			}
		}).execute();
	}

	private Savegames.LoadSavegameResult continueWorld() {
		Context ctx = androidContext.get();
		return Savegames.loadWorld(world, controllers, ctx, loadFromSlot);
	}

	private void createNewWorld() {
		Context ctx = androidContext.get();
		world.model = new ModelContainer(newHeroStartLives, newHeroUnlimitedSaves);

		try {
			WorldMapController.initializeWorldMap(world);
		} catch (IOException e) {
			L.log("Error initializing worldmap: " + e.toString());
		}

		world.model.player.initializeNewPlayer(world.dropLists, newHeroName, newHeroIcon);

		controllers.actorStatsController.recalculatePlayerStats(world.model.player);
		controllers.movementController.respawnPlayer(ctx.getResources());
		controllers.mapController.lotsOfTimePassed();
	}


	public static interface OnSceneLoadedListener {
		void onSceneLoaded();
		void onSceneLoadFailed(Savegames.LoadSavegameResult loadResult);
	}
	public interface OnResourcesLoadedListener {
		void onResourcesLoaded();
	}
}

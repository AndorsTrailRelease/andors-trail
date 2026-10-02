package com.gpl.rpg.AndorsTrail.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Process;
import android.widget.Toast;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.activity.DisplayWorldMapActivity;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.model.ModelContainer;
import com.gpl.rpg.AndorsTrail.model.map.LayeredTileMap;
import com.gpl.rpg.AndorsTrail.model.map.MapLayer;
import com.gpl.rpg.AndorsTrail.model.map.PredefinedMap;
import com.gpl.rpg.AndorsTrail.model.map.TMXMapTranslator;
import com.gpl.rpg.AndorsTrail.model.map.WorldMapSegment;
import com.gpl.rpg.AndorsTrail.model.map.WorldMapSegment.NamedWorldMapArea;
import com.gpl.rpg.AndorsTrail.model.map.WorldMapSegment.WorldMapSegmentMap;
import com.gpl.rpg.AndorsTrail.resource.tiles.TileCollection;
import com.gpl.rpg.AndorsTrail.util.AndroidStorage;
import com.gpl.rpg.AndorsTrail.util.Coord;
import com.gpl.rpg.AndorsTrail.util.CoordRect;
import com.gpl.rpg.AndorsTrail.util.L;
import com.gpl.rpg.AndorsTrail.util.Size;

public final class WorldMapController {

	private static final int WORLDMAP_SCREENSHOT_TILESIZE = 8;
	public static final int WORLDMAP_DISPLAY_TILESIZE = WORLDMAP_SCREENSHOT_TILESIZE;

	public static void updateWorldMap(Context context, final WorldContext world, final Resources res) {
		updateWorldMap(context, world, world.model.currentMaps.map, world.model.currentMaps.tileMap, world.model.currentMaps.tiles, res);
	}

	private static void updateWorldMap(
			Context context, final WorldContext world,
			final PredefinedMap map,
			final LayeredTileMap mapTiles,
			final TileCollection cachedTiles,
			final Resources res) {
		final String worldMapSegmentName = world.maps.getWorldMapSegmentNameForMap(map.name);
		if (worldMapSegmentName == null) return;

		if (!shouldUpdateWorldMap(context, map, worldMapSegmentName, world.maps.worldMapRequiresUpdate)) return;

		(new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... arg0) {
				final MapRenderer renderer = new MapRenderer(world, map, mapTiles, cachedTiles);
				try {
					updateCachedBitmap(context, map, renderer);
					updateWorldMapSegment(context, res, world, worldMapSegmentName);
					world.maps.worldMapRequiresUpdate = false;
					if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) {
						L.log("WorldMapController: Updated worldmap segment " + worldMapSegmentName + " for map " + map.name);
					}
				} catch (IOException e) {
					L.log("Error creating worldmap file for map " + map.name + " : " + e.toString());
				}
				return null;
			}
		}).execute();
	}

	private static boolean shouldUpdateWorldMap(Context context, PredefinedMap map, String worldMapSegmentName, boolean forceUpdate) {
		if (forceUpdate) return true;
		if (!map.visited) return true;
		File file = getFileForMap(context, map, false);
		if (!file.exists()) return true;

		file = getCombinedWorldMapFile(context, worldMapSegmentName);
		if (!file.exists()) return true;

		return false;
	}

	private static void updateCachedBitmap(Context context, PredefinedMap map, MapRenderer renderer) throws IOException {
		ensureWorldmapDirectoryExists(context);

		File file = getFileForMap(context, map, false);
		if (file.exists()) return;

		Bitmap image = renderer.drawMap();
		File tempFile = getTempFile(file);
		FileOutputStream fos = new FileOutputStream(tempFile);
		boolean compressed = image.compress(Bitmap.CompressFormat.PNG, 70, fos);
		fos.flush();
		fos.close();
		image.recycle();
		if (!compressed) throw new IOException("Cannot compress " + file);
		replaceWithTempFile(tempFile, file);
		L.log("WorldMapController: Wrote " + file.getAbsolutePath());
	}

	private static final class MapRenderer {
		private final PredefinedMap map;
		private final LayeredTileMap mapTiles;
		private final TileCollection cachedTiles;
		private final int tileSize;
		private final float scale;
		private final Paint mPaint = new Paint();

		public MapRenderer(final WorldContext world, final PredefinedMap map, final LayeredTileMap mapTiles, final TileCollection cachedTiles) {
			this.map = map;
			this.mapTiles = mapTiles;
			this.cachedTiles = cachedTiles;
			this.tileSize = world.tileManager.tileSize;
			this.scale = (float) WORLDMAP_SCREENSHOT_TILESIZE / world.tileManager.tileSize;
			mapTiles.setColorFilter(mPaint, null, true);
		}

		public Bitmap drawMap() {
			Bitmap image = Bitmap.createBitmap(map.size.width * WORLDMAP_SCREENSHOT_TILESIZE, map.size.height * WORLDMAP_SCREENSHOT_TILESIZE, Config.RGB_565);
			image.setDensity(Bitmap.DENSITY_NONE);
			Canvas canvas = new Canvas(image);
			canvas.scale(scale, scale);

			synchronized (cachedTiles) {
				tryDrawMapLayer(canvas, mapTiles.currentLayout.layerBase);
				tryDrawMapLayer(canvas, mapTiles.currentLayout.layerGround);
				tryDrawMapLayer(canvas, mapTiles.currentLayout.layerObjects);
				tryDrawMapLayer(canvas, mapTiles.currentLayout.layerAbove);
				tryDrawMapLayer(canvas, mapTiles.currentLayout.layerTop);
			}
			return image;
		}

		private void tryDrawMapLayer(Canvas canvas, final MapLayer layer) {
			if (layer != null) drawMapLayer(canvas, layer);
		}

		private void drawMapLayer(Canvas canvas, final MapLayer layer) {
			int py = 0;
			for (int y = 0; y < map.size.height; ++y, py += tileSize) {
				int px = 0;
				for (int x = 0; x < map.size.width; ++x, px += tileSize) {
					final int tile = layer.tiles[x][y];
					if (tile == 0) continue;
					cachedTiles.drawTile(canvas, tile, px, py, mPaint);
				}
			}
		}
	}

	private static void ensureWorldmapDirectoryExists(Context context) throws IOException {
		File dir = AndroidStorage.getStorageDirectory(context, Constants.FILENAME_SAVEGAME_DIRECTORY);
		if (!dir.exists()) dir.mkdir();
		dir = new File(dir, Constants.FILENAME_WORLDMAP_DIRECTORY);
		if (!dir.exists()) dir.mkdir();

		File noMediaFile = new File(dir, ".nomedia");
		if (!noMediaFile.exists()) noMediaFile.createNewFile();
	}
	public static boolean fileForMapExists(Context context, PredefinedMap map) {
		if (map.lastSeenLayoutHash.length() > 0) {
			return getPngFile(context, map.name + '.' + map.lastSeenLayoutHash).exists();
		}
		return getPngFile(context, map.name).exists();
	}
	private static File getFileForMap(Context context, PredefinedMap map, boolean verifyFileExists) {
		if (map.lastSeenLayoutHash.length() > 0) {
			File fileWithHash = getPngFile(context, map.name + '.' + map.lastSeenLayoutHash);
			if (!verifyFileExists) return fileWithHash;
			else if (fileWithHash.exists()) return fileWithHash;
		}
		return getPngFile(context, map.name);
	}
	private static File getPngFile(Context context, String fileName) {
		return new File(getWorldmapDirectory(context), fileName + ".png");
	}
	private static File getWorldmapDirectory(Context context) {
		File dir = AndroidStorage.getStorageDirectory(context, Constants.FILENAME_SAVEGAME_DIRECTORY);
		return new File(dir, Constants.FILENAME_WORLDMAP_DIRECTORY);
	}
	public static File getCombinedWorldMapFile(Context context, String segmentName) {
		return new File(getWorldmapDirectory(context), Constants.FILENAME_WORLDMAP_HTMLFILE_PREFIX + segmentName + Constants.FILENAME_WORLDMAP_HTMLFILE_SUFFIX);
	}

	private static String getWorldMapSegmentAsHtml(Context context, Resources res, WorldContext world, String segmentName) {
		WorldMapSegment segment = world.maps.worldMapSegments.get(segmentName);

		Map<String, File> displayedMapFilenamesPerMapName = new HashMap<String, File>(segment.maps.size());
		Coord offsetWorldmapTo = new Coord(999999, 999999);
		for (WorldMapSegmentMap map : segment.maps.values()) {
			PredefinedMap predefinedMap = world.maps.findPredefinedMap(map.mapName);
			if (predefinedMap == null) continue;
			if (!predefinedMap.visited) continue;
			File f = WorldMapController.getFileForMap(context, predefinedMap, true);
			if (!f.exists()) continue;
			displayedMapFilenamesPerMapName.put(map.mapName, f);

			offsetWorldmapTo.x = Math.min(offsetWorldmapTo.x, map.worldPosition.x);
			offsetWorldmapTo.y = Math.min(offsetWorldmapTo.y, map.worldPosition.y);
		}

		Coord bottomRight = new Coord(0, 0);

		StringBuilder mapsAsHtml = new StringBuilder(1000);
		for (WorldMapSegmentMap segmentMap : segment.maps.values()) {
			File f = displayedMapFilenamesPerMapName.get(segmentMap.mapName);
			if (f == null) continue;

			Size size = getMapSize(segmentMap, world);
			mapsAsHtml
				.append("<img src=\"")
				.append(f.getName())
				.append("\" id=\"")
				.append(segmentMap.mapName)
				.append("\" style=\"width:")
				.append(size.width * WorldMapController.WORLDMAP_DISPLAY_TILESIZE)
				.append("px; height:")
				.append(size.height * WorldMapController.WORLDMAP_DISPLAY_TILESIZE)
				.append("px; left:")
				.append((segmentMap.worldPosition.x - offsetWorldmapTo.x) * WorldMapController.WORLDMAP_DISPLAY_TILESIZE)
				.append("px; top:")
				.append((segmentMap.worldPosition.y - offsetWorldmapTo.y) * WorldMapController.WORLDMAP_DISPLAY_TILESIZE)
				.append("px;\" />");
			if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) mapsAsHtml.append('\n');

			bottomRight.x = Math.max(bottomRight.x, segmentMap.worldPosition.x + size.width);
			bottomRight.y = Math.max(bottomRight.y, segmentMap.worldPosition.y + size.height);
		}
		Size worldmapSegmentSize = new Size(
				(bottomRight.x - offsetWorldmapTo.x) * WorldMapController.WORLDMAP_DISPLAY_TILESIZE
				,(bottomRight.y - offsetWorldmapTo.y) * WorldMapController.WORLDMAP_DISPLAY_TILESIZE
			);

		StringBuilder namedAreasAsHtml = new StringBuilder(500);
		for (NamedWorldMapArea area : segment.namedAreas.values()) {
			CoordRect r = determineNamedAreaBoundary(area, segment, world, displayedMapFilenamesPerMapName.keySet());
			if (r == null) continue;
			namedAreasAsHtml
				.append("<div class=\"namedarea ")
				.append(area.type)
				.append("\" style=\"width:")
				.append(r.size.width * WorldMapController.WORLDMAP_DISPLAY_TILESIZE)
				.append("px; line-height:")
				.append(r.size.height * WorldMapController.WORLDMAP_DISPLAY_TILESIZE)
				.append("px; left:")
				.append((r.topLeft.x - offsetWorldmapTo.x) * WorldMapController.WORLDMAP_DISPLAY_TILESIZE)
				.append("px; top:")
				.append((r.topLeft.y - offsetWorldmapTo.y) * WorldMapController.WORLDMAP_DISPLAY_TILESIZE)
				.append("px;\"><span>")
				.append(area.name)
				.append("</span></div>");
			if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) namedAreasAsHtml.append('\n');
		}

		// Number-to-string conversion not localized since these are only used for layout calculations in the HTML/CSS, and not shown to the user.
		return res.getString(R.string.worldmap_template)
				.replace("{{maps}}", mapsAsHtml.toString())
				.replace("{{areas}}", namedAreasAsHtml.toString())
				.replace("{{sizex}}", Integer.toString(worldmapSegmentSize.width))
				.replace("{{sizey}}", Integer.toString(worldmapSegmentSize.height))
				.replace("{{offsetx}}", Integer.toString(offsetWorldmapTo.x * WorldMapController.WORLDMAP_DISPLAY_TILESIZE))
				.replace("{{offsety}}", Integer.toString(offsetWorldmapTo.y * WorldMapController.WORLDMAP_DISPLAY_TILESIZE));
	}

	private static Size getMapSize(WorldMapSegmentMap map, WorldContext world) {
		return world.maps.findPredefinedMap(map.mapName).size;
	}

	private static CoordRect determineNamedAreaBoundary(NamedWorldMapArea area, WorldMapSegment segment, WorldContext world, Set<String> displayedMapNames) {
		Coord topLeft = null;
		Coord bottomRight = null;

		for (String mapName : area.mapNames) {
			if (!displayedMapNames.contains(mapName)) continue;
			WorldMapSegmentMap map = segment.maps.get(mapName);
			Size size = getMapSize(map, world);
			if (topLeft == null) {
				topLeft = new Coord(map.worldPosition);
			} else {
				topLeft.x = Math.min(topLeft.x, map.worldPosition.x);
				topLeft.y = Math.min(topLeft.y, map.worldPosition.y);
			}
			if (bottomRight == null) {
				bottomRight = new Coord(map.worldPosition.x + size.width, map.worldPosition.y + size.height);
			} else {
				bottomRight.x = Math.max(bottomRight.x, map.worldPosition.x + size.width);
				bottomRight.y = Math.max(bottomRight.y, map.worldPosition.y + size.height);
			}
		}
		if (topLeft == null) return null;
		return new CoordRect(topLeft, new Size(bottomRight.x - topLeft.x, bottomRight.y - topLeft.y));
	}

	public static void updateWorldMapSegment(Context context, Resources res, WorldContext world, String segmentName) throws IOException {
		String mapAsHtml = getWorldMapSegmentAsHtml(context, res, world, segmentName);
		File outputFile = getCombinedWorldMapFile(context, segmentName);
		File tempFile = getTempFile(outputFile);
		PrintWriter pw = new PrintWriter(tempFile);
		pw.write(mapAsHtml);
		pw.close();
		if (pw.checkError()) throw new IOException("Cannot write " + tempFile); // PrintWriter does not throw.
		replaceWithTempFile(tempFile, outputFile);
	}

	/**
	 * Returns the file that a world map file is written to before it replaces that file.
	 * See {@link #replaceWithTempFile}.
	 */
	private static File getTempFile(File file) {
		return new File(file.getPath() + ".tmp");
	}

	/**
	 * Replaces a world map file with its completely written temporary file.
	 *
	 * <p>Renaming is atomic, so a world map file that exists is always complete, also while it is being replaced
	 * and after the process was killed while writing it. All world map files are written on
	 * {@link AsyncTask#SERIAL_EXECUTOR}, so two writers never use the same temporary file at the same time.</p>
	 *
	 * @throws IOException if the file cannot be replaced; the previous file, if any, is then left unchanged.
	 */
	private static void replaceWithTempFile(File tempFile, File file) throws IOException {
		if (!tempFile.renameTo(file)) throw new IOException("Cannot rename " + tempFile + " to " + file);
	}

	public static boolean displayWorldMap(Context context, WorldContext world) {
		String worldMapSegmentName = world.maps.getWorldMapSegmentNameForMap(world.model.currentMaps.map.name);
		if (worldMapSegmentName == null) {
			Toast.makeText(context, context.getResources().getString(R.string.display_worldmap_not_available), Toast.LENGTH_LONG).show();
			return false;
		}

		Intent intent = new Intent(context, DisplayWorldMapActivity.class);
		intent.putExtra("worldMapSegmentName", worldMapSegmentName);
		context.startActivity(intent);

		return true;
	}

	private static volatile WorldMapPopulation currentPopulation;

	/**
	 * Starts generating the world map files that are missing for the maps visited in the loaded savegame.
	 *
	 * <p>The files are generated in the background, so that loading a savegame does not wait for them.
	 * Rendering hundreds of maps made loading a savegame with many visited maps very slow when its world
	 * map files had not been imported. See {@link WorldMapPopulation} for how the work is scheduled, and
	 * when it is stopped and repeated.</p>
	 *
	 * <p>Does nothing if the files of this savegame's player were generated completely before. Must be called
	 * on {@link AsyncTask#SERIAL_EXECUTOR}, after the savegame has been loaded into {@code world}.</p>
	 *
	 * @throws IOException if the world map directory cannot be created.
	 */
	public static void populateWorldMap(final Context context, final WorldContext world, final Resources res) throws IOException {
		ensureWorldmapDirectoryExists(context);
		File idFile = new File(getWorldmapDirectory(context), world.model.player.id);
		if (idFile.exists()) return;

		startPopulation(new WorldMapPopulation(
				AsyncTask.SERIAL_EXECUTOR,
				world,
				new ArrayList<PredefinedMap>(world.maps.getAllMaps()),
				new WorldMapPopulation.MapFileGenerator() {
					@Override
					public boolean generateMissingFiles(PredefinedMap map) throws IOException {
						return generateMissingWorldMapFiles(context, world, res, map);
					}
				},
				idFile));
	}

	/**
	 * Makes {@code population} the current population, which stops the previous one, and queues its first step.
	 */
	static void startPopulation(WorldMapPopulation population) {
		currentPopulation = population;
		population.executor.execute(population);
	}

	/**
	 * Stops the current world map population, if any: its next step does nothing. Must be called on
	 * {@link AsyncTask#SERIAL_EXECUTOR} before the world is reset for loading a savegame or starting a new game,
	 * because the population must not continue with the maps of another world, also if loading fails.
	 */
	public static void stopWorldMapPopulation() {
		currentPopulation = null;
	}

	/**
	 * Generates the world map image of a visited map, and the page of its world map segment, if either is missing.
	 *
	 * @return true if files were generated; false if the map is not visited, not part of the world map, or complete.
	 */
	private static boolean generateMissingWorldMapFiles(Context context, WorldContext world, Resources res, PredefinedMap map) throws IOException {
		if (!map.visited) return false;

		String worldMapSegmentName = world.maps.getWorldMapSegmentNameForMap(map.name);
		if (worldMapSegmentName == null) return false;

		boolean mapFileExists = fileForMapExists(context, map);
		File worldMapFile = getCombinedWorldMapFile(context, worldMapSegmentName);
		if (mapFileExists && worldMapFile.exists()) return false;

		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND); // As in AsyncTask, so that the game is not slowed down.
		LayeredTileMap mapTiles = TMXMapTranslator.readLayeredTileMap(res, world.tileManager.tileCache, map);
		mapTiles.changeColorFilter(map.currentColorFilter);
		TileCollection cachedTiles = world.tileManager.loadTilesFor(map, mapTiles, world, res);

		MapRenderer renderer = new MapRenderer(world, map, mapTiles, cachedTiles);
		updateCachedBitmap(context, map, renderer);
		updateWorldMapSegment(context, res, world, worldMapSegmentName);
		return true;
	}

	/**
	 * Generates the missing world map files of one loaded savegame in the background.
	 *
	 * <p>The maps are processed in steps on an executor. A step ends after the first map for which files had
	 * to be generated, and the next step is queued behind the tasks that were queued meanwhile. In the app the
	 * executor is {@link AsyncTask#SERIAL_EXECUTOR}, which also loads savegames, performs map transitions and
	 * updates the world map of the current map. So a step never runs at the same time as these tasks: it sees
	 * either the completely loaded world of its savegame or another world, never one that is being loaded, and
	 * no two tasks write the same world map file at the same time. A map transition waits for at most one
	 * step.</p>
	 *
	 * <p>The population stops when another population is started, when {@link #stopWorldMapPopulation} is
	 * called before the world is reset, or when the world holds another {@link ModelContainer}. The marker file
	 * is written only after all maps have been processed without an error. Otherwise, the population runs again
	 * at the next load of the savegame, and only generates the files that are still missing.</p>
	 */
	static final class WorldMapPopulation implements Runnable {
		/** Generates the missing world map files of one map. */
		interface MapFileGenerator {
			/**
			 * @return true if files were generated, false if nothing was missing.
			 */
			boolean generateMissingFiles(PredefinedMap map) throws IOException;
		}

		private final Executor executor;
		private final WorldContext world;
		private final ModelContainer model;
		private final List<PredefinedMap> maps;
		private final MapFileGenerator generator;
		private final File markerFile;
		private int nextMap = 0;
		private boolean failed = false;

		/**
		 * @param executor runs the steps; {@link AsyncTask#SERIAL_EXECUTOR} in the app, see the class documentation.
		 * @param world the world, loaded with the savegame whose files are generated.
		 * @param maps the maps to process.
		 * @param generator generates the files of one map.
		 * @param markerFile created when all maps have been processed without an error.
		 */
		WorldMapPopulation(Executor executor, WorldContext world, List<PredefinedMap> maps, MapFileGenerator generator, File markerFile) {
			this.executor = executor;
			this.world = world;
			this.model = world.model;
			this.maps = maps;
			this.generator = generator;
			this.markerFile = markerFile;
		}

		/**
		 * Runs one step: processes maps until files have been generated for one of them, then queues the next
		 * step. After the last map, writes the marker file if no map failed.
		 */
		@Override
		public void run() {
			if (currentPopulation != this || world.model != model) return; // Replaced, or another world was loaded.

			while (nextMap < maps.size()) {
				if (generateMissingFiles(maps.get(nextMap++))) {
					executor.execute(this); // Let the tasks that were queued meanwhile run first.
					return;
				}
			}

			currentPopulation = null;
			if (failed) return; // Retried at the next load.
			try {
				markerFile.createNewFile();
			} catch (IOException e) {
				L.error("WorldMapController: Cannot write " + markerFile + ": " + e);
			}
		}

		/**
		 * Generates the files of one map. A failure is remembered, so that the population is not marked
		 * as complete, and the remaining maps are still processed.
		 *
		 * @return true if generation work was performed and the population should yield before continuing.
		 */
		private boolean generateMissingFiles(PredefinedMap map) {
			try {
				return generator.generateMissingFiles(map);
			} catch (IOException | RuntimeException e) {
				failed = true;
				L.error("WorldMapController: Cannot generate the world map files of " + map.name, e);
				return true;
			}
		}
	}
}

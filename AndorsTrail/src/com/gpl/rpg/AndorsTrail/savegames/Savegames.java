package com.gpl.rpg.AndorsTrail.savegames;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.os.SystemClock;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.Constants;
import com.gpl.rpg.AndorsTrail.controller.WorldMapController;
import com.gpl.rpg.AndorsTrail.model.ModelContainer;
import com.gpl.rpg.AndorsTrail.resource.tiles.TileManager;
import com.gpl.rpg.AndorsTrail.util.L;

public final class Savegames {
	public static final int SLOT_QUICKSAVE = 0;
	public static final long DENY_LOADING_BECAUSE_GAME_IS_CURRENTLY_PLAYED = -1;

	private static long lastBackup = 0;

	public static enum LoadSavegameResult {
		success
		, unknownError
		, savegameIsFromAFutureVersion
		, cheatingDetected
	}

	public static boolean saveWorld(WorldContext world, Context androidContext, int slot) {
		try {
			final String displayInfo = androidContext.getString(R.string.savegame_currenthero_displayinfo, world.model.player.getLevel(), world.model.player.getTotalExperience(), world.model.player.getGold());
			if (slot != SLOT_QUICKSAVE && !world.model.statistics.hasUnlimitedSaves()) {
				world.model.player.savedVersion++;
			}
			String id = world.model.player.id;
			long savedVersion = world.model.player.savedVersion;

			// Create the savegame in a temporary memorystream first to ensure that the savegame can
			// be created correctly. We don't want to trash the user's file unneccessarily if there is an error.
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			saveWorld(world, bos, displayInfo);
			byte[] savegame = bos.toByteArray();
			bos.close();

			FileOutputStream fos = getOutputFile(androidContext, slot);
			fos.write(savegame);
			fos.close();

			if (!world.model.statistics.hasUnlimitedSaves()) {
				if (slot != SLOT_QUICKSAVE) {
					androidContext.deleteFile(Constants.FILENAME_SAVEGAME_QUICKSAVE);
					writeCheatCheck(androidContext, savedVersion, id);
				} else if (SystemClock.uptimeMillis() > lastBackup + 120000) {
					writeBackup(savegame, id);
					lastBackup = SystemClock.uptimeMillis();
				}
			}

			return true;
		} catch (IOException e) {
			L.log("Error saving world: " + e.toString());
			return false;
		}
	}

	private static void writeBackup(byte[] savegame, String playerId) throws IOException {
		File root = Environment.getExternalStorageDirectory();
		File cheatDetectionFolder = new File(root, Constants.CHEAT_DETECTION_FOLDER);
		if (!cheatDetectionFolder.exists()) cheatDetectionFolder.mkdir();
		File backupFile = new File(cheatDetectionFolder, playerId + "X");
		FileOutputStream fileOutputStream = new FileOutputStream(backupFile);
		fileOutputStream.write(savegame);
		fileOutputStream.close();
	}

	public static LoadSavegameResult loadWorld(WorldContext world, ControllerContext controllers, Context androidContext, int slot) {
		try {
			FileHeader fh = quickload(androidContext, slot);
			if(fh == null) {
				return LoadSavegameResult.unknownError;
			}
			if (!fh.hasUnlimitedSaves && slot != SLOT_QUICKSAVE && triedToCheat(androidContext, fh)) {
				return LoadSavegameResult.cheatingDetected;
			}

			FileInputStream fos = getInputFile(androidContext, slot);
			LoadSavegameResult result = loadWorld(androidContext.getResources(), world, controllers, fos, fh);
			fos.close();
			if (result == LoadSavegameResult.success && slot != SLOT_QUICKSAVE && !world.model.statistics.hasUnlimitedSaves()) {
                // save to the quicksave slot before deleting the file
                if (!saveWorld(world, androidContext, SLOT_QUICKSAVE)) {
					return LoadSavegameResult.unknownError;
				}
				getSlotFile(slot).delete();
				writeCheatCheck(androidContext, DENY_LOADING_BECAUSE_GAME_IS_CURRENTLY_PLAYED, fh.playerId);
			}
			return result;
		} catch (IOException e) {
			if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) {
				L.log("Error loading world: " + e.toString());
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				L.log("Load error: " + sw.toString());
			}
			return LoadSavegameResult.unknownError;
		}
	}

	private static boolean triedToCheat(Context androidContext, FileHeader fh) throws IOException {
		long savedVersionToCheck = 0;
		File root = Environment.getExternalStorageDirectory();
		File cheatDetectionFolder = new File(root, Constants.CHEAT_DETECTION_FOLDER);
		if (!cheatDetectionFolder.exists()) cheatDetectionFolder.mkdir();
		File cheatDetectionFile = new File(cheatDetectionFolder, fh.playerId);
		if (cheatDetectionFile.exists()) {
			FileInputStream fileInputStream = new FileInputStream(cheatDetectionFile);
			DataInputStream dataInputStream = new DataInputStream(fileInputStream);
			final CheatDetection cheatDetection = new CheatDetection(dataInputStream);
			savedVersionToCheck	= cheatDetection.savedVersion;
			dataInputStream.close();
			fileInputStream.close();
		}

		if (savedVersionToCheck == DENY_LOADING_BECAUSE_GAME_IS_CURRENTLY_PLAYED) {
			return true;
		}

		if (androidContext.getFileStreamPath(fh.playerId).exists()) {
			FileInputStream fileInputStream = androidContext.openFileInput(fh.playerId);
			DataInputStream dataInputStream = new DataInputStream(fileInputStream);
			final CheatDetection cheatDetection = new CheatDetection(dataInputStream);
			if (cheatDetection.savedVersion == DENY_LOADING_BECAUSE_GAME_IS_CURRENTLY_PLAYED) {
				savedVersionToCheck = DENY_LOADING_BECAUSE_GAME_IS_CURRENTLY_PLAYED;
			}
			else if (cheatDetection.savedVersion > savedVersionToCheck) {
				savedVersionToCheck = cheatDetection.savedVersion;
			}

			if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) {
				L.log("Internal cheatcheck file savedVersion: " + cheatDetection.savedVersion);
			}

			dataInputStream.close();
			fileInputStream.close();
		}

		return (savedVersionToCheck == DENY_LOADING_BECAUSE_GAME_IS_CURRENTLY_PLAYED || fh.savedVersion < savedVersionToCheck);
	}

	private static FileOutputStream getOutputFile(Context androidContext, int slot) throws IOException {
		if (slot == SLOT_QUICKSAVE) {
			return androidContext.openFileOutput(Constants.FILENAME_SAVEGAME_QUICKSAVE, Context.MODE_PRIVATE);
		} else {
			ensureSavegameDirectoryExists();
			return new FileOutputStream(getSlotFile(slot));
		}
	}
	private static void ensureSavegameDirectoryExists() {
		File root = Environment.getExternalStorageDirectory();
		File dir = new File(root, Constants.FILENAME_SAVEGAME_DIRECTORY);
		if (!dir.exists()) dir.mkdir();
	}
	private static FileInputStream getInputFile(Context androidContext, int slot) throws IOException {
		if (slot == SLOT_QUICKSAVE) {
			return androidContext.openFileInput(Constants.FILENAME_SAVEGAME_QUICKSAVE);
		} else {
			return new FileInputStream(getSlotFile(slot));
		}
	}

	public static File getSlotFile(int slot) {
		File root = getSavegameDirectory();
		return new File(root, Constants.FILENAME_SAVEGAME_FILENAME_PREFIX + slot);
	}

	private static File getSavegameDirectory() {
		File root = Environment.getExternalStorageDirectory();
		return new File(root, Constants.FILENAME_SAVEGAME_DIRECTORY);
	}

	public static void saveWorld(WorldContext world, OutputStream outStream, String displayInfo) throws IOException {
		DataOutputStream dest = new DataOutputStream(outStream);
		FileHeader.writeToParcel(dest, world.model.player.getName(),
				displayInfo, world.model.player.iconID,
				world.model.statistics.isDead(),
				world.model.statistics.hasUnlimitedSaves(),
				world.model.player.id,
				world.model.player.savedVersion);
		world.maps.writeToParcel(dest, world);
		world.model.writeToParcel(dest);
		dest.close();
	}

	public static LoadSavegameResult loadWorld(Resources res, WorldContext world, ControllerContext controllers, InputStream inState, FileHeader fh) throws IOException {
		DataInputStream src = new DataInputStream(inState);
		final FileHeader header = new FileHeader(src, fh.skipIcon);
		if (header.fileversion > AndorsTrailApplication.CURRENT_VERSION) return LoadSavegameResult.savegameIsFromAFutureVersion;

		world.maps.readFromParcel(src, world, controllers, header.fileversion);
		world.model = new ModelContainer(src, world, controllers, header.fileversion);
		WorldMapController.populateWorldMap(world, controllers.getResources());
		src.close();
		
		if (header.fileversion < 45) {
			LegacySavegamesContentAdaptations.adaptToNewContentForVersion45(world, controllers, res);
		}
		
		onWorldLoaded(res, world, controllers);

		return LoadSavegameResult.success;
	}

	private static void onWorldLoaded(Resources res, WorldContext world, ControllerContext controllers) {
		controllers.actorStatsController.recalculatePlayerStats(world.model.player);
		controllers.mapController.resetMapsNotRecentlyVisited();
		controllers.movementController.prepareMapAsCurrentMap(world.model.currentMaps.map, res, false);
		controllers.gameRoundController.resetRoundTimers();
	}

	public static FileHeader quickload(Context androidContext, int slot) {
		try {
			if (slot != SLOT_QUICKSAVE) {
				File f = getSlotFile(slot);
				if (!f.exists()) return null;
			}
			FileInputStream fos = getInputFile(androidContext, slot);
			DataInputStream src = new DataInputStream(fos);
			final FileHeader header = new FileHeader(src, false);
			src.close();
			fos.close();
			return header;
		} catch (Exception e) {
			return null;
		}
	}

	private static void writeCheatCheck(Context androidContext, long savedVersion, String playerId) throws IOException {
		File root = Environment.getExternalStorageDirectory();
		File cheatDetectionFolder = new File(root, Constants.CHEAT_DETECTION_FOLDER);
		if (!cheatDetectionFolder.exists()) cheatDetectionFolder.mkdir();
		File cheatDetectionFile = new File(cheatDetectionFolder, playerId);
		FileOutputStream fileOutputStream = new FileOutputStream(cheatDetectionFile);
		DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);
		CheatDetection.writeToParcel(dataOutputStream, savedVersion);
		dataOutputStream.close();
		fileOutputStream.close();

		fileOutputStream = androidContext.openFileOutput(playerId, Context.MODE_PRIVATE);
		dataOutputStream = new DataOutputStream(fileOutputStream);
		CheatDetection.writeToParcel(dataOutputStream, savedVersion);
		dataOutputStream.close();
		fileOutputStream.close();
	}

	private static final Pattern savegameFilenamePattern = Pattern.compile(Constants.FILENAME_SAVEGAME_FILENAME_PREFIX + "(\\d+)");
	public static List<Integer> getUsedSavegameSlots() {
		try {
			final List<Integer> result = new ArrayList<Integer>();
			getSavegameDirectory().listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File f, String filename) {
					Matcher m = savegameFilenamePattern.matcher(filename);
					if (m != null && m.matches()) {
						result.add(Integer.parseInt(m.group(1)));
						return true;
					}
					return false;
				}
			});
			Collections.sort(result);
			return result;
		} catch (Exception e) {
			return null;
		}
	}

	private static final class CheatDetection {
		public final int fileversion;
		public final long savedVersion;

		// ====== PARCELABLE ===================================================================

		public CheatDetection(DataInputStream src) throws IOException {
			this.fileversion = src.readInt();
			this.savedVersion = src.readLong();
		}

		public static void writeToParcel(DataOutputStream dest, long savedVersion) throws IOException {
			dest.writeInt(AndorsTrailApplication.CURRENT_VERSION);
			dest.writeLong(savedVersion);
		}
	}



	public static final class FileHeader {
		public final int fileversion;
		public final String playerName;
		public final String displayInfo;
		public final int iconID;
		public boolean skipIcon = false;
		public final boolean isDead;
		public final boolean hasUnlimitedSaves;
		public final String playerId;
		public final long savedVersion;

		public String describe() {
			return (fileversion == AndorsTrailApplication.DEVELOPMENT_INCOMPATIBLE_SAVEGAME_VERSION ? "(D) " : "") + playerName + ", " + displayInfo;
		}


		// ====== PARCELABLE ===================================================================

		public FileHeader(DataInputStream src, boolean skipIcon) throws IOException {
			int fileversion = src.readInt();
			if (fileversion == 11) fileversion = 5; // Fileversion 5 had no version identifier, but the first byte was 11.
			this.fileversion = fileversion;
			if (fileversion >= 14) { // Before fileversion 14 (0.6.7), we had no file header.
				this.playerName = src.readUTF();
				this.displayInfo = src.readUTF();
			} else {
				this.playerName = null;
				this.displayInfo = null;
			}

			if (fileversion >= 43) {
				int id = src.readInt();
				if (skipIcon || id > TileManager.LAST_HERO) {
					this.iconID = TileManager.CHAR_HERO_0;
					this.skipIcon = true;
				} else {
					this.iconID = id;
				}
			} else {
				this.iconID = TileManager.CHAR_HERO_0;
			}

			if (fileversion >= 49) {
				this.isDead = src.readBoolean();
				this.hasUnlimitedSaves = src.readBoolean();
				this.playerId = src.readUTF();
				this.savedVersion = src.readLong();
			} else {
				this.isDead = false;
				this.hasUnlimitedSaves = true;
				this.playerId = "";
				this.savedVersion = 0;
			}
		}

		public static void writeToParcel(DataOutputStream dest, String playerName, String displayInfo, int iconID, boolean isDead, boolean hasUnlimitedSaves, String playerId, long savedVersion) throws IOException {
			dest.writeInt(AndorsTrailApplication.CURRENT_VERSION);
			dest.writeUTF(playerName);
			dest.writeUTF(displayInfo);
			dest.writeInt(iconID);
			dest.writeBoolean(isDead);
			dest.writeBoolean(hasUnlimitedSaves);
			dest.writeUTF(playerId);
			dest.writeLong(savedVersion);
		}
	}
}

package com.gpl.rpg.AndorsTrail.controller;

import java.util.Random;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.util.ConstRange;
import com.gpl.rpg.AndorsTrail.util.Range;

public final class Constants {
	public static final int PERCENT_EXP_LOST_WHEN_DIED = 20;
	public static final int LEVELUP_EFFECT_HEALTH = 5;
	public static final int LEVELUP_EFFECT_ATK_CH = 5;
	public static final int LEVELUP_EFFECT_ATK_DMG = 1;
	public static final int LEVELUP_EFFECT_DEF_CH = 3;
	public static final int FIRST_SKILL_POINT_IS_GIVEN_AT_LEVEL = 4;
	public static final int NEW_SKILL_POINT_EVERY_N_LEVELS = 4;
	public static final int MARKET_PRICEFACTOR_PERCENT = 15;
	public static final int MONSTER_AGGRESSION_CHANCE_PERCENT = 15;
	public static final int EXP_FACTOR_DAMAGERESISTANCE = 9;
	public static final float EXP_FACTOR_SCALING = 0.7f;
	public static final int FLEE_FAIL_CHANCE_PERCENT = 20;
	public static final long MINIMUM_INPUT_INTERVAL_FAST = 50;
	public static final long MINIMUM_INPUT_INTERVAL_STD = 200;
	//TODO restore final modifier before release
	public static long MINIMUM_INPUT_INTERVAL = AndorsTrailApplication.DEVELOPMENT_FASTSPEED ? MINIMUM_INPUT_INTERVAL_FAST : MINIMUM_INPUT_INTERVAL_STD;
	public static final int MAX_MAP_WIDTH = 46;
	public static final int MAX_MAP_HEIGHT = 46;

	public static final int MONSTER_MOVEMENT_TURN_DURATION_MS = 1200;
	public static final int MONSTER_IMMOBILE_MOVE_COST = 999;

	public static final int TICK_DELAY = 500;
	private static final int ROUND_DURATION = 6000;
	private static final int FULLROUND_DURATION = 25000;
	public static final int TICKS_PER_ROUND = ROUND_DURATION / TICK_DELAY;
	public static final int TICKS_PER_FULLROUND = FULLROUND_DURATION / TICK_DELAY;
	public static final int SPLATTER_DURATION_MS = 20000;
	public static final int STATUS_TEXT_AUTOHIDE_DELAY = ROUND_DURATION;

	public static final ConstRange monsterWaitTurns = new ConstRange(5,1);
	public static final long MAP_UNVISITED_RESPAWN_DURATION_MS = 3 * 60 * 1000; // 3 min in milliseconds

	public static final String PREFERENCE_MODEL_LASTRUNVERSION = "lastversion";
	public static final String FILENAME_SAVEGAME_QUICKSAVE = "savegame";
	public static final String FILENAME_SAVEGAME_DIRECTORY = "andors-trail";
	public static final String FILENAME_WORLDMAP_DIRECTORY = "worldmap";
	public static final String FILENAME_WORLDMAP_HTMLFILE_PREFIX = "worldmap_";
	public static final String FILENAME_WORLDMAP_HTMLFILE_SUFFIX = ".html";
	public static final String FILENAME_SAVEGAME_FILENAME_PREFIX = "savegame";
	public static final String PLACEHOLDER_PLAYERNAME = "$playername";
	public static final String PLACEHOLDER_AKKU = "$akku";
	public static final String CHEAT_DETECTION_FOLDER = "dEAGyGE3YojqXjI3x4x7";
	public static final String PASSIVE_ACHIEVEMENT_CHECK_PHRASE = "passive_achievement_check";

	public static final String SAVEGAME_FILE_MIME_TYPE = "application/octet-stream";
	public static final String WORLDMAP_FILE_MIME_TYPE = "image/png";
	public static final String NO_FILE_EXTENSION_MIME_TYPE = "application/no_file_extension_mime_type";

	public static final Random rnd = new Random();
	public static int rollValue(final ConstRange r) { return rollValue(r.max, r.current); }
	public static int rollValue(final ConstRange r, int bias) { return rollValue((r.max + 1) * 100 -1, r.current * 100 + bias)/100; }
	public static int rollValue(final Range r) { return rollValue(r.max, r.current); }
	private static int rollValue(final int max, final int min) {
		if (max <= min) return max;
		else return rnd.nextInt(max - min + 1) + min;
	}
	public static boolean roll100(final int chance) { return rollResult(100, chance); }
	public static boolean rollResult(final ConstRange r) { return rollResult(r.max, r.current); }
	public static boolean rollResult(final ConstRange r, int bias) { return rollResult(r.max * 100, r.current * 100 + bias); }
	public static boolean rollResult(final Range r) { return rollResult(r.max, r.current); }
	private static boolean rollResult(final int probabilityMax, final int probabilityValue) { return rnd.nextInt(probabilityMax) < probabilityValue; }
}

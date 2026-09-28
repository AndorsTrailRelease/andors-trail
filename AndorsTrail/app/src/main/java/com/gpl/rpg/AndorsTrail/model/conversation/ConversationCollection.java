package com.gpl.rpg.AndorsTrail.model.conversation;

import java.util.Collection;
import java.util.HashMap;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.resource.parsers.ConversationListParser;
import com.gpl.rpg.AndorsTrail.util.L;

public final class ConversationCollection {
	public static final String PHRASE_CLOSE = "X";
	public static final String PHRASE_SHOP = "S";
	public static final String PHRASE_ATTACK = "F";
	public static final String PHRASE_REMOVE = "R";
	public static final String REPLY_NEXT = "N";
	public static final String REPLY_CONCAT = "C";

	private final HashMap<String, Phrase> phrases = new HashMap<String, Phrase>();

	public boolean hasPhrase(String id) {
		return phrases.containsKey(id);
	}
	public Phrase getPhrase(String id) {
		if (AndorsTrailApplication.DEVELOPMENT_VALIDATEDATA) {
			if (!phrases.containsKey(id)) {
				L.log("WARNING: Cannot find requested conversation phrase id \"" + id + "\".");
				return null;
			}
		}
		return phrases.get(id);
	}

	public Collection<String> initialize(ConversationListParser parser, String input) {
		return parser.parseRows(input, phrases);
	}

	// Unit test method. Not part of the game logic.
	public HashMap<String, Phrase> UNITTEST_getAllPhrases() {
		return phrases;
	}
}

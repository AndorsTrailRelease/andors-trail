package com.gpl.rpg.AndorsTrail.model.conversation;

import com.gpl.rpg.AndorsTrail.model.script.ScriptEffect;

public final class Phrase {
	private static final Reply[] NO_REPLIES = new Reply[0];

	public String message;
	public final Reply[] replies;
	public final ScriptEffect[] scriptEffects; // If this phrase is reached, all these effects will run
	public final String switchToNPC;

	public Phrase(
			String message
			, Reply[] replies
			, ScriptEffect[] scriptEffects
			, String switchToNPC
	) {
		this.message = message;
		if (replies == null || replies.length == 0) replies = NO_REPLIES;
		this.replies = replies;
		this.scriptEffects = scriptEffects;
		this.switchToNPC = switchToNPC;
	}
}

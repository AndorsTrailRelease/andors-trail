package com.gpl.rpg.AndorsTrail;

public final class SessionState {
	private boolean androidTVNoticeShown = false;

	public boolean hasShownAndroidTVNotice() {
		return androidTVNoticeShown;
	}

	public void markAndroidTVNoticeShown() {
		androidTVNoticeShown = true;
	}
}

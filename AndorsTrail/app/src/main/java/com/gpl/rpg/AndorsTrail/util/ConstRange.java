package com.gpl.rpg.AndorsTrail.util;

import androidx.annotation.NonNull;

public final class ConstRange {
	public final int max;
	public final int current;

	public ConstRange(Range r) {
		this.max = r.max;
		this.current = r.current;
	}

	public ConstRange(ConstRange r) {
		this.max = r.max;
		this.current = r.current;
	}

	public ConstRange(int max, int current) {
		this.max = max;
		this.current = current;
	}

	@NonNull
	public String toString() {
		return Format.localizeInt(current) + "/" + Format.localizeInt(max);
	}

	public String toMinMaxString() {
		if (isMax()) return Format.localizeInt(max);
		else return Format.localizeInt(current) + "-" + Format.localizeInt(max);
	}

	public String toMinMaxAbsString() {
		if (isMax()) return Format.localizeInt(Math.abs(max));
		else if (current < 0) return Format.localizeInt(Math.abs(max)) + "-" + Format.localizeInt(Math.abs(current));
		else return Format.localizeInt(Math.abs(current)) + "-" + Format.localizeInt(Math.abs(max));
	}

	public boolean isMax() {
		return max == current;
	}

	public int average() {
		return (max + current) / 2;
	}

	public float averagef() {
		return ((float) max + current) / 2f;
	}

	public String toPercentString() {
		return Format.localizePercentCeil((double) current / (double) max);
	}

}

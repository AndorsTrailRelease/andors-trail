package com.gpl.rpg.AndorsTrail.util;

import com.gpl.rpg.AndorsTrail.model.ChecksumBuilder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class Coord {
	public int x;
	public int y;
	public Coord() {}
	public Coord(int x, int y) { this.x = x; this.y = y; }
	public Coord(Coord p) { this.x = p.x; this.y = p.y; }

	public String toString() { return "(" + x + ',' + y + ')'; }
	public void set(int x, int y) { this.x = x; this.y = y; }
	public void set(Coord r) {
		this.x = r.x;
		this.y = r.y;
	}

	public boolean equals(final Coord p) { return p.x == this.x && p.y == this.y; }
	public boolean equals(final int x, final int y) { return x == this.x && y == this.y; }
	public boolean contains(final Coord p) { return p.x == this.x && p.y == this.y; }
	public boolean contains(final int x, final int y) { return x == this.x && y == this.y; }
	public boolean isAdjacentTo(Coord p) {
		final int dx = x - p.x;
		final int dy = y - p.y;
		if (dx == 0 && dy == 0) return false;
		if (Math.abs(dx) > 1) return false;
		if (Math.abs(dy) > 1) return false;
		return true;
	}



	// ====== PARCELABLE ===================================================================

	public Coord(DataInputStream src, int fileversion) throws IOException {
		this.readFromParcel(src, fileversion);
	}
	public void readFromParcel(DataInputStream src, int fileversion) throws IOException {
		this.x = src.readInt();
		this.y = src.readInt();
	}

	public void writeToParcel(DataOutputStream dest) throws IOException {
		dest.writeInt(x);
		dest.writeInt(y);
	}

	public void addToChecksum(ChecksumBuilder builder) {
		builder.add(x);
		builder.add(y);
	}
}

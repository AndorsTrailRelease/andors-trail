package com.gpl.rpg.AndorsTrail.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Test;

public final class WorldMapControllerTest {

	private static final String VERSION = "2-1a2b3c";

	@Test
	public void currentWorldMapHtmlIsRecognized() throws IOException {
		File file = createTempFile(WorldMapController.getWorldMapHtmlVersionMarker(VERSION) + "\n<!DOCTYPE html>");
		try {
			assertTrue(WorldMapController.isWorldMapHtmlCurrent(file, VERSION));
		} finally {
			assertTrue(file.delete());
		}
	}

	@Test
	public void worldMapHtmlFromOtherVersionIsRejected() throws IOException {
		File file = createTempFile(WorldMapController.getWorldMapHtmlVersionMarker("2-ffff") + "\n<!DOCTYPE html>");
		try {
			assertFalse(WorldMapController.isWorldMapHtmlCurrent(file, VERSION));
		} finally {
			assertTrue(file.delete());
		}
	}

	@Test
	public void worldMapHtmlWithFirstFormatMarkerIsRejected() throws IOException {
		File file = createTempFile("<!-- worldmap-format-version:1 -->\n<!DOCTYPE html>");
		try {
			assertFalse(WorldMapController.isWorldMapHtmlCurrent(file, VERSION));
		} finally {
			assertTrue(file.delete());
		}
	}

	@Test
	public void legacyWorldMapHtmlIsRejected() throws IOException {
		File file = createTempFile("<!DOCTYPE html><html><body></body></html>");
		try {
			assertFalse(WorldMapController.isWorldMapHtmlCurrent(file, VERSION));
		} finally {
			assertTrue(file.delete());
		}
	}

	@Test
	public void emptyWorldMapHtmlIsRejected() throws IOException {
		File file = createTempFile("");
		try {
			assertFalse(WorldMapController.isWorldMapHtmlCurrent(file, VERSION));
		} finally {
			assertTrue(file.delete());
		}
	}

	@Test
	public void missingWorldMapHtmlIsRejected() throws IOException {
		File file = File.createTempFile("andors-trail-worldmap-missing-", ".html");
		assertTrue(file.delete());
		assertFalse(WorldMapController.isWorldMapHtmlCurrent(file, VERSION));
	}

	private static File createTempFile(String content) throws IOException {
		File file = File.createTempFile("andors-trail-worldmap-", ".html");
		try (FileOutputStream output = new FileOutputStream(file)) {
			output.write(content.getBytes("UTF-8"));
		}
		return file;
	}
}

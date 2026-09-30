package com.gpl.rpg.AndorsTrail.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Checks res/values/worldmap_template.xml, which is compiled as a string resource by aapt2.
 * aapt2 removes unescaped double quotes and uses them to switch the preservation of whitespace on and off,
 * so a single missing backslash breaks the script of the world map page without any build error.
 */
public final class WorldMapTemplateTest {

	@Test
	public void templateHasNoUnescapedQuotesBesidesTheOpeningOne() throws Exception {
		String template = readTemplateResource();
		assertTrue("the template must start with a quote, to preserve its line breaks", template.startsWith("\""));
		assertEquals("unescaped double quotes in worldmap_template", 1, countUnescapedQuotes(template));
	}

	@Test
	public void compiledTemplateKeepsScriptIntact() throws Exception {
		String html = compileLikeAapt2(readTemplateResource());
		assertTrue("unexpected start of the page", html.startsWith("<!DOCTYPE html"));
		assertTrue("quotes or line breaks of the script were lost", html.contains("\tvar pos = params.split(\",\");\n"));
		assertTrue("the entry point used by DisplayWorldMapActivity is missing", html.contains("\twindow.startWorldMapLazyLoading = scheduleLoad;\n"));
		assertTrue("the page must be hidden until it is positioned", html.contains("\tdocument.documentElement.className = \"positioning\";\n"));
		assertTrue("the flag read by DisplayWorldMapActivity is missing", html.contains("\t\twindow.worldMapCenteredOnPlayer = true;\n"));
		assertTrue("the map must be centered again when the visible area changes",
				html.contains("\twindow.visualViewport.addEventListener(\"resize\", keepCenteredOnPlayer, false);\n"));
		for (String placeholder : new String[] { "{{maps}}", "{{areas}}", "{{sizex}}", "{{sizey}}", "{{offsetx}}", "{{offsety}}" }) {
			assertTrue("missing " + placeholder, html.contains(placeholder));
		}
	}

	private static String readTemplateResource() throws Exception {
		File file = findTemplateFile();
		NodeList strings = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("string");
		for (int i = 0; i < strings.getLength(); ++i) {
			Element string = (Element) strings.item(i);
			if ("worldmap_template".equals(string.getAttribute("name"))) return string.getTextContent();
		}
		throw new AssertionError("worldmap_template not found in " + file);
	}

	private static File findTemplateFile() {
		// Unit tests run in the app module directory, while the resources are in the parent project directory.
		for (File dir = new File("").getAbsoluteFile(); dir != null; dir = dir.getParentFile()) {
			File file = new File(dir, "res/values/worldmap_template.xml");
			if (file.isFile()) return file;
		}
		throw new AssertionError("res/values/worldmap_template.xml not found");
	}

	private static int countUnescapedQuotes(String s) {
		int count = 0;
		for (int i = 0; i < s.length(); ++i) {
			char c = s.charAt(i);
			if (c == '\\') ++i;
			else if (c == '"') ++count;
		}
		return count;
	}

	// Same processing as aapt2 applies to the text of a string resource (StringBuilder::AppendText in ResourceUtils.cpp).
	private static String compileLikeAapt2(String s) {
		StringBuilder result = new StringBuilder();
		boolean quoted = false;
		boolean lastWasSpace = false;
		for (int i = 0; i < s.length(); ++i) {
			char c = s.charAt(i);
			if (!quoted && Character.isWhitespace(c)) {
				if (!lastWasSpace) result.append(' ');
				lastWasSpace = true;
				continue;
			}
			lastWasSpace = false;
			if (c == '\\') {
				assertTrue("backslash at end of template", ++i < s.length());
				char escaped = s.charAt(i);
				assertTrue("unsupported escape \\" + escaped, escaped != 'u');
				if (escaped == 'n') result.append('\n');
				else if (escaped == 't') result.append('\t');
				else result.append(escaped);
			} else if (c == '"') {
				quoted = !quoted;
			} else {
				assertTrue("unescaped apostrophe outside of quotes", quoted || c != '\'');
				result.append(c);
			}
		}
		return result.toString().trim();
	}
}

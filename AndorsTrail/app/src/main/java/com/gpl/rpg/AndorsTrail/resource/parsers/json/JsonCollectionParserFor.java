package com.gpl.rpg.AndorsTrail.resource.parsers.json;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.util.L;
import android.util.Pair;

public abstract class JsonCollectionParserFor<T> extends JsonParserFor<Pair<String, T>> {
	public HashSet<String> parseRows(String input, HashMap<String, T> dest) {

		HashSet<String> ids = new HashSet<String>();
		ArrayList<Pair<String, T>> objects = new ArrayList<Pair<String, T>>();

		try {
			parseRows(new JSONArray(input), objects);
		} catch (JSONException e) {
			if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) {
				L.log("ERROR loading resource data: " + e.toString());
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				pw.close();
				sw.flush();
				L.log(sw.toString());
				L.log("Failing data: " + input);
			}
		}

		for (Pair<String, T> o : objects) {
			final String id = o.first;
			if (AndorsTrailApplication.DEVELOPMENT_VALIDATEDATA) {
				if (id == null || id.length() <= 0) {
					L.log("WARNING: Entity " + o.second.toString() + " has empty id.");
				} else if (dest.containsKey(id)) {
					L.log("WARNING: Entity " + id + " is duplicated.");
				}
			}
			dest.put(id, o.second);
			ids.add(id);
		}
		return ids;
	}
}

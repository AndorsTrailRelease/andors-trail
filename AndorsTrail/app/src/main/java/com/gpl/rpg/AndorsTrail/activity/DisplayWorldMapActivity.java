package com.gpl.rpg.AndorsTrail.activity;

import java.io.File;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.WorldMapController;
import com.gpl.rpg.AndorsTrail.model.map.PredefinedMap;
import com.gpl.rpg.AndorsTrail.model.map.WorldMapSegment;
import com.gpl.rpg.AndorsTrail.model.map.WorldMapSegment.WorldMapSegmentMap;
import com.gpl.rpg.AndorsTrail.util.AndroidStorage;
import com.gpl.rpg.AndorsTrail.util.Coord;
import com.gpl.rpg.AndorsTrail.util.L;
import com.gpl.rpg.AndorsTrail.util.ThemeHelper;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

public final class DisplayWorldMapActivity extends AndorsTrailBaseActivity {
	private WorldContext world;

	private WebView displayworldmap_webview;
	private String worldMapSegmentName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(ThemeHelper.getBaseTheme());
		super.onCreate(savedInstanceState);

		AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this);
		if (!app.isInitialized()) { finish(); return; }
		this.world = app.getWorld();
		initializeView(this, R.layout.displayworldmap, R.id.worldmap_root);

		displayworldmap_webview = (WebView) findViewById(R.id.displayworldmap_webview);
		displayworldmap_webview.setBackgroundColor(ThemeHelper.getThemeColor(this, R.attr.ui_theme_displayworldmap_bg_color));
		displayworldmap_webview.getSettings().setBuiltInZoomControls(true);
		displayworldmap_webview.getSettings().setUseWideViewPort(true);
		displayworldmap_webview.setVerticalScrollBarEnabled(true);
		displayworldmap_webview.setHorizontalScrollBarEnabled(true);
		displayworldmap_webview.getSettings().setAllowFileAccess(true);
		enableJavascript();

		Button b = (Button) findViewById(R.id.displayworldmap_close);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DisplayWorldMapActivity.this.finish();
			}
		});

		b = (Button) findViewById(R.id.displayworldmap_recenter);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				recenter();
			}
		});

		worldMapSegmentName = getIntent().getStringExtra("worldMapSegmentName");
	}

	@SuppressLint("SetJavaScriptEnabled")
	public void enableJavascript() {
		displayworldmap_webview.getSettings().setJavaScriptEnabled(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		update();
	}

	WorldMapSegmentMap map;
	Coord offsetWorldmapTo;
	@SuppressLint("NewApi")
	private void update() {
		File worldmap = WorldMapController.getCombinedWorldMapFile(this, worldMapSegmentName);

		if (!worldmap.exists()) {
			Toast.makeText(this, getResources().getString(R.string.menu_button_worldmap_failed), Toast.LENGTH_LONG).show();
			this.finish();
		}

		WorldMapSegment segment = world.maps.worldMapSegments.get(worldMapSegmentName);
		map = segment.maps.get(world.model.currentMaps.map.name);
		if (map == null) {
			this.finish();
			return;
		}
		
		offsetWorldmapTo = new Coord(999999, 999999);
		for (WorldMapSegmentMap map : segment.maps.values()) {
			PredefinedMap predefinedMap = world.maps.findPredefinedMap(map.mapName);
			if (predefinedMap == null) continue;
			if (!predefinedMap.visited) continue;
			if (!WorldMapController.fileForMapExists(this, predefinedMap)) continue;

			offsetWorldmapTo.x = Math.min(offsetWorldmapTo.x, map.worldPosition.x);
			offsetWorldmapTo.y = Math.min(offsetWorldmapTo.y, map.worldPosition.y);
		}

		String url = AndroidStorage.getUrlForFile(this, worldmap) + '?'
				+ (world.model.player.position.x + map.worldPosition.x) * WorldMapController.WORLDMAP_DISPLAY_TILESIZE
				+ ','
				+ (world.model.player.position.y + map.worldPosition.y-1) * WorldMapController.WORLDMAP_DISPLAY_TILESIZE;
		L.log("Showing " + url);
		displayworldmap_webview.loadUrl(url);
		displayworldmap_webview.setBackgroundColor(ThemeHelper.getThemeColor(this, R.attr.ui_theme_displayworldmap_bg_color));
		displayworldmap_webview.setWebViewClient(new WebViewClient() {
			@SuppressLint("NewApi")
			@Override
			public void onPageFinished(WebView view, String url)
			{
				recenter();
			}
		});
	}
	
	private void recenter() {
		displayworldmap_webview.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (map != null) {
					//Local map to global worldmap
					int x = world.model.player.position.x + map.worldPosition.x - offsetWorldmapTo.x;
					//Tile coord to pixel coord
					x *= WorldMapController.WORLDMAP_DISPLAY_TILESIZE;
					x += WorldMapController.WORLDMAP_DISPLAY_TILESIZE/2;
					//Zoom level
					x = (int)(x * displayworldmap_webview.getScale());
					//Center
					x -= displayworldmap_webview.getWidth() / 2;
					
					//Local map to global worldmap
					int y = world.model.player.position.y + map.worldPosition.y  - offsetWorldmapTo.y;
					//Tile coord to pixel coord
					y *= WorldMapController.WORLDMAP_DISPLAY_TILESIZE;
					y += WorldMapController.WORLDMAP_DISPLAY_TILESIZE/2;
					//Zoom level
					y = (int)(y * displayworldmap_webview.getScale());
					//Center
					y -= displayworldmap_webview.getHeight() / 2;
					
					displayworldmap_webview.scrollTo(x, y);
				}
			}
		}, 100);
	}
	
	@Override
	public void finish() {
	    ViewGroup view = (ViewGroup) getWindow().getDecorView();
	    view.removeAllViews();
	    super.finish();
	}
}

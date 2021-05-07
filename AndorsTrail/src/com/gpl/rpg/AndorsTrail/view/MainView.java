package com.gpl.rpg.AndorsTrail.view;

import java.lang.ref.WeakReference;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.AndorsTrailPreferences;
import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.Constants;
import com.gpl.rpg.AndorsTrail.controller.InputController;
import com.gpl.rpg.AndorsTrail.controller.VisualEffectController.BloodSplatter;
import com.gpl.rpg.AndorsTrail.controller.VisualEffectController.SpriteMoveAnimation;
import com.gpl.rpg.AndorsTrail.controller.VisualEffectController.VisualEffectAnimation;
import com.gpl.rpg.AndorsTrail.controller.listeners.CombatSelectionListener;
import com.gpl.rpg.AndorsTrail.controller.listeners.GameRoundListener;
import com.gpl.rpg.AndorsTrail.controller.listeners.MapLayoutListener;
import com.gpl.rpg.AndorsTrail.controller.listeners.MonsterMovementListener;
import com.gpl.rpg.AndorsTrail.controller.listeners.MonsterSpawnListener;
import com.gpl.rpg.AndorsTrail.controller.listeners.PlayerMovementListener;
import com.gpl.rpg.AndorsTrail.controller.listeners.VisualEffectFrameListener;
import com.gpl.rpg.AndorsTrail.model.ModelContainer;
import com.gpl.rpg.AndorsTrail.model.actor.Monster;
import com.gpl.rpg.AndorsTrail.model.item.Loot;
import com.gpl.rpg.AndorsTrail.model.map.LayeredTileMap;
import com.gpl.rpg.AndorsTrail.model.map.MapLayer;
import com.gpl.rpg.AndorsTrail.model.map.MonsterSpawnArea;
import com.gpl.rpg.AndorsTrail.model.map.PredefinedMap;
import com.gpl.rpg.AndorsTrail.resource.tiles.TileCollection;
import com.gpl.rpg.AndorsTrail.resource.tiles.TileManager;
import com.gpl.rpg.AndorsTrail.util.Coord;
import com.gpl.rpg.AndorsTrail.util.CoordRect;
import com.gpl.rpg.AndorsTrail.util.Size;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public final class MainView extends SurfaceView
	implements SurfaceHolder.Callback,
		PlayerMovementListener,
		CombatSelectionListener,
		MonsterSpawnListener,
		MonsterMovementListener,
		MapLayoutListener,
		VisualEffectFrameListener,
		GameRoundListener {

	private final int tileSize;
	private float scale;
	private int scaledTileSize;

	private Size screenSizeTileCount = null;
	private final Coord screenOffset = new Coord(); // pixel offset where the image begins
	private final Coord mapTopLeft = new Coord(); // Map coords of visible map
	private CoordRect mapViewArea; // Area in mapcoordinates containing the visible map. topleft == this.topleft
	private Rect redrawClip = new Rect(); //Area in screen coordinates containing the visible map.
	
	private final ModelContainer model;
	private final WorldContext world;
	private final ControllerContext controllers;
	private final InputController inputController;
	private final AndorsTrailPreferences preferences;

	private final SurfaceHolder holder;
	private final Paint mPaint = new Paint();
	private final CoordRect p1x1 = new CoordRect(new Coord(), new Size(1,1));
	private boolean hasSurface = false;
	
	//DEBUG
//	private Coord touchedTile = null;
//	private static Paint touchHighlight = new Paint();
//	private static Paint redrawHighlight = new Paint();
//	
//	static {
//		touchHighlight.setColor(Color.RED);
//		touchHighlight.setStrokeWidth(0f);
//		touchHighlight.setStyle(Style.STROKE);
//		redrawHighlight.setColor(Color.CYAN);
//		redrawHighlight.setStrokeWidth(0f);
//		redrawHighlight.setStyle(Style.STROKE);
//	}
	
	private PredefinedMap currentMap;
	private LayeredTileMap currentTileMap;
	private TileCollection tiles;
	
	private final Coord playerPosition = new Coord();
	private Size surfaceSize;
	private boolean redrawNextTick = false;
	
	private boolean scrolling = false;
	private Coord scrollVector;
	private long scrollStartTime;
	//TODO restore private final modifiers before release
	public static long SCROLL_DURATION = Constants.MINIMUM_INPUT_INTERVAL;
	private int movingSprites = 0;
	private SpriteMoveAnimationHandler movingSpritesRedrawTick = new SpriteMoveAnimationHandler(this);
	private Paint alternateColorFilterPaint = new Paint();
	private boolean useAlternateColorFilterPaint = false;

	public MainView(Context context, AttributeSet attr) {
		super(context, attr);
		this.holder = getHolder();

		AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivityContext(context);
		this.controllers = app.getControllerContext();
		this.world = app.getWorld();
		this.model = world.model;
		this.tileSize = world.tileManager.tileSize;
		this.inputController = controllers.inputController;
		this.preferences = app.getPreferences();

		alternateColorFilterPaint.setStyle(Style.FILL);
		
		holder.addCallback(this);

		setFocusable(true);
		requestFocus();
		setOnClickListener(this.inputController);
		setOnLongClickListener(this.inputController);
		
		
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
		// onKeyboardAction needs context to start new activities.
		// canAcceptInput() checks done in handler so it can preserve keystate even if it does not have focus.
		return inputController.onKeyboardAction(getContext(), keyEvent, canAcceptInput()) ||  super.onKeyDown(keyCode, keyEvent);
			
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent keyEvent) {
		// Android provides artificial ACTION_UP events when focus changes; we process them to prevent "stuck key" effect after dialogs close
		return inputController.onKeyboardAction(getContext(), keyEvent, canAcceptInput()) ||  super.onKeyUp(keyCode, keyEvent);
	}

	@Override
	public void surfaceChanged(SurfaceHolder sh, int format, int w, int h) {
		if (w <= 0 || h <= 0) return;

		
		this.scale = world.tileManager.scale;
		this.mPaint.setFilterBitmap(scale != 1);
		this.scaledTileSize = world.tileManager.viewTileSize;
//		this.surfaceSize = new Size(w, h);
		this.surfaceSize = new Size((int) (getWidth() / scale), (int) (getHeight() / scale));
		this.screenSizeTileCount = new Size(
				(int) Math.floor(getWidth() / scaledTileSize)
				,(int) Math.floor(getHeight() / scaledTileSize)
			);
		
		if (sh.getSurfaceFrame().right != surfaceSize.width || sh.getSurfaceFrame().bottom != surfaceSize.height) {
			sh.setFixedSize(surfaceSize.width, surfaceSize.height);
		}
		
		if (model.currentMaps.map != null) {
			onPlayerEnteredNewMap(model.currentMaps.map, model.player.position);
		} else {
			redrawAll(RedrawAllDebugReason.SurfaceChanged);
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder sh) {
		
		hasSurface = true;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder sh) {
		hasSurface = false;
		movingSpritesRedrawTick.stop();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!canAcceptInput()) return true;

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_MOVE:
			final int tile_x = (int) Math.floor(((int)event.getX() - screenOffset.x * scale) / scaledTileSize) + mapTopLeft.x;
			final int tile_y = (int) Math.floor(((int)event.getY() - screenOffset.y * scale) / scaledTileSize) + mapTopLeft.y;
//			touchedTile = new Coord(tile_x, tile_y);
			if (inputController.onTouchedTile(tile_x, tile_y)) return true;
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_OUTSIDE:
			inputController.onTouchCancel();
			break;
		}
		return super.onTouchEvent(event);
	}
	
	private boolean canAcceptInput() {
		if (!model.uiSelections.isMainActivityVisible) return false;
		if (!hasSurface) return false;
		return true;
	}

	private static enum RedrawAllDebugReason {
		SurfaceChanged, MapChanged, PlayerMoved, SpriteMoved, MapScrolling, FilterAnimation
	}
	private static enum RedrawAreaDebugReason {
		MonsterMoved, MonsterKilled, EffectCompleted, AsyncRequest
	}
	private static enum RedrawTileDebugReason {
		SelectionRemoved, SelectionAdded, Bag
	}

	private void redrawAll(RedrawAllDebugReason why) {
		if (preferences.enableUiAnimations) {
			if (scrolling && why != RedrawAllDebugReason.MapScrolling) return;
			if (!scrolling && movingSprites > 0 && why != RedrawAllDebugReason.SpriteMoved) return;
		}
		redrawArea_(mapViewArea, null, 0, 0);
	}
	private void redrawTile(final Coord p, RedrawTileDebugReason why) {
		if (scrolling) return;
		p1x1.topLeft.set(p);
		redrawArea_(p1x1, null, 0, 0);
	}
	private void redrawArea(final CoordRect area, RedrawAreaDebugReason why) {
		if (scrolling) return;
		redrawArea_(area, null, 0, 0);
	}
	private void redrawArea_(CoordRect area, final VisualEffectAnimation effect, int tileID, int textYOffset) {
		if (!hasSurface) return;

		
		if (!currentMap.intersects(area)) return;
		if (!mapViewArea.intersects(area)) return;

		if (shouldRedrawEverything()) {
			area = mapViewArea;
		}
		
		calculateRedrawRect(area);
		redrawRect.intersect(redrawClip);
		Canvas c = null;
		try {
			c = holder.lockCanvas(redrawRect);
			// lockCanvas sometimes changes redrawRect, when the double-buffer has not been
			// sufficiently filled beforehand. In those cases, we need to redraw the whole scene.
			if (area != mapViewArea) {
				if (isRedrawRectWholeScreen(redrawRect)) {
					area = mapViewArea;
				}
			}
			if (area == mapViewArea) {
				area = adaptAreaToScrolling(area);
			}
			
			synchronized (holder) { synchronized (tiles) {
				int xScroll = 0;
				int yScroll = 0;
				if (scrolling && scrollVector != null) {
					xScroll = (int) (tileSize - (tileSize * (System.currentTimeMillis() - scrollStartTime) / SCROLL_DURATION));
					xScroll = Math.max(0, Math.min(tileSize, xScroll)) * scrollVector.x;
					yScroll = (int) (tileSize - (tileSize * (System.currentTimeMillis() - scrollStartTime) / SCROLL_DURATION));
					yScroll = Math.max(0, Math.min(tileSize, yScroll)) * scrollVector.y;
				}
				c.clipRect(redrawClip);
				c.translate(screenOffset.x + xScroll, screenOffset.y + yScroll);
//				c.scale(scale, scale);
				doDrawRect(c, area);
				if (effect != null) {
					drawFromMapPosition(c, area, effect.position, tileID);
					if (effect.displayText != null) {
						drawEffectText(c, area, effect, textYOffset, effect.getTextPaint());
					}
				}

//				c.drawRect(new Rect(
//						(area.topLeft.x - mapViewArea.topLeft.x) * tileSize, 
//						(area.topLeft.y - mapViewArea.topLeft.y) * tileSize, 
//						(area.topLeft.x - mapViewArea.topLeft.x + area.size.width) * tileSize - 1, 
//						(area.topLeft.y - mapViewArea.topLeft.y + area.size.height) * tileSize - 1),
//						redrawHighlight);
//				if (touchedTile != null) c.drawRect(new Rect(
//						(touchedTile.x - mapViewArea.topLeft.x) * tileSize, 
//						(touchedTile.y - mapViewArea.topLeft.y) * tileSize, 
//						(touchedTile.x - mapViewArea.topLeft.x + 1) * tileSize - 1, 
//						(touchedTile.y - mapViewArea.topLeft.y + 1) * tileSize - 1), 
//						touchHighlight);
			} }
		} finally {
			if (c != null) holder.unlockCanvasAndPost(c);
		}
	}
	
	private boolean isRedrawRectWholeScreen(Rect redrawRect) {
//		if (redrawRect.width() < mapViewArea.size.width * scaledTileSize) return false;
//		if (redrawRect.height() < mapViewArea.size.height * scaledTileSize) return false;
		if (redrawRect.width() < mapViewArea.size.width * tileSize) return false;
		if (redrawRect.height() < mapViewArea.size.height * tileSize) return false;
		return true;
	}

	private boolean shouldRedrawEverything() {
		if (scrolling) return true;
		if (model.uiSelections.isInCombat) return true; // Discard the "optimized drawing" setting while in combat.
		if (preferences.optimizedDrawing) return false;
		return true;
	}
	private final Rect redrawRect = new Rect();
	private void redrawAreaWithEffect(final VisualEffectAnimation effect, int tileID, int textYOffset) {
		CoordRect area = effect.area;
//		if (shouldRedrawEverythingForVisualEffect()) area = mapViewArea;
		redrawArea_(area, effect, tileID, textYOffset);
	}
	private void clearCanvas() {
		if (!hasSurface) return;
		Canvas c = null;
		try {
			c = holder.lockCanvas();
			synchronized (holder) {
				c.drawColor(Color.BLACK);
			}
		} finally {
			if (c != null) holder.unlockCanvasAndPost(c);
		}
	}

	private CoordRect adaptAreaToScrolling(final CoordRect area) {
		
		if (!scrolling || scrollVector == null) return area;

		int x, y, w, h;
		if (scrollVector.x > 0) {
			x = area.topLeft.x - scrollVector.x;
			w = area.size.width + scrollVector.x;
		} else {
			x = area.topLeft.x;
			w = area.size.width - scrollVector.x;
		}
		if (scrollVector.y > 0) {
			y = area.topLeft.y - scrollVector.y;
			h = area.size.height + scrollVector.y;
		} else {
			y = area.topLeft.y;
			h = area.size.height - scrollVector.y;
		}
		CoordRect result = new CoordRect(new Coord(x, y), new Size(w, h)); 
		return result;
	}
	
	private void calculateRedrawRect(final CoordRect area) {
		worldCoordsToScreenCords(area, redrawRect);
	}

	private void worldCoordsToScreenCords(final CoordRect worldArea, Rect destScreenRect) {
//		destScreenRect.left = screenOffset.x + (worldArea.topLeft.x - mapViewArea.topLeft.x) * scaledTileSize;
//		destScreenRect.top = screenOffset.y + (worldArea.topLeft.y - mapViewArea.topLeft.y) * scaledTileSize;
//		destScreenRect.right = destScreenRect.left + worldArea.size.width * scaledTileSize;
//		destScreenRect.bottom = destScreenRect.top + worldArea.size.height * scaledTileSize;
		
		destScreenRect.left = screenOffset.x + (worldArea.topLeft.x - mapViewArea.topLeft.x) * tileSize;
		destScreenRect.top = screenOffset.y + (worldArea.topLeft.y - mapViewArea.topLeft.y) * tileSize;
		destScreenRect.right = destScreenRect.left + worldArea.size.width * tileSize;
		destScreenRect.bottom = destScreenRect.top + worldArea.size.height * tileSize;
	}
	
//	private void worldCoordsToBitmapCoords(final CoordRect worldArea, Rect dstBitmapArea) {
//		dstBitmapArea.left = worldArea.topLeft.x * tileSize;
//		dstBitmapArea.top = worldArea.topLeft.y * tileSize;
//		dstBitmapArea.right = dstBitmapArea.left + worldArea.size.width * tileSize;
//		dstBitmapArea.bottom = dstBitmapArea.top + worldArea.size.height * tileSize;
//		
//	}

	private void doDrawRect(Canvas canvas, CoordRect area) {
		doDrawRect_Ground(canvas, area);
		doDrawRect_Objects(canvas, area);
		doDrawRect_Above(canvas, area);
		if (useAlternateColorFilterPaint) {
			applyAlternateFilter(canvas, area);
		}
	}
	
	private void doDrawRect_Ground(Canvas canvas, CoordRect area) {
		drawMapLayer(canvas, area, currentTileMap.currentLayout.layerGround);
		tryDrawMapLayer(canvas, area, currentTileMap.currentLayout.layerObjects);
	}
	
	private void doDrawRect_Objects(Canvas canvas, CoordRect area) {
//		if (!tryDrawMapBitmap(canvas, area, objectsBitmap)) {
//			tryDrawMapLayer(canvas, area, currentTileMap.currentLayout.layerObjects);
//		}
		
		for (BloodSplatter splatter : currentMap.splatters) {
			drawFromMapPosition(canvas, area, splatter.position, splatter.iconID);
		}

		for (Loot l : currentMap.groundBags) {
			if (l.isVisible) {
				drawFromMapPosition(canvas, area, l.position, TileManager.iconID_groundbag);
			}
		}

		if (!model.player.hasVFXRunning) {
			drawFromMapPosition(canvas, area, playerPosition, model.player.iconID);
		} else if (area.contains(playerPosition)) {
			int vfxElapsedTime = (int) (System.currentTimeMillis() - model.player.vfxStartTime);
			if (vfxElapsedTime > model.player.vfxDuration) vfxElapsedTime = model.player.vfxDuration;
			int x = ((model.player.position.x - mapViewArea.topLeft.x) * tileSize * vfxElapsedTime + ((model.player.lastPosition.x - mapViewArea.topLeft.x) * tileSize * (model.player.vfxDuration - vfxElapsedTime))) / model.player.vfxDuration;
			int y = ((model.player.position.y - mapViewArea.topLeft.y) * tileSize * vfxElapsedTime + ((model.player.lastPosition.y - mapViewArea.topLeft.y) * tileSize * (model.player.vfxDuration - vfxElapsedTime))) / model.player.vfxDuration;
			tiles.drawTile(canvas, model.player.iconID, x, y, mPaint);
		}
		for (MonsterSpawnArea a : currentMap.spawnAreas) {
			for (Monster m : a.monsters) {
				if (!m.hasVFXRunning) {
					drawFromMapPosition(canvas, area, m.rectPosition, m.iconID);
				} else if (area.intersects(m.rectPosition) || area.intersects(new CoordRect(m.lastPosition,m.rectPosition.size))) {
					int vfxElapsedTime = (int) (System.currentTimeMillis() - m.vfxStartTime);
					if (vfxElapsedTime > m.vfxDuration) vfxElapsedTime = m.vfxDuration;
					int x = ((m.position.x - mapViewArea.topLeft.x) * tileSize * vfxElapsedTime + ((m.lastPosition.x - mapViewArea.topLeft.x) * tileSize * (m.vfxDuration - vfxElapsedTime))) / m.vfxDuration;
					int y = ((m.position.y - mapViewArea.topLeft.y) * tileSize * vfxElapsedTime + ((m.lastPosition.y - mapViewArea.topLeft.y) * tileSize * (m.vfxDuration - vfxElapsedTime))) / m.vfxDuration;
					tiles.drawTile(canvas, m.iconID, x, y, mPaint);
				}
			}
		}
	}
	
	private void doDrawRect_Above(Canvas canvas, CoordRect area) {
		tryDrawMapLayer(canvas, area, currentTileMap.currentLayout.layerAbove);
		tryDrawMapLayer(canvas, area, currentTileMap.currentLayout.layerTop);
		
		if (model.uiSelections.selectedPosition != null) {
			if (model.uiSelections.selectedMonster != null) {
				drawFromMapPosition(canvas, area, model.uiSelections.selectedPosition, TileManager.iconID_attackselect);
			} else {
				drawFromMapPosition(canvas, area, model.uiSelections.selectedPosition, TileManager.iconID_moveselect);
			}
		}
	}

	private void tryDrawMapLayer(Canvas canvas, final CoordRect area, final MapLayer layer) {
		if (layer != null) drawMapLayer(canvas, area, layer);
	}

	private void drawMapLayer(Canvas canvas, final CoordRect area, final MapLayer layer) {
		int my = area.topLeft.y;
		int py = (area.topLeft.y - mapViewArea.topLeft.y) * tileSize;
		int px0 = (area.topLeft.x - mapViewArea.topLeft.x) * tileSize;
		for (int y = 0; y < area.size.height; ++y, ++my, py += tileSize) {
			int mx = area.topLeft.x;
			if (my < 0) continue;
			if (my >= currentMap.size.height) break;
			int px = px0;
			for (int x = 0; x < area.size.width; ++x, ++mx, px += tileSize) {
				if (mx < 0) continue;
				if (mx >= currentMap.size.width) break;
				final int tile = layer.tiles[mx][my];
				if (tile == 0) continue;
				tiles.drawTile(canvas, tile, px, py, mPaint);
			}
		}
	}
	

	private void applyAlternateFilter(Canvas canvas, CoordRect area) {
		canvas.drawRect(canvas.getClipBounds(), alternateColorFilterPaint);
		
	}
	
	private void drawFromMapPosition(Canvas canvas, final CoordRect area, final Coord p, final int tile) {
		if (!area.contains(p)) return;
		_drawFromMapPosition(canvas, area, p.x, p.y, tile);
	}
	private void drawFromMapPosition(Canvas canvas, final CoordRect area, final CoordRect p, final int tile) {
		if (!area.intersects(p)) return;
		_drawFromMapPosition(canvas, area, p.topLeft.x, p.topLeft.y, tile);
	}
	private void _drawFromMapPosition(Canvas canvas, final CoordRect area, int x, int y, final int tile) {
		x -= mapViewArea.topLeft.x;
		y -= mapViewArea.topLeft.y;
//		if (	   (x >= 0 && x < mapViewArea.size.width)
//				&& (y >= 0 && y < mapViewArea.size.height)) {
			tiles.drawTile(canvas, tile, x * tileSize, y * tileSize, mPaint);
//		}
	}

	private void drawEffectText(Canvas canvas, final CoordRect area, final VisualEffectAnimation e, int textYOffset, Paint textPaint) {
		int x = (e.position.x - mapViewArea.topLeft.x) * tileSize + tileSize/2;
		int y = (e.position.y - mapViewArea.topLeft.y) * tileSize + tileSize/2 + textYOffset;
		canvas.drawText(e.displayText, x, y, textPaint);
	}

	@Override
	public void onPlayerEnteredNewMap(PredefinedMap map, Coord p) {
		movingSpritesRedrawTick.start();
		synchronized (holder) {
			currentMap = map;
			currentTileMap = model.currentMaps.tileMap;
			tiles = world.model.currentMaps.tiles;
			movingSprites = 0;
			Size visibleNumberOfTiles = new Size(
					Math.min(screenSizeTileCount.width, currentMap.size.width)
					,Math.min(screenSizeTileCount.height, currentMap.size.height)
				);
			mapViewArea = new CoordRect(mapTopLeft, visibleNumberOfTiles);
			updateClip();

//			screenOffset.set(
//					(surfaceSize.width - scaledTileSize * visibleNumberOfTiles.width) / 2
//					,(surfaceSize.height - scaledTileSize * visibleNumberOfTiles.height) / 2
//				);
			

			screenOffset.set(
					(surfaceSize.width - tileSize * visibleNumberOfTiles.width) / 2
					,(surfaceSize.height - tileSize * visibleNumberOfTiles.height) / 2
				);

			useAlternateColorFilterPaint = currentTileMap.setColorFilter(this.mPaint, this.alternateColorFilterPaint, preferences.highQualityFilters);
		}

//		touchedTile = null;
		
		clearCanvas();

		recalculateMapTopLeft(model.player.position, false);
		redrawAll(RedrawAllDebugReason.MapChanged);
	}

	private void recalculateMapTopLeft(Coord playerPosition, boolean allowScrolling) {
		synchronized (holder) {
			int oldX = mapTopLeft.x;
			int oldY = mapTopLeft.y;
			this.playerPosition.set(playerPosition);
			mapTopLeft.set(0, 0);

			if (currentMap.size.width > screenSizeTileCount.width) {
				mapTopLeft.x = Math.max(0, playerPosition.x - mapViewArea.size.width/2);
				mapTopLeft.x = Math.min(mapTopLeft.x, currentMap.size.width - mapViewArea.size.width);
			}
			if (currentMap.size.height > screenSizeTileCount.height) {
				mapTopLeft.y = Math.max(0, playerPosition.y - mapViewArea.size.height/2);
				mapTopLeft.y = Math.min(mapTopLeft.y, currentMap.size.height - mapViewArea.size.height);
			}
			updateClip();
			if (allowScrolling) {
				if (mapTopLeft.x != oldX || mapTopLeft.y != oldY) {
					scrollVector = new Coord(mapTopLeft.x - oldX, mapTopLeft.y - oldY);
					new ScrollAnimationHandler(this).start();
				}
			} else {
				scrolling = false;
			}
		}
	}
	
	private void updateClip() {
		worldCoordsToScreenCords(mapViewArea, redrawClip);
	}
	

	public static final class ScrollAnimationHandler extends Handler implements Runnable {

		private static final int FRAME_DURATION = 40;

		private final WeakReference<MainView> view;
		
		public ScrollAnimationHandler(MainView view) {
			this.view = new WeakReference<MainView>(view);
		}
		
		@Override
		public void run() {
			MainView v = view.get();
			if (v == null) return;
			if (System.currentTimeMillis() - v.scrollStartTime >= SCROLL_DURATION) {
				onCompleted();
			} else {
				postDelayed(this, FRAME_DURATION);
			}
			update();
		}

		private void update() {
			MainView v = view.get();
			if (v == null) return;
			v.redrawAll(RedrawAllDebugReason.MapScrolling);
		}

		private void onCompleted() {
			MainView v = view.get();
			if (v == null) return;
			v.scrolling = false;
			v.scrollVector = null;
		}

		public void start() {
			MainView v = view.get();
			if (v == null) return;
			v.scrolling = true;
			v.scrollStartTime = System.currentTimeMillis();
			postDelayed(this, 0);
		}
	}
	
	
	public static final class SpriteMoveAnimationHandler extends Handler implements Runnable {

		private static final int FRAME_DURATION = 40;
		private final WeakReference<MainView> view;
		private boolean stop = true;
		
		public SpriteMoveAnimationHandler(MainView view) {
			this.view = new WeakReference<MainView>(view);
		}
		
		@Override
		public void run() {
			if (!stop) postDelayed(this, FRAME_DURATION);
			update();
		}

		private void update() {
//			L.log("stop="+stop+" - scroll="+scrolling+" - moving="+movingSprites);
			if (stop) return;
			MainView v = view.get();
			if (v == null) return;
			if (!v.scrolling) {
				if (v.movingSprites > 0) {
					//TODO : limit redraw area when shouldRedrawEverything() returns false. 
					//Implies keeping track of the animation bounding box in a thread-safe way... :'(
					v.redrawAll(RedrawAllDebugReason.SpriteMoved);
				}
			}
			synchronized (this) {
				if (v.movingSprites <= 0) stop();
			}
		}

		public void start() {
			if (stop) {
				stop = false;
				MainView v = view.get();
				if (v == null) return;
				if (v.controllers.preferences.enableUiAnimations) postDelayed(this, 0);
			}
		}
		
		public void stop() {
			stop = true;
		}
	}
	
	
	
	@Override
	public void onPlayerMoved(PredefinedMap map, Coord newPosition, Coord previousPosition) {
		if (map != currentMap) return;
		recalculateMapTopLeft(newPosition, preferences.enableUiAnimations);
		redrawAll(RedrawAllDebugReason.PlayerMoved);
	}

	public void subscribe() {
		controllers.gameRoundController.gameRoundListeners.add(this);
		controllers.effectController.visualEffectFrameListeners.add(this);
		controllers.mapController.mapLayoutListeners.add(this);
		controllers.movementController.playerMovementListeners.add(this);
		controllers.combatController.combatSelectionListeners.add(this);
		controllers.monsterSpawnController.monsterSpawnListeners.add(this);
		controllers.monsterMovementController.monsterMovementListeners.add(this);
	}
	public void unsubscribe() {
		controllers.monsterMovementController.monsterMovementListeners.remove(this);
		controllers.monsterSpawnController.monsterSpawnListeners.remove(this);
		controllers.combatController.combatSelectionListeners.remove(this);
		controllers.movementController.playerMovementListeners.remove(this);
		controllers.mapController.mapLayoutListeners.remove(this);
		controllers.effectController.visualEffectFrameListeners.remove(this);
		controllers.gameRoundController.gameRoundListeners.remove(this);
	}

	@Override
	public void onMonsterSelected(Monster m, Coord selectedPosition, Coord previousSelection) {
		if (previousSelection != null) redrawTile(previousSelection, RedrawTileDebugReason.SelectionRemoved);
		redrawTile(selectedPosition, RedrawTileDebugReason.SelectionAdded);
	}

	@Override
	public void onMovementDestinationSelected(Coord selectedPosition, Coord previousSelection) {
		if (previousSelection != null) redrawTile(previousSelection, RedrawTileDebugReason.SelectionRemoved);
		redrawTile(selectedPosition, RedrawTileDebugReason.SelectionAdded);
	}

	@Override
	public void onCombatSelectionCleared(Coord previousSelection) {
		redrawTile(previousSelection, RedrawTileDebugReason.SelectionRemoved);
	}

	@Override
	public void onMonsterSpawned(PredefinedMap map, Monster m) {
		if (map != currentMap) return;
		if (!mapViewArea.intersects(m.rectPosition)) return;
		redrawNextTick = true;
	}

	@Override
	public void onMonsterRemoved(PredefinedMap map, Monster m, CoordRect previousPosition) {
		if (map != currentMap) return;
		redrawArea(previousPosition, RedrawAreaDebugReason.MonsterKilled);
	}

	@Override
	public void onMonsterSteppedOnPlayer(Monster m) {
	}

	@Override
	public void onMonsterMoved(PredefinedMap map, Monster m, CoordRect previousPosition) {
		if (map != currentMap) return;
		if (!mapViewArea.intersects(m.rectPosition) && !mapViewArea.intersects(previousPosition)) return;
		if (model.uiSelections.isInCombat) {
			redrawArea(previousPosition, RedrawAreaDebugReason.MonsterMoved);
			redrawArea(m.rectPosition, RedrawAreaDebugReason.MonsterMoved);
		} else {
			redrawNextTick = true;
		}
	}

	@Override
	public void onSplatterAdded(PredefinedMap map, Coord p) {
		if (map != currentMap) return;
		if (!mapViewArea.contains(p)) return;
		redrawNextTick = true;
	}

	@Override
	public void onSplatterChanged(PredefinedMap map, Coord p) {
		if (map != currentMap) return;
		if (!mapViewArea.contains(p)) return;
		redrawNextTick = true;
	}

	@Override
	public void onSplatterRemoved(PredefinedMap map, Coord p) {
		if (map != currentMap) return;
		if (!mapViewArea.contains(p)) return;
		redrawNextTick = true;
	}

	@Override
	public void onLootBagCreated(PredefinedMap map, Coord p) {
		if (map != currentMap) return;
		redrawTile(p, RedrawTileDebugReason.Bag);
	}

	@Override
	public void onLootBagRemoved(PredefinedMap map, Coord p) {
		if (map != currentMap) return;
		redrawTile(p, RedrawTileDebugReason.Bag);
	}

	@Override
	public void onMapTilesChanged(PredefinedMap map, LayeredTileMap tileMap) {
		if (map != currentMap) return;
		useAlternateColorFilterPaint = currentTileMap.setColorFilter(this.mPaint, this.alternateColorFilterPaint, preferences.highQualityFilters);
		redrawAll(RedrawAllDebugReason.MapChanged);
	}

	@Override
	public void onNewAnimationFrame(VisualEffectAnimation animation, int tileID, int textYOffset) {
		redrawAreaWithEffect(animation, tileID, textYOffset);
	}

	@Override
	public void onAnimationCompleted(VisualEffectAnimation animation) {
		redrawArea(animation.area, RedrawAreaDebugReason.EffectCompleted);
	}

	@Override
	public void onSpriteMoveStarted(SpriteMoveAnimation animation) {
		if (animation.map != currentMap) return;
		synchronized (movingSpritesRedrawTick) {
			movingSprites++;
			movingSpritesRedrawTick.start();
		}
	}
	
	@Override
	public void onNewSpriteMoveFrame(SpriteMoveAnimation animation) {
		//redrawMoveArea_(CoordRect.getBoundingRect(animation.origin, animation.destination, animation.actor.tileSize), animation);
	}
	
	@Override
	public void onSpriteMoveCompleted(SpriteMoveAnimation animation) {
		if (animation.map != currentMap) return;
		movingSprites--;
		redrawArea(CoordRect.getBoundingRect(animation.origin, animation.destination, animation.actor.tileSize), RedrawAreaDebugReason.EffectCompleted);
	}
	
	@Override
	public void onAsyncAreaUpdate(CoordRect area) {
		redrawArea(area, RedrawAreaDebugReason.AsyncRequest);
	}

	@Override
	public void onNewTick() {
		if (!redrawNextTick) return;

		redrawAll(RedrawAllDebugReason.PlayerMoved);

		redrawNextTick = false;
	}

	@Override
	public void onNewRound() { }

	@Override
	public void onNewFullRound() { }
}

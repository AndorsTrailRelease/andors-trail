package com.gpl.rpg.AndorsTrail.view;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.gpl.rpg.AndorsTrail.AndorsTrailPreferences;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.listeners.ActorConditionListener;
import com.gpl.rpg.AndorsTrail.model.ability.ActorCondition;
import com.gpl.rpg.AndorsTrail.model.actor.Actor;
import com.gpl.rpg.AndorsTrail.resource.tiles.TileManager;
import com.gpl.rpg.AndorsTrail.util.ThemeHelper;

public final class DisplayActiveActorConditionIcons implements ActorConditionListener {

	private final AndorsTrailPreferences preferences;
	private final TileManager tileManager;
	private final ControllerContext controllers;
	private final WorldContext world;
	private final RelativeLayout activeConditions;
	private final ArrayList<ActiveConditionIcon> currentConditionIcons = new ArrayList<ActiveConditionIcon>();
	private final WeakReference<Context> androidContext;
	
	private Actor target;

	public DisplayActiveActorConditionIcons(
			final ControllerContext controllers,
			final WorldContext world,
			Context androidContext,
			RelativeLayout activeConditions) {
		this.controllers = controllers;
		this.world = world;
		this.preferences = controllers.preferences;
		this.tileManager = world.tileManager;
		this.androidContext = new WeakReference<Context>(androidContext);
		this.activeConditions = activeConditions;
	}
	
	public void setTarget(Actor target) {
		if (this.target == target) return;
		this.target = target;
		cleanUp();
	}

	@Override
	public void onActorConditionAdded(Actor actor, ActorCondition condition) {
		if (actor != target) return;
		ActiveConditionIcon icon = getFirstFreeIcon();
		icon.setActiveCondition(condition, false);
		icon.show();
		updateIconState();
	}

	@Override
	public void onActorConditionRemoved(Actor actor, ActorCondition condition) {
		if (actor != target) return;
		ActiveConditionIcon icon = getIconFor(condition, false);
		if (icon == null) return;
		icon.hide(true);
		updateIconState();
	}

	@Override
	public void onActorConditionDurationChanged(Actor actor, ActorCondition condition) { 
		if (actor != target) return;
		ActiveConditionIcon icon = getIconFor(condition, false);
		if (icon == null) return;
		icon.setIconAndText();
	}

	@Override
	public void onActorConditionMagnitudeChanged(Actor actor, ActorCondition condition) {
		if (actor != target) return;
		ActiveConditionIcon icon = getIconFor(condition, false);
		if (icon == null) return;
		icon.setIconAndText();
	}

	@Override
	public void onActorConditionRoundEffectApplied(Actor actor, ActorCondition condition) {
		if (actor != target) return;
		ActiveConditionIcon icon = getIconFor(condition, false);
		if (icon == null) return;
		icon.pulseAnimate();
	}


	@Override
	public void onActorConditionImmunityAdded(Actor actor, ActorCondition condition) {
		if (actor != target) return;
		ActiveConditionIcon icon = getFirstFreeIcon();
		icon.setActiveCondition(condition, true);
		icon.show();
		updateIconState();
	}

	@Override
	public void onActorConditionImmunityRemoved(Actor actor, ActorCondition condition) {
		if (actor != target) return;
		ActiveConditionIcon icon = getIconFor(condition, true);
		if (icon == null) return;
		icon.hide(true);
		updateIconState();
	}

	@Override
	public void onActorConditionImmunityDurationChanged(Actor actor, ActorCondition condition) {
		if (actor != target) return;
		ActiveConditionIcon icon = getIconFor(condition, true);
		if (icon == null) return;
		icon.setIconAndText();
	}
	
	public void unsubscribe() {
		controllers.actorStatsController.actorConditionListeners.remove(this);
		for (ActiveConditionIcon icon : currentConditionIcons) icon.condition = null;
	}

	public void subscribe() {
		cleanUp();
		controllers.actorStatsController.actorConditionListeners.add(this);
	}
	
	private void cleanUp() {
		for (ActiveConditionIcon icon : currentConditionIcons) icon.hide(false);
		if (target != null) {
			for (ActorCondition condition : target.conditions) {
				getFirstFreeIcon().setActiveCondition(condition, false);
			}
			for (ActorCondition condition : target.immunities) {
				getFirstFreeIcon().setActiveCondition(condition, true);
			}
		}
		updateIconState();
	}
	
	private static class ActiveConditionIconImageView extends ImageView {
		enum Position { first, mid, last, single }
		enum Direction { horizontal, vertical }
		
		private Position pos=Position.single;
		private Direction dir=Direction.horizontal;
		private boolean reverse=false;
		
		public ActiveConditionIconImageView(Context context) {
			super(context);
		}
		
		@Override
		public int[] onCreateDrawableState(int extraSpace) {
			int[] parentState =  super.onCreateDrawableState(extraSpace+3);
			switch (pos) {
			case first:
				mergeDrawableStates(parentState, new int[]{R.attr.first});
				break;
			case last:
				mergeDrawableStates(parentState, new int[]{R.attr.last});
				break;
			case single:
				mergeDrawableStates(parentState, new int[]{R.attr.single});
				break;
			case mid:
			default:
				break;
			}
			switch (dir) {
			case horizontal:
				mergeDrawableStates(parentState, new int[]{R.attr.horizontal});
				break;
			case vertical:
				mergeDrawableStates(parentState, new int[]{R.attr.vertical});
				break;
			default:
				break;
			}
			if (reverse) mergeDrawableStates(parentState, new int[]{R.attr.reverse});
			return parentState;
		}
		
		public void setVertical() {
			dir = Direction.vertical;
			refreshDrawableState();
		}
		
		public void setHorizontal() {
			dir = Direction.horizontal;
			refreshDrawableState();
		}
		
		public void reverse() {
			reverse = !reverse;
			refreshDrawableState();
		}
		
		public void setFirst() {
			pos = Position.first;
			refreshDrawableState();
		}

		public void setMid() {
			pos = Position.mid;
			refreshDrawableState();
		}

		public void setLast() {
			pos = Position.last;
			refreshDrawableState();
		}

		public void setSingle() {
			pos = Position.single;
			refreshDrawableState();
		}
		
	}
	
	private void updateIconState() {
		ActiveConditionIcon first = null;
		ActiveConditionIcon last = null;
		for (ActiveConditionIcon icon : currentConditionIcons) {
			if (icon.isVisible()) {
				if (first == null) {
					first = icon;
				} else {
					icon.image.setMid();
				}
				last = icon;
			}
		}
		if (first == null) return; 
		if (first == last) {
			first.image.setSingle();
		} else if (last != null) {
			first.image.setFirst();
			last.image.setLast();
		}
	}

	private final class ActiveConditionIcon implements AnimationListener {
		public final int id;
		private boolean immunity = false;
		public ActorCondition condition;
		public final ActiveConditionIconImageView image;
//		public final TextView text;
		private final Animation onNewIconAnimation;
		private final Animation onRemovedIconAnimation;
		private final Animation onAppliedEffectAnimation;
		final Resources res;
		
		public ActiveConditionIcon(Context context, int id) {
			this.id = id;
			this.image = new ActiveConditionIconImageView(context);
			this.image.setId(id);
			this.image.setBackgroundResource(ThemeHelper.getThemeResource(context, R.attr.ui_theme_buttonbar_drawable));
			this.image.setHorizontal();
			this.image.reverse();
			this.onNewIconAnimation = AnimationUtils.loadAnimation(context, R.anim.scaleup);
			this.onRemovedIconAnimation = AnimationUtils.loadAnimation(context, R.anim.scaledown);
			this.onAppliedEffectAnimation = AnimationUtils.loadAnimation(context, R.anim.scalebeat);
			this.onRemovedIconAnimation.setAnimationListener(this);

			res = context.getResources();
		}

		private void setActiveCondition(ActorCondition condition, boolean immunity) {
			this.immunity = immunity;
			this.condition = condition;
			
			
			setIconAndText();
			image.setVisibility(View.VISIBLE);
		}

		public void setIconAndText() {
			String duration = null;
			String magnitude = null;
			boolean showMagnitude = (condition.magnitude != 1 && condition.magnitude != ActorCondition.MAGNITUDE_REMOVE_ALL);
			if (showMagnitude) {
				magnitude = res.getString(R.string.actorcondition_icon_magnitude, condition.magnitude);
			}
			if (condition.duration == ActorCondition.DURATION_FOREVER || condition.duration == ActorCondition.DURATION_NONE) {
				duration = res.getString(R.string.actorcondition_icon_duration_infinite);
			} else if (condition.duration == ActorCondition.DURATION_FOREVER_UNTIL_SLEEP) {
				duration = "";
			} else {
				duration = res.getString(R.string.actorcondition_icon_duration, condition.duration);
			}
			tileManager.setImageViewTile(DisplayActiveActorConditionIcons.this.androidContext.get(), image, condition.conditionType, immunity, magnitude, duration);
			
		}

		public void hide(boolean useAnimation) {
			if (useAnimation) {
				if (preferences.enableUiAnimations) {
					image.startAnimation(onRemovedIconAnimation);
				} else {
					onAnimationEnd(onRemovedIconAnimation);
				}
			} else {
				image.setVisibility(View.GONE);
				condition = null;
			}
//			text.setVisibility(View.GONE);
		}
		public void show() {
			if (!preferences.enableUiAnimations) return;
			image.startAnimation(onNewIconAnimation);
//			if (text.getVisibility() == View.VISIBLE) text.startAnimation(onNewIconAnimation);
		}

		public void pulseAnimate() {
			if (!preferences.enableUiAnimations) return;
			image.startAnimation(onAppliedEffectAnimation);
		}

		public boolean isVisible() {
			return condition != null;
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			if (animation == this.onRemovedIconAnimation) {
				hide(false);
				rearrangeIconsLeftOf(this);
				updateIconState();
			}
		}

		@Override public void onAnimationRepeat(Animation animation) { }
		@Override public void onAnimationStart(Animation animation) { }
	}

	private void rearrangeIconsLeftOf(ActiveConditionIcon icon) {
		int i = currentConditionIcons.indexOf(icon);
		currentConditionIcons.remove(i);
		currentConditionIcons.add(icon);
		for(; i < currentConditionIcons.size(); ++i) {
			currentConditionIcons.get(i).image.setLayoutParams(getLayoutParamsForIconIndex(i));
		}
	}

	private ActiveConditionIcon getIconFor(ActorCondition condition, boolean immunity) {
		for (ActiveConditionIcon icon : currentConditionIcons) {
			if (icon.condition == condition && icon.immunity == immunity) return icon;
		}
		return null;
	}
	private ActiveConditionIcon getFirstFreeIcon() {
		for (ActiveConditionIcon icon : currentConditionIcons) {
			if (!icon.isVisible()) return icon;
		}
		return addNewActiveConditionIcon();
	}

	private RelativeLayout.LayoutParams getLayoutParamsForIconIndex(int index) {
		RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		if (target == world.model.player) layout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		else layout.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		if (index == 0) {
			layout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		} else {
			layout.addRule(RelativeLayout.LEFT_OF, currentConditionIcons.get(index-1).id);
		}
		return layout;
	}

	private ActiveConditionIcon addNewActiveConditionIcon() {
		int index = currentConditionIcons.size();

		ActiveConditionIcon icon = new ActiveConditionIcon(androidContext.get(), index+1);

		activeConditions.addView(icon.image, getLayoutParamsForIconIndex(index));

//		RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
//		layout.addRule(RelativeLayout.ALIGN_RIGHT, icon.id);
//		layout.addRule(RelativeLayout.ALIGN_BOTTOM, icon.id);
//		activeConditions.addView(icon.text, layout);

		currentConditionIcons.add(icon);

		return icon;
	}
}

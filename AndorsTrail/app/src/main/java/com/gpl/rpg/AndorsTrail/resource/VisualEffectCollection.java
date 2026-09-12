package com.gpl.rpg.AndorsTrail.resource;

import android.graphics.Color;

import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.util.ConstRange;

public final class VisualEffectCollection {

	public static enum VisualEffectID {
		redSplash
		,blueSwirl
		,greenSplash
		,miss
		,poleAttack
		,longswordAttack
		,axeAttack
		,lightBladeCut
		,lightBladeThrust
		,dualAxesAttack
		,broadswordAttack
		,whipAttack
		,groundShockwave
		,bluntImpact
		,rapierThrust
		,footSweep;;

		public static VisualEffectID fromString(String s, VisualEffectID default_) {
			if (s == null) return default_;
			return valueOf(s);
		}
	}

	private final VisualEffect[] effects = new VisualEffect[VisualEffectID.values().length];

	public void initialize(DynamicTileLoader loader) {
		effects[VisualEffectID.redSplash.ordinal()] = createEffect(loader, R.drawable.effect_blood4, new ConstRange(14, 0), 400, Color.RED);
		effects[VisualEffectID.blueSwirl.ordinal()] = createEffect(loader, R.drawable.effect_heal2, new ConstRange(16, 0), 400, Color.rgb(150, 150, 255));
		effects[VisualEffectID.greenSplash.ordinal()] = createEffect(loader, R.drawable.effect_poison1, new ConstRange(16, 0), 400, Color.GREEN);
		effects[VisualEffectID.miss.ordinal()] = createEffect(loader, R.drawable.effect_miss1, new ConstRange(16, 0), 400, Color.GRAY);
		effects[VisualEffectID.poleAttack.ordinal()] = createEffect(loader, R.drawable.pole_attack_ne_16f_96x96_v2, new ConstRange(16, 0), 533, Color.WHITE, 3, 3);
		effects[VisualEffectID.longswordAttack.ordinal()] = createEffect(loader, R.drawable.longsword_attack_ne_16f_96x96, new ConstRange(16, 0), 533, Color.WHITE, 3, 3);
		effects[VisualEffectID.axeAttack.ordinal()] = createEffect(loader, R.drawable.axe_attack_ne_16f_96x96_v4, new ConstRange(16, 0), 320, Color.WHITE, 3, 3);
		effects[VisualEffectID.lightBladeCut.ordinal()] = createEffect(loader, R.drawable.light_blade_cut_shared_16f_96x96_v2, new ConstRange(16, 0), 400, Color.WHITE, 3, 3);
		effects[VisualEffectID.lightBladeThrust.ordinal()] = createEffect(loader, R.drawable.light_blade_thrust_ne_shared_16f_96x96_v2, new ConstRange(16, 0), 400, Color.WHITE, 3, 3);
		effects[VisualEffectID.dualAxesAttack.ordinal()] = createEffect(loader, R.drawable.dual_axes_attack_ne_16f_96x96_v1, new ConstRange(16, 0),  267, Color.WHITE, 3, 3);
		effects[VisualEffectID.broadswordAttack.ordinal()] = createEffect(loader, R.drawable.broadsword_attack_ne_16f_96x96_v2, new ConstRange(16, 0), 640, Color.WHITE, 3, 3);
		effects[VisualEffectID.whipAttack.ordinal()] = createEffect(loader, R.drawable.whip_attack_ne_16f_96x96_v4, new ConstRange(16, 0), 400, Color.WHITE, 3, 3);
		effects[VisualEffectID.groundShockwave.ordinal()] = createEffect(loader, R.drawable.ground_shockwave_ellipse_16f_32x32_v1, new ConstRange(16, 0), 267, Color.WHITE);
		effects[VisualEffectID.bluntImpact.ordinal()] = createEffect(loader, R.drawable.blunt_impact_hollow_circle_16f_32x32_v3, new ConstRange(16, 0), 267, Color.WHITE);
		effects[VisualEffectID.rapierThrust.ordinal()] = createEffect(loader, R.drawable.rapier_thrust_ne_16f_96x96_v1, new ConstRange(16, 0), 267, Color.WHITE, 3, 3);
		effects[VisualEffectID.footSweep.ordinal()] = createEffect(loader, R.drawable.foot_sweep_16f_32x32_v2, new ConstRange(16, 0), 267, Color.WHITE);
	}

	public VisualEffect getVisualEffect(VisualEffectID effectID) {
		return effects[effectID.ordinal()];
	}

	private static VisualEffect createEffect(DynamicTileLoader loader, int drawableID, ConstRange frameRange, int duration, int textColor) {
		return createEffect(loader, drawableID, frameRange, duration, textColor, 1, 1);
	}

	private static VisualEffect createEffect(DynamicTileLoader loader, int drawableID, ConstRange frameRange, int duration, int textColor, int widthInTiles, int heightInTiles) {
		int[] frameIconIDs = new int[frameRange.max - frameRange.current];
		for(int i = 0; i < frameIconIDs.length; ++i) {
			frameIconIDs[i] = loader.prepareTileID(drawableID, frameRange.current + i);
		}
		return new VisualEffect(frameIconIDs, duration, textColor, widthInTiles, heightInTiles);
	}

	public static final class VisualEffect {
		public final int[] frameIconIDs;
		public final int duration; // milliseconds
		public final int textColor;
		//public final int fps = ModelContainer.attackAnimationFPS;
		//public final int millisecondPerFrame = 1000 / fps;
		//public final int totalFrames = duration / millisecondPerFrame;
		public final int fps;
		public final int millisecondPerFrame;
		public final int totalFrames;
		public final int lastFrame;
		public final int widthInTiles;
		public final int heightInTiles;

		public VisualEffect(int[] frameIconIDs, int duration, int textColor) {
			this(frameIconIDs, duration, textColor, 1, 1);
		}

		public VisualEffect(int[] frameIconIDs, int duration, int textColor, int widthInTiles, int heightInTiles) {
			this.frameIconIDs = frameIconIDs;
			this.duration = duration;
			this.textColor = textColor;
			this.widthInTiles = widthInTiles;
			this.heightInTiles = heightInTiles;
			totalFrames = frameIconIDs.length;
			lastFrame = totalFrames - 1;
			millisecondPerFrame = duration / totalFrames;
			fps = 1000 / millisecondPerFrame;
		}
	}
}

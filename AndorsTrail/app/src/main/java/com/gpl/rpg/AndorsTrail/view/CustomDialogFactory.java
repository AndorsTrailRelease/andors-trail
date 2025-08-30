package com.gpl.rpg.AndorsTrail.view;

import android.content.Context;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.util.ThemeHelper;

public class CustomDialogFactory {

	public static class CustomDialog extends android.app.Dialog {
		public CustomDialog(Context context) {
			super(context);
		}
		boolean verticalButtons = false;
	}

	public static CustomDialog createDialog(final Context context, String title, Drawable icon,
											String desc, View content, boolean hasButtons, boolean canDismiss) {
		return createDialog(context, title, icon, desc, content, hasButtons, canDismiss, false);
	}

	public static CustomDialog createDialog(final Context context, String title, Drawable icon,
											String desc, View content, boolean hasButtons) {
		return createDialog(context, title, icon, desc, content, hasButtons, true);
	}

	public static CustomDialog createDialog(final Context context, String title, Drawable icon,
											String desc, View content, boolean hasButtons,
											final boolean canDismiss, final boolean verticalButtons) {
		final CustomDialog dialog = new CustomDialog(new ContextThemeWrapper(context, ThemeHelper.getDialogTheme())) {
			@Override
			public boolean onTouchEvent(MotionEvent event) {
				Rect r = new Rect();
				this.getWindow().getDecorView().findViewById(R.id.dialog_hitrect).getHitRect(r);

				if (r.contains((int)event.getX(), (int)event.getY())) {
					return super.onTouchEvent(event);
				} else {
					if (canDismiss) {
						this.dismiss();
						return true;
					}
					return false;
				}
			}

			@Override
			public void onWindowFocusChanged(boolean hasFocus) {
				super.onWindowFocusChanged(hasFocus);
				TextView title = (TextView) this.getWindow().getDecorView().findViewById(R.id.dialog_title);
				if (title != null && title.getCompoundDrawables() != null && title.getCompoundDrawables()[0] != null) {
					if (title.getCompoundDrawables()[0] instanceof AnimationDrawable) {
						((AnimationDrawable)title.getCompoundDrawables()[0]).start();
					}
				}
			}
		};
		dialog.verticalButtons = verticalButtons;

		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.custom_dialog_title_icon);
		dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
		boolean fullscreen = ((AndorsTrailApplication) context.getApplicationContext()).getPreferences().fullscreen;
		AndorsTrailApplication.setFullscreenMode(fullscreen, dialog.getWindow());

		setTitle(dialog, title, icon);

		setDesc(dialog, desc);

		setContent(dialog, content);

		ViewGroup buttonsHolder = getButtonContainer(dialog);
		ViewGroup unusedButtonsHolder = getUnusedButtonContainer(dialog);

		unusedButtonsHolder.setVisibility(View.GONE);
		if (hasButtons) {
			buttonsHolder.setVisibility(View.VISIBLE);
		} else {
			buttonsHolder.setVisibility(View.GONE);
		}

		return dialog;
	}

	public static CustomDialog createErrorDialog(final Context context, String title, String description) {
		final CustomDialog d = createDialog(context,
				title,
				context.getResources().getDrawable(android.R.drawable.ic_dialog_alert),
				description,
				null,
				true);
		CustomDialogFactory.addDismissButton(d, android.R.string.ok);
		return d;
	}

	public static CustomDialog setTitle(final CustomDialog dialog, String title, Drawable icon) {
		TextView titleView = (TextView) dialog.findViewById(R.id.dialog_title);
		if (title != null || icon != null) {
			titleView.setText(title);
			titleView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
			titleView.setVisibility(View.VISIBLE);
		} else {
			titleView.setVisibility(View.GONE);
		}
		return dialog;
	}

	public static CustomDialog setDesc(final CustomDialog dialog, String desc) {
		TextView descView = (TextView) dialog.findViewById(R.id.dialog_description);
		ViewGroup descHolder = (ViewGroup) dialog.findViewById(R.id.dialog_description_container);
		if (desc != null) {
			descView.setText(desc);
			descHolder.setVisibility(View.VISIBLE);
			descView.setVisibility(View.VISIBLE);
		} else {
			descHolder.setVisibility(View.GONE);
		}
		return dialog;
	}

	public static CustomDialog setContent(final CustomDialog dialog, View content) {
		ViewGroup contentHolder = (ViewGroup) dialog.findViewById(R.id.dialog_content_container);
		if (content != null) {
			contentHolder.addView(content);
			contentHolder.setVisibility(View.VISIBLE);
		} else {
			contentHolder.setVisibility(View.GONE);
		}
		return dialog;
	}

	public static CustomDialog addButton(final CustomDialog dialog, String text, final OnClickListener listener) {
		return addButton(dialog, -1, text, listener);
	}
	public static CustomDialog addButton(final CustomDialog dialog, int textId, final OnClickListener listener) {
		return addButton(dialog, textId, null, listener);
	}
	public static CustomDialog addButton(final CustomDialog dialog, int textId, String text, final OnClickListener listener) {
		Button template = getButtonTemplate(dialog);
		LayoutParams params = template.getLayoutParams();
		ViewGroup buttonsHolder = getButtonContainer(dialog);

		Button b = new Button(dialog.getContext());
		b.setLayoutParams(params);
		//Old android versions need this "reminder"
		b.setBackgroundDrawable(ThemeHelper.getThemeDrawable(dialog.getContext(), R.attr.ui_theme_textbutton_drawable));
		b.setTextColor(ThemeHelper.getThemeColor(dialog.getContext(), R.attr.ui_theme_dialogue_light_color));

		if(textId != -1) {
			b.setText(textId);
		} else {
			b.setText(text);
		}

		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				listener.onClick(v);
				dialog.dismiss();
			}
		});

		buttonsHolder.addView(b, params);
		return dialog;
	}

	public static CustomDialog addDismissButton(final CustomDialog dialog, int textId) {
		return CustomDialogFactory.addButton(dialog, textId, new OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
	}
	public static CustomDialog addCancelButton(final CustomDialog dialog, int textId) {
		return CustomDialogFactory.addButton(dialog, textId, new OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.cancel();
			}
		});
	}

	public static CustomDialog setDismissListener(CustomDialog dialog, OnDismissListener listener) {
		dialog.setOnDismissListener(listener);

		return dialog;
	}

	public static CustomDialog setCancelListener(CustomDialog dialog, OnCancelListener listener) {
		dialog.setOnCancelListener(listener);
		return dialog;
	}

	public static void show(CustomDialog dialog) {

		dialog.findViewById(R.id.dialog_template_button).setVisibility(View.GONE);
		dialog.findViewById(R.id.dialog_template_button_vertical).setVisibility(View.GONE);
		dialog.show();

	}


	private static ViewGroup getUnusedButtonContainer(CustomDialog dialog) {
		if (dialog.verticalButtons)
			return (ViewGroup) dialog.findViewById(R.id.dialog_button_container);
		else
			return (ViewGroup) dialog.findViewById(R.id.dialog_button_container_vertical);
	}

	private static ViewGroup getButtonContainer(CustomDialog dialog) {
		if (dialog.verticalButtons)
			return (ViewGroup) dialog.findViewById(R.id.dialog_button_container_vertical);
		else
			return (ViewGroup) dialog.findViewById(R.id.dialog_button_container);
	}

	private static Button getButtonTemplate(CustomDialog dialog) {
		if (dialog.verticalButtons)
			return (Button) dialog.findViewById(R.id.dialog_template_button_vertical);
		else
			return (Button) dialog.findViewById(R.id.dialog_template_button);
	}

}

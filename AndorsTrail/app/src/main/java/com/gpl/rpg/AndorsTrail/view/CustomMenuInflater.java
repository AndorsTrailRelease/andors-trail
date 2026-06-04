package com.gpl.rpg.AndorsTrail.view;

import java.util.ArrayList;
import java.util.List;

import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.view.CustomDialogFactory.CustomDialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.ActionProvider;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class CustomMenuInflater {
	
	public interface MenuItemSelectedListener {
		public void onMenuItemSelected(MenuItem item, Object data);
	}
	
	public static Dialog showMenuInDialog(Activity activity, Menu menu, Drawable icon, String title, Object data, MenuItemSelectedListener listener ) {
		Dialog d = getMenuDialog(activity, menu, icon, title, data, listener);
		d.show();
		return d;
	}
	
	public static Dialog getMenuDialog(Activity activity, Menu menu, Drawable icon, String title, Object data, MenuItemSelectedListener listener ) {
		final CustomDialog dialog = CustomDialogFactory.createDialog(activity, title, icon, null, null, false);
		ViewGroup vg = getMenuView(activity, menu, icon, title, data, dialog, listener);
		vg.setLayoutParams(getItemLayoutParams());
		CustomDialogFactory.setContent(dialog, vg);
		vg.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		vg.post(() -> focusFirstMenuItem(vg));
		return dialog;
	}

	public static ViewGroup getMenuView(Activity activity, Menu menu, Drawable icon, String title, Object data, Dialog dialog, MenuItemSelectedListener listener ) {
		ViewGroup scroll = (ViewGroup) activity.getLayoutInflater().inflate(R.layout.custom_menu_layout, null);
		ViewGroup vg = (ViewGroup) scroll.findViewById(R.id.custom_menu_items_wrapper);
		MenuItem item;
		boolean first =true;
		for (int i = 0; i < menu.size(); i++) {
			item = menu.getItem(i);
			if (item.isVisible()) {
				if (first) {
					first = false;
				} else {
					addMenuItemSeparator(activity, vg);
				}
				if (item.hasSubMenu()) {
					addSubMenuItemView(activity, vg, icon, title, item, data, dialog, listener);
				} else {
					addMenuItemView(activity, vg, item, data, dialog, listener);
				}
			}
		}
		return scroll;
	}
	
	private static void addMenuItemView(Activity activity, ViewGroup vg, final MenuItem item, final Object data, final Dialog dialog, final MenuItemSelectedListener listener) {
		TextView tv = (TextView) activity.getLayoutInflater().inflate(R.layout.custom_menu_item_layout, null);
		tv.setText(item.getTitle());
		tv.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				listener.onMenuItemSelected(item, data);
				dialog.dismiss();
			}
		});
		tv.setLayoutParams(getItemLayoutParams());
		vg.addView(tv);
	}
	

	
	private static void addSubMenuItemView(final Activity activity, ViewGroup vg, final Drawable icon, final String title, final MenuItem item, final Object data, final Dialog dialog, final MenuItemSelectedListener listener) {
		TextView tv = (TextView) activity.getLayoutInflater().inflate(R.layout.custom_menu_submenu_layout, null);
		tv.setText(item.getTitle());
		tv.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showMenuInDialog(activity, item.getSubMenu(), icon, title, data, listener);
				dialog.dismiss();
			}
		});
		tv.setLayoutParams(getItemLayoutParams());
		vg.addView(tv);
	}
	
	
	private static void addMenuItemSeparator(final Activity activity, ViewGroup vg) {
		View v = activity.getLayoutInflater().inflate(R.layout.custom_menu_item_separator_layout, null);
		v.setLayoutParams(getItemLayoutParams());
		vg.addView(v);
	}
	
	private static LayoutParams getItemLayoutParams() {
		return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	}

	private static void focusFirstMenuItem(ViewGroup root) {
		if ((root.getChildCount() != 1) || !(root.getChildAt(0) instanceof ViewGroup)) return;

		// The menu layout is a wrapper with a single child, which is the actual container of the menu items.
		ViewGroup group = (ViewGroup) root.getChildAt(0);

		for (int i = 0; i < group.getChildCount(); i++) {
			View child = group.getChildAt(i);

			if (child.isFocusable() && child.isShown() && child.isEnabled()) {
				child.requestFocus();
				return;
			}
		}
	}

	public static Menu newMenuInstance(Context context) {
		return new DummyMenu(context);
	}
	
	private static class DummyMenu implements Menu {
		
		Context context;
		List<MenuItem> items = new ArrayList<MenuItem>();
		
		public DummyMenu(Context context) {
			this.context = context;
		}

		@Override
		public MenuItem add(CharSequence title) {
			MenuItem item = new DummyMenuItem(context, title, -1, null);
			items.add(item);
			return item;
		}

		@Override
		public MenuItem add(int titleRes) {
			return add(context.getString(titleRes));
		}

		@Override
		public MenuItem add(int groupId, int itemId, int order, CharSequence title) {
			MenuItem item = new DummyMenuItem(context, title, itemId, null);
			items.add(item);
			return item;
		}

		@Override
		public MenuItem add(int groupId, int itemId, int order, int titleRes) {
			return add(groupId, itemId, order, context.getString(titleRes));
		}

		@Override
		public int addIntentOptions(int groupId, int itemId, int order, ComponentName caller, Intent[] specifics,
				Intent intent, int flags, MenuItem[] outSpecificItems) {
			return 0;
		}

		@Override
		public SubMenu addSubMenu(CharSequence title) {
			DummySubMenu sm = new DummySubMenu(context, title);
			MenuItem item = new DummyMenuItem(context, title, 0, sm);
			items.add(item);
			return sm;
		}

		@Override
		public SubMenu addSubMenu(int titleRes) {
			return addSubMenu(context.getString(titleRes));
		}

		@Override
		public SubMenu addSubMenu(int groupId, int itemId, int order, CharSequence title) {
			DummySubMenu sm = new DummySubMenu(context, title);
			MenuItem item = new DummyMenuItem(context, title, itemId, sm);
			items.add(item);
			return sm;
		}

		@Override
		public SubMenu addSubMenu(int groupId, int itemId, int order, int titleRes) {
			return addSubMenu(groupId, itemId, order, context.getString(titleRes));
		}

		@Override
		public void clear() {
			items.clear();
		}

		@Override
		public void close() {}

		@Override
		public MenuItem findItem(int id) {
			MenuItem found = null;
			for (MenuItem item : items) {
				if (item.getItemId() == id) return item;
				if (item.hasSubMenu()) {
					found = item.getSubMenu().findItem(id);
					if (found != null) return found;
				}
			}
			return null;
		}

		@Override
		public MenuItem getItem(int index) {
			return items.get(index);
		}

		@Override
		public boolean hasVisibleItems() {
			for (MenuItem item : items) {
				if (item.isVisible()) return true;
			}
			return false;
		}

		@Override
		public boolean isShortcutKey(int keyCode, KeyEvent event) {
			return false;
		}

		@Override
		public boolean performIdentifierAction(int id, int flags) {
			return false;
		}

		@Override
		public boolean performShortcut(int keyCode, KeyEvent event, int flags) {
			return false;
		}

		@Override
		public void removeGroup(int groupId) {
		}

		@Override
		// This doesn't actually do anything???
		public void removeItem(int id) {
			MenuItem found = null;
			for (MenuItem item : items) {
				if (item.getItemId() == id) found=item;
				if (item.hasSubMenu()) {
					item.getSubMenu().removeItem(id);
				}
			}
		}

		@Override
		public void setGroupCheckable(int group, boolean checkable, boolean exclusive) {
		}

		@Override
		public void setGroupEnabled(int group, boolean enabled) {
		}

		@Override
		public void setGroupVisible(int group, boolean visible) {
		}

		@Override
		public void setQwertyMode(boolean isQwerty) {
		}

		@Override
		public int size() {
			return items.size();
		}
		
	}
	
	private static class DummyMenuItem implements MenuItem {

		Context context;
		CharSequence title;
		int id;
		DummySubMenu subMenu;
		boolean visible = true;
		
		public DummyMenuItem(Context context, CharSequence title, int id, DummySubMenu subMenu) {
			this.context = context;
			this.title = title;
			this.id = id;
			this.subMenu = subMenu;
			if (subMenu != null) subMenu.setItem(this);
		}
		
		@Override
		public boolean collapseActionView() {
			return false;
		}

		@Override
		public boolean expandActionView() {
			return false;
		}

		@Override
		public ActionProvider getActionProvider() {
			return null;
		}

		@Override
		public View getActionView() {
			return null;
		}

		@Override
		public char getAlphabeticShortcut() {
			return 0;
		}

		@Override
		public int getGroupId() {
			return 0;
		}

		@Override
		public Drawable getIcon() {
			return null;
		}

		@Override
		public Intent getIntent() {
			return null;
		}

		@Override
		public int getItemId() {
			return id;
		}

		@Override
		public ContextMenuInfo getMenuInfo() {
			return null;
		}

		@Override
		public char getNumericShortcut() {
			return 0;
		}

		@Override
		public int getOrder() {
			return 0;
		}

		@Override
		public SubMenu getSubMenu() {
			return subMenu;
		}

		@Override
		public CharSequence getTitle() {
			return title;
		}

		@Override
		public CharSequence getTitleCondensed() {
			return null;
		}

		@Override
		public boolean hasSubMenu() {
			return subMenu != null;
		}

		@Override
		public boolean isActionViewExpanded() {
			return false;
		}

		@Override
		public boolean isCheckable() {
			return false;
		}

		@Override
		public boolean isChecked() {
			return false;
		}

		@Override
		public boolean isEnabled() {
			return true;
		}

		@Override
		public boolean isVisible() {
			return visible;
		}

		@Override
		public MenuItem setActionProvider(ActionProvider actionProvider) {
			return this;
		}

		@Override
		public MenuItem setActionView(View view) {
			return this;
		}

		@Override
		public MenuItem setActionView(int resId) {
			return this;
		}

		@Override
		public MenuItem setAlphabeticShortcut(char alphaChar) {
			return this;
		}

		@Override
		public MenuItem setCheckable(boolean checkable) {
			return this;
		}

		@Override
		public MenuItem setChecked(boolean checked) {
			return this;
		}

		@Override
		public MenuItem setEnabled(boolean enabled) {
			return this;
		}

		@Override
		public MenuItem setIcon(Drawable icon) {
			return this;
		}

		@Override
		public MenuItem setIcon(int iconRes) {
			return this;
		}

		@Override
		public MenuItem setIntent(Intent intent) {
			return this;
		}

		@Override
		public MenuItem setNumericShortcut(char numericChar) {
			return this;
		}

		@Override
		public MenuItem setOnActionExpandListener(OnActionExpandListener listener) {
			return this;
		}

		@Override
		public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener) {
			return this;
		}

		@Override
		public MenuItem setShortcut(char numericChar, char alphaChar) {
			return this;
		}

		@Override
		public void setShowAsAction(int actionEnum) {
		}

		@Override
		public MenuItem setShowAsActionFlags(int actionEnum) {
			return this;
		}

		@Override
		public MenuItem setTitle(CharSequence title) {
			this.title = title;
			return this;
		}

		@Override
		public MenuItem setTitle(int title) {
			setTitle(context.getString(title));
			return this;
		}

		@Override
		public MenuItem setTitleCondensed(CharSequence title) {
			return this;
		}

		@Override
		public MenuItem setVisible(boolean visible) {
			this.visible = visible;
			return this;
		}
		
	}
	
	private static class DummySubMenu extends DummyMenu implements SubMenu {

		CharSequence title;
		MenuItem parent;
		
		public DummySubMenu(Context context, CharSequence title) {
			super(context);
			this.title = title;
		}
		
		@Override
		public void clearHeader() {
		}
		
		public void setItem(MenuItem item) {
			this.parent = item;
		}

		@Override
		public MenuItem getItem() {
			return parent;
		}

		@Override
		public SubMenu setHeaderIcon(int iconRes) {
			return this;
		}

		@Override
		public SubMenu setHeaderIcon(Drawable icon) {
			return this;
		}

		@Override
		public SubMenu setHeaderTitle(int titleRes) {
			return this;
		}

		@Override
		public SubMenu setHeaderTitle(CharSequence title) {
			return this;
		}

		@Override
		public SubMenu setHeaderView(View view) {
			return this;
		}

		@Override
		public SubMenu setIcon(int iconRes) {
			return this;
		}

		@Override
		public SubMenu setIcon(Drawable icon) {
			return this;
		}
		
		
	}
}

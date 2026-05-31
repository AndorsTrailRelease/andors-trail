package com.gpl.rpg.AndorsTrail;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.Constants;
import com.gpl.rpg.AndorsTrail.util.AndroidStorage;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Insets;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;


public final class AndorsTrailApplication extends Application {
	public static final String CURRENT_VERSION_DISPLAY = BuildConfig.VERSION_NAME;
	public static final boolean IS_DEV_VERSION = CURRENT_VERSION_DISPLAY.endsWith("dev");
	public static final boolean IS_BETA_VERSION = CURRENT_VERSION_DISPLAY.endsWith("beta");
	public static final boolean IS_RELEASE_VERSION = !CURRENT_VERSION_DISPLAY.matches(".*[a-zA-Z].*");

	public static final boolean DEVELOPMENT_DEBUGRESOURCES = false;
	public static final boolean DEVELOPMENT_FORCE_STARTNEWGAME = false;
	public static final boolean DEVELOPMENT_FORCE_CONTINUEGAME = false;
    public static final boolean DEVELOPMENT_DEBUGBUTTONS = IS_DEV_VERSION;
	public static final boolean DEVELOPMENT_FASTSPEED = false;
	public static final boolean DEVELOPMENT_VALIDATEDATA = ( IS_BETA_VERSION || IS_DEV_VERSION );
	public static final boolean DEVELOPMENT_DEBUGMESSAGES = IS_DEV_VERSION;
	public static final boolean DEVELOPMENT_INCOMPATIBLE_SAVEGAMES = DEVELOPMENT_DEBUGRESOURCES || DEVELOPMENT_DEBUGBUTTONS || DEVELOPMENT_FASTSPEED || !IS_RELEASE_VERSION;
	public static final int DEVELOPMENT_INCOMPATIBLE_SAVEGAME_VERSION = 999;
	public static final int CURRENT_VERSION = DEVELOPMENT_INCOMPATIBLE_SAVEGAMES ? DEVELOPMENT_INCOMPATIBLE_SAVEGAME_VERSION : BuildConfig.VERSION_CODE;

	private final AndorsTrailPreferences preferences = new AndorsTrailPreferences();
	private WorldContext world = new WorldContext();
	private ControllerContext controllers = new ControllerContext(this, world);
	private WorldSetup setup = new WorldSetup(world, controllers, this);
	public WorldContext getWorld() { return world; }
	public WorldSetup getWorldSetup() { return setup; }
	public AndorsTrailPreferences getPreferences() { return preferences; }
	public ControllerContext getControllerContext() { return controllers; }

	public static AndorsTrailApplication getApplicationFromActivity(Activity activity) {
		return ((AndorsTrailApplication) activity.getApplication());
	}
	public static AndorsTrailApplication getApplicationFromActivityContext(Context context) {
		return getApplicationFromActivity(getActivityFromActivityContext(context));
	}
	private static Activity getActivityFromActivityContext(Context context) {
		return (Activity) context;
	}

	public boolean isInitialized() { return world.model != null; }

	public void setWindowParameters(Activity activity) {
		activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
	}

	public void setFullscreenMode(Activity activity) {
		setFullscreenMode(preferences.fullscreen, activity.getWindow());
	}
	public static void setFullscreenMode(boolean fullscreen, Window window) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

			final WindowInsetsController insetsController = window.getInsetsController();
			if (insetsController != null) {
				insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
				int insetType = WindowInsets.Type.statusBars();
				if (fullscreen) {
					insetsController.hide(insetType);
				} else {
					insetsController.show(insetType);
				}
			}
		} else {
			if (fullscreen) {
				window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

			} else {
				window.setFlags(0, WindowManager.LayoutParams.FLAG_FULLSCREEN);
			}
		}
	}

	@RequiresApi(Build.VERSION_CODES.R)
	public int getUsableTouchAreaInsetMask(){
		int i = 0;
		i |= WindowInsets.Type.displayCutout();
		i |= WindowInsets.Type.navigationBars();
		if (!preferences.fullscreen) {
			i |= WindowInsets.Type.statusBars();
		}
		return i;
	}

	//Get default locale at startup, as somehow it seems that changing the app's 
	//configured locale impacts the value returned by Locale.getDefault() nowadays.
	private final Locale defaultLocale = Locale.getDefault();

	private Pair<String, Locale> lastLocale = null;

	public boolean setLocale(Activity context) {
		Resources res = context.getResources();
		Configuration conf = res.getConfiguration();

		Locale targetLocale;

		if (lastLocale != null && lastLocale.first == preferences.language) {
			targetLocale = lastLocale.second;
		} else {
			targetLocale = localeForLanguageTag(preferences.language);
			lastLocale = new Pair<String, Locale>(preferences.language, targetLocale);
		}

		if (targetLocale.equals(conf.locale)) {
			return false;
		}

		conf.locale = targetLocale;
		res.updateConfiguration(conf, res.getDisplayMetrics());
		this.getResources().updateConfiguration(conf, res.getDisplayMetrics());
		
		return true;
	}

	// Supports language or language_COUNTRY in short form e.g. "en" or "en_US"
	private Locale localeForLanguageTag(String languageTag) {
		Locale locale = null;
		if (languageTag != null && !languageTag.equalsIgnoreCase("default")) {
			final int pos = languageTag.indexOf('-');
			if (pos == -1) {
				locale = new Locale(languageTag);
			}
			else locale = new Locale(languageTag.substring(0, pos), languageTag.substring(pos+1));
		}
		if (locale == null) {
			locale = defaultLocale;
		}
		return locale;
	}

	/**
	 * Logging to text file system as found on https://stackoverflow.com/questions/19565685/saving-logcat-to-a-text-file-in-android-device
	 */
	
	public void onCreate() {
		super.onCreate();

		if ( DEVELOPMENT_DEBUGMESSAGES && isExternalStorageWritable() ) {
			File appDirectory = AndroidStorage.getStorageDirectory(getApplicationContext(), Constants.FILENAME_SAVEGAME_DIRECTORY);
			File logDirectory = new File( appDirectory, "log" );
			File logFile = new File( logDirectory, "logcat" + System.currentTimeMillis() + ".txt" );

			// create app folder
			if ( !appDirectory.exists() ) {
				appDirectory.mkdir();
			}

			// create log folder
			if ( !logDirectory.exists() ) {
				logDirectory.mkdir();
			}

			// clear the previous logcat and then write the new one to the file
			try {
				Process process = Runtime.getRuntime().exec("logcat -c");
				process = Runtime.getRuntime().exec("logcat -f " + logFile+" *:W");
			} catch ( IOException e ) {
				e.printStackTrace();
			}

		}

		// If we're in dev mode, add focus listener to each activity
		if (BuildConfig.DEBUG) {
			registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
				@Override
				public void onActivityStarted(final Activity activity) {
					// Attach a listener to the view tree of every activity that starts
					activity.getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener(new android.view.ViewTreeObserver.OnGlobalFocusChangeListener() {
						@Override
						public void onGlobalFocusChanged(View oldFocus, View newFocus) {
							if (newFocus != null) {
								String idName = "no-id";
								try {
									// Attempt to get the human-readable XML ID
									idName = activity.getResources().getResourceEntryName(newFocus.getId());
								} catch (Exception ignored) {
									// Some system views or root containers don't have IDs
								}
								Log.d("GlobalFocus", "Activity: " + activity.getClass().getSimpleName()
										+ " | Focused: " + newFocus.getClass().getSimpleName() + " [ID: " + idName + "]");
							} else {
								Log.d("GlobalFocus", "Activity: " + activity.getClass().getSimpleName() + " | Focus lost");
							}
						}
					});
				}

				// Required boilerplate for the interface (these can remain empty)
				@Override public void onActivityCreated(android.app.Activity activity, android.os.Bundle savedInstanceState) {}
				@Override public void onActivityResumed(android.app.Activity activity) {}
				@Override public void onActivityPaused(android.app.Activity activity) {}
				@Override public void onActivityStopped(android.app.Activity activity) {}
				@Override public void onActivitySaveInstanceState(android.app.Activity activity, android.os.Bundle outState) {}
				@Override public void onActivityDestroyed(android.app.Activity activity) {}
			});
		}

	}

	/* Checks if external storage is available for read and write */
	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if ( Environment.MEDIA_MOUNTED.equals( state ) ) {
			return true;
		}
		return false;
	}

	/* Checks if external storage is available to at least read */
	public boolean isExternalStorageReadable() {
		String state = Environment.getExternalStorageState();
		if ( Environment.MEDIA_MOUNTED.equals( state ) ||
				Environment.MEDIA_MOUNTED_READ_ONLY.equals( state ) ) {
			return true;
		}
		return false;
	}
	public void discardWorld() {
		world = new WorldContext();
		controllers = new ControllerContext(this, world);
		setup = new WorldSetup(world, controllers, getApplicationContext());
	}

	public void setUsablePadding(View root) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			root.setOnApplyWindowInsetsListener((v, insets) -> {
				Insets bars = insets.getInsets(getUsableTouchAreaInsetMask());
				int left = Math.max(bars.left, v.getPaddingLeft());
				int top = Math.max(bars.top, v.getPaddingTop());
				int right = Math.max(bars.right, v.getPaddingRight());
				int bottom = Math.max(bars.bottom, v.getPaddingBottom());
				v.setPadding(left, top, right, bottom);
				return WindowInsets.CONSUMED;
			});
		}
	}

	// Returns true if we're on an Android TV platform (assume no touchscreen, landscape, etc.)
	private Boolean is_atv = null;
	public boolean isAndroidTV() {
		Context context = getApplicationContext();
		if (is_atv == null) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				is_atv = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
			} else {
				is_atv = false;
			}
		}
		return is_atv;
	}


}

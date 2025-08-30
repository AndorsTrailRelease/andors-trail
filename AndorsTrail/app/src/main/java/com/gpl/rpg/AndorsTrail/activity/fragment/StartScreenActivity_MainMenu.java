package com.gpl.rpg.AndorsTrail.activity.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.AndorsTrailPreferences;
import com.gpl.rpg.AndorsTrail.Dialogs;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.WorldSetup;
import com.gpl.rpg.AndorsTrail.activity.AboutActivity;
import com.gpl.rpg.AndorsTrail.activity.LoadingActivity;
import com.gpl.rpg.AndorsTrail.activity.Preferences;
import com.gpl.rpg.AndorsTrail.controller.Constants;
import com.gpl.rpg.AndorsTrail.resource.tiles.TileManager;
import com.gpl.rpg.AndorsTrail.savegames.Savegames;
import com.gpl.rpg.AndorsTrail.savegames.Savegames.FileHeader;
import com.gpl.rpg.AndorsTrail.util.AndroidStorage;
import com.gpl.rpg.AndorsTrail.util.L;
import com.gpl.rpg.AndorsTrail.util.ThemeHelper;
import com.gpl.rpg.AndorsTrail.view.CustomDialogFactory;
import com.gpl.rpg.AndorsTrail.view.CustomDialogFactory.CustomDialog;

public class StartScreenActivity_MainMenu extends Fragment {

    private static final int INTENTREQUEST_PREFERENCES = 7;
    public static final int INTENTREQUEST_LOADGAME = 9;

    private boolean hasExistingGame = false;
    private Button startscreen_continue;
    private Button startscreen_newgame;
    private Button startscreen_load;
    private ViewGroup save_preview_holder;
    private ImageView save_preview_hero_icon;
    private TextView save_preview_hero_desc;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        updatePreferences(false);
        super.onCreateView(inflater, container, savedInstanceState);

		if (container != null) {
			container.removeAllViews();
		}
		
		View root = inflater.inflate(R.layout.startscreen_mainmenu, container, false);

		save_preview_holder = (ViewGroup) root.findViewById(R.id.save_preview_holder);
		save_preview_hero_icon = (ImageView) root.findViewById(R.id.save_preview_hero_icon);
		save_preview_hero_desc = (TextView) root.findViewById(R.id.save_preview_hero_desc);


		startscreen_continue = (Button) root.findViewById(R.id.startscreen_continue);
		startscreen_continue.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				continueGame(false, Savegames.SLOT_QUICKSAVE, null);
			}
		});

		startscreen_newgame = (Button) root.findViewById(R.id.startscreen_newgame);
		startscreen_newgame.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (hasExistingGame) {
					comfirmNewGame();
				} else {
					createNewGame();
				}
			}
		});

		Button b = (Button) root.findViewById(R.id.startscreen_about);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				startActivity(new Intent(getActivity(), AboutActivity.class));
			}
		});

		b = (Button) root.findViewById(R.id.startscreen_preferences);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(getActivity(), Preferences.class);
				StartScreenActivity_MainMenu.this.startActivityForResult(intent, INTENTREQUEST_PREFERENCES);
			}
		});

		startscreen_load = (Button) root.findViewById(R.id.startscreen_load);
		startscreen_load.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(getActivity());
				if (hasExistingGame && app != null && app.getWorld() != null && app.getWorld().model != null
						&& app.getWorld().model.statistics != null && !app.getWorld().model.statistics.hasUnlimitedSaves()) {
					final CustomDialog d = CustomDialogFactory.createDialog(getActivity(),
							getString(R.string.startscreen_load_game),
							getResources().getDrawable(android.R.drawable.ic_delete),
							getString(R.string.startscreen_load_game_confirm),
							null,
							true);
					CustomDialogFactory.addButton(d, android.R.string.ok, new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Dialogs.showLoad(StartScreenActivity_MainMenu.this);
						}
					});
					CustomDialogFactory.addDismissButton(d, android.R.string.cancel);
					CustomDialogFactory.show(d);

				} else {
					Dialogs.showLoad(StartScreenActivity_MainMenu.this);
				}
			}
		});


		if (AndorsTrailApplication.DEVELOPMENT_FORCE_STARTNEWGAME) {
			if (AndorsTrailApplication.DEVELOPMENT_DEBUGRESOURCES) {
				continueGame(true, 0, "Debug player");
			} else {
				continueGame(true, 0, "Player");
			}
		} else if (AndorsTrailApplication.DEVELOPMENT_FORCE_CONTINUEGAME) {
			continueGame(false, Savegames.SLOT_QUICKSAVE, null);
		}

		// if it is a new version we first fire a welcome screen in onResume
		// and afterwards check the permissions
		if (!isNewVersion()) {
			checkAndRequestPermissions(getActivity());
			migrateDataOnDemand(getActivity());
		}

		return root;
	}

	@Override
	public void onResume() {
		super.onResume();

		String playerName;
		String displayInfo = null;
		int iconID = TileManager.CHAR_HERO;
		boolean isDead = false;

		FileHeader header = Savegames.quickload(getActivity(), Savegames.SLOT_QUICKSAVE);
		if (header != null && header.playerName != null) {
			playerName = header.playerName;
			displayInfo = header.displayInfo;
			iconID = header.iconID;
			isDead = header.isDead;
		} else {
			// Before fileversion 14 (v0.6.7), quicksave was stored in Shared preferences
			SharedPreferences p = getActivity().getSharedPreferences("quicksave", Activity.MODE_PRIVATE);
			playerName = p.getString("playername", null);
			if (playerName != null) {
				displayInfo = "level " + p.getInt("level", -1);
			}
		}
		hasExistingGame = (playerName != null);
		setButtonState(playerName, displayInfo, iconID, isDead);

		if (isNewVersion()) {
			Dialogs.showNewVersion(getActivity(), new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface arg0) {
					setCurrentVersionForVersionCheck();
					checkAndRequestPermissions(getActivity());
					migrateDataOnDemand(getActivity());
				}
			});
		}

	}

	public void migrateDataOnDemand(final Activity activity) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			if (activity.getApplicationContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
				if (AndroidStorage.shouldMigrateToInternalStorage(activity.getApplicationContext())) {
					final CustomDialog d = CustomDialogFactory.createDialog(activity,
							getString(R.string.startscreen_migration_title),
							activity.getResources().getDrawable(android.R.drawable.ic_dialog_alert),
							getString(R.string.startscreen_migration_text),
							null,
							true);
					CustomDialogFactory.addDismissButton(d, android.R.string.ok);
					CustomDialogFactory.show(d);
					if (!AndroidStorage.migrateToInternalStorage(activity.getApplicationContext())) {
						final CustomDialog errorDlg = CustomDialogFactory.createDialog(activity,
								getString(R.string.startscreen_migration_title),
								activity.getResources().getDrawable(android.R.drawable.ic_dialog_alert),
								getString(R.string.startscreen_migration_failure),
								null,
								true);
						CustomDialogFactory.addDismissButton(errorDlg, android.R.string.ok);
						d.cancel();
						CustomDialogFactory.show(errorDlg);
					}
				} else {
					L.log("INFO: No external files or destination folder ist not empty. No data migration.");
				}
			} else {
				L.log("INFO: No read permission on external folder. No data migration.");
			}
		}
	}

	private static final int READ_EXTERNAL_STORAGE_REQUEST=1;
	private static final int WRITE_EXTERNAL_STORAGE_REQUEST=2;

	public static void checkAndRequestPermissions(final Activity activity) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
			if (activity.getApplicationContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				activity.requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_REQUEST);
			}
			if (activity.getApplicationContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				activity.requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_REQUEST);
			}
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		listener = (OnNewGameRequestedListener) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		listener = null;
	}

	private void setButtonState(final String playerName, final String displayInfo, int iconID, boolean isDead) {
		startscreen_continue.setEnabled(hasExistingGame && !isDead);
		startscreen_newgame.setEnabled(true);
		if (hasExistingGame) {
			TileManager tm = AndorsTrailApplication.getApplicationFromActivity(getActivity()).getWorld().tileManager;
			tm.setImageViewTileForPlayer(getResources(), save_preview_hero_icon, iconID);
			save_preview_hero_desc.setText((isDead ? getString(R.string.rip_startscreen) : "") + playerName + ", " + displayInfo);
			save_preview_holder.setVisibility(View.VISIBLE);
		} else {
			save_preview_holder.setVisibility(View.GONE);
		}
	}

	private void continueGame(boolean createNewCharacter, int loadFromSlot, String name) {
		final WorldSetup setup = AndorsTrailApplication.getApplicationFromActivity(getActivity()).getWorldSetup();
		setup.createNewCharacter = createNewCharacter;
		setup.loadFromSlot = loadFromSlot;
		setup.newHeroName = name;
		startActivity(new Intent(getActivity(), LoadingActivity.class));
	}

	private void comfirmNewGame() {
//		new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AndorsTrailStyle_Dialog))
//		.setTitle(R.string.startscreen_newgame)
//		.setMessage(R.string.startscreen_newgame_confirm)
//		.setIcon(android.R.drawable.ic_delete)
//		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//				//continueGame(true);
////				hasExistingGame = false;
////				setButtonState(null, null, 0);
//				createNewGame();
//			}
//		})
//		.setNegativeButton(android.R.string.cancel, null)
//		.create().show();
//		
//		
		final CustomDialog d = CustomDialogFactory.createDialog(getActivity(),
				getString(R.string.startscreen_newgame),
				getResources().getDrawable(android.R.drawable.ic_delete),
				getResources().getString(R.string.startscreen_newgame_confirm),
				null,
				true);
		CustomDialogFactory.addButton(d, android.R.string.ok, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				createNewGame();
			}
		});
		CustomDialogFactory.addDismissButton(d, android.R.string.cancel);

		CustomDialogFactory.show(d);

	}

	private static final String versionCheck = "lastversion";
	private boolean isNewVersion() {
		SharedPreferences s = getActivity().getSharedPreferences(Constants.PREFERENCE_MODEL_LASTRUNVERSION, Activity.MODE_PRIVATE);
		int lastversion = s.getInt(versionCheck, 0);
		if (lastversion >= AndorsTrailApplication.CURRENT_VERSION) return false;
		return true;
	}

	private void setCurrentVersionForVersionCheck() {
		SharedPreferences s = getActivity().getSharedPreferences(Constants.PREFERENCE_MODEL_LASTRUNVERSION, Activity.MODE_PRIVATE);
		Editor e = s.edit();
		e.putInt(versionCheck, AndorsTrailApplication.CURRENT_VERSION);
		e.commit();
	}


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case INTENTREQUEST_LOADGAME:
				boolean unsuccessful = resultCode != Activity.RESULT_OK;
				if(data == null) break;

                final boolean wasImportOrExport = data.getBooleanExtra("import_export", false);
                if (wasImportOrExport) {
					String message = getImportExportMessage(!unsuccessful, data);
					Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
					break;
				}
				if (unsuccessful) break;
                final int slot = data.getIntExtra("slot", 1);
                continueGame(false, slot, null);
                break;
            case INTENTREQUEST_PREFERENCES:
                updatePreferences(true);
                break;
        }
    }

	private String getImportExportMessage(boolean successful, Intent data) {
		String message = "";
		boolean isImportWorldmap = data.getBooleanExtra("import_worldmap", false);
		boolean isImportSaves = data.getBooleanExtra("import_savegames", false);
		boolean isExport = data.getBooleanExtra("export", false);

		if(isImportWorldmap) {
			message = getString(successful ? R.string.loadsave_import_worldmap_successfull : R.string.loadsave_import_worldmap_unsuccessfull);
		} else if(isImportSaves) {
			message = getString(successful ? R.string.loadsave_import_save_successfull : R.string.loadsave_import_save_unsuccessfull);
		} else if(isExport) {
			message = getString(successful ? R.string.loadsave_export_successfull : R.string.loadsave_export_unsuccessfull);
		}

		return message;
	}

	private void updatePreferences(boolean alreadyStartedLoadingResources) {
        AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(getActivity());
        AndorsTrailPreferences preferences = app.getPreferences();
        preferences.read(getActivity());
        if (app.setLocale(getActivity())) {
            if (alreadyStartedLoadingResources) {
                // Changing the locale after having loaded the game requires resources to
                // be re-loaded. Therefore, we just exit here.
                Toast.makeText(getActivity(), R.string.change_locale_requires_restart, Toast.LENGTH_LONG).show();
                doFinish();
                return;
            }
        }
        if (ThemeHelper.changeTheme(preferences.selectedTheme)) {
            // Changing the theme requires a restart to re-create all activities.
            Toast.makeText(getActivity(), R.string.change_theme_requires_restart, Toast.LENGTH_LONG).show();
            doFinish();
            return;
        }
        app.getWorld().tileManager.updatePreferences(preferences);
    }

    @SuppressLint("NewApi")
    private void doFinish() {
        //For Lollipop and above
        ((AndorsTrailApplication) getActivity().getApplication()).discardWorld();
        getActivity().finish();
    }


	public interface OnNewGameRequestedListener {
		public void onNewGameRequested();
	}

	private OnNewGameRequestedListener listener = null;

	private void createNewGame() {
		if (listener != null) {
			listener.onNewGameRequested();
		}
	}


}

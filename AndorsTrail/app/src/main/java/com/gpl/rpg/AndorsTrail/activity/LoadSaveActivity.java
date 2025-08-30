package com.gpl.rpg.AndorsTrail.activity;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.AndorsTrailPreferences;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.controller.Constants;
import com.gpl.rpg.AndorsTrail.model.ModelContainer;
import com.gpl.rpg.AndorsTrail.resource.tiles.TileManager;
import com.gpl.rpg.AndorsTrail.savegames.Savegames;
import com.gpl.rpg.AndorsTrail.savegames.Savegames.FileHeader;
import com.gpl.rpg.AndorsTrail.util.AndroidStorage;
import com.gpl.rpg.AndorsTrail.util.ThemeHelper;
import com.gpl.rpg.AndorsTrail.view.CustomDialogFactory;
import com.gpl.rpg.AndorsTrail.view.CustomDialogFactory.CustomDialog;

public final class LoadSaveActivity extends AndorsTrailBaseActivity implements OnClickListener {
    private boolean isLoading = true;
    //region special slot numbers
    private static final int SLOT_NUMBER_CREATE_NEW_SLOT = -1;
    public static final int SLOT_NUMBER_EXPORT_SAVEGAMES = -2;
    public static final int SLOT_NUMBER_IMPORT_SAVEGAMES = -3;
    public static final int SLOT_NUMBER_IMPORT_WORLDMAP = -4;
    private static final int SLOT_NUMBER_FIRST_SLOT = 1;
    //endregion
    private ModelContainer model;
    private TileManager tileManager;
    private AndorsTrailPreferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeHelper.getDialogTheme());
        super.onCreate(savedInstanceState);

        final AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this);
        app.setWindowParameters(this);
        this.model = app.getWorld().model;
        this.preferences = app.getPreferences();
        this.tileManager = app.getWorld().tileManager;

        String loadsave = getIntent().getData().getLastPathSegment();
        isLoading = (loadsave.equalsIgnoreCase("load"));

        initializeView(this, R.layout.loadsave, R.id.loadsave_root);

        TextView tv = (TextView) findViewById(R.id.loadsave_title);
        if (isLoading) {
            tv.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_search, 0, 0, 0);
            tv.setText(R.string.loadsave_title_load);
        } else {
            tv.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0);
            tv.setText(R.string.loadsave_title_save);
        }

        ViewGroup slotList = (ViewGroup) findViewById(R.id.loadsave_slot_list);
        Button slotTemplateButton = (Button) findViewById(R.id.loadsave_slot_n);
        LayoutParams params = slotTemplateButton.getLayoutParams();
        slotList.removeView(slotTemplateButton);

        ViewGroup newSlotContainer = (ViewGroup) findViewById(R.id.loadsave_save_to_new_slot_container);
        Button createNewSlot = (Button) findViewById(R.id.loadsave_save_to_new_slot);

        Button exportSaves = (Button) findViewById(R.id.loadsave_export_save);
        Button importSaves = (Button) findViewById(R.id.loadsave_import_save);
        Button importWorldmap = (Button) findViewById(R.id.loadsave_import_worldmap);

        exportSaves.setTag(SLOT_NUMBER_EXPORT_SAVEGAMES);
        importSaves.setTag(SLOT_NUMBER_IMPORT_SAVEGAMES);
        importWorldmap.setTag(SLOT_NUMBER_IMPORT_WORLDMAP);

        ViewGroup exportImportContainer = (ViewGroup) findViewById(R.id.loadsave_export_import_save_container);


        addSavegameSlotButtons(slotList, params, Savegames.getUsedSavegameSlots(this));

        checkAndRequestPermissions();

        if (!isLoading) {
            createNewSlot.setTag(SLOT_NUMBER_CREATE_NEW_SLOT);
            createNewSlot.setOnClickListener(this);
            newSlotContainer.setVisibility(View.VISIBLE);
            exportImportContainer.setVisibility(View.GONE);
        } else {
            newSlotContainer.setVisibility(View.GONE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                exportSaves.setOnClickListener(this);
                importSaves.setOnClickListener(this);
                importWorldmap.setOnClickListener(this);
                exportImportContainer.setVisibility(View.VISIBLE);

                boolean hasSavegames = !Savegames.getUsedSavegameSlots(this).isEmpty();
                exportSaves.setEnabled(hasSavegames);
            } else {
                exportImportContainer.setVisibility(View.GONE);
            }
        }
    }

    private static final int READ_EXTERNAL_STORAGE_REQUEST = 1;
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST = 2;

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (getApplicationContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                this.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                        READ_EXTERNAL_STORAGE_REQUEST);
            }
            if (getApplicationContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        WRITE_EXTERNAL_STORAGE_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.storage_permissions_mandatory, Toast.LENGTH_LONG).show();
            ((AndorsTrailApplication) getApplication()).discardWorld();
            finish();
        }
    }

    private void addSavegameSlotButtons(ViewGroup parent,
                                        LayoutParams params,
                                        List<Integer> usedSavegameSlots) {
        int unused = 1;
        for (int slot : usedSavegameSlots) {
            final FileHeader header = Savegames.quickload(this, slot);
            if (header == null) {
                continue;
            }

            while (unused < slot) {
                Button b = new Button(this);
                b.setLayoutParams(params);
                b.setTag(unused);
                b.setOnClickListener(this);
                b.setText(getString(R.string.loadsave_empty_slot, unused));
                tileManager.setImageViewTileForPlayer(getResources(), b, header.iconID);
                parent.addView(b, params);
                unused++;
            }
            unused++;

            Button b = new Button(this);
            b.setLayoutParams(params);
            b.setTag(slot);
            b.setOnClickListener(this);
            b.setText(slot + ". " + header.describe());
            tileManager.setImageViewTileForPlayer(getResources(), b, header.iconID);
            parent.addView(b, params);
        }
    }

    private void cancelLoadSaveActivity(int slot) {
        completeLoadSaveActivity(slot, false);
    }

    private void completeLoadSaveActivity(int slot) {
        completeLoadSaveActivity(slot, true);
    }

    private void completeLoadSaveActivity(int slot, boolean success) {
        Intent i = new Intent();
        if (slot == SLOT_NUMBER_CREATE_NEW_SLOT) {
            slot = getFirstFreeSlot();
        } else if (slot == SLOT_NUMBER_EXPORT_SAVEGAMES
                   || slot == SLOT_NUMBER_IMPORT_SAVEGAMES
                   || slot == SLOT_NUMBER_IMPORT_WORLDMAP) {
            i.putExtra("import_export", true);

            if (slot == SLOT_NUMBER_IMPORT_WORLDMAP) {
                i.putExtra("import_worldmap", true);
            }
            if (slot == SLOT_NUMBER_IMPORT_SAVEGAMES) {
                i.putExtra("import_savegames", true);
            }
            if (slot == SLOT_NUMBER_EXPORT_SAVEGAMES) {
                i.putExtra("export", true);
            }

        } else if (slot < SLOT_NUMBER_FIRST_SLOT) {
            slot = SLOT_NUMBER_FIRST_SLOT;
        }

        i.putExtra("slot", slot);
        if (success) {
            setResult(Activity.RESULT_OK, i);
        } else {
            setResult(Activity.RESULT_CANCELED, i);
        }
        LoadSaveActivity.this.finish();
    }

    private int getFirstFreeSlot() {
        int slot;
        List<Integer> usedSlots = Savegames.getUsedSavegameSlots(this);
        if (usedSlots.isEmpty()) {
            slot = SLOT_NUMBER_FIRST_SLOT;
        } else {
            slot = Collections.max(usedSlots) + 1;
        }
        return slot;
    }

    private String getConfirmOverwriteQuestion(int slot) {
        if (isLoading) {
            return null;
        }

        return getConfirmOverwriteQuestionIgnoringLoading(slot);
    }

    private String getConfirmOverwriteQuestionIgnoringLoading(int slot) {
        if (slot == SLOT_NUMBER_CREATE_NEW_SLOT) {
            return null;//creating a new savegame
        }

        if (!Savegames.getSlotFile(slot, this).exists()) {
            return null;//nothing in slot to overwrite
        }

        if (preferences.displayOverwriteSavegame
            == AndorsTrailPreferences.CONFIRM_OVERWRITE_SAVEGAME_ALWAYS) {
            return getString(R.string.loadsave_save_overwrite_confirmation_all);
        }
        if (preferences.displayOverwriteSavegame == AndorsTrailPreferences.CONFIRM_OVERWRITE_SAVEGAME_NEVER) {
            return null;
        }

        final String currentPlayerName = model.player.getName();
        final FileHeader header = Savegames.quickload(this, slot);
        if (header == null) {
            return null;
        }

        final String savedPlayerName = header.playerName;
        if (currentPlayerName.equals(savedPlayerName)) {
            return null; //if the names match
        }

        return getString(R.string.loadsave_save_overwrite_confirmation, savedPlayerName, currentPlayerName);
    }

    @Override
    public void onClick(View view) {
        final int slot = (Integer) view.getTag();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switch (slot) {
                case SLOT_NUMBER_IMPORT_WORLDMAP:
                    clickImportWorldmap();
                    return;
                case SLOT_NUMBER_IMPORT_SAVEGAMES:
                    clickImportSaveGames();
                    return;
                case SLOT_NUMBER_EXPORT_SAVEGAMES:
                    clickExportSaveGames();
                    return;
            }
        }
        if (!isLoading
            && slot != SLOT_NUMBER_CREATE_NEW_SLOT
            && AndorsTrailApplication.CURRENT_VERSION
               == AndorsTrailApplication.DEVELOPMENT_INCOMPATIBLE_SAVEGAME_VERSION) {
            if (!isOverwriteTargetInIncompatibleVersion(slot)) {
                saveOrOverwriteSavegame(slot);
            }
        } else if (isLoading) {
            loadSaveGame(slot);
        } else {
            saveOrOverwriteSavegame(slot);
        }
    }

    private void saveOrOverwriteSavegame(int slot) {
        final String message = getConfirmOverwriteQuestion(slot);
        if (message != null) {
            showConfirmOverwriteQuestion(slot, message);
        } else {
            completeLoadSaveActivity(slot);
        }
    }

    private boolean isOverwriteTargetInIncompatibleVersion(int slot) {
        final FileHeader header = Savegames.quickload(this, slot);
        if (header != null
            && header.fileversion != AndorsTrailApplication.DEVELOPMENT_INCOMPATIBLE_SAVEGAME_VERSION) {
            final CustomDialog d = CustomDialogFactory.createErrorDialog(this,
                                                                         "Overwriting not allowed",
                                                                         "You are currently using a development version of Andor's trail. Overwriting a regular savegame is not allowed in development mode.");
            CustomDialogFactory.show(d);
            return true;
        }
        return false;
    }

    //region Imports/Exports

    //region Export

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void exportSaveGames(Intent data) {
        Uri uri = data.getData();

        Context context = getApplicationContext();
        ContentResolver resolver = AndorsTrailApplication.getApplicationFromActivity(this)
                                                         .getContentResolver();

        File storageDir = AndroidStorage.getStorageDirectory(context,
                                                             Constants.FILENAME_SAVEGAME_DIRECTORY);
        DocumentFile target = DocumentFile.fromTreeUri(context, uri);
        if (target == null) {
            return;
        }

        File[] files = storageDir.listFiles();
        if (files == null) {
            showErrorExportingSaveGamesUnknown();
            return;
        }

        boolean hasExistingFiles = false;
        for (File file : files) {
            String fileName = file.getName();

            DocumentFile existingFile = target.findFile(fileName);
            if (existingFile != null) {
                hasExistingFiles = true;
                break;
            }
        }

        if (hasExistingFiles) {
            showConfirmOverwriteByExportQuestion(resolver, target, files);
        } else {
            exportSaveGamesFolderContentToFolder(target, files);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void exportSaveGamesFolderContentToFolder(DocumentFile target, File[] files) {
        DocumentFile[] sourceFiles = new DocumentFile[files.length];

        File[] worldmapFiles = null;

        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isFile()) {
                sourceFiles[i] = DocumentFile.fromFile(file);
            } else if (file.isDirectory() && Objects.equals(file.getName(),
                                                            Constants.FILENAME_WORLDMAP_DIRECTORY)) {
                worldmapFiles = file.listFiles();
            }
        }
        Context context = this;
        File[] finalWorldmapFiles = worldmapFiles;
        CopyFilesToExternalFolder(target, sourceFiles, context, finalWorldmapFiles);

    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void CopyFilesToExternalFolder(DocumentFile target,
                                           DocumentFile[] sourceFiles,
                                           Context context,
                                           File[] finalWorldmapFiles) {
        AndroidStorage.copyDocumentFilesToDirAsync(sourceFiles,
                                                   context,
                                                   target,
                                                   getString(R.string.loadsave_exporting_savegames),
                                                   (success) -> {
                                                       if (success) {
                                                           CopyWorldmapFilesAsZip(target,
                                                                                  context,
                                                                                  finalWorldmapFiles);
                                                       } else {
                                                           completeLoadSaveActivity(
                                                                   SLOT_NUMBER_EXPORT_SAVEGAMES,
                                                                   false);
                                                       }
                                                   });
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void CopyWorldmapFilesAsZip(DocumentFile target,
                                        Context context,
                                        File[] finalWorldmapFiles) {
        AndroidStorage.createZipDocumentFileFromFilesAsync(finalWorldmapFiles,
                                                           context,
                                                           target,
                                                           Constants.FILENAME_WORLDMAP_DIRECTORY,
                                                           getString(R.string.loadsave_exporting_worldmap),
                                                           (successWorldmap) -> completeLoadSaveActivity(
                                                                   SLOT_NUMBER_EXPORT_SAVEGAMES,
                                                                   successWorldmap));
    }

    //endregion

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void importSaveGames(Intent data) {
        Uri uri = data.getData();
        ClipData uris = data.getClipData();

        if (uri == null && uris == null) {
            //no file was selected
            return;
        }

        Context context = getApplicationContext();
        ContentResolver resolver = AndorsTrailApplication.getApplicationFromActivity(this)
                .getContentResolver();

        File storageDir = AndroidStorage.getStorageDirectory(context, Constants.FILENAME_SAVEGAME_DIRECTORY);
        DocumentFile appSavegameFolder = DocumentFile.fromFile(storageDir);

        List<Uri> uriList = new ArrayList<>();
        if (uri != null) {
            uriList.add(uri);
        } else {
            for (int i = 0; i < uris.getItemCount(); i++) {
                uriList.add(uris.getItemAt(i).getUri());
            }
        }
        importSaveGamesFromUris(context, resolver, appSavegameFolder, uriList);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void importSaveGamesFromUris(Context context,
                                         ContentResolver resolver,
                                         DocumentFile appSavegameFolder,
                                         List<Uri> uriList) {
        int count = uriList.size();

        ArrayList<DocumentFile> alreadyExistingFiles = new ArrayList<>();
        ArrayList<DocumentFile> newFiles = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Uri item = uriList.get(i);
            DocumentFile itemFile = DocumentFile.fromSingleUri(context, item);
            boolean fileAlreadyExists = getExistsSavegameInOwnFiles(itemFile, appSavegameFolder);
            if (fileAlreadyExists) {
                alreadyExistingFiles.add(itemFile);
            } else {
                newFiles.add(itemFile);
            }
        }

        if (alreadyExistingFiles.size() > 0) {
            showConfirmOverwriteByImportQuestion(resolver, appSavegameFolder, alreadyExistingFiles, newFiles);
        } else {
            importSaveGames(resolver, appSavegameFolder, newFiles);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void importSaveGames(ContentResolver resolver,
                                 DocumentFile appSavegameFolder,
                                 List<DocumentFile> saveFiles) {
        int size = saveFiles.size();
        DocumentFile[] sources = new DocumentFile[size];
        DocumentFile[] targets = new DocumentFile[size];

        boolean saveAsNew = false;
        for (int i = 0; i < size; i++) {
            DocumentFile file = saveFiles.get(i);
            if (file == null) {//null is value a marker that the next should be saved as new
                saveAsNew = true;
                continue;
            }

            int slot = getSlotFromSavegameFileName(file.getName());
            if (slot == -1) {
                //invalid file name
                continue;
            }

            if (saveAsNew) {
                slot = getFirstFreeSlot();
                saveAsNew = false;
            }

            String targetName = Savegames.getSlotFileName(slot);
            sources[i] = file;
            targets[i] = getOrCreateDocumentFile(appSavegameFolder, targetName);
        }

        AndroidStorage.copyDocumentFilesFromToAsync(sources,
                                                    this,
                                                    targets,
                                                    getString(R.string.loadsave_importing_savegames),
                                                    (sucess) -> completeLoadSaveActivity(
                                                            SLOT_NUMBER_IMPORT_SAVEGAMES,
                                                            sucess));
    }

    private boolean getExistsSavegameInOwnFiles(DocumentFile savegameFile, DocumentFile appSavegameFolder) {
        if (savegameFile == null) {
            return false;
        }

        DocumentFile foundFile = appSavegameFolder.findFile(Objects.requireNonNull(savegameFile.getName()));
        return foundFile != null && foundFile.exists();
    }

    private int getSlotFromSavegameFileName(String fileName) {
        if (fileName == null || !fileName.startsWith(Constants.FILENAME_SAVEGAME_FILENAME_PREFIX)) {
            return -1;
        }
        String slotStr = fileName.substring(Constants.FILENAME_SAVEGAME_FILENAME_PREFIX.length());

        int slot;
        try {
            slot = Integer.parseInt(slotStr);
            return slot;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private DocumentFile getOrCreateDocumentFile(DocumentFile folder, String targetName) {
        DocumentFile targetFile = folder.findFile(targetName);//try finding the file
        if (targetFile == null)//no file found, creating new one
        {
            targetFile = folder.createFile(Constants.NO_FILE_EXTENSION_MIME_TYPE, targetName);
        }
        return targetFile;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void importWorldmap(Intent data) {
        Uri uri = data.getData();

        Context context = AndorsTrailApplication.getApplicationFromActivity(this).getApplicationContext();

        DocumentFile chosenZip = DocumentFile.fromSingleUri(context, uri);
        if (chosenZip == null || !chosenZip.isFile()) {
            showErrorImportingWorldmapWrongDirectory();
            return;
        }
        String chosenZipName = chosenZip.getName();
        if (!chosenZipName.startsWith(Constants.FILENAME_WORLDMAP_DIRECTORY)) {
            showErrorImportingWorldmapWrongDirectory();
            return;
        }

        File ownWorldmapFolder = getOwnWorldmapFolder(context);


        AndroidStorage.unzipDocumentFileToDirectoryAsync(chosenZip,
                                                         this,
                                                         ownWorldmapFolder,
                                                         false,
                                                         getString(R.string.loadsave_importing_worldmap),
                                                         (success) -> completeLoadSaveActivity(
                                                                 SLOT_NUMBER_IMPORT_WORLDMAP,
                                                                 success));
    }

    private File getOwnWorldmapFolder(Context context) {
        File storageDir = AndroidStorage.getStorageDirectory(context, Constants.FILENAME_SAVEGAME_DIRECTORY);
        File ownWorldmapFolder = null;
        for (File f : storageDir.listFiles()) {
            if (f.getName().equals(Constants.FILENAME_WORLDMAP_DIRECTORY)) {
                ownWorldmapFolder = f;
                break;
            }
        }
        if (ownWorldmapFolder == null) {
            ownWorldmapFolder = new File(storageDir, Constants.FILENAME_WORLDMAP_DIRECTORY);
            ownWorldmapFolder.mkdir();
        }
        return ownWorldmapFolder;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void clickExportSaveGames() {
        showStartExportInfo(view -> startActivityForResult(AndroidStorage.getNewOpenDirectoryIntent(),
                                                           -SLOT_NUMBER_EXPORT_SAVEGAMES));
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void clickImportSaveGames() {
        showStartImportSavesInfo(view -> startActivityForResult(AndroidStorage.getNewSelectMultipleSavegameFilesIntent(),
                                                                -SLOT_NUMBER_IMPORT_SAVEGAMES));
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void clickImportWorldmap() {
        showStartImportWorldmapInfo(view -> startActivityForResult(AndroidStorage.getNewSelectZipIntent(),
                                                                   -SLOT_NUMBER_IMPORT_WORLDMAP));

    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void showConfirmOverwriteByExportQuestion(ContentResolver resolver,
                                                      DocumentFile targetFolder,
                                                      File[] files) {
        final CustomDialog d = CustomDialogFactory.createDialog(this,
                                                                getString(R.string.loadsave_export_overwrite_confirmation_title),
                                                                getResources().getDrawable(android.R.drawable.ic_dialog_alert),
                                                                getString(R.string.loadsave_export_overwrite_confirmation),
                                                                null,
                                                                true);

        CustomDialogFactory.addButton(d,
                                      android.R.string.yes,
                                      v -> exportSaveGamesFolderContentToFolder(targetFolder, files));
        CustomDialogFactory.addDismissButton(d, android.R.string.no);

        CustomDialogFactory.show(d);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void showConfirmOverwriteByImportQuestion(ContentResolver resolver,
                                                      DocumentFile appSavegameFolder,
                                                      List<DocumentFile> alreadyExistingFiles,
                                                      List<DocumentFile> newFiles) {
        final String title = getString(R.string.loadsave_import_overwrite_confirmation_title);
        String message = getString(R.string.loadsave_import_file_exists_question);

        StringBuilder sb = new StringBuilder();
        sb.append('\n');
        int amount = alreadyExistingFiles.size();

        Context context = AndorsTrailApplication.getApplicationFromActivity(this).getApplicationContext();

        ArrayList<CustomDialog> dialogs = new ArrayList<>(amount);

        for (int i = 0; i < amount; i++) {
            DocumentFile alreadyExistingFile = alreadyExistingFiles.get(i);
            int slot = getSlotFromSavegameFileName(alreadyExistingFile.getName());
            FileHeader existingFileHeader = Savegames.quickload(context, slot);
            FileHeader importedFileHeader = null;
            try (InputStream stream = resolver.openInputStream(alreadyExistingFile.getUri());
                 DataInputStream dataStream = new DataInputStream(stream)) {
                importedFileHeader = new FileHeader(dataStream, true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                continue;
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            StringBuilder messageSb = new StringBuilder();
            String existingFileDescription = getString(R.string.loadsave_import_existing_description,
                                                       Integer.toString(slot),
                                                       existingFileHeader.describe());
            String importedFileDescription = getString(R.string.loadsave_import_imported_description,
                                                       Integer.toString(slot),
                                                       importedFileHeader.describe());
            messageSb.append(getString(R.string.loadsave_import_file_exists_question,
                                       existingFileDescription,
                                       importedFileDescription));


            String m = messageSb.toString();
            CustomDialog dialog = CustomDialogFactory.createDialog(this,
                                                                   title,
                                                                   getResources().getDrawable(android.R.drawable.ic_dialog_alert),
                                                                   m,
                                                                   null,
                                                                   true,
                                                                   false,
                                                                   true);

            CustomDialogFactory.addButton(dialog, R.string.loadsave_import_option_keep_existing, v -> {
                //do nothing
                GoToNextConflictOrFinish(resolver, appSavegameFolder, newFiles, dialogs);
            });

            CustomDialogFactory.addButton(dialog, R.string.loadsave_import_option_keep_imported, v -> {
                newFiles.add(alreadyExistingFile);
                GoToNextConflictOrFinish(resolver, appSavegameFolder, newFiles, dialogs);
            });

            CustomDialogFactory.addButton(dialog, R.string.loadsave_import_option_add_as_new, v -> {
                newFiles.add(null);//add a null element as marker to know later if the next file
                // should be imported as new or overwrite the existing one
                newFiles.add(alreadyExistingFile);
                GoToNextConflictOrFinish(resolver, appSavegameFolder, newFiles, dialogs);
            });

            CustomDialogFactory.addCancelButton(dialog, android.R.string.cancel);
            CustomDialogFactory.setCancelListener(dialog, v -> {
                completeLoadSaveActivity(SLOT_NUMBER_IMPORT_SAVEGAMES, false);
            });

            dialogs.add(dialog);
        }

        GoToNextConflictOrFinish(resolver, appSavegameFolder, newFiles, dialogs);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void GoToNextConflictOrFinish(ContentResolver resolver,
                                          DocumentFile appSavegameFolder,
                                          List<DocumentFile> newFiles,
                                          ArrayList<CustomDialog> dialogs) {
        if (dialogs.stream().count() > 0) {
            CustomDialog d = dialogs.remove(0);
            CustomDialogFactory.show(d);
        } else {
            importSaveGames(resolver, appSavegameFolder, newFiles);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switch (-requestCode) {
                case SLOT_NUMBER_EXPORT_SAVEGAMES:
                    exportSaveGames(data);
                    return;
                case SLOT_NUMBER_IMPORT_SAVEGAMES:
                    importSaveGames(data);
                    return;
                case SLOT_NUMBER_IMPORT_WORLDMAP:
                    importWorldmap(data);
                    return;
            }
        }

    }

    //endregion

    private void loadSaveGame(int slot) {
        if (!Savegames.getSlotFile(slot, this).exists()) {
            showErrorLoadingEmptySlot();
        } else {
            final FileHeader header = Savegames.quickload(this, slot);
            if (header != null && !header.hasUnlimitedSaves) {
                showSlotGetsDeletedOnLoadWarning(slot);
            } else {
                completeLoadSaveActivity(slot);
            }
        }
    }

    //region show Dialogs

    //region Import/Export

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void showStartExportInfo(OnClickListener onOk) {
        final CustomDialog d = CustomDialogFactory.createDialog(this,
                                                                getString(R.string.loadsave_export),
                                                                getResources().getDrawable(android.R.drawable.ic_dialog_info),
                                                                getString(R.string.loadsave_export_info),
                                                                null,
                                                                true);
        CustomDialogFactory.addButton(d, android.R.string.yes, onOk);
        CustomDialogFactory.addDismissButton(d, android.R.string.no);
        CustomDialogFactory.show(d);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void showStartImportSavesInfo(OnClickListener onOk) {
        final CustomDialog d = CustomDialogFactory.createDialog(this,
                                                                getString(R.string.loadsave_import_save),
                                                                getResources().getDrawable(android.R.drawable.ic_dialog_info),
                                                                getString(R.string.loadsave_import_save_info),
                                                                null,
                                                                true);
        CustomDialogFactory.addButton(d, android.R.string.yes, onOk);
        CustomDialogFactory.addDismissButton(d, android.R.string.no);
        CustomDialogFactory.show(d);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void showStartImportWorldmapInfo(OnClickListener onOk) {
        final CustomDialog d = CustomDialogFactory.createDialog(this,
                                                                getString(R.string.loadsave_import_worldmap),
                                                                getResources().getDrawable(android.R.drawable.ic_dialog_info),
                                                                getString(R.string.loadsave_import_worldmap_info),
                                                                null,
                                                                true);
        CustomDialogFactory.addButton(d, android.R.string.yes, onOk);
        CustomDialogFactory.addDismissButton(d, android.R.string.no);
        CustomDialogFactory.show(d);
    }

    private void showErrorImportingWorldmapWrongDirectory() {
        final CustomDialog d = CustomDialogFactory.createErrorDialog(this,
                                                                     getString(R.string.loadsave_import_worldmap_unsuccessfull),
                                                                     getString(R.string.loadsave_import_worldmap_wrong_file));
        CustomDialogFactory.show(d);
    }

    private void showErrorExportingSaveGamesUnknown() {
        final CustomDialog d = CustomDialogFactory.createErrorDialog(this,
                                                                     getString(R.string.loadsave_export_unsuccessfull),
                                                                     getString(R.string.loadsave_export_error_unknown));
        CustomDialogFactory.show(d);
    }

    //endregion

    private void showErrorLoadingEmptySlot() {
        final CustomDialog d = CustomDialogFactory.createErrorDialog(this,
                                                                     getString(R.string.startscreen_error_loading_game),
                                                                     getString(R.string.startscreen_error_loading_empty_slot));
        CustomDialogFactory.show(d);
    }

    private void showSlotGetsDeletedOnLoadWarning(final int slot) {
        final CustomDialog d = CustomDialogFactory.createDialog(this,
                                                                getString(R.string.startscreen_attention_slot_gets_delete_on_load),
                                                                getResources().getDrawable(android.R.drawable.ic_dialog_alert),
                                                                getString(R.string.startscreen_attention_message_slot_gets_delete_on_load),
                                                                null,
                                                                true);
        CustomDialogFactory.addButton(d, android.R.string.ok, v -> completeLoadSaveActivity(slot));
        CustomDialogFactory.show(d);
    }

    private void showConfirmOverwriteQuestion(final int slot, String message) {
		final String title = getString(R.string.loadsave_save_overwrite_confirmation_title) + ' '
							 + getString(R.string.loadsave_save_overwrite_confirmation_slot, slot);
        final CustomDialog d = CustomDialogFactory.createDialog(this,
                                                                title,
                                                                getResources().getDrawable(android.R.drawable.ic_dialog_alert),
                                                                message,
                                                                null,
                                                                true);

        CustomDialogFactory.addButton(d, android.R.string.yes, v -> completeLoadSaveActivity(slot));
        CustomDialogFactory.addDismissButton(d, android.R.string.no);
        CustomDialogFactory.show(d);
    }

    //endregion

}

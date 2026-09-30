package com.ayashii.wallpaper;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private View layoutEmptyState;
    private View layoutLoadingState;
    private View layoutReadyState;
    private TextView tvVideoDetails;
    private VideoView videoPreview;
    private AppCompatButton btnPickVideo;
    private AppCompatButton btnApplyWallpaper;
    private AppCompatButton btnRemoveVideo;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<String> videoPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupVideoPicker();
        setupListeners();
        checkCurrentWallpaper();
    }

    private void initViews() {
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        layoutLoadingState = findViewById(R.id.layoutLoadingState);
        layoutReadyState = findViewById(R.id.layoutReadyState);
        tvVideoDetails = findViewById(R.id.tvVideoDetails);
        videoPreview = findViewById(R.id.videoPreview);
        btnPickVideo = findViewById(R.id.btnPickVideo);
        btnApplyWallpaper = findViewById(R.id.btnApplyWallpaper);
        btnRemoveVideo = findViewById(R.id.btnRemoveVideo);

        videoPreview.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
                mp.setVolume(0f, 0f);
            }
        });
    }

    private void setupVideoPicker() {
        videoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        saveVideoFile(uri);
                    }
                }
        );
    }

    private void setupListeners() {
        btnPickVideo.setOnClickListener(v -> videoPickerLauncher.launch("video/*"));

        btnApplyWallpaper.setOnClickListener(v -> applyWallpaper());

        btnRemoveVideo.setOnClickListener(v -> removeVideo());
    }

    private void checkCurrentWallpaper() {
        File videoFile = new File(getFilesDir(), VideoWallpaperService.VIDEO_FILE_NAME);
        if (videoFile.exists() && videoFile.length() > 0) {
            showReadyState(videoFile);
        } else {
            showEmptyState();
        }
    }

    private void saveVideoFile(Uri uri) {
        showLoadingState();

        executorService.execute(() -> {
            File targetFile = new File(getFilesDir(), VideoWallpaperService.VIDEO_FILE_NAME);
            boolean success = false;

            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(targetFile)) {

                if (in != null) {
                    byte[] buffer = new byte[16384];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                    success = true;
                }
            } catch (Exception e) {
                success = false;
            }

            final boolean finalSuccess = success;
            runOnUiThread(() -> {
                if (finalSuccess) {
                    notifyWallpaperUpdated();
                    showReadyState(targetFile);
                    Toast.makeText(MainActivity.this, R.string.msg_video_loaded, Toast.LENGTH_SHORT).show();
                } else {
                    showEmptyState();
                    Toast.makeText(MainActivity.this, R.string.msg_video_error, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void showEmptyState() {
        layoutEmptyState.setVisibility(View.VISIBLE);
        layoutLoadingState.setVisibility(View.GONE);
        layoutReadyState.setVisibility(View.GONE);

        btnApplyWallpaper.setEnabled(false);
        btnRemoveVideo.setVisibility(View.GONE);

        if (videoPreview.isPlaying()) {
            videoPreview.stopPlayback();
        }
    }

    private void showLoadingState() {
        layoutEmptyState.setVisibility(View.GONE);
        layoutLoadingState.setVisibility(View.VISIBLE);
        layoutReadyState.setVisibility(View.GONE);

        btnApplyWallpaper.setEnabled(false);
        btnRemoveVideo.setVisibility(View.GONE);
    }

    private void showReadyState(File videoFile) {
        layoutEmptyState.setVisibility(View.GONE);
        layoutLoadingState.setVisibility(View.GONE);
        layoutReadyState.setVisibility(View.VISIBLE);

        btnApplyWallpaper.setEnabled(true);
        btnRemoveVideo.setVisibility(View.VISIBLE);

        double sizeMb = (double) videoFile.length() / (1024 * 1024);
        long durationSec = 0;

        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(videoFile.getAbsolutePath());
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (time != null) {
                durationSec = Long.parseLong(time) / 1000;
            }
            retriever.release();
        } catch (Exception ignored) {
        }

        String details = String.format(Locale.getDefault(),
                "Ukuran: %.2f MB  |  Durasi: %d detik",
                sizeMb, durationSec);
        tvVideoDetails.setText(details);

        videoPreview.setVideoPath(videoFile.getAbsolutePath());
        videoPreview.start();
    }

    private void applyWallpaper() {
        File videoFile = new File(getFilesDir(), VideoWallpaperService.VIDEO_FILE_NAME);
        if (!videoFile.exists() || videoFile.length() == 0) {
            Toast.makeText(this, R.string.msg_please_select_video, Toast.LENGTH_SHORT).show();
            return;
        }

        ComponentName componentName = new ComponentName(getPackageName(), VideoWallpaperService.class.getName());

        // Strategy 1: Standard ACTION_CHANGE_LIVE_WALLPAPER
        try {
            Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, componentName);
            intent.putExtra("android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT", componentName);
            intent.putExtra("SET_LOCKSCREEN_WALLPAPER", true);
            startActivity(intent);
            return;
        } catch (Exception ignored) {
        }

        // Strategy 2: ACTION_LIVE_WALLPAPER_CHOOSER
        try {
            Intent intent = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
            startActivity(intent);
            return;
        } catch (Exception ignored) {
        }

        // Strategy 3: Direct LivePicker package
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.wallpaper.livepicker",
                    "com.android.wallpaper.livepicker.LiveWallpaperPreview"));
            intent.putExtra("android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT", componentName);
            startActivity(intent);
            return;
        } catch (Exception ignored) {
        }

        // Strategy 4: Google Wallpapers app if installed
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.google.android.apps.wallpaper",
                    "com.google.android.apps.wallpaper.picker.CategoryPickerActivity"));
            startActivity(intent);
            return;
        } catch (Exception ignored) {
        }

        // Strategy 5: Generic ACTION_SET_WALLPAPER
        try {
            Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
            startActivity(Intent.createChooser(intent, "Setel Wallpaper"));
            return;
        } catch (Exception ignored) {
        }

        // If all system selectors fail: show helpful guidance dialog with direct Play Store link
        showInstallPickerPrompt();
    }

    private void showInstallPickerPrompt() {
        new AlertDialog.Builder(this)
                .setTitle("Komponen Sistem Belum Tersedia")
                .setMessage("HP Anda belum memiliki pemilih Live Wallpaper bawaan (biasanya terjadi pada Android Go atau ROM tertentu).\n\nPasang aplikasi 'Wallpaper' resmi dari Google (gratis di Play Store) untuk membuka pratinjau live wallpaper?")
                .setPositiveButton("Buka Google Play Store", (dialog, which) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=com.google.android.apps.wallpaper")));
                    } catch (Exception e) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.wallpaper")));
                    }
                })
                .setNegativeButton("Tutup", null)
                .show();
    }

    private void removeVideo() {
        File videoFile = new File(getFilesDir(), VideoWallpaperService.VIDEO_FILE_NAME);
        if (videoFile.exists()) {
            videoFile.delete();
        }

        notifyWallpaperUpdated();
        showEmptyState();
        Toast.makeText(this, R.string.msg_video_removed, Toast.LENGTH_SHORT).show();
    }

    private void notifyWallpaperUpdated() {
        Intent intent = new Intent(VideoWallpaperService.ACTION_VIDEO_UPDATED);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (layoutReadyState.getVisibility() == View.VISIBLE && !videoPreview.isPlaying()) {
            videoPreview.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoPreview.isPlaying()) {
            videoPreview.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}

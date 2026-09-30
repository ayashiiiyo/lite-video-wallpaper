package com.ayashii.wallpaper;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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

    private static final String TAG = "MainActivity";

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
                try {
                    mp.setLooping(true);
                    mp.setVolume(0f, 0f);
                } catch (Throwable ignored) {
                }
            }
        });

        videoPreview.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return true;
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
        btnPickVideo.setOnClickListener(v -> {
            try {
                videoPickerLauncher.launch("video/*");
            } catch (Throwable t) {
                Toast.makeText(this, "Tidak dapat membuka galeri.", Toast.LENGTH_SHORT).show();
            }
        });

        btnApplyWallpaper.setOnClickListener(v -> applyWallpaper());

        btnRemoveVideo.setOnClickListener(v -> removeVideo());
    }

    private void checkCurrentWallpaper() {
        try {
            File videoFile = new File(getFilesDir(), VideoWallpaperService.VIDEO_FILE_NAME);
            if (videoFile.exists() && videoFile.length() > 0) {
                showReadyState(videoFile);
            } else {
                showEmptyState();
            }
        } catch (Throwable t) {
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
            } catch (Throwable e) {
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

        try {
            if (videoPreview.isPlaying()) {
                videoPreview.stopPlayback();
            }
        } catch (Throwable ignored) {
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
        } catch (Throwable ignored) {
        }

        String details = String.format(Locale.getDefault(),
                "Ukuran: %.2f MB  |  Durasi: %d detik",
                sizeMb, durationSec);
        tvVideoDetails.setText(details);

        try {
            videoPreview.setVideoPath(videoFile.getAbsolutePath());
            videoPreview.start();
        } catch (Throwable t) {
            Log.w(TAG, "Error playing preview", t);
        }
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
        } catch (Throwable ignored) {
        }

        // Strategy 2: ACTION_LIVE_WALLPAPER_CHOOSER
        try {
            Intent intent = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
            startActivity(intent);
            return;
        } catch (Throwable ignored) {
        }

        // Strategy 3: Direct LivePicker package
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.wallpaper.livepicker",
                    "com.android.wallpaper.livepicker.LiveWallpaperPreview"));
            intent.putExtra("android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT", componentName);
            startActivity(intent);
            return;
        } catch (Throwable ignored) {
        }

        // Strategy 4: Google Wallpapers app if installed
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.google.android.apps.wallpaper",
                    "com.google.android.apps.wallpaper.picker.CategoryPickerActivity"));
            startActivity(intent);
            return;
        } catch (Throwable ignored) {
        }

        // Strategy 5: Generic ACTION_SET_WALLPAPER
        try {
            Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
            startActivity(Intent.createChooser(intent, "Setel Wallpaper"));
            return;
        } catch (Throwable ignored) {
        }

        showInstallPickerPrompt();
    }

    private void showInstallPickerPrompt() {
        try {
            new AlertDialog.Builder(this)
                    .setTitle("Komponen Sistem Belum Tersedia")
                    .setMessage("HP Anda belum memiliki pemilih Live Wallpaper bawaan (biasanya terjadi pada Android Go atau ROM tertentu).\n\nPasang aplikasi 'Wallpaper' resmi dari Google (gratis di Play Store) untuk membuka pratinjau live wallpaper?")
                    .setPositiveButton("Buka Google Play Store", (dialog, which) -> {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=com.google.android.apps.wallpaper")));
                        } catch (Throwable e) {
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.wallpaper")));
                            } catch (Throwable ignored) {
                            }
                        }
                    })
                    .setNegativeButton("Tutup", null)
                    .show();
        } catch (Throwable ignored) {
        }
    }

    private void removeVideo() {
        try {
            File videoFile = new File(getFilesDir(), VideoWallpaperService.VIDEO_FILE_NAME);
            if (videoFile.exists()) {
                videoFile.delete();
            }
        } catch (Throwable ignored) {
        }

        notifyWallpaperUpdated();
        showEmptyState();
        Toast.makeText(this, R.string.msg_video_removed, Toast.LENGTH_SHORT).show();
    }

    private void notifyWallpaperUpdated() {
        try {
            Intent intent = new Intent(VideoWallpaperService.ACTION_VIDEO_UPDATED);
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (layoutReadyState.getVisibility() == View.VISIBLE && !videoPreview.isPlaying()) {
                videoPreview.start();
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (videoPreview.isPlaying()) {
                videoPreview.pause();
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            executorService.shutdown();
        } catch (Throwable ignored) {
        }
    }
}

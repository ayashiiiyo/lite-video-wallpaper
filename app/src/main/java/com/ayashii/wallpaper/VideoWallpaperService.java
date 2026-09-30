package com.ayashii.wallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Build;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.File;

public class VideoWallpaperService extends WallpaperService {

    private static final String TAG = "VideoWallpaper";
    public static final String ACTION_VIDEO_UPDATED = "com.ayashii.wallpaper.ACTION_VIDEO_UPDATED";
    public static final String VIDEO_FILE_NAME = "wallpaper.mp4";

    @Override
    public Engine onCreateEngine() {
        return new VideoWallpaperEngine();
    }

    class VideoWallpaperEngine extends Engine {

        private MediaPlayer mediaPlayer;
        private boolean isVisible = false;
        private boolean isReceiverRegistered = false;

        private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_VIDEO_UPDATED.equals(intent.getAction())) {
                    startPlayer();
                }
            }
        };

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            try {
                IntentFilter filter = new IntentFilter(ACTION_VIDEO_UPDATED);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
                } else {
                    registerReceiver(updateReceiver, filter);
                }
                isReceiverRegistered = true;
            } catch (Throwable t) {
                Log.e(TAG, "Error registering receiver", t);
            }
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            startPlayer();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            if (mediaPlayer == null && isVisible) {
                startPlayer();
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            releasePlayer();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            this.isVisible = visible;
            if (mediaPlayer != null) {
                try {
                    if (visible) {
                        mediaPlayer.start();
                    } else {
                        mediaPlayer.pause();
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "Error toggling playback in onVisibilityChanged", t);
                }
            } else if (visible) {
                startPlayer();
            }
        }

        private synchronized void startPlayer() {
            File videoFile = new File(getFilesDir(), VIDEO_FILE_NAME);
            if (!videoFile.exists() || videoFile.length() == 0) {
                return;
            }

            SurfaceHolder holder = getSurfaceHolder();
            if (holder == null) return;
            Surface surface = holder.getSurface();
            if (surface == null || !surface.isValid()) return;

            releasePlayer();

            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setSurface(surface);
                mediaPlayer.setDataSource(videoFile.getAbsolutePath());
                mediaPlayer.setLooping(true);
                mediaPlayer.setVolume(0f, 0f);

                mediaPlayer.setOnPreparedListener(mp -> {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                        }
                        if (isVisible) {
                            mp.start();
                        }
                    } catch (Throwable t) {
                        Log.e(TAG, "Error starting player in onPrepared", t);
                    }
                });

                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "MediaPlayer error: " + what + ", " + extra);
                    releasePlayer();
                    return true;
                });

                mediaPlayer.prepareAsync();

            } catch (Throwable t) {
                Log.e(TAG, "Failed to initialize MediaPlayer", t);
                releasePlayer();
            }
        }

        private synchronized void releasePlayer() {
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.reset();
                    mediaPlayer.release();
                } catch (Throwable t) {
                    Log.w(TAG, "Error releasing MediaPlayer", t);
                } finally {
                    mediaPlayer = null;
                }
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (isReceiverRegistered) {
                try {
                    unregisterReceiver(updateReceiver);
                    isReceiverRegistered = false;
                } catch (Throwable ignored) {
                }
            }
            releasePlayer();
        }
    }
}

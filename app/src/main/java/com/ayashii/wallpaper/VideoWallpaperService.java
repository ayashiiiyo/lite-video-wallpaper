package com.ayashii.wallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.os.Build;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.IOException;

public class VideoWallpaperService extends WallpaperService {

    private static final String TAG = "VideoWallpaperService";
    public static final String ACTION_VIDEO_UPDATED = "com.ayashii.wallpaper.ACTION_VIDEO_UPDATED";
    public static final String VIDEO_FILE_NAME = "wallpaper.mp4";

    @Override
    public Engine onCreateEngine() {
        return new VideoWallpaperEngine();
    }

    class VideoWallpaperEngine extends Engine {

        private MediaPlayer mediaPlayer;
        private boolean isVisible = false;
        private SurfaceHolder surfaceHolder;

        private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_VIDEO_UPDATED.equals(intent.getAction())) {
                    reloadVideo();
                }
            }
        };

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            this.surfaceHolder = surfaceHolder;

            IntentFilter filter = new IntentFilter(ACTION_VIDEO_UPDATED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(updateReceiver, filter);
            }
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            this.surfaceHolder = holder;
            startPlayer();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            this.surfaceHolder = holder;
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            releasePlayer();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.isVisible = visible;
            if (mediaPlayer != null) {
                if (visible) {
                    if (!mediaPlayer.isPlaying()) {
                        mediaPlayer.start();
                    }
                } else {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                    }
                }
            }
        }

        private void startPlayer() {
            File videoFile = new File(getFilesDir(), VIDEO_FILE_NAME);
            if (!videoFile.exists() || videoFile.length() == 0) {
                drawPlaceholderCanvas();
                return;
            }

            releasePlayer();

            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDisplay(surfaceHolder);
                mediaPlayer.setDataSource(videoFile.getAbsolutePath());
                mediaPlayer.setLooping(true);
                mediaPlayer.setVolume(0f, 0f);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                }

                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        if (isVisible) {
                            mp.start();
                        }
                    }
                });

                mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
                        releasePlayer();
                        drawPlaceholderCanvas();
                        return true;
                    }
                });

                mediaPlayer.prepareAsync();

            } catch (IOException e) {
                Log.e(TAG, "Failed to initialize MediaPlayer", e);
                releasePlayer();
                drawPlaceholderCanvas();
            }
        }

        private void reloadVideo() {
            if (surfaceHolder != null && surfaceHolder.getSurface().isValid()) {
                startPlayer();
            }
        }

        private void releasePlayer() {
            if (mediaPlayer != null) {
                try {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                    mediaPlayer.reset();
                    mediaPlayer.release();
                } catch (Exception e) {
                    Log.w(TAG, "Error releasing MediaPlayer", e);
                } finally {
                    mediaPlayer = null;
                }
            }
        }

        private void drawPlaceholderCanvas() {
            if (surfaceHolder == null) return;
            Canvas canvas = null;
            try {
                canvas = surfaceHolder.lockCanvas();
                if (canvas != null) {
                    canvas.drawColor(Color.parseColor("#0F1117"));
                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setColor(Color.parseColor("#9CA3AF"));
                    paint.setTextSize(36f);
                    paint.setTextAlign(Paint.Align.CENTER);

                    float x = canvas.getWidth() / 2f;
                    float y = canvas.getHeight() / 2f;
                    canvas.drawText("Pilih video di aplikasi", x, y, paint);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error drawing placeholder canvas", e);
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            try {
                unregisterReceiver(updateReceiver);
            } catch (Exception ignored) {
            }
            releasePlayer();
        }
    }
}

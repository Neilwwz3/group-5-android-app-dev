package hk.hkucs.group_6_android_app_dev.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Loads posters from assets, local files (G7 manual/TMDB posters) or https URLs
 * (search-result thumbnails) asynchronously with a small in-memory cache.
 */
public final class PosterLoader {

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final long FADE_MS = 220;
    /** Posters are shown at most a few hundred dp wide; decoding full size wastes memory. */
    private static final int TARGET_WIDTH = 500;
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(
            (int) (Runtime.getRuntime().maxMemory() / 8)) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    private PosterLoader() {
    }

    /**
     * Shows the poster image when {@code file} resolves to an asset, otherwise keeps
     * {@code fallback} (the letter placeholder) visible. Never decodes on the main thread.
     */
    public static void load(@NonNull Context context, @Nullable String file,
            @NonNull ImageView image, @NonNull View fallback) {
        image.setTag(file);
        if (file == null || file.isEmpty()) {
            image.setVisibility(View.GONE);
            fallback.setVisibility(View.VISIBLE);
            return;
        }
        Bitmap cached = CACHE.get(file);
        if (cached != null) {
            show(image, fallback, cached, false);
            return;
        }
        image.setVisibility(View.GONE);
        fallback.setVisibility(View.VISIBLE);
        IO.execute(() -> {
            Bitmap bmp = decodeSource(context.getApplicationContext(), file);
            MAIN.post(() -> {
                if (bmp != null) {
                    CACHE.put(file, bmp);
                }
                if (file.equals(image.getTag()) && bmp != null) {
                    show(image, fallback, bmp, true);
                }
            });
        });
    }

    /** Fades the poster in when it arrives after a decode, so it never pops in abruptly. */
    private static void show(ImageView image, View fallback, Bitmap bmp, boolean fade) {
        image.setImageBitmap(bmp);
        image.setVisibility(View.VISIBLE);
        fallback.setVisibility(View.GONE);
        image.animate().cancel();
        if (fade) {
            image.setAlpha(0f);
            image.animate().alpha(1f).setDuration(FADE_MS).start();
        } else {
            image.setAlpha(1f);
        }
    }

    /**
     * Resolves {@code file} against the three supported sources: https URL (search
     * thumbnails), absolute local path (manual/TMDB posters saved by G7) or assets.
     */
    @Nullable
    private static Bitmap decodeSource(Context context, String file) {
        try {
            if (file.startsWith("http://") || file.startsWith("https://")) {
                HttpURLConnection conn = (HttpURLConnection) new URL(file).openConnection();
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(15_000);
                try (InputStream in = conn.getInputStream()) {
                    return decodeSampled(readAll(in), TARGET_WIDTH);
                } finally {
                    conn.disconnect();
                }
            }
            if (file.startsWith("/")) {
                File f = new File(file);
                if (!f.isFile()) {
                    return null;
                }
                try (InputStream in = new FileInputStream(f)) {
                    return decodeSampled(readAll(in), TARGET_WIDTH);
                }
            }
            String asset = file.startsWith("sample/") ? file : "sample/posters/" + file;
            try (InputStream in = context.getAssets().open(asset)) {
                return decodeSampled(readAll(in), TARGET_WIDTH);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /** Decodes {@code data}, downsampling so the result is at most about {@code targetWidth} wide. */
    @Nullable
    public static Bitmap decodeSampled(byte[] data, int targetWidth) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
        if (bounds.outWidth <= 0) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= targetWidth) {
            sample *= 2;
        }
        options.inSampleSize = sample;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    public static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }
}

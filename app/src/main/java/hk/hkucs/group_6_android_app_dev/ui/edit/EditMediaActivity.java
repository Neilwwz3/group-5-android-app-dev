package hk.hkucs.group_6_android_app_dev.ui.edit;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import hk.hkucs.group_6_android_app_dev.R;
import hk.hkucs.group_6_android_app_dev.data.db.entity.MediaItem;
import hk.hkucs.group_6_android_app_dev.data.model.Genres;
import hk.hkucs.group_6_android_app_dev.data.model.MediaStatus;
import hk.hkucs.group_6_android_app_dev.data.model.MediaType;
import hk.hkucs.group_6_android_app_dev.ui.search.TmdbSearchActivity;
import hk.hkucs.group_6_android_app_dev.util.MediaMeta;
import hk.hkucs.group_6_android_app_dev.util.PosterLoader;
import hk.hkucs.group_6_android_app_dev.util.TmdbApi;

/** Add or edit one title. Entry contract: {@link #newIntent(Context, String)}. */
public class EditMediaActivity extends AppCompatActivity {

    public static final String EXTRA_MEDIA_ID =
            "hk.hkucs.group_6_android_app_dev.extra.MEDIA_ID";

    private EditMediaViewModel vm;
    private TextInputLayout titleLayout;
    private TextInputLayout yearLayout;
    private TextInputLayout runtimeLayout;
    private TextInputLayout episodesLayout;
    private TextInputLayout ratingLayout;
    private TextInputEditText titleField;
    private TextInputEditText originalField;
    private TextInputEditText yearField;
    private TextInputEditText directorsField;
    private TextInputEditText castField;
    private TextInputEditText tagsField;
    private TextInputEditText overviewField;
    private TextInputEditText runtimeField;
    private TextInputEditText episodesField;
    private TextInputEditText ratingField;
    private ChipGroup typeGroup;
    private ChipGroup genresGroup;
    private ChipGroup statusGroup;

    private final Set<String> selectedGenres = new LinkedHashSet<>();
    private MediaItem loaded;
    private boolean populated;
    private boolean invalidNumber;

    // Poster + online-lookup state that is not bound to a text field.
    private ImageView posterImage;
    private TextView posterFallback;
    /** null = unchanged; "" = cleared by the user; otherwise the new file path. */
    @Nullable private String pendingPoster;
    @Nullable private String pendingTmdbId;
    @Nullable private String pendingImdbId;
    private final Executor posterIo = Executors.newSingleThreadExecutor();
    private final Handler posterMain = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String> pickPoster =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    importPoster(uri);
                }
            });

    private final ActivityResultLauncher<Intent> tmdbSearch =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            startLoadingDetails(result.getData());
                        }
                    });

    public static Intent newIntent(Context context, @Nullable String mediaId) {
        if (mediaId != null && mediaId.trim().isEmpty()) {
            throw new IllegalArgumentException("mediaId must be null or nonblank");
        }
        return new Intent(context, EditMediaActivity.class).putExtra(EXTRA_MEDIA_ID, mediaId);
    }

    /** Add mode accepts an absent/null ID; edit mode needs a nonblank String. */
    @SuppressWarnings("deprecation")
    private boolean hasValidArguments() {
        try {
            Bundle extras = getIntent().getExtras();
            Object value = extras == null ? null : extras.get(EXTRA_MEDIA_ID);
            return value == null
                    || (value instanceof String && !((String) value).trim().isEmpty());
        } catch (RuntimeException invalidExtras) {
            return false;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (!hasValidArguments()) {
            Toast.makeText(this, R.string.invalid_navigation_arguments, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_media);
        View root = findViewById(R.id.edit_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.edit_toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
        String mediaId = getIntent().getStringExtra(EXTRA_MEDIA_ID);
        toolbar.setTitle(mediaId == null ? R.string.add_title : R.string.edit_title);

        titleLayout = findViewById(R.id.title_layout);
        yearLayout = findViewById(R.id.year_layout);
        runtimeLayout = findViewById(R.id.runtime_layout);
        episodesLayout = findViewById(R.id.episodes_layout);
        titleField = findViewById(R.id.edit_title);
        originalField = findViewById(R.id.edit_original_title);
        yearField = findViewById(R.id.edit_year);
        directorsField = findViewById(R.id.edit_directors);
        castField = findViewById(R.id.edit_cast);
        tagsField = findViewById(R.id.edit_tags);
        overviewField = findViewById(R.id.edit_overview);
        runtimeField = findViewById(R.id.edit_runtime);
        episodesField = findViewById(R.id.edit_episodes);
        typeGroup = findViewById(R.id.type_group);
        genresGroup = findViewById(R.id.genres_group);
        statusGroup = findViewById(R.id.status_group);
        ratingLayout = findViewById(R.id.rating_layout);
        ratingField = findViewById(R.id.edit_rating);

        buildGenreChips();
        typeGroup.setOnCheckedStateChangeListener((group, ids) -> updateEpisodesVisibility());
        findViewById(R.id.clear_rating).setOnClickListener(v -> {
            ratingField.setText(null);
            ratingLayout.setError(null);
        });
        findViewById(R.id.save_button).setOnClickListener(v -> save());

        posterImage = findViewById(R.id.edit_poster_image);
        posterFallback = findViewById(R.id.edit_poster);
        findViewById(R.id.choose_poster).setOnClickListener(v -> pickPoster.launch("image/*"));
        findViewById(R.id.remove_poster).setOnClickListener(v -> {
            pendingPoster = "";
            showPoster(null, titleField.getText().toString());
        });
        // The online lookup entry only exists in builds that carry a TMDB key.
        View searchEntry = findViewById(R.id.search_tmdb);
        searchEntry.setVisibility(TmdbApi.isEnabled() ? View.VISIBLE : View.GONE);
        searchEntry.setOnClickListener(v -> tmdbSearch.launch(TmdbSearchActivity.newIntent(this)));

        vm = new ViewModelProvider(this).get(EditMediaViewModel.class);
        vm.setMediaId(mediaId);
        vm.getItem().observe(this, item -> {
            loaded = item;
            if (item != null && !populated) {
                populated = true;
                fillForm(item);
            }
        });
    }

    private void buildGenreChips() {
        LayoutInflater inflater = getLayoutInflater();
        for (String genre : Genres.ALL) {
            Chip chip = (Chip) inflater.inflate(R.layout.chip_genre, genresGroup, false);
            chip.setText(genre);
            chip.setCheckable(true);
            chip.setTag(genre);
            chip.setOnCheckedChangeListener((button, checked) -> {
                if (checked) {
                    selectedGenres.add(genre);
                } else {
                    selectedGenres.remove(genre);
                }
            });
            genresGroup.addView(chip);
        }
    }

    private void fillForm(MediaItem item) {
        titleField.setText(item.title);
        originalField.setText(item.originalTitle);
        yearField.setText(item.year == null ? null : String.valueOf(item.year));
        checkChipByTag(typeGroup, item.type);
        checkChipByTag(statusGroup, item.status);
        applyGenres(item.genres);
        directorsField.setText(joinComma(item.directors));
        castField.setText(joinComma(item.cast));
        tagsField.setText(joinComma(item.tags));
        overviewField.setText(item.overview);
        runtimeField.setText(item.runtimeMinutes == null
                ? null : String.valueOf(item.runtimeMinutes));
        episodesField.setText(item.totalEpisodes == null
                ? null : String.valueOf(item.totalEpisodes));
        ratingField.setText(item.rating == null ? null : MediaMeta.score(item.rating));
        updateEpisodesVisibility();
        showPoster(item.poster, item.title);
    }

    private void applyGenres(List<String> genres) {
        selectedGenres.clear();
        selectedGenres.addAll(genres);
        for (int i = 0; i < genresGroup.getChildCount(); i++) {
            View child = genresGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setChecked(genres.contains(chip.getTag()));
            }
        }
    }

    /** Shows the poster (any PosterLoader source) or the letter placeholder. */
    private void showPoster(@Nullable String path, @Nullable String title) {
        PosterLoader.load(this, path, posterImage, posterFallback);
        posterFallback.setText(title == null || title.isEmpty()
                ? "?" : title.substring(0, 1).toUpperCase());
    }

    /** Copies the picked image into the app's private poster dir, downsampled. */
    private void importPoster(Uri uri) {
        posterIo.execute(() -> {
            String saved = null;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in != null) {
                    Bitmap bmp = PosterLoader.decodeSampled(PosterLoader.readAll(in), 500);
                    if (bmp != null) {
                        File dir = new File(getFilesDir(), "posters");
                        if (dir.isDirectory() || dir.mkdirs()) {
                            File out = new File(dir, "manual_" + UUID.randomUUID() + ".jpg");
                            try (FileOutputStream fos = new FileOutputStream(out)) {
                                bmp.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                            }
                            saved = out.getAbsolutePath();
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            final String path = saved;
            posterMain.post(() -> {
                if (path == null) {
                    Toast.makeText(this, R.string.poster_invalid, Toast.LENGTH_SHORT).show();
                } else {
                    discardPendingPoster();
                    pendingPoster = path;
                    showPoster(path, titleField.getText().toString());
                }
            });
        });
    }

    /** After picking a search result, fetch full details and fill the form. */
    private void startLoadingDetails(Intent data) {
        String posterUrl = data.getStringExtra(TmdbSearchActivity.EXTRA_POSTER_URL);
        int year = data.getIntExtra(TmdbSearchActivity.EXTRA_YEAR, -1);
        TmdbApi.Hit hit = new TmdbApi.Hit(
                data.getStringExtra(TmdbSearchActivity.EXTRA_TMDB_ID),
                data.getStringExtra(TmdbSearchActivity.EXTRA_MEDIA_TYPE),
                data.getStringExtra(TmdbSearchActivity.EXTRA_TITLE),
                year < 0 ? null : year,
                posterUrl);
        findViewById(R.id.search_tmdb).setEnabled(false);
        Toast.makeText(this, R.string.tmdb_loading_details, Toast.LENGTH_SHORT).show();
        TmdbApi.buildItem(this, hit, item -> {
            findViewById(R.id.search_tmdb).setEnabled(true);
            applyTmdbItem(item);
        }, () -> {
            findViewById(R.id.search_tmdb).setEnabled(true);
            Toast.makeText(this, R.string.tmdb_offline, Toast.LENGTH_LONG).show();
        });
    }

    private void applyTmdbItem(MediaItem item) {
        titleField.setText(item.title);
        originalField.setText(item.originalTitle);
        yearField.setText(item.year == null ? null : String.valueOf(item.year));
        checkChipByTag(typeGroup, item.type);
        applyGenres(item.genres);
        directorsField.setText(joinComma(item.directors));
        castField.setText(joinComma(item.cast));
        overviewField.setText(item.overview);
        runtimeField.setText(item.runtimeMinutes == null
                ? null : String.valueOf(item.runtimeMinutes));
        episodesField.setText(item.totalEpisodes == null
                ? null : String.valueOf(item.totalEpisodes));
        updateEpisodesVisibility();
        pendingTmdbId = item.tmdbId;
        pendingImdbId = item.imdbId;
        if (item.poster != null) {
            discardPendingPoster();
            pendingPoster = item.poster;
        }
        String display = pendingPoster != null ? pendingPoster
                : (loaded == null ? null : loaded.poster);
        showPoster(display, item.title);
    }

    /** Deletes a poster file created during this editing session (not the stored one). */
    private void discardPendingPoster() {
        if (pendingPoster != null && !pendingPoster.isEmpty()) {
            deletePosterFile(pendingPoster);
        }
    }

    private void deletePosterFile(String path) {
        File dir = new File(getFilesDir(), "posters");
        if (path.startsWith(dir.getAbsolutePath() + File.separator)) {
            posterIo.execute(() -> new File(path).delete());
        }
    }

    private void save() {
        String title = titleField.getText().toString().trim();
        if (title.isEmpty()) {
            titleLayout.setError(getString(R.string.title_required));
            return;
        }
        titleLayout.setError(null);

        invalidNumber = false;
        Integer year = numberOrNull(yearField, yearLayout);
        Integer runtime = numberOrNull(runtimeField, runtimeLayout);
        Integer episodes = numberOrNull(episodesField, episodesLayout);
        Float rating = ratingOrNull();
        if (invalidNumber) {
            return;
        }

        MediaItem item = new MediaItem();
        if (loaded != null) {
            item.id = loaded.id;
            item.createdAt = loaded.createdAt;
            item.source = loaded.source;
            item.poster = loaded.poster;
            item.tmdbId = loaded.tmdbId;
            item.imdbId = loaded.imdbId;
        }
        // A title picked from TMDB keeps its stable tmdb_ id so it can never
        // be stored twice (the search page blocks re-picking the same tmdbId).
        if (pendingTmdbId != null) {
            if (item.id.trim().isEmpty()) {
                item.id = "tmdb_" + pendingTmdbId;
                item.source = "tmdb";
            }
            item.tmdbId = pendingTmdbId;
            item.imdbId = pendingImdbId;
        }
        if (pendingPoster != null) {
            String previous = item.poster;
            item.poster = pendingPoster.isEmpty() ? null : pendingPoster;
            if (previous != null && !previous.equals(item.poster)) {
                deletePosterFile(previous);
            }
        }
        item.title = title;
        item.originalTitle = textOrNull(originalField);
        item.year = year;
        item.type = checkedTag(typeGroup, MediaType.MOVIE);
        item.status = checkedTag(statusGroup, MediaStatus.WANT);
        List<String> genres = new ArrayList<>();
        for (String genre : Genres.ALL) {
            if (selectedGenres.contains(genre)) {
                genres.add(genre);
            }
        }
        item.genres = genres;
        item.directors = splitComma(directorsField);
        item.cast = splitComma(castField);
        item.tags = splitComma(tagsField);
        item.overview = textOrNull(overviewField);
        item.runtimeMinutes = runtime;
        item.totalEpisodes = MediaType.SERIES.equals(item.type) ? episodes : null;
        item.rating = rating;

        vm.save(item, savedId -> finish());
    }

    private void updateEpisodesVisibility() {
        boolean series = MediaType.SERIES.equals(checkedTag(typeGroup, MediaType.MOVIE));
        episodesLayout.setVisibility(series ? View.VISIBLE : View.GONE);
    }

    /** Parses the 0–10 rating with one-decimal precision; blank means not rated. */
    @Nullable
    private Float ratingOrNull() {
        String text = ratingField.getText().toString().trim();
        if (text.isEmpty()) {
            ratingLayout.setError(null);
            return null;
        }
        try {
            float value = Float.parseFloat(text);
            if (value < 0f || value > 10f) {
                throw new NumberFormatException();
            }
            ratingLayout.setError(null);
            return Math.round(value * 10f) / 10f;
        } catch (NumberFormatException e) {
            ratingLayout.setError(getString(R.string.invalid_rating));
            invalidNumber = true;
            return null;
        }
    }

    @Nullable
    private Integer numberOrNull(TextInputEditText field, TextInputLayout layout) {
        String text = field.getText().toString().trim();
        if (text.isEmpty()) {
            layout.setError(null);
            return null;
        }
        try {
            int value = Integer.parseInt(text);
            layout.setError(null);
            return value;
        } catch (NumberFormatException e) {
            layout.setError(getString(R.string.invalid_number));
            invalidNumber = true;
            return null;
        }
    }

    @Nullable
    private static String textOrNull(TextInputEditText field) {
        String text = field.getText().toString().trim();
        return text.isEmpty() ? null : text;
    }

    private static List<String> splitComma(TextInputEditText field) {
        List<String> parts = new ArrayList<>();
        String text = field.getText().toString();
        for (String part : text.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
            }
        }
        return parts;
    }

    private static String joinComma(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(value);
        }
        return sb.toString();
    }

    private static String checkedTag(ChipGroup group, String fallback) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof Chip && ((Chip) child).isChecked()) {
                Object tag = child.getTag();
                if (tag instanceof String) {
                    return (String) tag;
                }
            }
        }
        return fallback;
    }

    private static void checkChipByTag(ChipGroup group, String tag) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof Chip) {
                ((Chip) child).setChecked(tag != null && tag.equals(child.getTag()));
            }
        }
    }
}

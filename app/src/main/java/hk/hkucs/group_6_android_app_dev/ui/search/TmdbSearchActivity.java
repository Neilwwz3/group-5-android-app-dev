package hk.hkucs.group_6_android_app_dev.ui.search;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import hk.hkucs.group_6_android_app_dev.R;
import hk.hkucs.group_6_android_app_dev.data.db.entity.MediaItem;
import hk.hkucs.group_6_android_app_dev.data.repository.MediaRepository;
import hk.hkucs.group_6_android_app_dev.util.TmdbApi;

/**
 * Look titles up on TMDB. Picking one returns the hit to the caller, which
 * fetches the full details. Titles already stored (matched by tmdbId, sample data
 * included) cannot be picked again, so one title can never be added twice.
 */
public class TmdbSearchActivity extends AppCompatActivity {

    public static final String EXTRA_TMDB_ID =
            "hk.hkucs.group_6_android_app_dev.extra.TMDB_ID";
    public static final String EXTRA_MEDIA_TYPE =
            "hk.hkucs.group_6_android_app_dev.extra.MEDIA_TYPE";
    public static final String EXTRA_TITLE =
            "hk.hkucs.group_6_android_app_dev.extra.TITLE";
    public static final String EXTRA_YEAR =
            "hk.hkucs.group_6_android_app_dev.extra.YEAR";
    public static final String EXTRA_POSTER_URL =
            "hk.hkucs.group_6_android_app_dev.extra.POSTER_URL";

    private TmdbSearchViewModel vm;
    private TmdbResultAdapter adapter;
    private TextInputEditText queryField;
    private final Set<String> storedTmdbIds = new HashSet<>();

    public static Intent newIntent(Context context) {
        return new Intent(context, TmdbSearchActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_tmdb_search);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.search_root), (view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.search_toolbar);
        toolbar.setTitle(R.string.tmdb_search_title);
        toolbar.setNavigationOnClickListener(v -> finish());

        queryField = findViewById(R.id.search_query);
        queryField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                closeKeyboard();
                runSearch();
                return true;
            }
            return false;
        });
        findViewById(R.id.search_button).setOnClickListener(v -> {
            closeKeyboard();
            runSearch();
        });

        adapter = new TmdbResultAdapter(hit -> {
            if (storedTmdbIds.contains(hit.tmdbId)) {
                Toast.makeText(this, R.string.already_in_library, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent result = new Intent()
                    .putExtra(EXTRA_TMDB_ID, hit.tmdbId)
                    .putExtra(EXTRA_MEDIA_TYPE, hit.mediaType)
                    .putExtra(EXTRA_TITLE, hit.title)
                    .putExtra(EXTRA_POSTER_URL, hit.posterUrl);
            if (hit.year != null) {
                result.putExtra(EXTRA_YEAR, hit.year);
            }
            setResult(RESULT_OK, result);
            finish();
        });
        RecyclerView list = findViewById(R.id.search_results);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        // Dedup source: every stored title's tmdbId, sample data included.
        MediaRepository.get(this).observeAllMedia().observe(this, items -> {
            storedTmdbIds.clear();
            for (MediaItem item : items) {
                if (item.tmdbId != null && !item.tmdbId.isEmpty()) {
                    storedTmdbIds.add(item.tmdbId);
                }
            }
        });

        vm = new ViewModelProvider(this).get(TmdbSearchViewModel.class);
        TextView statusView = findViewById(R.id.search_status);
        vm.getStatus().observe(this, status -> {
            switch (status) {
                case LOADING:
                    statusView.setVisibility(View.VISIBLE);
                    statusView.setText(R.string.tmdb_searching);
                    break;
                case OFFLINE:
                    statusView.setVisibility(View.VISIBLE);
                    statusView.setText(R.string.tmdb_offline);
                    adapter.submit(null);
                    break;
                case EMPTY:
                    statusView.setVisibility(View.VISIBLE);
                    statusView.setText(R.string.tmdb_no_results);
                    adapter.submit(null);
                    break;
                case IDLE:
                case RESULTS:
                default:
                    statusView.setVisibility(View.GONE);
                    break;
            }
        });
        vm.getResults().observe(this, adapter::submit);
    }

    private void runSearch() {
        String query = queryField.getText() == null
                ? "" : queryField.getText().toString().trim();
        if (!query.isEmpty()) {
            vm.search(query);
        }
    }

    private void closeKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(queryField.getWindowToken(), 0);
        }
    }
}

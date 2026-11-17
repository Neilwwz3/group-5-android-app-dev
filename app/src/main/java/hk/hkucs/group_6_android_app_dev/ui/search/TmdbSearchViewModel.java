package hk.hkucs.group_6_android_app_dev.ui.search;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import hk.hkucs.group_6_android_app_dev.util.TmdbApi;

/** Runs TMDB searches off the main thread and exposes the page state. */
public class TmdbSearchViewModel extends AndroidViewModel {

    public enum Status { IDLE, LOADING, RESULTS, EMPTY, OFFLINE }

    private final MutableLiveData<Status> status = new MutableLiveData<>(Status.IDLE);
    private final MutableLiveData<List<TmdbApi.Hit>> results = new MutableLiveData<>();

    public TmdbSearchViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Status> getStatus() {
        return status;
    }

    public LiveData<List<TmdbApi.Hit>> getResults() {
        return results;
    }

    public void search(String query) {
        status.setValue(Status.LOADING);
        TmdbApi.search(query, hits -> {
            results.setValue(hits);
            status.setValue(hits.isEmpty() ? Status.EMPTY : Status.RESULTS);
        }, () -> status.setValue(Status.OFFLINE));
    }
}

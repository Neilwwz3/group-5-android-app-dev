package hk.hkucs.group_6_android_app_dev.ui.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import hk.hkucs.group_6_android_app_dev.R;
import hk.hkucs.group_6_android_app_dev.data.model.MediaType;
import hk.hkucs.group_6_android_app_dev.util.PosterLoader;
import hk.hkucs.group_6_android_app_dev.util.TmdbApi;

/** Search results list: poster, title and "year · type" per row. */
public class TmdbResultAdapter extends RecyclerView.Adapter<TmdbResultAdapter.Holder> {

    public interface OnPick {
        void onPick(TmdbApi.Hit hit);
    }

    private final List<TmdbApi.Hit> items = new ArrayList<>();
    private final OnPick onPick;

    TmdbResultAdapter(OnPick onPick) {
        this.onPick = onPick;
    }

    void submit(@Nullable List<TmdbApi.Hit> hits) {
        items.clear();
        if (hits != null) {
            items.addAll(hits);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tmdb_result, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        TmdbApi.Hit hit = items.get(position);
        holder.title.setText(hit.title);
        StringBuilder meta = new StringBuilder();
        if (hit.year != null) {
            meta.append(hit.year).append(" · ");
        }
        meta.append(holder.itemView.getContext().getString(MediaType.MOVIE.equals(hit.mediaType)
                ? R.string.type_movie : R.string.type_series));
        holder.meta.setText(meta);
        PosterLoader.load(holder.itemView.getContext(), hit.posterUrl,
                holder.poster, holder.posterFallback);
        holder.posterFallback.setText(hit.title.isEmpty()
                ? "?" : hit.title.substring(0, 1).toUpperCase());
        holder.itemView.setOnClickListener(v -> onPick.onPick(hit));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView meta;
        final ImageView poster;
        final TextView posterFallback;

        Holder(@NonNull View view) {
            super(view);
            title = view.findViewById(R.id.result_title);
            meta = view.findViewById(R.id.result_meta);
            poster = view.findViewById(R.id.result_poster_image);
            posterFallback = view.findViewById(R.id.result_poster);
        }
    }
}

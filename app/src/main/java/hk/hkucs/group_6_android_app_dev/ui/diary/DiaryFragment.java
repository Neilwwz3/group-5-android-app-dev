package hk.hkucs.group_6_android_app_dev.ui.diary;

import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import hk.hkucs.group_6_android_app_dev.MainActivity;
import hk.hkucs.group_6_android_app_dev.R;
import hk.hkucs.group_6_android_app_dev.data.model.RecordWithMedia;
import hk.hkucs.group_6_android_app_dev.ui.detail.MediaDetailActivity;

/**
 * G4 Diary tab: last year's heatmap above a timeline of watch records grouped by date.
 * Double-tapping the Diary tab jumps back to the top and reloads the page.
 */
public class DiaryFragment extends Fragment implements MainActivity.OnTabDoubleTapListener {

    private DiaryViewModel viewModel;
    private DiaryAdapter adapter;
    private RecyclerView list;
    private LinearLayoutManager layoutManager;
    /** Slide the timeline in when the refreshed rows arrive. */
    private boolean animateNextRows;

    public DiaryFragment() {
        super(R.layout.fragment_diary);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        viewModel = new ViewModelProvider(this).get(DiaryViewModel.class);

        adapter = new DiaryAdapter(new DiaryAdapter.Listener() {
            @Override
            public void onDayClick(@NonNull String date, int count) {
                viewModel.selectDate(date);
                scrollToDate(date);
            }

            @Override
            public void onRecordClick(@NonNull RecordWithMedia record) {
                startActivity(MediaDetailActivity.newIntent(requireContext(),
                        record.record.mediaId));
            }
        });

        list = view.findViewById(R.id.diary_page);
        layoutManager = new LinearLayoutManager(requireContext());
        list.setLayoutManager(layoutManager);
        list.setAdapter(adapter);

        viewModel.getRows().observe(getViewLifecycleOwner(), rows -> {
            adapter.setRows(rows);
            if (animateNextRows) {
                animateNextRows = false;
                // Same entrance as the Library list after a filter change.
                list.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(requireContext(),
                        R.anim.list_enter));
                list.scheduleLayoutAnimation();
            }
        });
        viewModel.getDailyCounts().observe(getViewLifecycleOwner(), adapter::setDailyCounts);
        viewModel.getSelectedDate().observe(getViewLifecycleOwner(), adapter::setSelectedDate);
        viewModel.getToday().observe(getViewLifecycleOwner(), adapter::setToday);
    }

    @Override
    public void onDestroyView() {
        list = null;
        super.onDestroyView();
    }

    /** Back to the top at once (a long timeline would take seconds to scroll), then reload. */
    @Override
    public void onTabDoubleTap() {
        if (list == null) {
            return;
        }
        list.stopScroll();
        layoutManager.scrollToPositionWithOffset(0, 0);
        animateNextRows = true;
        adapter.playRefresh();
        viewModel.refresh();
    }

    private void scrollToDate(String date) {
        int position = adapter.positionOfDate(date);
        if (position < 0) {
            Toast.makeText(requireContext(),
                    getString(R.string.diary_no_entries_on, DiaryAdapter.longDate(date)),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        layoutManager.scrollToPositionWithOffset(position, 0);
    }
}

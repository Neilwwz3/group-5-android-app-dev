package hk.hkucs.group_6_android_app_dev.ui.diary;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.doubleClick;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.SystemClock;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import hk.hkucs.group_6_android_app_dev.MainActivity;
import hk.hkucs.group_6_android_app_dev.R;
import hk.hkucs.group_6_android_app_dev.data.repository.MediaRepository;
import hk.hkucs.group_6_android_app_dev.data.repository.SampleDataLoader;

@RunWith(AndroidJUnit4.class)
public class DiaryScreenTest {
    /** Longer than the double-tap window, so the next tap counts on its own. */
    private static final long PAUSE_MS = 700;

    private Context context;

    @Before
    public void setUp() throws InterruptedException {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        clearData();
        CountDownLatch loaded = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();
        SampleDataLoader.load(context, loaded::countDown, exception -> {
            error.set(exception);
            loaded.countDown();
        });
        assertTrue(loaded.await(10, TimeUnit.SECONDS));
        assertNull(error.get());
    }

    @After
    public void tearDown() throws InterruptedException {
        clearData();
    }

    @Test
    public void doubleTappingTheDiaryTabGoesBackToTheTopAndClearsThePickedDay() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.nav_diary)).perform(click());
            waitForDiary();
            scenario.onActivity(activity -> {
                // As if the user picked Feb 22 on the heatmap and read down the timeline.
                diaryViewModel(activity).selectDate("2026-02-22");
                RecyclerView list = activity.findViewById(R.id.diary_page);
                list.scrollToPosition(list.getAdapter().getItemCount() - 1);
            });
            waitForDiary();
            assertTrue(firstVisible(scenario) > 0);

            // One tap on the open tab keeps the reader's place.
            SystemClock.sleep(PAUSE_MS);
            onView(withId(R.id.nav_diary)).perform(click());
            waitForDiary();
            assertTrue(firstVisible(scenario) > 0);

            SystemClock.sleep(PAUSE_MS);
            onView(withId(R.id.nav_diary)).perform(doubleClick());
            waitForDiary();
            assertEquals(0, firstVisible(scenario));
            scenario.onActivity(activity ->
                    assertNull(diaryViewModel(activity).getSelectedDate().getValue()));
        }
    }

    @Test
    public void doubleTappingDiaryFromAnotherTabOpensItAtTheTop() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.nav_diary)).perform(click());
            waitForDiary();
            scenario.onActivity(activity -> {
                RecyclerView list = activity.findViewById(R.id.diary_page);
                list.scrollToPosition(list.getAdapter().getItemCount() - 1);
            });
            SystemClock.sleep(PAUSE_MS);
            onView(withId(R.id.nav_home)).perform(click());
            SystemClock.sleep(PAUSE_MS);
            onView(withId(R.id.nav_diary)).perform(doubleClick());
            waitForDiary();
            onView(withId(R.id.diary_page)).check(matches(isDisplayed()));
            assertEquals(0, firstVisible(scenario));
        }
    }

    private static DiaryViewModel diaryViewModel(MainActivity activity) {
        Fragment page = activity.getSupportFragmentManager().getPrimaryNavigationFragment();
        assertTrue(page instanceof DiaryFragment);
        return new ViewModelProvider(page).get(DiaryViewModel.class);
    }

    private static int firstVisible(ActivityScenario<MainActivity> scenario) {
        AtomicInteger first = new AtomicInteger(-1);
        scenario.onActivity(activity -> {
            RecyclerView list = activity.findViewById(R.id.diary_page);
            first.set(((LinearLayoutManager) list.getLayoutManager())
                    .findFirstVisibleItemPosition());
        });
        return first.get();
    }

    /** Lets the Room queries come back and the list lay out. */
    private static void waitForDiary() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        SystemClock.sleep(500);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private void clearData() throws InterruptedException {
        CountDownLatch cleared = new CountDownLatch(1);
        MediaRepository.get(context).clearAll(cleared::countDown);
        assertTrue(cleared.await(10, TimeUnit.SECONDS));
    }
}

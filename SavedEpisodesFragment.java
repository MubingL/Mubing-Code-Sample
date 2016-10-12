sodpackage com.intrepid.wbur.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.intrepid.wbur.DeleteEpisodeDelegate;
import com.intrepid.wbur.PlaybackQueueManager;
import com.intrepid.wbur.R;
import com.intrepid.wbur.adapters.SavedEpisodesAdapter;
import com.intrepid.wbur.analytics.AnalyticsKeys;
import com.intrepid.wbur.analytics.AnalyticsScreen;
import com.intrepid.wbur.callbacks.ProgramCallback;
import com.intrepid.wbur.contracts.SavedEpisodesContract;
import com.intrepid.wbur.database.WburDatabaseManager;
import com.intrepid.wbur.database.loaders.DBEpisodesLoader;
import com.intrepid.wbur.database.loaders.SavedEpisodesLoader;
import com.intrepid.wbur.models.Episode;

import com.intrepid.wbur.presenters.PresenterConfiguration;
import com.intrepid.wbur.presenters.SavedEpisodesPresenter;
import com.intrepid.wbur.views.ConfirmationDialog;
import com.intrepid.wbur.views.RightLeftSwipeableViewContainer;
import com.intrepid.wbur.views.SavedEpisodeView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SavedEpisodesFragment extends WburBaseMvpFragment<SavedEpisodesContract.Presenter> implements LoaderManager.LoaderCallbacks<Cursor>,
        RightLeftSwipeableViewContainer.OnHorizontalSwipeListener<Episode>, ConfirmationDialog.DialogHost,
        AnalyticsScreen, SavedEpisodeView.ClickListener, SavedEpisodesContract.View, DeleteEpisodeDelegate.Callback {
    public static final String TAG = SavedEpisodesFragment.class.getName();

    @BindView(R.id.saved_episodes_list_view)
    ListView saveEpisodesListView;
    @BindView(R.id.about_save_layout)
    View aboutSaveLayout;

    private Snackbar episodeUnsavedSnackbar;

    private SavedEpisodesAdapter savedEpisodeAdapter;
    private SaveEpisodesCallback saveEpisodesCallback;
    private ProgramCallback programCallback;

    public SavedEpisodesFragment() {
    }

    @NonNull
    @Override
    public SavedEpisodesContract.Presenter createPresenter(PresenterConfiguration configuration) {
        PlaybackQueueManager playbackQueueManager = PlaybackQueueManager.getInstance();
        WburDatabaseManager wburDatabaseManager = WburDatabaseManager.getInstance(getContext());
        DeleteEpisodeDelegate deleteEpisodeDelegate = new DeleteEpisodeDelegate((AppCompatActivity)getActivity(), this);
        DeletedEpisode deletedEpisode = new DeletedEpisode();

        return new SavedEpisodesPresenter(this, configuration, playbackQueueManager, programCallback, wburDatabaseManager, deleteEpisodeDelegate, deletedEpisode);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_saved_episodes, container, false);

        setTitle(R.string.save_episodes_title);

        ButterKnife.bind(this, rootView);

        initEpisodeUnsavedSnackbar();

        savedEpisodeAdapter = new SavedEpisodesAdapter(getActivity(), presenter.getEpisodes(), this, this);
        saveEpisodesListView.setAdapter(savedEpisodeAdapter);

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        saveEpisodesCallback = (SaveEpisodesCallback) context;
        programCallback = (ProgramCallback) context;

        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new SavedEpisodesLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        List<Episode> episodes = ((DBEpisodesLoader) loader).getEpisodes();
        presenter.onLoadFinished(episodes);

        getLoaderManager().destroyLoader(0);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onSwipe(int dx) {
    }

    @Override
    public void onSwipeAnimated(int endSize) {
    }

    @Override
    public void onSwipeDelete(Episode item) {
        presenter.swipeToDelete(item);
        updatedEpisodeListView();
    }

    @Override
    public void onCollapseAnimEnded(Episode item) {
        presenter.deleteEpisodeFromList(item);
        episodeUnsavedSnackbar.show();
    }

    @Override
    public AnalyticsKeys.ScreenName getAnalyticsScreenName() {
        return AnalyticsKeys.ScreenName.SAVED_EPISODES;
    }

    @OnClick(R.id.browse_shows_button)
    void openListenScreen() {
        saveEpisodesCallback.showListenScreen();
    }

    @Override
    public void showDeleteSavedContentDialog(Episode episode) {
        ConfirmationDialog dialog = ConfirmationDialog.newInstance(episode.getId(),
                                                                   R.string.dialog_remove_episode_title,
                                                                   R.string.dialog_remove_episode_body);
        dialog.show(getActivity().getSupportFragmentManager(), null);
        dialog.setHost(this);
    }

    @Override
    public void onPositiveAction(ConfirmationDialog dialog, String contentId) {
        presenter.onPositiveAction(contentId);
        dialog.dismiss();
    }

    @Override
    public void onNegativeAction(ConfirmationDialog dialog, String contentId) {
        dialog.dismiss();
    }

    private void initEpisodeUnsavedSnackbar() {
        episodeUnsavedSnackbar = Snackbar.make(saveEpisodesListView,
                                               "Removed from your saved list",
                                               Snackbar.LENGTH_LONG)
                .setAction("UNDO", presenter.getDeletedEpisodeTypeEpisode())
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        presenter.onSnackBarDismissed();
                    }
                });
        View snackbarView = episodeUnsavedSnackbar.getView();
        snackbarView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.snackbar_blue));
        int snackbarHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                                             56,
                                                             getResources().getDisplayMetrics());
        snackbarView.setMinimumHeight(snackbarHeight);
    }

    @Override
    public void configureSavedEpisodesScreen(boolean hasSavedEpisodes) {
        aboutSaveLayout.setVisibility(hasSavedEpisodes ? View.GONE : View.VISIBLE);
        saveEpisodesListView.setVisibility(hasSavedEpisodes ? View.VISIBLE : View.GONE);
    }

    @Override
    public void updateSavedEpisodesListView(List<Episode> episodes) {
        presenter.addAllEpisodes(episodes);
        savedEpisodeAdapter.notifyDataSetChanged();
    }

    @Override
    public void updatedEpisodeListView() {
        savedEpisodeAdapter.notifyDataSetChanged();
    }

    @Override
    public void updateEpisodePlayingStatus(Episode episode, boolean isPlaying, boolean isPlayingAlive) {
        savedEpisodeAdapter.updatePlayingEpisode(episode, isPlaying, isPlayingAlive);
    }

    @Override
    public void refreshEpisodeView(int position) {
        int firstVisiblePosition = saveEpisodesListView.getFirstVisiblePosition();
        if (position >= firstVisiblePosition && position <= saveEpisodesListView.getLastVisiblePosition()) {
            View child = saveEpisodesListView.getChildAt(position - firstVisiblePosition);
            saveEpisodesListView.getAdapter().getView(position, child, saveEpisodesListView);
        }
    }

    @Override
    public void updateUi(int position) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshEpisodeView(position);
            }
        });
    }

    @Override
    public void onEpisodeClicked(Episode episode) {
        presenter.onEpisodeClicked(episode);
    }

    @Override
    public void onMoreInfoClicked(Episode episode) {
        presenter.onShowEpisodeDetailClick(episode);
    }

    @Override
    public void onEpisodeDeleted(Episode episode) {
        updateUi(presenter.getEpisodePosition(episode.getId()));
    }

    public class DeletedEpisode implements View.OnClickListener {
        private Episode deleted;
        private int position;

        @Override
        public void onClick(View v) {
            if (deleted != null) {
                presenter.getEpisodes().add(position, deleted);
                updatedEpisodeListView();
                clear();
            }
        }

        public void clear() {
            deleted = null;
        }

        public Episode getDeleted() {
            return deleted;
        }

        public void setDeleted(Episode deleted, int position) {
            this.deleted = deleted;
            this.position = position;
        }
    }

    public interface SaveEpisodesCallback {
        void showListenScreen();
    }
}

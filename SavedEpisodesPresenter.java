package com.intrepid.wbur.presenters;

import com.intrepid.wbur.DeleteEpisodeDelegate;
import com.intrepid.wbur.PlaybackQueueManager;
import com.intrepid.wbur.application.WburApplication;
import com.intrepid.wbur.base.BasePresenterImpl;
import com.intrepid.wbur.callbacks.ProgramCallback;
import com.intrepid.wbur.contracts.SavedEpisodesContract;
import com.intrepid.wbur.database.WburDatabaseManager;
import com.intrepid.wbur.events.DeleteEpisodeRequestEvent;
import com.intrepid.wbur.events.EpisodeDeletedEvent;
import com.intrepid.wbur.events.EpisodeDownloadProgressEvent;
import com.intrepid.wbur.events.OttoEvent;
import com.intrepid.wbur.fragments.SavedEpisodesFragment;
import com.intrepid.wbur.models.DownloadStatus;
import com.intrepid.wbur.models.Episode;
import com.intrepid.wbur.utils.StringUtils;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

public class SavedEpisodesPresenter extends BasePresenterImpl<SavedEpisodesContract.View> implements SavedEpisodesContract.Presenter {
    private PlaybackQueueManager playbackQueueManager;
    private List<Episode> savedEpisodesList = new ArrayList<>();
    private SavedEpisodesFragment.DeletedEpisode deletedEpisode;
    private ProgramCallback programCallback;
    private WburDatabaseManager wburDatabaseManager;
    private DeleteEpisodeDelegate deleteEpisodeDelegate;
    private boolean isRegistered;

    public SavedEpisodesPresenter(SavedEpisodesContract.View view,
                                  PresenterConfiguration configuration,
                                  PlaybackQueueManager playbackQueueManager,
                                  ProgramCallback programCallback,
                                  WburDatabaseManager wburDatabaseManager,
                                  DeleteEpisodeDelegate deleteEpisodeDelegate,
                                  SavedEpisodesFragment.DeletedEpisode deletedEpisode) {
        super(view, configuration);
        this.playbackQueueManager = playbackQueueManager;
        this.programCallback = programCallback;
        this.wburDatabaseManager = wburDatabaseManager;
        this.deleteEpisodeDelegate = deleteEpisodeDelegate;
        this.deletedEpisode = deletedEpisode;
    }

    @Override
    protected void onBindView() {
        super.onBindView();
        if (!isRegistered) {
            WburApplication.BUS.register(this);
            isRegistered = true;
        }
    }

    @Override
    public void unbindView() {
        super.unbindView();

        WburApplication.BUS.unregister(this);
        isRegistered = false;
        clearUnsavedEpisode();
    }

    @Subscribe
    public void onEvent(OttoEvent.PlaybackStatusEvent event) {
        view.updateEpisodePlayingStatus(getPlayingEpisode(event), isPlaying(event), isPlayerAlive(event));
    }

    @Subscribe
    public void deleteEpisodeRequested(DeleteEpisodeRequestEvent event) {
        view.showDeleteSavedContentDialog(event.getEpisode());
    }

    @Subscribe
    public void episodeDownloadProgressUpdate(EpisodeDownloadProgressEvent event) {
        final Episode episode = getEpisode(event.getEpisodeId()); // episode is in list or deleted episode

        if (episode != null) {
            if (event.getDownloadStatus() == DownloadStatus.PENDING) {
                analyticsLogger.logDownloadEpisodeToggled(episode, true);
            }
            episode.setDownloadStatus(event.getDownloadStatus());
            episode.setDownloadProgress(event.getProgress());

            /*
             * episode that has been deleted remains in list of saved episodes until the end of the listView
             * cell-collapse animation for this episode; do not update view during this period
             */
            if (!episode.equals(getDeletedEpisode())) {
                view.updateUi(getEpisodePosition(episode.getId()));
            }
        }
    }

    @Subscribe
    public void episodeDeleted(EpisodeDeletedEvent event) {
        int position = getEpisodePosition(event.getEpisode().getId());

        if (position >= 0) {
            Episode episode = savedEpisodesList.get(position);
            analyticsLogger.logDownloadEpisodeToggled(episode, false);
            episode.setDownloadStatus(DownloadStatus.REMOTE_ONLY);
            view.updateUi(getEpisodePosition(episode.getId()));
        }
    }

    private boolean isPlaying(OttoEvent.PlaybackStatusEvent event) {
        return event.isPlaying();
    }

    private boolean isPlayerAlive(OttoEvent.PlaybackStatusEvent event) {
        return event.isAlive();
    }

    private Episode getPlayingEpisode(OttoEvent.PlaybackStatusEvent event) {
        return event.getCurrentEpisode();
    }

    public Episode getEpisode(String episodeId) {
        int position = getEpisodePosition(episodeId);
        if (position >= 0) {
            return savedEpisodesList.get(position);
        } else {
            Episode deleted = deletedEpisode.getDeleted();
            return (deleted != null && StringUtils.equals(episodeId, deleted.getId())) ? deleted : null;
        }
    }

    @Override
    public int getEpisodePosition(String episodeId) {
        int episodeCount = savedEpisodesList.size();
        for (int i = 0; i < episodeCount; i++) {
            if (StringUtils.equals(savedEpisodesList.get(i).getId(), episodeId)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onLoadFinished(List<Episode> loadedSavedEpisodes) {
        if (savedEpisodesList != null) {
            savedEpisodesList.clear();
        }

        if (loadedSavedEpisodes != null) {
            view.updateSavedEpisodesListView(loadedSavedEpisodes);
        }

        boolean hasSavedEpisodes = loadedSavedEpisodes != null && loadedSavedEpisodes.size() > 0;
        view.configureSavedEpisodesScreen(hasSavedEpisodes);
    }

    @Override
    public List<Episode> getEpisodes() {
        return savedEpisodesList;
    }

    @Override
    public void deleteEpisodeFromList(Episode episode) {
        savedEpisodesList.remove(getEpisodePosition(episode.getId()));
    }

    @Override
    public void onShowEpisodeDetailClick(Episode episode) {
        programCallback.showEpisodeDetail(episode);
    }

    @Override
    public void onEpisodeClicked(Episode episode) {
        int index = savedEpisodesList.indexOf(episode);
        List<Episode> queueList = savedEpisodesList.subList(index + 1, savedEpisodesList.size());
        playbackQueueManager.queueUpSavedEpisodes(queueList);
        programCallback.playProgram(episode);
    }

    private Episode getDeletedEpisode() {
        return deletedEpisode.getDeleted();
    }

    @Override
    public SavedEpisodesFragment.DeletedEpisode getDeletedEpisodeTypeEpisode() {
        return deletedEpisode;
    }

    private void clearUnsavedEpisode() {
        SavedEpisodesFragment.DeletedEpisode deletedEpisode = getDeletedEpisodeTypeEpisode();
        Episode deleted = deletedEpisode.getDeleted();
        deletedEpisode.clear();
        if (deleted != null) {
            wburDatabaseManager.setEpisodeSaved(deleted, false);
            wburDatabaseManager.saveEpisode(deleted);
            permanentlyDeleteEpisode(deleted);
        }
    }

    @Override
    public void onPositiveAction(String contentId) {
        int position = getEpisodePosition(contentId);
        if (position >= 0) {
            permanentlyDeleteEpisode(savedEpisodesList.get(position));
        }
    }

    @Override
    public void addAllEpisodes(List<Episode> episodes) {
        getEpisodes().addAll(episodes);
    }

    @Override
    public void swipeToDelete(Episode episode) {
        clearUnsavedEpisode();
        int position = getEpisodePosition(episode.getId());
        deletedEpisode.setDeleted(episode, position);
    }

    @Override
    public void onSnackBarDismissed() {
        clearUnsavedEpisode();
        if (getEpisodes().isEmpty()) {
            view.configureSavedEpisodesScreen(false);
        }
    }

    private void permanentlyDeleteEpisode(Episode episode) {
        deleteEpisodeDelegate.deleteFromLocalStorage(episode);
    }
}

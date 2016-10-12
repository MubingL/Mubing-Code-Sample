epackage com.intrepid.wbur.contracts;

import com.intrepid.wbur.base.BasePresenter;
import com.intrepid.wbur.base.BaseView;
import com.intrepid.wbur.events.EpisodeDeletedEvent;
import com.intrepid.wbur.events.EpisodeDownloadProgressEvent;
import com.intrepid.wbur.events.OttoEvent;
import com.intrepid.wbur.fragments.SavedEpisodesFragment;
import com.intrepid.wbur.models.Episode;

import java.util.List;

public class SavedEpisodesContract {
    public interface View extends BaseView {
        void refreshEpisodeView(int position);

        void configureSavedEpisodesScreen(boolean hasSavedEpisodes);

        void updatedEpisodeListView();

        void updateSavedEpisodesListView(List<Episode> episodes);

        void updateUi(int position);

        void showDeleteSavedContentDialog(Episode episode);

        void updateEpisodePlayingStatus(Episode episode, boolean isPlaying, boolean isPlayingAlive);
    }

    public interface Presenter extends BasePresenter<View> {
        List<Episode> getEpisodes();

        void onLoadFinished(List<Episode> loadedSavedEpisodeList);

        void swipeToDelete(Episode episode);

        void deleteEpisodeFromList(Episode episode);

        void onPositiveAction(String contentId);

        SavedEpisodesFragment.DeletedEpisode getDeletedEpisodeTypeEpisode();

        void addAllEpisodes(List<Episode> episodes);

        void onEpisodeClicked(Episode episode);

        void onShowEpisodeDetailClick(Episode episode);

        int getEpisodePosition(String episodeId);

        void onSnackBarDismissed();
    }
}

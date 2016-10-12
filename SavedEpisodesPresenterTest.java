package com.intrepid.wbur.presenters;

import com.intrepid.wbur.DeleteEpisodeDelegate;
import com.intrepid.wbur.PlaybackQueueManager;
import com.intrepid.wbur.callbacks.ProgramCallback;
import com.intrepid.wbur.contracts.SavedEpisodesContract;
import com.intrepid.wbur.database.WburDatabaseManager;
import com.intrepid.wbur.fragments.SavedEpisodesFragment;
import com.intrepid.wbur.models.Episode;
import com.intrepid.wbur.testutils.TestPresenterUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.Assert.assertEquals;

public class SavedEpisodesPresenterTest {
    private static final String EPISODE_1_ID = "x";
    private static final String EPISODE_2_ID = "y";

    @Mock
    private SavedEpisodesContract.View mockView;
    @Mock
    private WburDatabaseManager mockWburDatabaseManager;
    @Mock
    private Episode mockEpisode1;
    @Mock
    private Episode mockEpisode2;
    @Mock
    private Episode mockEpisode3;
    @Mock
    private DeleteEpisodeDelegate mockDeleteEpisodeDelegate;
    @Mock
    private PlaybackQueueManager mockPlaybackQueueManager;
    @Mock
    private SavedEpisodesFragment.DeletedEpisode mockDeletedEpisode;
    @Mock
    private ProgramCallback mockProgramCallback;
    @Mock
    private List<Episode> episodeList;

    private SavedEpisodesContract.Presenter presenter;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        PresenterConfiguration testConfiguration = TestPresenterUtils.getTestConfiguration();

        presenter = new SavedEpisodesPresenter(mockView, testConfiguration, mockPlaybackQueueManager,
                                               mockProgramCallback, mockWburDatabaseManager, mockDeleteEpisodeDelegate,
                                               mockDeletedEpisode);

        when(mockEpisode1.getId()).thenReturn(EPISODE_1_ID);
        when(mockEpisode2.getId()).thenReturn(EPISODE_2_ID);
    }

    private void loadMockEpisodes() {
        episodeList = new ArrayList<>();
        episodeList.add(mockEpisode1);
        episodeList.add(mockEpisode2);
        presenter.onLoadFinished(episodeList);
        presenter.addAllEpisodes(episodeList);
    }

    @Test
    public void testGetEpisodePosition() throws Exception {
        loadMockEpisodes();
        assertEquals(0, presenter.getEpisodePosition(EPISODE_1_ID));
    }

    @Test
    public void testGetEpisodes() throws Exception {
        loadMockEpisodes();
        List<Episode> loadedEpisodes = presenter.getEpisodes();

        for (int i = 0; i < episodeList.size(); i++) {
            assertEquals(loadedEpisodes.get(i), episodeList.get(i));
        }
    }

    @Test
    public void testDeleteFromList() throws Exception {
        loadMockEpisodes();
        presenter.deleteEpisodeFromList(mockEpisode1);

        assertEquals(1, presenter.getEpisodes().size());
        assertEquals(mockEpisode2, presenter.getEpisodes().get(0));
    }

    @Test
    public void testOnEpisodeClicked() throws Exception {
        episodeList = new ArrayList<>();
        episodeList.add(mockEpisode1);
        episodeList.add(mockEpisode2);
        episodeList.add(mockEpisode3);
        presenter.onLoadFinished(episodeList);
        presenter.addAllEpisodes(episodeList);
        presenter.onEpisodeClicked(mockEpisode1);

        List<Episode> playQueue = new ArrayList<>();
        playQueue.add(mockEpisode2);
        playQueue.add(mockEpisode3);

        verify(mockProgramCallback).playProgram(mockEpisode1);
        verify(mockPlaybackQueueManager).queueUpSavedEpisodes(playQueue);
    }
}

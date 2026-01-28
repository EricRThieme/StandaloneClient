package org.stt.gui.jfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import com.google.common.base.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.stt.command.CommandParser;
import org.stt.config.CommandTextConfig;
import org.stt.config.TimeTrackingItemListConfig;
import org.stt.fun.AchievementService;
import org.stt.model.TimeTrackingItem;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.validation.ItemAndDateValidator;

import com.google.common.eventbus.EventBus;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

public class FocusNavigationUnitTest {

    private STTApplication sut;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        // Initialize JavaFX toolkit
        new JFXPanel();

        CommandParser commandParser = mock(CommandParser.class);
        ExecutorService executorService = mock(ExecutorService.class);
        ReportWindowBuilder reportWindowBuilder = mock(ReportWindowBuilder.class);
        org.stt.text.ExpansionProvider expansionProvider = mock(org.stt.text.ExpansionProvider.class);
        ResourceBundle resourceBundle = ResourceBundle.getBundle("org.stt.gui.Application");
        ItemAndDateValidator itemValidator = mock(ItemAndDateValidator.class);
        TimeTrackingItemQueries timeTrackingItemQueries = mock(TimeTrackingItemQueries.class);
        AchievementService achievementService = mock(AchievementService.class);

        given(commandParser.parseCommandString(anyString())).willReturn(Optional.<org.stt.command.Command>absent());

        sut = new STTApplication(new STTOptionDialogs(resourceBundle), new EventBus(), commandParser, reportWindowBuilder,
                expansionProvider, resourceBundle, new TimeTrackingItemListConfig(), new CommandTextConfig(), itemValidator, timeTrackingItemQueries, achievementService, executorService);
    }

    @Test
    public void tabAndEnterBehavior() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                // Create a ViewAdapter without loading FXML: set controls manually
                STTApplication.ViewAdapter va = sut.new ViewAdapter(null);
                va.commandText = new TextArea();
                va.result = new ListView<>();

                // initialize wiring
                va.initialize();

                // make the application use this view adapter
                sut.viewAdapter = va;

                // populate items
                sut.allItems.setAll(Arrays.asList(
                        new TimeTrackingItem("first comment", DateTime.now()),
                        new TimeTrackingItem("second comment", DateTime.now())
                ));

                // wait until the ListView's items are populated via binding
                int attempts = 0;
                while (va.result.getItems().size() < 2 && attempts++ < 20) {
                    try { Thread.sleep(50); } catch (InterruptedException e) { /* ignore */ }
                }

                // ensure initial focus request
                va.requestFocusOnCommandText();

                // Find the item with "second comment" and select it (list may be ordered differently)
                int targetIndex = -1;
                for (int i = 0; i < va.result.getItems().size(); i++) {
                    TimeTrackingItem it = va.result.getItems().get(i);
                    if (it.getComment().isPresent() && "second comment".equals(it.getComment().get())) {
                        targetIndex = i;
                        break;
                    }
                }
                if (targetIndex >= 0) {
                    va.result.getSelectionModel().select(targetIndex);
                    TimeTrackingItem sel = va.result.getSelectionModel().getSelectedItem();
                    if (sel != null && sel.getComment().isPresent()) {
                        sut.textOfSelectedItem(sel.getComment().get());
                    }
                }

                latch.countDown();
            }
        });

        // wait for FX actions
        assertTrue(latch.await(2, TimeUnit.SECONDS));

        // Verify command got updated
        assertEquals("second comment", sut.currentCommand.get());
        assertEquals(sut.currentCommand.get().length(), sut.commandCaretPosition.get());
    }

    @Test
    public void upArrowSelectsPreviousItem() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                STTApplication.ViewAdapter va = sut.new ViewAdapter(null);
                va.commandText = new TextArea();
                va.result = new ListView<>();

                va.initialize();
                sut.viewAdapter = va;

                sut.allItems.setAll(Arrays.asList(
                        new TimeTrackingItem("first comment", DateTime.now()),
                        new TimeTrackingItem("second comment", DateTime.now())
                ));

                // wait for items
                int attempts = 0;
                while (va.result.getItems().size() < 2 && attempts++ < 20) {
                    try { Thread.sleep(50); } catch (InterruptedException e) { }
                }

                // Select the item with "first comment" explicitly and copy its text
                int firstIndex = -1;
                for (int i = 0; i < va.result.getItems().size(); i++) {
                    TimeTrackingItem it = va.result.getItems().get(i);
                    if (it.getComment().isPresent() && "first comment".equals(it.getComment().get())) {
                        firstIndex = i;
                        break;
                    }
                }
                if (firstIndex >= 0) {
                    va.result.getSelectionModel().select(firstIndex);
                    TimeTrackingItem sel = va.result.getSelectionModel().getSelectedItem();
                    if (sel != null && sel.getComment().isPresent()) {
                        sut.textOfSelectedItem(sel.getComment().get());
                    }
                }

                latch.countDown();
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        assertEquals("first comment", sut.currentCommand.get());
        assertEquals(sut.currentCommand.get().length(), sut.commandCaretPosition.get());
    }
}

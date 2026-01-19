package org.stt.gui.jfx

import javafx.scene.text.Font
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.stt.gui.jfx.TimeTrackingItemCellWithActions.ActionsHandler
import org.stt.model.TimeTrackingItem
import java.time.LocalDateTime
import java.util.*
import java.util.function.Predicate

class TimeTrackingItemCellTest {

    private var sut: TimeTrackingItemCellWithActions? = null
    private var closeable: AutoCloseable? = null
    @Mock
    private val actionsHandler: ActionsHandler? = null
    private var fontAwesome: Font? = null

    @Before
    fun setup() {
        TestFX.installTK()
        fontAwesome = Font.loadFont(javaClass.getResourceAsStream("/fontawesome-webfont.ttf"), 0.0)
        closeable = MockitoAnnotations.openMocks(this)
        val resourceBundle = ResourceBundle
                .getBundle("org.stt.gui.Application")

        sut = TimeTrackingItemCellWithActions(fontAwesome!!, resourceBundle, Predicate { false }, actionsHandler!!, { it })
    }

    @After
    fun tearDown() {
        closeable?.close()
    }

    @Test
    fun shouldCallDeleteHandlerOnClickOnDelete() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.now())
        sut!!.updateItem(item, false)

        val deleteButton = sut!!.deleteButton

        // WHEN
        deleteButton.fire()

        // THEN
        verify<ActionsHandler>(actionsHandler).delete(item)
    }

    @Test
    fun shouldCallContinueHandlerOnClickOnContinue() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.now())
        sut!!.updateItem(item, false)

        val continueButton = sut!!.continueButton

        // WHEN
        continueButton.fire()

        // THEN
        verify<ActionsHandler>(actionsHandler).continueItem(item)
    }

    @Test
    fun shouldCallEditHandlerOnClickOnEdit() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.now())
        sut!!.updateItem(item, false)

        val editButton = sut!!.editButton
        // WHEN
        editButton.fire()

        // THEN
        verify<ActionsHandler>(actionsHandler).edit(item)
    }

    @Test
    fun shouldCallStopHandlerOnClickOnEdit() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.now())
        sut!!.updateItem(item, false)

        val stopButton = sut!!.stopButton
        // WHEN
        stopButton.fire()

        // THEN
        verify<ActionsHandler>(actionsHandler).stop(item)
    }
}

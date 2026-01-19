package org.stt.persistence

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.FileFileFilter
import org.apache.commons.io.filefilter.FileFilterUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.MockitoAnnotations
import org.stt.config.ConfigRoot
import org.stt.config.PathSetting
import org.stt.time.DateTimes
import java.io.File
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.time.LocalDate

class BackupCreatorTest {
    private var closeable: AutoCloseable? = null

    @field:Rule
    @JvmField
    var tempFolder = TemporaryFolder()

    private val configRoot = ConfigRoot()
    private val backupConfig = configRoot.backup

    private var currentTempFolder: File? = null

    private lateinit var currentSttFile: File
    private var sut: BackupCreator? = null

    @Before
    fun setup() {
        closeable = MockitoAnnotations.openMocks(this)

        currentTempFolder = tempFolder.newFolder()
        currentSttFile = tempFolder.newFile()
        PrintWriter(currentSttFile, StandardCharsets.UTF_8.name()).use { out -> out.print("blubb, just a test line") }

        backupConfig.backupRetentionCount = 3
        backupConfig.backupLocation = PathSetting(currentTempFolder!!.absolutePath)

        sut = BackupCreator(backupConfig, currentSttFile, "")
    }

    @After
    fun tearDown() {
        closeable?.close()
    }

    @Test
    fun existingBackupShouldPreventNewBackup() {
        // GIVEN
        val threeDaysAgo = DateTimes.prettyPrintDate(LocalDate.now()
                .minusDays(3))
        val sttFileName = currentSttFile.name
        val backedUp = File(currentTempFolder, sttFileName + "-"
                + threeDaysAgo)
        createNewFile(backedUp)

        // WHEN
        sut!!.start()

        // THEN
        val files = FileUtils.listFiles(currentTempFolder!!,
            FileFilterUtils.fileFileFilter(), null)

        assertThat(1).isEqualTo(files.size.toLong())
        assertThat(files).first().isEqualTo(backedUp.absoluteFile)
    }

    @Test
    fun oldBackupShouldBeDeleted() {
        // GIVEN
        for (i in 0 until backupConfig.backupRetentionCount) {
            val xDaysAgo = DateTimes.prettyPrintDate(LocalDate.now()
                    .minusDays(i.toLong()))
            val sttFileName = currentSttFile.name
            val oldFile = File(currentTempFolder, sttFileName + "-"
                    + xDaysAgo)
            createNewFile(oldFile)
        }

        val xDaysAgo = DateTimes.prettyPrintDate(LocalDate.now()
                .minusDays((backupConfig.backupRetentionCount + 1).toLong()))
        val sttFileName = currentSttFile.name
        val oldFile = File(currentTempFolder, "$sttFileName-$xDaysAgo")
        createNewFile(oldFile)

        // WHEN
        sut!!.start()

        // THEN
        assertThat(oldFile).doesNotExist()
    }

    @Test
    fun initialBackupShouldBeCreated() {
        // GIVEN
        val currentDate = DateTimes.prettyPrintDate(LocalDate.now())
        val sttFileName = currentSttFile.name
        val expectedFile = File(currentTempFolder, sttFileName + "-"
                + currentDate)

        // WHEN
        sut!!.start()

        // THEN
        val currentContent = FileUtils.readFileToString(currentSttFile, StandardCharsets.UTF_8)
        val expectedContent = FileUtils.readFileToString(expectedFile, StandardCharsets.UTF_8)
        assertThat(currentContent).isEqualTo(expectedContent)
    }

    @Test
    fun existingFileShouldNotBeOverwritten() {
        // GIVEN
        val currentDate = DateTimes.prettyPrintDate(LocalDate.now())
        val sttFileName = currentSttFile.name
        val existingFile = File(currentTempFolder, sttFileName + "-"
                + currentDate)
        createNewFile(existingFile)

        // WHEN
        sut!!.start()

        // THEN
        assertThat(listOf(currentSttFile))
                .noneSatisfy { file ->
                        val fileObj = file as? File ?: return@noneSatisfy
                        val fileContent = FileUtils.readFileToString(fileObj, StandardCharsets.UTF_8)
                    val existingContent = FileUtils.readFileToString(existingFile, StandardCharsets.UTF_8)
                    assertThat(fileContent).isEqualTo(existingContent)
                }
    }

    private fun createNewFile(toCreate: File) {
        assertThat(toCreate.createNewFile()).isTrue()
    }
}

package org.stt;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class ConfigurationTest {
	private Configuration sut;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private File currentTempFolder;

	@Before
	public void setUp() throws IOException {
		currentTempFolder = tempFolder.newFolder();
		// remove any pre-existing .sttrc in the temp folder to ensure defaults are used
		File props = new File(currentTempFolder, ".sttrc");
		if (props.exists()) {
			boolean deleted = props.delete();
			if (!deleted) {
				throw new IOException("Could not delete pre-existing .sttrc in temp folder: " + props.getAbsolutePath());
			}
		}
		// create an empty .sttrc so Configuration won't copy the example into HOME
		boolean created = props.createNewFile();
		if (!created) {
			throw new IOException("Could not create .sttrc in temp folder: " + props.getAbsolutePath());
		}
		// ensure Configuration uses the temporary folder as HOME
		System.setProperty("user.home", currentTempFolder.getAbsolutePath());
		// construct Configuration after ensuring no properties file is present
		sut = Mockito.spy(new Configuration());
		org.mockito.Mockito.doReturn(currentTempFolder).when(sut).determineBaseDir();
	}

	@Test
	public void shouldBeAbleToProvideSTTFile() {
		// GIVEN

		// WHEN
		File sttFile = sut.getSttFile();

		// THEN
		
		assertThat(sttFile.getAbsoluteFile(), is(new File(currentTempFolder,
				".stt").getAbsoluteFile()));
	}

	@Test
	public void shouldReturnDefaultBreakTimes() {

		// GIVEN

		// WHEN
		Collection<String> breakTimeComments = sut.getBreakTimeComments();

		// THEN
		assertThat(breakTimeComments,
				containsInAnyOrder("break", "pause", "coffee"));
	}

}

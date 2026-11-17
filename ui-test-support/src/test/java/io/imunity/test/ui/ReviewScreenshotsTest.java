package io.imunity.test.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.base.json.JsonUtil;

class ReviewScreenshotsTest
{
	@TempDir
	Path directory;

	@Test
	void shouldLeaveBrowserUntouchedWhenReviewCaptureIsDisabled()
	{
		WebDriver browser = mock(WebDriver.class);
		SeleniumTestBase harness = new SeleniumTestBase() {};

		harness.screenshot(browser, "Profile editor");

		verifyNoInteractions(browser);
		assertThat(directory).isEmptyDirectory();
	}

	@Test
	void shouldKeepRepeatedCheckpointsAndParameterizedCasesSeparate() throws Exception
	{
		WebDriver browser = browser();
		ReviewScreenshots first = new ReviewScreenshots(directory, "ProfileTest", "invocation:1", "English");
		ReviewScreenshots second = new ReviewScreenshots(directory, "ProfileTest", "invocation:2", "Polish");

		first.capture(browser, "Profile / editor");
		first.capture(browser, "Profile / editor");
		second.capture(browser, "Profile / editor");

		List<JsonNode> manifest = Files.readAllLines(directory.resolve("manifest.jsonl")).stream()
				.map(JsonUtil::parse).map(JsonNode.class::cast).toList();
		assertThat(manifest).hasSize(3);
		assertThat(manifest).extracting(entry -> entry.get("file").asText()).doesNotHaveDuplicates();
		assertThat(manifest).extracting(entry -> entry.get("scenario").asText())
				.containsExactly("English", "English", "Polish");
		for (JsonNode entry : manifest)
		{
			Path image = directory.resolve(entry.get("file").asText());
			assertThat(image.normalize()).startsWith(directory);
			assertThat(Files.readAllBytes(image)).containsExactly(1, 2, 3);
			assertThat(entry.get("checkpoint").asText()).isEqualTo("Profile / editor");
			assertThat(entry.get("url").asText()).isEqualTo("https://localhost:2443/home/profile");
			assertThat(entry.get("browserVersion").asText()).isEqualTo("152");
			assertThat(Files.readString(directory.resolve("index.html"))).contains(entry.get("file").asText());
		}
	}

	@Test
	void shouldEscapeScenarioAndCheckpointTextInGallery() throws Exception
	{
		ReviewScreenshots screenshots = new ReviewScreenshots(directory, "ProfileTest", "invocation:1", "<Polish>");

		screenshots.capture(browser(), "Editor <script> & \"name\"");

		assertThat(Files.readString(directory.resolve("index.html")))
				.contains("&lt;Polish&gt;", "Editor &lt;script&gt; &amp; &quot;name&quot;")
				.doesNotContain("<script>");
	}

	@Test
	void shouldReportCaptureFailureInsteadOfClaimingAnImageWasSaved()
	{
		WebDriver browser = browser();
		when(((TakesScreenshot) browser).getScreenshotAs(OutputType.BYTES))
				.thenThrow(new WebDriverException("Browser disconnected"));
		ReviewScreenshots screenshots = new ReviewScreenshots(directory, "ProfileTest", "invocation:1", "English");

		assertThatThrownBy(() -> screenshots.capture(browser, "Editor"))
				.isInstanceOf(WebDriverException.class).hasMessageContaining("Browser disconnected");
		assertThat(directory.resolve("manifest.jsonl")).doesNotExist();
	}

	private static WebDriver browser()
	{
		WebDriver browser = mock(WebDriver.class,
				withSettings().extraInterfaces(TakesScreenshot.class, HasCapabilities.class));
		when(((TakesScreenshot) browser).getScreenshotAs(OutputType.BYTES)).thenReturn(new byte[] {1, 2, 3});
		when(((HasCapabilities) browser).getCapabilities())
				.thenReturn(new ImmutableCapabilities("browserName", "chrome", "browserVersion", "152"));
		when(browser.getCurrentUrl()).thenReturn("https://localhost:2443/home/profile");
		return browser;
	}
}

package io.imunity.test.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;

import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import pl.edu.icm.unity.base.json.JsonUtil;

final class ReviewScreenshots
{
	private final Path directory;
	private final String testClass;
	private final String testId;
	private final String scenario;
	private int sequence;

	ReviewScreenshots(Path directory, String testClass, String testId, String scenario)
	{
		this.directory = directory;
		this.testClass = testClass;
		this.testId = testId;
		this.scenario = scenario;
	}

	void capture(WebDriver browser, String checkpoint) throws IOException
	{
		String caseId = UUID.nameUUIDFromBytes(testId.getBytes(StandardCharsets.UTF_8)).toString();
		String label = checkpoint.replaceAll("[^a-zA-Z0-9._-]+", "-");
		String file = testClass + "/" + caseId + "/" + "%02d-".formatted(++sequence) + label + ".png";
		Path image = directory.resolve(file);
		Files.createDirectories(image.getParent());
		Files.write(image, ((TakesScreenshot) browser).getScreenshotAs(OutputType.BYTES));
		var capabilities = ((HasCapabilities) browser).getCapabilities();
		Screenshot screenshot = new Screenshot(testClass, testId, scenario, checkpoint, file,
				browser.getCurrentUrl(), capabilities.getBrowserName(), capabilities.getBrowserVersion(),
				Instant.now().toString());
		Files.writeString(directory.resolve("manifest.jsonl"), JsonUtil.toJsonString(screenshot) + "\n",
				StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		appendToGallery(screenshot);
	}

	private void appendToGallery(Screenshot screenshot) throws IOException
	{
		Path index = directory.resolve("index.html");
		if (!Files.exists(index))
			Files.writeString(index, """
					<!doctype html>
					<html lang="en">
					<meta charset="utf-8">
					<title>Selenium screenshot review</title>
					<style>
					body { font: 16px sans-serif; margin: 2rem; }
					section { margin-bottom: 3rem; }
					img { max-width: 100%; border: 1px solid #aaa; }
					code { overflow-wrap: anywhere; }
					</style>
					<h1>Selenium screenshot review</h1>
					<p>Named workflow checkpoints. Test outcomes are in the JUnit reports.</p>
					""", StandardCharsets.UTF_8);
		Files.writeString(index, """
				<section>
				<h2>%s — %s</h2>
				<h3>%s</h3>
				<p><code>%s</code> · %s %s</p>
				<a href="%s"><img loading="lazy" src="%s" alt="%s"></a>
				</section>
				""".formatted(html(screenshot.testClass()), html(screenshot.scenario()), html(screenshot.checkpoint()),
						html(screenshot.url()), html(screenshot.browser()), html(screenshot.browserVersion()),
						html(screenshot.file()), html(screenshot.file()), html(screenshot.checkpoint())),
				StandardCharsets.UTF_8, StandardOpenOption.APPEND);
	}

	private static String html(String value)
	{
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\"", "&quot;").replace("'", "&#39;");
	}

	private record Screenshot(String testClass, String testId, String scenario, String checkpoint, String file,
			String url, String browser, String browserVersion, String capturedAt)
	{
	}
}

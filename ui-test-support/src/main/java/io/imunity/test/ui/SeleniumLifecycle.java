package io.imunity.test.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogType;

public final class SeleniumLifecycle implements AfterEachCallback, TestExecutionExceptionHandler,
		LifecycleMethodExecutionExceptionHandler, TestWatcher
{
	private static final ExtensionContext.Namespace NAMESPACE =
			ExtensionContext.Namespace.create(SeleniumLifecycle.class);

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable failure) throws Throwable
	{
		capture(context, failure);
		throw failure;
	}

	@Override
	public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable failure) throws Throwable
	{
		capture(context, failure);
		throw failure;
	}

	@Override
	public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable failure) throws Throwable
	{
		capture(context, failure);
		throw failure;
	}

	@Override
	public void testFailed(ExtensionContext context, Throwable failure)
	{
		capture(context, failure);
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception
	{
		try
		{
			test(context).closeResources();
		} catch (Exception cleanupFailure)
		{
			Throwable original = context.getStore(NAMESPACE).get("failure", Throwable.class);
			if (original == null)
				original = context.getExecutionException().orElse(null);
			if (original == null)
				throw cleanupFailure;
			original.addSuppressed(cleanupFailure);
		}
	}

	private void capture(ExtensionContext context, Throwable failure)
	{
		if (context.getStore(NAMESPACE).get("failure") != null)
			return;
		context.getStore(NAMESPACE).put("failure", failure);
		Path directory = Path.of("target", "selenium-failures",
				context.getRequiredTestClass().getSimpleName() + "-" + UUID.randomUUID());
		attempt(failure, () -> Files.createDirectories(directory));
		attempt(failure, () -> write(directory.resolve("test.txt"),
				context.getUniqueId() + "\n" + context.getDisplayName() + "\nfixture="
						+ test(context).getClass().getName() + "\n" + failure));
		int index = 0;
		for (WebDriver browser : test(context).browsers())
		{
			String prefix = "browser-" + index++;
			attempt(failure, () -> write(directory.resolve(prefix + "-url.txt"), browser.getCurrentUrl()));
			attempt(failure, () -> write(directory.resolve(prefix + "-dom.html"), browser.getPageSource()));
			attempt(failure, () -> Files.write(directory.resolve(prefix + ".png"),
					((TakesScreenshot) browser).getScreenshotAs(OutputType.BYTES)));
			attempt(failure, () -> write(directory.resolve(prefix + "-console.txt"),
					browser.manage().logs().get(LogType.BROWSER).getAll().toString()));
			attempt(failure, () -> write(directory.resolve(prefix + "-capabilities.txt"),
					((HasCapabilities) browser).getCapabilities().toString()));
		}
		attempt(failure, () -> Files.copy(Path.of("target", "unity-server.log"), directory.resolve("server.log"),
				StandardCopyOption.REPLACE_EXISTING));
	}

	private static SeleniumTestBase test(ExtensionContext context)
	{
		return (SeleniumTestBase) context.getRequiredTestInstance();
	}

	private static void write(Path path, String content) throws IOException
	{
		Files.writeString(path, content, StandardCharsets.UTF_8);
	}

	private static void attempt(Throwable failure, CaptureAction action)
	{
		try
		{
			action.run();
		} catch (Exception e)
		{
			failure.addSuppressed(e);
		}
	}

	@FunctionalInterface
	private interface CaptureAction
	{
		void run() throws Exception;
	}
}

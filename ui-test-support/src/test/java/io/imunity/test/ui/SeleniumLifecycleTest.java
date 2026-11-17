package io.imunity.test.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import pl.edu.icm.unity.engine.server.JettyServer;

class SeleniumLifecycleTest
{
	@Test
	void shouldStopServerAfterBrowserCreationFails() throws Exception
	{
		TestHarness harness = new TestHarness();
		harness.creationFailure = new IllegalStateException("Chrome did not start");
		SeleniumLifecycle lifecycle = new SeleniumLifecycle();
		ExtensionContext context = context(harness);

		Throwable failure = catchThrowable(harness::startBrowser);
		Throwable reported = catchThrowable(() -> lifecycle.handleBeforeEachMethodExecutionException(context, failure));
		lifecycle.afterEach(context);

		assertThat(reported).isSameAs(failure);
		verify(harness.httpServer).stop();
	}

	@Test
	void shouldPreserveAssertionWhenDiagnosticsAndCleanupFail() throws Exception
	{
		TestHarness harness = new TestHarness();
		WebDriver browser = harness.browser;
		when(browser.getCurrentUrl()).thenThrow(new IllegalStateException("Browser disconnected"));
		doThrow(new IllegalStateException("Quit failed")).when(browser).quit();
		harness.startBrowser();
		SeleniumLifecycle lifecycle = new SeleniumLifecycle();
		ExtensionContext context = context(harness);
		AssertionError failure = new AssertionError("User was not saved");

		Throwable reported = catchThrowable(() -> lifecycle.handleTestExecutionException(context, failure));
		lifecycle.afterEach(context);

		assertThat(reported).isSameAs(failure);
		assertThat(failure.getSuppressed()).anySatisfy(suppressed ->
				assertThat(suppressed).hasMessage("Browser disconnected"));
		assertThat(failure.getSuppressed()).anySatisfy(suppressed ->
				assertThat(suppressed).hasMessage("Quit failed"));
		verify(browser).quit();
		verify(harness.httpServer).stop();
	}

	@Test
	void shouldReportCleanupFailureOnOtherwiseSuccessfulTest() throws Exception
	{
		TestHarness harness = new TestHarness();
		harness.startBrowser();
		doThrow(new IllegalStateException("Quit failed")).when(harness.browser).quit();

		Throwable failure = catchThrowable(() -> new SeleniumLifecycle().afterEach(context(harness)));

		assertThat(failure).hasMessage("Quit failed");
		verify(harness.httpServer).stop();
	}

	@Test
	void shouldCloseEveryActorBrowser() throws Exception
	{
		TestHarness harness = new TestHarness();
		harness.startBrowser();
		WebDriver first = harness.browser;
		harness.browser = browser();
		harness.newBrowser();
		doThrow(new IllegalStateException("First browser disconnected")).when(first).quit();

		catchThrowable(harness::closeResources);

		verify(first).quit();
		verify(harness.browser).quit();
		verify(harness.httpServer).stop();
		assertThat(harness.browsers()).isEmpty();
	}

	private static ExtensionContext context(SeleniumTestBase harness)
	{
		ExtensionContext context = mock(ExtensionContext.class);
		ExtensionContext.Store store = mock(ExtensionContext.Store.class);
		Map<Object, Object> values = new HashMap<>();
		when(context.getStore(any())).thenReturn(store);
		when(context.getRequiredTestInstance()).thenReturn(harness);
		when(context.getRequiredTestClass()).thenAnswer(ignored -> SeleniumLifecycleTest.class);
		when(context.getUniqueId()).thenReturn("harness-regression");
		when(context.getDisplayName()).thenReturn("harness lifecycle regression");
		when(store.get(any())).thenAnswer(invocation -> values.get(invocation.getArgument(0)));
		when(store.get(anyString(), any())).thenAnswer(invocation -> values.get(invocation.getArgument(0)));
		doAnswer(invocation -> values.put(invocation.getArgument(0), invocation.getArgument(1)))
				.when(store).put(any(), any());
		return context;
	}

	private static WebDriver browser()
	{
		WebDriver browser = mock(WebDriver.class);
		WebDriver.Options options = mock(WebDriver.Options.class);
		when(browser.manage()).thenReturn(options);
		when(options.timeouts()).thenReturn(mock(WebDriver.Timeouts.class));
		return browser;
	}

	private static final class TestHarness extends SeleniumTestBase
	{
		private WebDriver browser = browser();
		private RuntimeException creationFailure;

		private TestHarness()
		{
			httpServer = mock(JettyServer.class);
		}

		@Override
		WebDriver createDriver(ChromeOptions options)
		{
			if (creationFailure != null)
				throw creationFailure;
			return browser;
		}
	}
}

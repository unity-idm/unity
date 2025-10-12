package pl.edu.icm.unity.saml.sp.web;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Image;
import io.imunity.vaadin.endpoint.common.file.LocalOrRemoteResource;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import pl.edu.icm.unity.saml.metadata.cfg.ExternalLogoFileLoader;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LogoExposingServiceTest {

	@Test
	void shouldReturnNullWhenLogoUriIsNull()
	{
		ExternalLogoFileLoader external = mock(ExternalLogoFileLoader.class);
		VaadinLogoImageLoader vaadinLoader = mock(VaadinLogoImageLoader.class);
		LogoExposingService service = new LogoExposingService(external, vaadinLoader);

		IdPVisalSettings cfg = new IdPVisalSettings(null, Set.of(), "name", null);
		Image result = service.getAsResource(cfg, new TrustedIdPKey("k"));

		assertThat(result).isNull();
	}

	@Test
	void shouldReturnImageFromLoaderWhenDirectLogoRequestedAndCanBeLoaded()
	{
		ExternalLogoFileLoader external = mock(ExternalLogoFileLoader.class);
		VaadinLogoImageLoader vaadinLoader = mock(VaadinLogoImageLoader.class);
		LogoExposingService service = new LogoExposingService(external, vaadinLoader);
		LocalOrRemoteResource expected = new LocalOrRemoteResource("http://example/logo.png", "");
		when(vaadinLoader.loadImageFromUri(eq("http://example/logo.png")))
				.thenReturn(Optional.of(expected));

		IdPVisalSettings cfg = new IdPVisalSettings("http://example/logo.png", Set.of(), "name", null);
		Image result = service.getAsResource(cfg, new TrustedIdPKey("k"));

		assertThat(result).isSameAs(expected);
	}

	@Test
	void shouldReturnNullWhenDirectLogoRequestedAndCanNotBeLoaded()
	{
		ExternalLogoFileLoader external = mock(ExternalLogoFileLoader.class);
		VaadinLogoImageLoader vaadinLoader = mock(VaadinLogoImageLoader.class);
		LogoExposingService service = new LogoExposingService(external, vaadinLoader);
		when(vaadinLoader.loadImageFromUri(eq("file:/tmp/logo.png")))
				.thenReturn(Optional.empty());

		IdPVisalSettings cfg = new IdPVisalSettings("file:/tmp/logo.png", Set.of(), "name", null);
		Image result = service.getAsResource(cfg, new TrustedIdPKey("k"));

		assertThat(result).isNull();
	}

	@Test
	void shouldReturnPrefetchedFederationLogo(@TempDir Path tempDir) throws IOException
	{
		Path file = tempDir.resolve("logo.bin");
		Files.writeString(file, "data");

		ExternalLogoFileLoader external = mock(ExternalLogoFileLoader.class);
		VaadinLogoImageLoader vaadinLoader = mock(VaadinLogoImageLoader.class);
		LogoExposingService service = new LogoExposingService(external, vaadinLoader);

		when(external.getFile(eq("fed"), any(TrustedIdPKey.class), any(Locale.class)))
				.thenReturn(Optional.of(file.toFile()));

		IdPVisalSettings cfg = new IdPVisalSettings("http://idp/logo", Set.of(), "name", "fed");

		UI mockUI = Mockito.mock(UI.class);
		when(mockUI.getId()).thenReturn(Optional.of("uiId"));
		UI.setCurrent(mockUI);

		Image result = service.getAsResource(cfg, new TrustedIdPKey("k"));

		assertThat(result).isNotNull();
		assertThat(result.getClassNames()).contains("u-logo-image");
		assertThat(result.getElement().getAttribute("src")).endsWith("/logo.bin");
	}

	@Test
	void shouldReturnImageWithoutClassWhenPrefetchedFileMissing()
	{
		ExternalLogoFileLoader external = mock(ExternalLogoFileLoader.class);
		VaadinLogoImageLoader vaadinLoader = mock(VaadinLogoImageLoader.class);
		LogoExposingService service = new LogoExposingService(external, vaadinLoader);

		File missing = new File("/path/that/does/not/exist/logo.png");
		when(external.getFile(eq("fed"), any(TrustedIdPKey.class), any(Locale.class)))
				.thenReturn(Optional.of(missing));

		IdPVisalSettings cfg = new IdPVisalSettings("http://idp/logo", Set.of(), "name", "fed");
		Image result = service.getAsResource(cfg, new TrustedIdPKey("k"));

		assertThat(result).isNotNull();
		assertThat(result.getClassNames()).doesNotContain("logo-image");
	}
}

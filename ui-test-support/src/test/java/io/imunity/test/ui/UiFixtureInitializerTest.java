package io.imunity.test.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class UiFixtureInitializerTest
{
	@Test
	void shouldExtractPackagedFixtureWithAbsoluteConfigurationPaths() throws Exception
	{
		new UiFixtureInitializer().initialize(null);

		Path root = Path.of("target", "ui-fixtures").toAbsolutePath();
		String serverConfig = Files.readString(root.resolve("unityServer.conf"));
		assertThat(serverConfig).doesNotContain("@FIXTURE_ROOT@")
				.contains("unityServer.core.pkiConfigFile=" + root.resolve("pki.properties"))
				.contains("jdbc:h2:mem:ui-selenium");
		assertThat(Files.readString(root.resolve("pki.properties")))
				.contains(root.resolve("unity-demo.jks").toString());
		assertThat(root.resolve("unity-demo.jks")).isRegularFile();
		assertThat(root.resolve("templates.properties")).isRegularFile();
	}
}

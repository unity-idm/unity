package io.imunity.test.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public final class UiFixtureInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>
{
	@Override
	public void initialize(ConfigurableApplicationContext context)
	{
		try
		{
			Path root = Path.of("target", "ui-fixtures").toAbsolutePath();
			for (Resource resource : new PathMatchingResourcePatternResolver().getResources("classpath*:ui-fixtures/*"))
			{
				String name = resource.getFilename();
				Path destination = root.resolve(name);
				Files.createDirectories(destination.getParent());
				try (InputStream source = resource.getInputStream())
				{
					if (name.endsWith(".conf") || name.endsWith(".properties"))
					{
						String content = new String(source.readAllBytes(), StandardCharsets.UTF_8)
								.replace("@FIXTURE_ROOT@", root.toString());
						Files.writeString(destination, content, StandardCharsets.UTF_8);
					} else
					{
						Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}
		} catch (IOException e)
		{
			throw new UncheckedIOException("Cannot prepare Selenium server fixture", e);
		}
	}
}

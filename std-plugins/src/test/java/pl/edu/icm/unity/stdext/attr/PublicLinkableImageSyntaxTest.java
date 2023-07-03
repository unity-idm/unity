/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.attribute.image.LinkableImage;
import pl.edu.icm.unity.base.attribute.image.UnityImage;

public class PublicLinkableImageSyntaxTest
{
	@Test
	public void shouldSerializeToSimple() throws IOException
	{
		// given
		URL serverUrl = new URL("https://localhost:234");
		PublicLinkableImageSyntax syntax = new PublicLinkableImageSyntax(serverUrl);
		UnityImage image = new UnityImage(Paths.get("src/test/resources/img/test-image_100x100.jpg"));
		UUID uuid = UUID.fromString("2c55a386-99a0-4342-b724-12f3441263c4");
		LinkableImage value = new LinkableImage(image, uuid);

		// when
		String simple = syntax.serializeSimple(value);

		// then
		Assertions.assertThat(simple)
				.isEqualTo("https://localhost:234/unitygw/content/2c55a386-99a0-4342-b724-12f3441263c4");
	}

	@Test
	public void shouldDeserialzeFromSimple() throws MalformedURLException, IllegalAttributeValueException
	{
		// given
		URL serverUrl = new URL("https://localhost:234");
		PublicLinkableImageSyntax syntax = new PublicLinkableImageSyntax(serverUrl);
		String simpleValue = "https://localhost:234/unitygw/content/2c55a386-99a0-4342-b724-12f3441263c4";

		// when
		LinkableImage value = syntax.deserializeSimple(simpleValue);

		// then
		Assertions.assertThat(value).isEqualTo(new LinkableImage(
				new URL(simpleValue), UUID.fromString("2c55a386-99a0-4342-b724-12f3441263c4")));
	}

}

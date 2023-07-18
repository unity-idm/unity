/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.attribute.image.ImageType;
import pl.edu.icm.unity.base.attribute.image.UnityImage;

public class TestUnityImage
{
	private static final String JPG_SRC = "src/test/resources/img/test-image_100x100.jpg";
	private static final String PNG_SRC = "src/test/resources/img/test-image_100x100.png";
	private static final String GIF_SRC = "src/test/resources/img/test-image_100x100.gif";

	@Test
	public void shouldSerializeAndDeserializeWithoutModification() throws IOException
	{
		UnityImage image = new UnityImage(Paths.get(PNG_SRC));

		// serialize
		String str = image.serialize();
		// deserialize
		UnityImage image2 = new UnityImage(str);

		assertThat(image.getType()).isEqualTo(image2.getType());
		assertThat(image.getImage()).isEqualTo(image2.getImage());
	}

	@Test
	public void conversionShouldNotImpactQualityPng() throws IOException
	{
		conversionShouldNotImpactQuality(PNG_SRC, ImageType.PNG);
	}

	@Test
	public void conversionShouldNotImpactQualityGif() throws IOException
	{
		conversionShouldNotImpactQuality(GIF_SRC, ImageType.GIF);
	}

	// This is not true for jpg - something that require further investigation.
	// At this moment it is not main concern. Note: the raw image length go up and
	// down, not only down....
	// @Test
	// public void conversionShouldNotImpactQualityJpg() throws IOException {
	// testNoQualityImpact("src/test/resources/img/test-image.jpg",
	// UnityImage.ImageType.JPG);
	// }

	public void conversionShouldNotImpactQuality(String fileName, ImageType type) throws IOException
	{
		UnityImage original = new UnityImage(Paths.get(fileName));

		// Creating image based on BufferedImage
		UnityImage image1 = new UnityImage(original.getBufferedImage(), original.getType());
		// Creating second image based on byte[] buffer (BufferedImage recreated)
		UnityImage image2 = new UnityImage(image1.getImage(), original.getType());
		// Creating third image based on BufferedImage from prev step - byte[] data
		// recreated from conversion
		UnityImage image3 = new UnityImage(image2.getBufferedImage(), original.getType());

		assertThat(image1.getImage()).isEqualTo(image2.getImage());
		assertThat(image2.getImage()).isEqualTo(image3.getImage());
	}

	@Test
	public void shouldNotScaleDownIfMatchingSize() throws IOException
	{
		byte[] bytes = Files.readAllBytes(Paths.get(JPG_SRC));
		UnityImage image = new UnityImage(bytes, ImageType.JPG);
		assertThat(100).isEqualTo(image.getWidth());
		assertThat(100).isEqualTo(image.getHeight());

		// no action expected
		image.scaleDown(100, 100);
		assertThat(100).isEqualTo(image.getHeight());
		assertThat(100).isEqualTo(image.getWidth());
		assertThat(bytes).isEqualTo(image.getImage());
	}

	@Test
	public void shouldNotScaleDownWithConversionIfMatchingSize() throws IOException
	{
		byte[] bytes = Files.readAllBytes(Paths.get(JPG_SRC));
		UnityImage image = new UnityImage(bytes, ImageType.JPG);
		assertThat(100).isEqualTo(image.getWidth());
		assertThat(100).isEqualTo(image.getHeight());

		// no action expected
		byte[] ba = image.getScaledDownImage(100, 100);
		UnityImage image2 = new UnityImage(ba, ImageType.JPG);
		assertThat(100).isEqualTo(image2.getHeight());
		assertThat(100).isEqualTo(image2.getWidth());
		assertThat(bytes).isEqualTo(image2.getImage());
	}

	@Test
	public void shouldScaleDown() throws IOException
	{
		byte[] bytes = Files.readAllBytes(Paths.get(JPG_SRC));
		UnityImage image = new UnityImage(bytes, ImageType.JPG);
		assertThat(100).isEqualTo(image.getWidth());
		assertThat(100).isEqualTo(image.getHeight());

		// change main image
		image.scaleDown(50, 50);
		assertThat(50).isEqualTo(image.getHeight());
		assertThat(50).isEqualTo(image.getWidth());

		// Compare with previously saved file
		byte[] bytes2 = Files.readAllBytes(Paths.get("src/test/resources/img/test-image_50x50.jpg"));
		assertThat(image.getImage()).isEqualTo(bytes2);
	}

	@Test
	public void shouldScaleDownWithoutOriginalObjectChange() throws IOException
	{
		UnityImage image = new UnityImage(Paths.get(JPG_SRC));
		assertThat(100).isEqualTo(image.getWidth());
		assertThat(100).isEqualTo(image.getHeight());

		byte[] ba = image.getScaledDownImage(50, 50);
		UnityImage image2 = new UnityImage(ba, ImageType.JPG);
		assertThat(50).isEqualTo(image2.getWidth());
		assertThat(50).isEqualTo(image2.getHeight());
		assertThat(100).isEqualTo(image.getWidth());
		assertThat(100).isEqualTo(image.getHeight());

		// Compare with previously saved file
		byte[] bytes2 = Files.readAllBytes(Paths.get("src/test/resources/img/test-image_50x50.jpg"));
		assertThat(image2.getImage()).isEqualTo(bytes2);
	}
}

/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.util;

import org.junit.Assert;
import org.junit.Test;
import pl.edu.icm.unity.stdext.utils.UnityImage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestUnityImage
{
	private static final String JPG_SRC = "src/test/resources/img/test-image_100x100.jpg";
	private static final String PNG_SRC = "src/test/resources/img/test-image_100x100.png";
	private static final String GIF_SRC = "src/test/resources/img/test-image_100x100.gif";

	@Test
	public void shouldSerializeAndDeserializeWithoutModification() throws IOException {
		UnityImage image = new UnityImage(Paths.get(PNG_SRC));

		// serialize
		String str = image.serialize();
		// deserialize
		UnityImage image2 = new UnityImage(str);

		Assert.assertEquals(image.getType(), image2.getType());
		Assert.assertArrayEquals(image.getImage(), image2.getImage());
	}

	@Test
	public void conversionShouldNotImpactQualityPng() throws IOException {
		conversionShouldNotImpactQuality(PNG_SRC, UnityImage.ImageType.PNG);
	}

	@Test
	public void conversionShouldNotImpactQualityGif() throws IOException {
		conversionShouldNotImpactQuality(GIF_SRC, UnityImage.ImageType.GIF);
	}

	// This is not true for jpg - something that require further investigation.
	// At this moment it is not main concern. Note: the raw image length go up and down, not only down....
	// @Test
	// public void conversionShouldNotImpactQualityJpg() throws IOException {
	// 	testNoQualityImpact("src/test/resources/img/test-image.jpg", UnityImage.ImageType.JPG);
	// }

	public void conversionShouldNotImpactQuality(String fileName, UnityImage.ImageType type) throws IOException {
		UnityImage original = new UnityImage(Paths.get(fileName));

		// Creating image based on BufferedImage
		UnityImage image1 = new UnityImage(original.getBufferedImage(), original.getType());
		// Creating second image based on byte[] buffer (BufferedImage recreated)
		UnityImage image2 = new UnityImage(image1.getImage(), original.getType());
		// Creating third image based on BufferedImage from prev step - byte[] data recreated from conversion
		UnityImage image3 = new UnityImage(image2.getBufferedImage(), original.getType());

		Assert.assertArrayEquals(image1.getImage(), image2.getImage());
		Assert.assertArrayEquals(image2.getImage(), image3.getImage());
	}

	@Test
	public void shouldNotScaleDownIfMatchingSize() throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(JPG_SRC));
		UnityImage image = new UnityImage(bytes, UnityImage.ImageType.JPG);
		Assert.assertEquals(100, image.getWidth());
		Assert.assertEquals(100, image.getHeight());

		// no action expected
		image.scaleDown(100, 100);
		Assert.assertEquals(100, image.getHeight());
		Assert.assertEquals(100, image.getWidth());
		Assert.assertArrayEquals(bytes, image.getImage());
	}

	@Test
	public void shouldNotScaleDownWithConversionIfMatchingSize() throws IOException
	{
		byte[] bytes = Files.readAllBytes(Paths.get(JPG_SRC));
		UnityImage image = new UnityImage(bytes, UnityImage.ImageType.JPG);
		Assert.assertEquals(100, image.getWidth());
		Assert.assertEquals(100, image.getHeight());

		// no action expected
		byte[] ba = image.getScaledDownImage(100, 100);
		UnityImage image2 = new UnityImage(ba, UnityImage.ImageType.JPG);
		Assert.assertEquals(100, image2.getHeight());
		Assert.assertEquals(100, image2.getWidth());
		Assert.assertArrayEquals(bytes, image2.getImage());
	}

	@Test
	public void shouldScaleDown() throws IOException
	{
		byte[] bytes = Files.readAllBytes(Paths.get(JPG_SRC));
		UnityImage image = new UnityImage(bytes, UnityImage.ImageType.JPG);
		Assert.assertEquals(100, image.getWidth());
		Assert.assertEquals(100, image.getHeight());

		// change main image
		image.scaleDown(50, 50);
		Assert.assertEquals(50, image.getHeight());
		Assert.assertEquals(50, image.getWidth());

		// Compare with previously saved file
		byte[] bytes2 = Files.readAllBytes(Paths.get("src/test/resources/img/test-image_50x50.jpg"));
		Assert.assertArrayEquals(image.getImage(), bytes2);
	}

	@Test
	public void shouldScaleDownWithoutOriginalObjectChange() throws IOException
	{
		UnityImage image = new UnityImage(Paths.get(JPG_SRC));
		Assert.assertEquals(100, image.getWidth());
		Assert.assertEquals(100, image.getHeight());

		byte[] ba = image.getScaledDownImage(50, 50);
		UnityImage image2 = new UnityImage(ba, UnityImage.ImageType.JPG);
		Assert.assertEquals(50, image2.getWidth());
		Assert.assertEquals(50, image2.getHeight());
		Assert.assertEquals(100, image.getWidth());
		Assert.assertEquals(100, image.getHeight());

		// Compare with previously saved file
		byte[] bytes2 = Files.readAllBytes(Paths.get("src/test/resources/img/test-image_50x50.jpg"));
		Assert.assertArrayEquals(image2.getImage(), bytes2);
	}
}

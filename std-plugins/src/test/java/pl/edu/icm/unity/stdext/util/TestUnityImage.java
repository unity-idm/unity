/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.util;

import org.junit.Assert;
import org.junit.Test;
import pl.edu.icm.unity.base.utils.Escaper;
import pl.edu.icm.unity.stdext.utils.UnityImage;
import sun.misc.IOUtils;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestUnityImage
{
	private static final String JPG_SRC = "src/test/resources/img/test-image.jpg";
	private static final String PNG_SRC = "src/test/resources/img/test-image.png";
	private static final String GIF_SRC = "src/test/resources/img/test-image.gif";

	@Test
	public void testSerialize() throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(JPG_SRC));

		UnityImage image = new UnityImage(bytes, UnityImage.ImageType.PNG);

		String str = image.serialize();
		UnityImage image2 = new UnityImage(str);

		Assert.assertEquals(image.getType(), image2.getType());
		Assert.assertArrayEquals(image.getImage(), image2.getImage());
	}

	@Test
	public void testNoQualityImpactPng() throws IOException {
		testNoQualityImpact(PNG_SRC, UnityImage.ImageType.PNG);
	}

	@Test
	public void testNoQualityImpactGif() throws IOException {
		testNoQualityImpact(GIF_SRC, UnityImage.ImageType.GIF);
	}

	// This is not true for jpg - something that require further investigation.
	// At this moment it is not main concern. Note: the raw image length go up and down, not only down....
	// @Test
	// public void testNoQualityImpactJpg() throws IOException {
	// 	testNoQualityImpact("src/test/resources/img/test-image.jpg", UnityImage.ImageType.JPG);
	// }


	public void testNoQualityImpact(String fileName, UnityImage.ImageType type) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(fileName));

		UnityImage original = new UnityImage(bytes,type);
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
	public void testScaleDown() throws IOException
	{
		byte[] bytes = Files.readAllBytes(Paths.get(JPG_SRC));
		UnityImage image = new UnityImage(bytes, UnityImage.ImageType.JPG);

		int initialW = image.getWidth();
		int initialH = image.getWidth();

		// no action expected
		image.scaleDown(100, 100);
		Assert.assertEquals(initialH, image.getHeight());
		Assert.assertEquals(initialW, image.getWidth());
		Assert.assertArrayEquals(bytes, image.getImage());

		// no action expected
		byte[] ba = image.getScaledDownImage(100, 100);
		UnityImage image2 = new UnityImage(ba, UnityImage.ImageType.JPG);
		Assert.assertEquals(initialH, image2.getHeight());
		Assert.assertEquals(initialW, image2.getWidth());
		Assert.assertArrayEquals(bytes, image2.getImage());

		// changed only image2
		ba = image.getScaledDownImage(50, 50);
		image2 = new UnityImage(ba, UnityImage.ImageType.JPG);
		Assert.assertEquals(50, image2.getHeight());
		Assert.assertEquals(50, image2.getWidth());
		Assert.assertEquals(initialH, image.getHeight());
		Assert.assertEquals(initialW, image.getWidth());

		// change main image
		image.scaleDown(50, 50);
		Assert.assertEquals(50, image.getHeight());
		Assert.assertEquals(50, image.getWidth());
	}
}

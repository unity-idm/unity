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
	@Test
	public void testSerialize() throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get("src/test/resources/img/add.png"));

		UnityImage image = new UnityImage(bytes, UnityImage.ImageType.PNG);

		String str = image.serialize();
		UnityImage image2 = new UnityImage(str);

		Assert.assertEquals(image.getType(), image2.getType());
		Assert.assertArrayEquals(image.getImage(), image2.getImage());
	}

	@Test
	public void testNoQualityImpact() throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get("src/test/resources/img/test-image.png"));

		UnityImage original = new UnityImage(bytes, UnityImage.ImageType.PNG);
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
	public void testNoQualityImpactPng() throws IOException {
		testNoQualityImpact("src/test/resources/img/test-image.png", UnityImage.ImageType.PNG);
	}

	@Test
	public void testNoQualityImpactJpg() throws IOException {
		testNoQualityImpact("src/test/resources/img/test-image.jpg", UnityImage.ImageType.JPG);
	}

	@Test
	public void testNoQualityImpactGif() throws IOException {
		testNoQualityImpact("src/test/resources/img/test-image.gif", UnityImage.ImageType.GIF);
	}

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
}

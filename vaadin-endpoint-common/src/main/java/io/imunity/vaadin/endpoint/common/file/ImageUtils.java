/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.file;

import java.util.Base64;

import com.vaadin.flow.component.html.Image;

import pl.edu.icm.unity.base.attribute.image.UnityImage;

/**
 * Utility class for creating and manipulating Image components.
 * Provides alternatives to deprecated StreamResource-based image handling.
 */
public final class ImageUtils
{
	private static final int DATA_URL_SIZE_THRESHOLD = 32 * 1024;

	private ImageUtils()
	{
	}

	/**
	 * Creates an Image component from byte array data using a data URL.
	 * Suitable for small to medium images (recommended under 32KB).
	 *
	 * @param imageData the image byte array
	 * @param mimeType  the MIME type (e.g., "image/png", "image/jpeg")
	 * @param altText   alternative text for the image
	 * @return an Image component
	 */
	public static Image createFromBytes(byte[] imageData, String mimeType, String altText)
	{
		String dataUrl = createDataUrl(imageData, mimeType);
		return new Image(dataUrl, altText);
	}

	/**
	 * Creates an Image component from a UnityImage using a data URL.
	 *
	 * @param unityImage the UnityImage
	 * @param altText    alternative text for the image
	 * @return an Image component
	 */
	public static Image createFromUnityImage(UnityImage unityImage, String altText)
	{
		return createFromBytes(unityImage.getImage(), unityImage.getType().getMimeType(), altText);
	}

	/**
	 * Sets the image source using a data URL.
	 * Suitable for small to medium images (recommended under 32KB).
	 *
	 * @param image     the Image component to update
	 * @param imageData the image byte array
	 * @param mimeType  the MIME type (e.g., "image/png", "image/jpeg")
	 */
	public static void setSrcFromBytes(Image image, byte[] imageData, String mimeType)
	{
		String dataUrl = createDataUrl(imageData, mimeType);
		image.setSrc(dataUrl);
	}

	/**
	 * Sets the image source from a UnityImage using a data URL.
	 *
	 * @param image      the Image component to update
	 * @param unityImage the UnityImage
	 */
	public static void setSrcFromUnityImage(Image image, UnityImage unityImage)
	{
		setSrcFromBytes(image, unityImage.getImage(), unityImage.getType().getMimeType());
	}

	/**
	 * Clears the image source.
	 *
	 * @param image the Image component to clear
	 */
	public static void clearSrc(Image image)
	{
		image.setSrc("");
	}

	/**
	 * Creates a base64 data URL from byte array.
	 *
	 * @param data     the byte array
	 * @param mimeType the MIME type
	 * @return the data URL string
	 */
	public static String createDataUrl(byte[] data, String mimeType)
	{
		if (data == null || data.length == 0)
		{
			return "";
		}
		String base64 = Base64.getEncoder().encodeToString(data);
		return "data:" + mimeType + ";base64," + base64;
	}

	/**
	 * Checks if an image is small enough for efficient data URL usage.
	 * Images larger than 32KB may benefit from DownloadHandler approach instead.
	 *
	 * @param imageData the image byte array
	 * @return true if the image is suitable for data URL usage
	 */
	public static boolean isSuitableForDataUrl(byte[] imageData)
	{
		return imageData != null && imageData.length <= DATA_URL_SIZE_THRESHOLD;
	}

	/**
	 * Determines the MIME type from a filename extension.
	 *
	 * @param filename the filename
	 * @return the MIME type, or "application/octet-stream" if unknown
	 */
	public static String getMimeTypeFromFilename(String filename)
	{
		if (filename == null)
		{
			return "application/octet-stream";
		}
		String lower = filename.toLowerCase();
		if (lower.endsWith(".png"))
		{
			return "image/png";
		}
		if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
		{
			return "image/jpeg";
		}
		if (lower.endsWith(".gif"))
		{
			return "image/gif";
		}
		if (lower.endsWith(".svg"))
		{
			return "image/svg+xml";
		}
		if (lower.endsWith(".webp"))
		{
			return "image/webp";
		}
		if (lower.endsWith(".bmp"))
		{
			return "image/bmp";
		}
		if (lower.endsWith(".ico"))
		{
			return "image/x-icon";
		}
		return "application/octet-stream";
	}
}

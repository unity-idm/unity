/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.file;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.function.Supplier;

import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;

import pl.edu.icm.unity.base.attribute.image.UnityImage;

/**
 * Factory methods for creating {@link DownloadHandler} instances.
 * Replaces deprecated {@code StreamResource} API.
 */
public final class DownloadHandlers
{
	private DownloadHandlers()
	{
	}

	/**
	 * Creates a DownloadHandler for byte array data.
	 *
	 * @param data     the byte array to download
	 * @param filename the filename for the download
	 * @param mimeType the MIME type of the content
	 * @return a DownloadHandler instance
	 */
	public static DownloadHandler forBytes(byte[] data, String filename, String mimeType)
	{
		return new InputStreamDownloadHandler(
			event -> new DownloadResponse(
				new ByteArrayInputStream(data),
				filename,
				mimeType,
				data.length
			)
		);
	}

	/**
	 * Creates a DownloadHandler for byte array data with a default MIME type of "application/octet-stream".
	 *
	 * @param data     the byte array to download
	 * @param filename the filename for the download
	 * @return a DownloadHandler instance
	 */
	public static DownloadHandler forBytes(byte[] data, String filename)
	{
		return forBytes(data, filename, "application/octet-stream");
	}

	/**
	 * Creates a DownloadHandler for lazily-supplied byte array data.
	 *
	 * @param dataSupplier supplier for the byte array
	 * @param filename     the filename for the download
	 * @param mimeType     the MIME type of the content
	 * @return a DownloadHandler instance
	 */
	public static DownloadHandler forBytes(Supplier<byte[]> dataSupplier, String filename, String mimeType)
	{
		return new InputStreamDownloadHandler(
			event ->
			{
				byte[] data = dataSupplier.get();
				return new DownloadResponse(
					new ByteArrayInputStream(data),
					filename,
					mimeType,
					data.length
				);
			}
		);
	}

	/**
	 * Creates a DownloadHandler for an InputStream supplier.
	 *
	 * @param inputStreamSupplier supplier for the input stream
	 * @param filename            the filename for the download
	 * @param mimeType            the MIME type of the content
	 * @return a DownloadHandler instance
	 */
	public static DownloadHandler forInputStream(
		Supplier<InputStream> inputStreamSupplier,
		String filename,
		String mimeType)
	{
		return new InputStreamDownloadHandler(
			event -> new DownloadResponse(
				inputStreamSupplier.get(),
				filename,
				mimeType,
				-1
			)
		);
	}

	/**
	 * Creates a DownloadHandler for an InputStream supplier with known content length.
	 *
	 * @param inputStreamSupplier supplier for the input stream
	 * @param filename            the filename for the download
	 * @param mimeType            the MIME type of the content
	 * @param contentLength       the content length in bytes
	 * @return a DownloadHandler instance
	 */
	public static DownloadHandler forInputStream(
		Supplier<InputStream> inputStreamSupplier,
		String filename,
		String mimeType,
		long contentLength)
	{
		return new InputStreamDownloadHandler(
			event -> new DownloadResponse(
				inputStreamSupplier.get(),
				filename,
				mimeType,
				contentLength
			)
		);
	}

	/**
	 * Creates a DownloadHandler for a UnityImage.
	 *
	 * @param image    the UnityImage to download
	 * @param filename the filename for the download
	 * @return a DownloadHandler instance
	 */
	public static DownloadHandler forUnityImage(UnityImage image, String filename)
	{
		return new InputStreamDownloadHandler(
			event -> new DownloadResponse(
				new ByteArrayInputStream(image.getImage()),
				filename,
				image.getType().getMimeType(),
				image.getImage().length
			)
		);
	}

	/**
	 * Creates a DownloadHandler for JSON content.
	 *
	 * @param jsonDataSupplier supplier for the JSON byte array
	 * @param filename         the filename for the download (should end with .json)
	 * @return a DownloadHandler instance
	 */
	public static DownloadHandler forJson(Supplier<byte[]> jsonDataSupplier, String filename)
	{
		return forBytes(jsonDataSupplier, filename, "application/json");
	}
}

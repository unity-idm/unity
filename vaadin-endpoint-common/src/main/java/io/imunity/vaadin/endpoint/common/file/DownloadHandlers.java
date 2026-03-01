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

	public static DownloadHandler forJson(Supplier<byte[]> jsonDataSupplier, String filename)
	{
		return forBytes(jsonDataSupplier, filename, "application/json");
	}
}

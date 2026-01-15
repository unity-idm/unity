/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.file;

import java.io.ByteArrayInputStream;

import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadCallback;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;

import pl.edu.icm.unity.base.attribute.image.UnityImage;

/**
 * A {@link InputStreamDownloadHandler} implementation for Unity image attributes.
 * Provides proper MIME type and content length handling for image downloads.
 */
public class UnityImageDownloadHandler extends InputStreamDownloadHandler
{
	public UnityImageDownloadHandler(UnityImage image, String filename)
	{
		super(createCallback(image, filename));
	}

	private static InputStreamDownloadCallback createCallback(UnityImage image, String filename)
	{
		return downloadEvent -> new DownloadResponse(
			new ByteArrayInputStream(image.getImage()),
			filename,
			image.getType().getMimeType(),
			image.getImage().length
		);
	}
}

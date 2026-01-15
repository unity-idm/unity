package io.imunity.vaadin.endpoint.common.plugins.attributes.ext.img;

import java.io.ByteArrayInputStream;

import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadCallback;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;

import pl.edu.icm.unity.base.attribute.image.UnityImage;

class UnityImageDownloadHandler extends InputStreamDownloadHandler
{
	public UnityImageDownloadHandler(
		UnityImage image,
		String filename)
	{
		super(getCallback(image, filename));
	}

	private static InputStreamDownloadCallback getCallback(
		UnityImage image,
		String filename)
	{
		return downloadEvent -> new DownloadResponse(
			new ByteArrayInputStream(image.getImage()), filename, image.getType().getMimeType(), -1);
	}
}

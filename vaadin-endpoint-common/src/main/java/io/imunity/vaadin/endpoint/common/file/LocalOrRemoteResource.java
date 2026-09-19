/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.file;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.streams.DownloadHandler;

import static io.imunity.vaadin.elements.CssClassNames.LOGO_IMAGE;

public class LocalOrRemoteResource extends Image
{
	private byte[] local;
	private String mimeType;

	public LocalOrRemoteResource()
	{
		addClassName(LOGO_IMAGE.getName());
	}

	public LocalOrRemoteResource(String src, String alt)
	{
		super(src, alt);
		addClassName(LOGO_IMAGE.getName());
	}

	public LocalOrRemoteResource(DownloadHandler src, String alt, byte[] local)
	{
		super(src, alt);
		this.local = local;
	}

	public void setSrc(DownloadHandler src, byte[] local)
	{
		this.local = local;
		super.setSrc(src);
	}

	public byte[] getLocal()
	{
		return local;
	}

	public void setLocal(byte[] local)
	{
		this.local = local;
	}

	public String getMimeType()
	{
		return mimeType;
	}

	public void setMimeType(String mimeType)
	{
		this.mimeType = mimeType;
	}

	@Override
	public LocalOrRemoteResource clone()
	{
		LocalOrRemoteResource clone = new LocalOrRemoteResource(getSrc(), getAlt().orElse(null));
		clone.local = this.local != null ? this.local.clone() : null;
		clone.mimeType = this.mimeType;
		return clone;
	}
}

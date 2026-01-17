/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.file;

import com.vaadin.flow.component.html.Image;

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

	public LocalOrRemoteResource(byte[] local, String mimeType, String alt)
	{
		this.local = local;
		this.mimeType = mimeType;
		addClassName(LOGO_IMAGE.getName());
		if (local != null && local.length > 0)
		{
			super.setSrc(ImageUtils.createDataUrl(local, mimeType));
		}
		setAlt(alt);
	}

	public void setSrc(byte[] local, String mimeType)
	{
		this.local = local;
		this.mimeType = mimeType;
		if (local != null && local.length > 0)
		{
			super.setSrc(ImageUtils.createDataUrl(local, mimeType));
		}
		else
		{
			super.setSrc("");
		}
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
		if (local == null)
		{
			return new LocalOrRemoteResource(getSrc(), getAlt().orElse(null));
		}
		return new LocalOrRemoteResource(local.clone(), mimeType, getAlt().orElse(null));
	}
}

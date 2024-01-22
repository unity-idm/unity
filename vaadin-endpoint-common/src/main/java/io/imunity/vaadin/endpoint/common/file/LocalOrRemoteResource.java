/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.file;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.StreamResource;

import java.io.ByteArrayInputStream;

public class LocalOrRemoteResource extends Image
{
	private byte[] local;

	public LocalOrRemoteResource()
	{
	}

	public LocalOrRemoteResource(String src, String alt)
	{
		super(src, alt);
	}

	public LocalOrRemoteResource(AbstractStreamResource src, String alt, byte[] local)
	{
		super(src, alt);
		this.local = local;
	}

	public byte[] getLocal()
	{
		return local;
	}

	public LocalOrRemoteResource clone()
	{
		if(local == null)
			return new LocalOrRemoteResource(getSrc(), getAlt().orElse(null));
		return new LocalOrRemoteResource(new StreamResource("file", () -> new ByteArrayInputStream(local)), "", local.clone());
	}

}
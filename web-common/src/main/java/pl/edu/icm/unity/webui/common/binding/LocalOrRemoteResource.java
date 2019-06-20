/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.binding;

import org.bouncycastle.util.Arrays;

/**
 * Represent local or remote resource.
 * @author P.Piernik
 *
 */
public class LocalOrRemoteResource
{
	private byte[] local;
	private String localUri;
	private String remote;

	public LocalOrRemoteResource()
	{
	}
	
	public LocalOrRemoteResource(String remote)
	{
		setRemote(remote);
	}
	
	public LocalOrRemoteResource(byte[] local)
	{
		setLocal(local);
	}

	public LocalOrRemoteResource(byte[] local, String localUri)
	{
		setLocal(local);
		this.localUri = localUri;
	}

	public String getRemote()
	{
		return remote;
	}

	public void setRemote(String remote)
	{
		this.remote = remote;
		this.local = null;
		this.localUri = null;
	}

	public byte[] getLocal()
	{
		return local;
	}

	public void setLocal(byte[] local)
	{
		this.local = local;
		this.remote = null;
		this.localUri = null;
	}

	public void setLocal(byte[] local, String uri)
	{
		this.local = local;
		this.localUri = uri;
		this.remote = null;
	}

	public String getLocalUri()
	{
		return localUri;
	}

	public LocalOrRemoteResource clone()
	{
		LocalOrRemoteResource clone = new LocalOrRemoteResource();
		if (local != null)
		{
			clone.local = Arrays.copyOf(local, local.length);
		}

		if (localUri != null)
		{
			clone.localUri = new String(this.getLocalUri());
		}

		if (remote != null)
		{
			clone.remote = new String(this.getRemote());
		}

		return clone;
	}
}
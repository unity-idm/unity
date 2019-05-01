/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common;

import org.bouncycastle.util.Arrays;

/**
 * 
 * @author P.Piernik
 *
 */
public class LocalOrUrlResource
{
	private byte[] local;
	private String remote;

	public LocalOrUrlResource()
	{
	}

	public LocalOrUrlResource(String remote) 
	{
		setRemote(remote);
	}

	public LocalOrUrlResource(byte[] local)
	{
		setLocal(local);
	}
	
	public String getRemote()
	{
		return remote;
	}

	public void setRemote(String remote)
	{
		this.remote = remote;
		this.local = null;
	}

	public byte[] getLocal()
	{
		return local;

	}

	public void setLocal(byte[] local)
	{
		this.local = local;
		this.remote = null;
	}

	public LocalOrUrlResource clone()
	{
		LocalOrUrlResource clone = new LocalOrUrlResource();
		if (local != null)
		{
			clone.local = Arrays.copyOf(local, local.length);
		}
		if (remote != null)
		{
			clone.remote = new String(this.getRemote());
		}
		return clone;
	}
}
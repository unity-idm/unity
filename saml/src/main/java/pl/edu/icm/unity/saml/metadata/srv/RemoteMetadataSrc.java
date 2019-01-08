/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.srv;

/**
 * Describes source of the remote metadata
 * @author K. Benedyczak
 */
class RemoteMetadataSrc
{
	final String url;
	final String truststore;

	RemoteMetadataSrc(String url, String truststore)
	{
		this.url = url;
		this.truststore = truststore;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((truststore == null) ? 0 : truststore.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RemoteMetadataSrc other = (RemoteMetadataSrc) obj;
		if (truststore == null)
		{
			if (other.truststore != null)
				return false;
		} else if (!truststore.equals(other.truststore))
			return false;
		if (url == null)
		{
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}
}
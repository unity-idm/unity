package pl.edu.icm.unity.oauth.client;

import java.net.URISyntaxException;
import java.net.URL;

import org.apache.hc.core5.net.URIBuilder;

class URLObfuscator
{
	static String obfuscateURLParams(URL url)
	{
		String obfuscatedQuery = url.getQuery() != null ?
				url.getQuery().replaceFirst("client_secret=[^&]*", "client_secret=xxxxxx") : "";
		try
		{
			return new URIBuilder(url.toURI()).setCustomQuery(obfuscatedQuery).build().toString();
		} catch (URISyntaxException e)
		{
			throw new IllegalStateException("Can't build URI from a valid URL", e);
		}
	}
}

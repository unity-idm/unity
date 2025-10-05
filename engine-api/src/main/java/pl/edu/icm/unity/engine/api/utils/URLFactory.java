package pl.edu.icm.unity.engine.api.utils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class URLFactory
{
	public static URL of(String urlString) throws MalformedURLException
	{
		try
		{
			return new URI(urlString).toURL();
		} catch (URISyntaxException e)
		{
			throw new MalformedURLException(e.toString());
		}
	}
}

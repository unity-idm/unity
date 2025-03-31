package io.imunity.vaadin.auth.idp;

import java.net.URISyntaxException;
import java.net.URL;

import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;

public class PathWithQueryProvider
{
	private final URL url;

	public PathWithQueryProvider(URL url)
	{
		this.url = url;
	}

	public String getPathAndQueryOnly()
	{
		return getPathAndQuery(toURIBuilder());
	}

	public String getPathWithQueryParamsIncluding(BasicNameValuePair pair)
	{
		URIBuilder uriBuilder = toURIBuilder();
		uriBuilder.addParameter(pair);
		return getPathAndQuery(uriBuilder);
	}

	private URIBuilder toURIBuilder()
	{
		try
		{
			return new URIBuilder(url.toURI());
		} catch (URISyntaxException e)
		{
			throw new RuntimeException("Can't extract path and query from URL", e);
		}
	}

	private String getPathAndQuery(URIBuilder uriBuilder)
	{
		try
		{
			String path = uriBuilder.getPath();
			String query = uriBuilder.getQueryParams().isEmpty() ? "" : "?" + uriBuilder.build().getRawQuery();
			return path + query;
		} catch (URISyntaxException e)
		{
			throw new RuntimeException("Can't extract path and query from URL", e);
		}
	}
}

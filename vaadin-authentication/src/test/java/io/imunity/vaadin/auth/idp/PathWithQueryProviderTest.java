package io.imunity.vaadin.auth.idp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.engine.api.utils.URLFactory;

public class PathWithQueryProviderTest
{
	private URL simpleUrl;
	private URL urlWithParams;
	private URL urlWithMultipleParams;
	private URL emptyPathUrl;
	private URL encodedParamUrl;

	@BeforeEach
	public void setUp() throws MalformedURLException
	{
		simpleUrl = URLFactory.of("https://example.com/path");
		urlWithParams = URLFactory.of("https://example.com/path?param1=value1");
		urlWithMultipleParams = URLFactory.of("https://example.com/path?param1=value1¶m2=value2");
		emptyPathUrl = URLFactory.of("https://example.com");
		encodedParamUrl = URLFactory.of("https://example.com/path?param=value%20with%20spaces");
	}

	@Test
	void shouldReturnPathWithoutQueryParams()
	{
		// given
		PathWithQueryProvider provider = new PathWithQueryProvider(simpleUrl);

		// when
		String result = provider.getPathAndQueryOnly();

		// then
		assertEquals("/path", result);
	}

	@Test
	void shouldReturnPathWithExistingQueryParams()
	{
		// given
		PathWithQueryProvider provider = new PathWithQueryProvider(urlWithParams);

		// when
		String result = provider.getPathAndQueryOnly();

		// then
		assertEquals("/path?param1=value1", result);
	}

	@Test
	void shouldReturnPathWithMultipleExistingQueryParams()
	{
		// given
		PathWithQueryProvider provider = new PathWithQueryProvider(urlWithMultipleParams);

		// when
		String result = provider.getPathAndQueryOnly();

		// then
		assertEquals("/path?param1=value1¶m2=value2", result);
	}

	@Test
	void shouldAddNewQueryParam()
	{
		// given
		PathWithQueryProvider provider = new PathWithQueryProvider(simpleUrl);
		BasicNameValuePair newParam = new BasicNameValuePair("newParam", "newValue");

		// when
		String result = provider.getPathWithQueryParamsIncluding(newParam);

		// then
		assertEquals("/path?newParam=newValue", result);
	}

	@Test
	void shouldAddNewQueryParamToExistingParams()
	{
		// given
		PathWithQueryProvider provider = new PathWithQueryProvider(urlWithParams);
		BasicNameValuePair newParam = new BasicNameValuePair("param2", "value2");

		// when
		String result = provider.getPathWithQueryParamsIncluding(newParam);

		// then
		assertEquals("/path?param1=value1&param2=value2", result);
	}

	@Test
	void shouldHandleEmptyPath()
	{
		// given
		PathWithQueryProvider provider = new PathWithQueryProvider(emptyPathUrl);

		// when
		String result = provider.getPathAndQueryOnly();

		// then
		assertEquals("", result);
	}

	@Test
	void shouldHandleEmptyPathWithQueryParam() throws MalformedURLException
	{
		// given
		URL url = URLFactory.of("https://example.com?param=value");
		PathWithQueryProvider provider = new PathWithQueryProvider(url);

		// when
		String result = provider.getPathAndQueryOnly();

		// then
		assertEquals("?param=value", result);
	}

	@Test
	void shouldPreserveEncodedQueryParameters()
	{
		// given
		PathWithQueryProvider provider = new PathWithQueryProvider(encodedParamUrl);

		// when
		String result = provider.getPathAndQueryOnly();

		// then
		assertEquals("/path?param=value%20with%20spaces", result);
	}

	@Test
	void shouldAddEncodedQueryParameter()
	{
		// given
		PathWithQueryProvider provider = new PathWithQueryProvider(simpleUrl);
		BasicNameValuePair newParam = new BasicNameValuePair("param", "value with spaces");

		// when
		String result = provider.getPathWithQueryParamsIncluding(newParam);

		// then
		assertEquals("/path?param=value%20with%20spaces", result);
	}

	@Test
	void shouldHandleSpecialCharactersInQueryParams()
	{
		// given
		PathWithQueryProvider provider = new PathWithQueryProvider(simpleUrl);
		BasicNameValuePair newParam = new BasicNameValuePair("param", "!@#$%^&*()");

		// when
		String result = provider.getPathWithQueryParamsIncluding(newParam);

		// then
		assertEquals("/path?param=%21%40%23%24%25%5E%26%2A%28%29", result);
	}
}

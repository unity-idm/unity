/**********************************************************************
 *                     Copyright (c) 2023, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.engine.api.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hc.core5.net.URIBuilder;
import org.junit.jupiter.api.Test;

public class TestURIBuilderFixer
{
	@Test
	public void shouldParseStringWithPlusEncodedSpaceInPath() throws URISyntaxException
	{
		URIBuilder builder = new URIBuilder("foo://some/path:name+ooo");
		builder.addParameter("p", "val");
		URI uri = builder.build();
		
		assertThat(uri.toASCIIString()).isEqualTo("foo://some/path:name+ooo?p=val");
	}

	@Test
	public void shouldParseURIWithPlusEncodedSpaceInParam() throws URISyntaxException
	{
		URIBuilder builder = URIBuilderFixer.newInstance("https://some.domain.com?param1=aaa+bbb");
		builder.addParameter("p", "val");
		URI uri = builder.build();
		
		assertThat(uri.toASCIIString()).isEqualTo("https://some.domain.com?param1=aaa%20bbb&p=val");
	}
	
	@Test
	public void shouldParseURIWithPlusEncodedSpaceInURIParam() throws URISyntaxException
	{
		URI input = new URI("https://some.domain.com?param1=aaa+bbb");
		URIBuilder builder = URIBuilderFixer.newInstance(input);
		builder.addParameter("p", "val");
		URI uri = builder.build();
		
		assertThat(uri.toASCIIString()).isEqualTo("https://some.domain.com?param1=aaa%20bbb&p=val");
	}
	
	@Test
	public void shouldParseURIWithPercentEncodedSpaceInParam() throws URISyntaxException
	{
		URIBuilder builder = new URIBuilder("https://some.domain.com?param1=aaa%20bbb");
		builder.addParameter("p", "val");
		URI uri = builder.build();
		
		assertThat(uri.toASCIIString()).isEqualTo("https://some.domain.com?param1=aaa%20bbb&p=val");
	}
}

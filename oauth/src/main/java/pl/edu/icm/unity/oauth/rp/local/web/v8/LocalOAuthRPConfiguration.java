/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.rp.local.web.v8;

import eu.unicore.util.configuration.ConfigurationException;
import org.eclipse.jetty.util.StringUtil;
import org.springframework.util.CollectionUtils;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.oauth.client.console.v8.OAuthBaseConfiguration;
import pl.edu.icm.unity.oauth.rp.local.LocalOAuthRPProperties;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class LocalOAuthRPConfiguration extends OAuthBaseConfiguration
{
	private List<String> requiredScopes;
	private String credential;

	public LocalOAuthRPConfiguration()
	{

	}

	public void fromProperties(String source)
	{
		Properties raw = new Properties();
		try
		{
			raw.load(new StringReader(source));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the oauth-rp verificator", e);
		}

		LocalOAuthRPProperties oauthRPprop = new LocalOAuthRPProperties(raw);

		setRequiredScopes(oauthRPprop.getListOfValues(LocalOAuthRPProperties.REQUIRED_SCOPES).stream()
				.filter(StringUtil::isNotBlank).collect(Collectors.toList()));
		setCredential(oauthRPprop.getValue(LocalOAuthRPProperties.CREDENTIAL));

	}

	public String toProperties() throws ConfigurationException
	{
		Properties raw = new Properties();

		if (!CollectionUtils.isEmpty(requiredScopes))
		{
			for (int i = 0; i < requiredScopes.size(); i++)
			{
				String scope = requiredScopes.get(i);
				if (StringUtil.isNotBlank(scope))
				{
					raw.put(LocalOAuthRPProperties.PREFIX + LocalOAuthRPProperties.REQUIRED_SCOPES + (i + 1),
							scope.trim());
				}
			}
		}
		if (credential != null)
			raw.put(LocalOAuthRPProperties.PREFIX + LocalOAuthRPProperties.CREDENTIAL, credential);

		LocalOAuthRPProperties prop = new LocalOAuthRPProperties(raw);
		return prop.getAsString();

	}

	public List<String> getRequiredScopes()
	{
		return requiredScopes;
	}

	public void setRequiredScopes(List<String> requiredScopes)
	{
		this.requiredScopes = requiredScopes;
	}

	public String getCredential()
	{
		return credential;
	}

	public void setCredential(String credential)
	{
		this.credential = credential;
	}
}
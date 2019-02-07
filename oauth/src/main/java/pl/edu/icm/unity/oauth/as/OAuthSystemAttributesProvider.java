/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.nimbusds.oauth2.sdk.client.ClientType;

import pl.edu.icm.unity.engine.api.attributes.SystemAttributesProvider;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Provides attribute types used internally by OAuth
 * @author K. Benedyczak
 */
@Component
public class OAuthSystemAttributesProvider implements SystemAttributesProvider
{
	private List<AttributeType> oauthAttributes = new ArrayList<AttributeType>();
	
	public static final String ALLOWED_FLOWS = "sys:oauth:allowedGrantFlows";
	public static final String ALLOWED_RETURN_URI = "sys:oauth:allowedReturnURI";
	public static final String PER_CLIENT_GROUP = "sys:oauth:groupForClient";
	public static final String CLIENT_NAME = "sys:oauth:clientName";
	public static final String CLIENT_LOGO = "sys:oauth:clientLogo";
	public static final String CLIENT_TYPE = "sys:oauth:clientType";
	
	public static final int MAXIMUM_ALLOWED_URIS = 512;
	
	public enum GrantFlow {authorizationCode, implicit, openidHybrid, client};
	
	private UnityMessageSource msg;
	
	@Autowired
	public OAuthSystemAttributesProvider(UnityMessageSource msg)
	{
		this.msg = msg;
		oauthAttributes.add(getAllowedGrantFlowsAT());
		oauthAttributes.add(getClientTypeAT());
		oauthAttributes.add(getAllowedURIsAT());
		oauthAttributes.add(getLogoAT());
		oauthAttributes.add(getNameAT());
		oauthAttributes.add(getPerClientGroupAT());
	}
	
	private AttributeType getAllowedGrantFlowsAT()
	{
		Set<String> allowed = new HashSet<>();
		for (GrantFlow gf: GrantFlow.values())
			allowed.add(gf.toString());
		EnumAttributeSyntax syntax = new EnumAttributeSyntax(allowed);
		AttributeType allowedGrantsAt = new AttributeType(ALLOWED_FLOWS, 
				EnumAttributeSyntax.ID, msg);
		allowedGrantsAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		allowedGrantsAt.setMinElements(1);
		allowedGrantsAt.setMaxElements(5);
		allowedGrantsAt.setUniqueValues(true);
		allowedGrantsAt.setValueSyntaxConfiguration(syntax.getSerializedConfiguration());
		return allowedGrantsAt;
	}
	
	private AttributeType getClientTypeAT()
	{
		Set<String> allowed = Sets.newHashSet(ClientType.CONFIDENTIAL.toString(), ClientType.PUBLIC.toString());
		EnumAttributeSyntax syntax = new EnumAttributeSyntax(allowed);
		AttributeType allowedGrantsAt = new AttributeType(CLIENT_TYPE, 
				EnumAttributeSyntax.ID, msg);
		allowedGrantsAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		allowedGrantsAt.setMinElements(1);
		allowedGrantsAt.setMaxElements(1);
		allowedGrantsAt.setValueSyntaxConfiguration(syntax.getSerializedConfiguration());
		return allowedGrantsAt;
	}
	
	private AttributeType getAllowedURIsAT()
	{
		AttributeType authorizationAt = new AttributeType(ALLOWED_RETURN_URI, StringAttributeSyntax.ID, msg);
		authorizationAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		authorizationAt.setMinElements(1);
		authorizationAt.setMaxElements(MAXIMUM_ALLOWED_URIS);
		authorizationAt.setUniqueValues(false);
		return authorizationAt;
	}
	
	private AttributeType getLogoAT()
	{
		JpegImageAttributeSyntax syntax = new JpegImageAttributeSyntax();
		try
		{
			syntax.setMaxHeight(200);
			syntax.setMaxWidth(400);
			syntax.setMaxSize(4000000);
		} catch (WrongArgumentException e)
		{
			throw new IllegalArgumentException(e);
		}
		AttributeType logoAt = new AttributeType(CLIENT_LOGO, syntax.getValueSyntaxId(), msg);
		logoAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		logoAt.setMinElements(1);
		logoAt.setMaxElements(1);
		logoAt.setUniqueValues(false);
		logoAt.setValueSyntaxConfiguration(syntax.getSerializedConfiguration());
		return logoAt;
	}

	private AttributeType getNameAT()
	{
		AttributeType nameAt = new AttributeType(CLIENT_NAME, StringAttributeSyntax.ID, msg);
		nameAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		nameAt.setMinElements(1);
		nameAt.setMaxElements(1);
		nameAt.setUniqueValues(false);
		return nameAt;
	}

	private AttributeType getPerClientGroupAT()
	{
		AttributeType nameAt = new AttributeType(PER_CLIENT_GROUP, StringAttributeSyntax.ID, msg);
		nameAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		nameAt.setMinElements(1);
		nameAt.setMaxElements(1);
		nameAt.setUniqueValues(false);
		return nameAt;
	}

	@Override
	public List<AttributeType> getSystemAttributes()
	{
		return oauthAttributes;
	}

	@Override
	public boolean requiresUpdate(AttributeType at)
	{
		return false;
	}
}

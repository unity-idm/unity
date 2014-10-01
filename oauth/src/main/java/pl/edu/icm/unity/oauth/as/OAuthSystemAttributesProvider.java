/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.attributes.SystemAttributesProvider;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

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
	public static final String CLIENT_NAME = "sys:oauth:name";
	public static final String CLIENT_LOGO = "sys:oauth:logo";
	
	public enum GrantFlow {authorizationCode, implicit, resourceOwnerPassword, clientCredentials};
	
	
	public OAuthSystemAttributesProvider()
	{
		oauthAttributes.add(getAllowedGrantFlowsAT());
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
		AttributeType allowedGrantsAt = new AttributeType(ALLOWED_FLOWS, new EnumAttributeSyntax(allowed));
		allowedGrantsAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		allowedGrantsAt.setDescription("OAuth Client specific attribute. Defines which grants are allowed "
				+ "for the client. If undefined then only the Authorization Code grant is allowed.");
		allowedGrantsAt.setMinElements(1);
		allowedGrantsAt.setMaxElements(5);
		allowedGrantsAt.setUniqueValues(true);
		allowedGrantsAt.setVisibility(AttributeVisibility.local);
		return allowedGrantsAt;
	}
	
	private AttributeType getAllowedURIsAT()
	{
		AttributeType authorizationAt = new AttributeType(ALLOWED_RETURN_URI, new StringAttributeSyntax());
		authorizationAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		authorizationAt.setDescription("OAuth Client specific attribute. Defines which return redirect URIs"
				+ " are allowed for the client. This is important security measure for the "
				+ "authorization code and implicit grants."
				+ "If undefined then no URI is allowed and both implicit and authorization code grants"
				+ " will be effectively disabled for the client.");
		authorizationAt.setMinElements(1);
		authorizationAt.setMaxElements(5);
		authorizationAt.setUniqueValues(false);
		authorizationAt.setVisibility(AttributeVisibility.local);
		return authorizationAt;
	}
	
	private AttributeType getLogoAT()
	{
		AttributeType logoAt = new AttributeType(CLIENT_LOGO, new JpegImageAttributeSyntax());
		logoAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		logoAt.setDescription("OAuth Client specific attribute. Defines a logo which is "
				+ "displayed for the client.");
		logoAt.setMinElements(1);
		logoAt.setMaxElements(1);
		logoAt.setUniqueValues(false);
		logoAt.setVisibility(AttributeVisibility.local);
		return logoAt;
	}

	private AttributeType getNameAT()
	{
		AttributeType nameAt = new AttributeType(CLIENT_NAME, new StringAttributeSyntax());
		nameAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		nameAt.setDescription("OAuth Client specific attribute. Defines human readable name of the client.");
		nameAt.setMinElements(1);
		nameAt.setMaxElements(1);
		nameAt.setUniqueValues(false);
		nameAt.setVisibility(AttributeVisibility.local);
		return nameAt;
	}

	private AttributeType getPerClientGroupAT()
	{
		AttributeType nameAt = new AttributeType(PER_CLIENT_GROUP, new StringAttributeSyntax());
		nameAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		nameAt.setDescription("OAuth Client specific attribute. Defines a group path, "
				+ "where users of this client should be present and where their attributes are resolved."
				+ " This attribute overrides the default group configured per endpoint.");
		nameAt.setMinElements(1);
		nameAt.setMaxElements(1);
		nameAt.setUniqueValues(false);
		nameAt.setVisibility(AttributeVisibility.local);
		return nameAt;
	}

	@Override
	public List<AttributeType> getSystemAttributes()
	{
		return oauthAttributes;
	}
}

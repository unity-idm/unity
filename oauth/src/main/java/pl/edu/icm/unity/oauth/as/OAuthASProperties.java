/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.stdext.identity.TargetedPersistentIdentity;

/**
 * Configuration of the OAuth AS. Used for OpenID Connect mode too. Shared by web and rest endpoints (authz and token
 * respectively).
 * @author K. Benedyczak
 */
public class OAuthASProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLegacyLogger(Log.U_SERVER_CFG, OAuthASProperties.class);
	
	@DocumentationReferencePrefix
	public static final String P = "unity.oauth2.as.";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> defaults = new HashMap<>();

	public enum SigningAlgorithms
	{
		RS256, RS384, RS512, HS256, HS384, HS512, ES256, ES384, ES512;
	};
	
	public static final String ISSUER_URI = "issuerUri";
	public static final String ACCESS_TOKEN_VALIDITY = "accessTokenValidity";
	public static final String MAX_EXTEND_ACCESS_TOKEN_VALIDITY = "extendAccessTokenValidityUpTo";
	public static final String CODE_TOKEN_VALIDITY = "codeTokenValidity";
	public static final String ID_TOKEN_VALIDITY = "idTokenValidity";
	public static final String REFRESH_TOKEN_VALIDITY = "refreshTokenValidity";
	public static final String CREDENTIAL = "signingCredential";
	public static final String IDENTITY_TYPE_FOR_SUBJECT = "identityTypeForSubject";
	public static final String ALLOW_FOR_WILDCARDS_IN_ALLOWED_URI = "allowForWildcardsInAllowedURI";
	
	public static final String CLIENTS_GROUP = "clientsGroup";
	public static final String USERS_GROUP = "usersGroup";
	
	public static final String SCOPES = "scopes.";
	public static final String SCOPE_ATTRIBUTES = "attributes.";
	public static final String SCOPE_DESCRIPTION = "description";
	public static final String SCOPE_NAME = "name";
	
	public static final String SIGNING_ALGORITHM = "signingAlgorithm"; 
	public static final String SIGNING_SECRET = "signingSecret"; 
	
	static
	{
		defaults.put(ISSUER_URI, new PropertyMD().setMandatory().
				setDescription("This property controls the server's URI which is used in tokens to identify the Authorization Server. "
						+ "To be compliant with OAuth&OIDC specifications it should be the server's URL of the OAuth endpoint."));
		defaults.put(CODE_TOKEN_VALIDITY, new PropertyMD("600").setPositive().
				setDescription("Controls the maximum validity period of a code token returned to a client (in seconds)."));
		defaults.put(ID_TOKEN_VALIDITY, new PropertyMD("3600").setPositive().
				setDescription("Controls the maximum validity period of an OpenID Connect Id token (in seconds)."));
		defaults.put(REFRESH_TOKEN_VALIDITY, new PropertyMD("-1")
				.setDescription("Controls the validity period of a refresh token (in seconds). "
						+ "If is set to a negative number refresh tokens won’t be issued. "
						+ "If is set to 0 refresh tokens will have an unlimited lifetime. "));
		defaults.put(ACCESS_TOKEN_VALIDITY, new PropertyMD("3600").setPositive().
				setDescription("Controls the maximum validity period of an Access token (in seconds)."));
		defaults.put(MAX_EXTEND_ACCESS_TOKEN_VALIDITY, new PropertyMD().setInt().setPositive().
				setDescription("If defined then Unity will extend lifetime of a previously issued access token"
						+ " up to this time (so must be larger then " + ACCESS_TOKEN_VALIDITY + 
						"). Lifetime will be extended on each successful check of the token, and each time"
						+ "the enhancement will be for the standard validity time. "
						+ "However the token won't be ever valid after the time specified in this property."));
		defaults.put(CREDENTIAL, new PropertyMD().setDescription(
				"Name of a credential which is used to sign tokens. "
						+ "Used only for the OpenId Connect mode and when one of RS* or ES* algorithms is set for token signing"));
		defaults.put(IDENTITY_TYPE_FOR_SUBJECT,
				new PropertyMD(TargetedPersistentIdentity.ID).setDescription(
						"Allows for selecting the identity type which is used to create a mandatory "
								+ "'sub' claim of OAuth token. By default the targeted persistent identifier"
						+ " is used, but can be changed to use for instance the global persistent identity."));
		defaults.put(CLIENTS_GROUP, new PropertyMD("/oauth-clients").
				setDescription("Group in which authorized OAuth Clients must be present. "
						+ "OAuth related attributes defined in this group are used"
						+ "to configure the client."));
		defaults.put(USERS_GROUP, new PropertyMD("/").
				setDescription("Group for resolving attributes of OAuth users. "
						+ "Only members of this group can authorize with OAuth."));
		defaults.put(ALLOW_FOR_WILDCARDS_IN_ALLOWED_URI, new PropertyMD("false").
				setDescription("By enabling this option Unity allows to put Ant-style wildcards as allowed return URIs. "
						+ "There are three important implications. "
						+ "(1) this is generally considered as not very secure setting, "
						+ "rather for development infrastructures. (2) The first allowed URI "
						+ "should rather be a plain URI as will be used as "
						+ "a default return URI if client has not provided any. "
						+ "(3) Ant-style wildcards use single star to match arbitrary characters "
						+ "in single path segment and two stars to match strings across path segments."));
		defaults.put(SCOPES, new PropertyMD().setStructuredList(false).
				setDescription("Under this prefix OAuth scopes can be defined. In general scope"
						+ " defines a set of attribtues returned when it is requested."));
		defaults.put(SCOPE_NAME, new PropertyMD().setStructuredListEntry(SCOPES).setMandatory().
				setDescription("Name of the scope as used in OAuth protocol."));
		defaults.put(SCOPE_DESCRIPTION, new PropertyMD().setStructuredListEntry(SCOPES).
				setDescription("Human readable description of the scope meaning."));
		defaults.put(SCOPE_ATTRIBUTES, new PropertyMD().setStructuredListEntry(SCOPES).setList(false).
				setDescription("List of Unity attributes that should be returned when the scope is "
						+ "requested. Note that those attribtues are merely an input to the "
						+ "configured output translation profile."));
		defaults.put(SIGNING_ALGORITHM, new PropertyMD(SigningAlgorithms.RS256)
				.setDescription("An algorithm used for token signing"));
		defaults.put(SIGNING_SECRET, new PropertyMD().setDescription(
				"Secret key used when one of HS* algorithms is set for token signing"));
		
		defaults.putAll(CommonIdPProperties.getDefaults("Name of an output translation profile "
				+ "which can be used to dynamically modify the "
				+ "data being returned on this endpoint. "
				+ "When not defined the default profile is used which simply return all Unity attribtues.", null));
	}
	
	private String baseAddress; 
	private TokenSigner tokenSigner;
	
	public OAuthASProperties(Properties properties, PKIManagement pkiManamgenet, 
			String baseAddress) throws ConfigurationException
	{
		super(P, properties, defaults, log);
		this.baseAddress = baseAddress;
		
		tokenSigner = new TokenSigner(this, pkiManamgenet);
	}

	
	public String getSigningAlgorithm()
	{
		return getEnumValue(SIGNING_ALGORITHM, SigningAlgorithms.class).toString();
	}
	
	public String getSigningSecret()
	{
		return getValue(SIGNING_SECRET);
	}
	
	public TokenSigner getTokenSigner()
	{
		return tokenSigner;
	}
	
	public String getBaseAddress()
	{
		return baseAddress;
	}
	
	public boolean isSkipConsent()
	{
		return getBooleanValue(CommonIdPProperties.SKIP_CONSENT);
	}

	public int getCodeTokenValidity()
	{
		return getIntValue(OAuthASProperties.CODE_TOKEN_VALIDITY);
	}

	public int getAccessTokenValidity()
	{
		return getIntValue(OAuthASProperties.ACCESS_TOKEN_VALIDITY);
	}

	public int getMaxExtendedAccessTokenValidity()
	{
		return isSet(OAuthASProperties.MAX_EXTEND_ACCESS_TOKEN_VALIDITY) ?
				getIntValue(OAuthASProperties.MAX_EXTEND_ACCESS_TOKEN_VALIDITY) : 0;
	}

	public int getIdTokenValidity()
	{
		return getIntValue(OAuthASProperties.ID_TOKEN_VALIDITY);
	}
	
	public int getRefreshTokenValidity()
	{
		return getIntValue(OAuthASProperties.REFRESH_TOKEN_VALIDITY);
	}

	public String getIssuerName()
	{
		return getValue(OAuthASProperties.ISSUER_URI);
	}

	public String getSubjectIdentityType()
	{
		return getValue(OAuthASProperties.IDENTITY_TYPE_FOR_SUBJECT);
	}
}

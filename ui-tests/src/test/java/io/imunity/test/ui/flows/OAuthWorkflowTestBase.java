package io.imunity.test.ui.flows;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttribute;

public abstract class OAuthWorkflowTestBase extends EndUserTestBase
{
	protected static final String CLIENT_GROUP = "/ui-oauth-clients";
	protected static final String CONFIGURATION = """
			unity.oauth2.as.issuerUri=https://localhost:2443/oauth/token
			unity.oauth2.as.signingCredential=MAIN
			unity.oauth2.as.clientsGroup=/ui-oauth-clients
			unity.oauth2.as.usersGroup=/
			unity.oauth2.as.translationProfile=sys:default
			unity.oauth2.as.identityTypeForSubject=userName
			unity.oauth2.as.deviceGrantEnabled=true
			unity.oauth2.as.scopes.1.name=profile
			unity.oauth2.as.scopes.1.attributes.1=name
			unity.oauth2.as.scopes.2.name=openid
			""";

	@Autowired
	@Qualifier("insecure")
	private GroupsManagement groups;

	protected final TestUser oauthClient() throws Exception
	{
		groups.addGroup(new Group(CLIENT_GROUP));
		TestUser client = createUser("Local OAuth client");
		groups.addMemberFromParent(CLIENT_GROUP, client.entity());
		attributes.setAttribute(client.entity(), EnumAttribute.of("sys:oauth:clientType", CLIENT_GROUP, "PUBLIC"));
		attributes.setAttribute(client.entity(), EnumAttribute.of("sys:oauth:allowedGrantFlows", CLIENT_GROUP,
				List.of("deviceCode", "authorizationCode")));
		attributes.setAttribute(client.entity(), StringAttribute.of("sys:oauth:clientName", CLIENT_GROUP,
				"Local Selenium client"));
		attributes.setAttribute(client.entity(), StringAttribute.of("sys:oauth:allowedReturnURI", CLIENT_GROUP,
				baseUrl + "/unitygw/pub/registration/oauth-callback"));
		return client;
	}

	protected final void deployDeviceFlow() throws Exception
	{
		deployAt("OAuth2Token", "/oauth", CONFIGURATION, List.of("pwdSys"));
		deployAt("OAuth2DeviceSignIn", "/device", CONFIGURATION);
	}
}

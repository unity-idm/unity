/*
 * Script with default schema when Unity server is used as OAuth AS.
 * As this is common case, typically should be enabled.
 * 
 * Creates group for oauth clients and creates example client. 
 * Password should be changed though.
 */

 import com.google.common.collect.Lists

import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow
import pl.edu.icm.unity.stdext.attr.EnumAttribute
import pl.edu.icm.unity.stdext.attr.StringAttribute
import pl.edu.icm.unity.stdext.credential.PasswordToken
import pl.edu.icm.unity.stdext.identity.UsernameIdentity
import pl.edu.icm.unity.types.basic.Attribute
import pl.edu.icm.unity.types.basic.EntityParam
import pl.edu.icm.unity.types.basic.EntityState
import pl.edu.icm.unity.types.basic.Group
import pl.edu.icm.unity.types.basic.Identity
import pl.edu.icm.unity.types.basic.IdentityParam

if (!isColdStart)
{
	log.debug("Database already initialized with content, skipping...");
	return;
}

try
{
	groupsManagement.addGroup(new Group("/oauth-clients"));
	IdentityParam oauthClient = new IdentityParam(UsernameIdentity.ID, "oauth-client");
	Identity oauthClientA = entityManagement.addEntity(oauthClient, "Password requirement",
			EntityState.valid, false);
	PasswordToken pToken2 = new PasswordToken("oauth-pass");
	entityCredentialManagement.setEntityCredential(new EntityParam(oauthClientA.getEntityId()), "Password credential",
			pToken2.toJson());
	log.warn("Default OAuth client user was created with default password. Please change it!", e);
	groupsManagement.addMemberFromParent("/oauth-clients", new EntityParam(oauthClientA.getEntityId()));
	Attribute flowsA = EnumAttribute.of(OAuthSystemAttributesProvider.ALLOWED_FLOWS,
			"/oauth-clients",
			Lists.newArrayList(
			GrantFlow.authorizationCode.toString(), GrantFlow.implicit.toString(),
			GrantFlow.openidHybrid.toString()));
	attributesManagement.setAttribute(new EntityParam(oauthClientA.getEntityId()), flowsA, false);
	Attribute returnUrlA = StringAttribute.of(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI,
			"/oauth-clients",
			"https://localhost:2443/unitygw/oauth2ResponseConsumer");
	attributesManagement.setAttribute(new EntityParam(oauthClientA.getEntityId()), returnUrlA, false);
} catch (Exception e)
{
	log.warn("Error loading OAuth demo contents. This can happen and by far is not critical." +
			"It means that demonstration contents was not loaded to your database, " +
			"usaully due to conflict with its existing data", e);
}

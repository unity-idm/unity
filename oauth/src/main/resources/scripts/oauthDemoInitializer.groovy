/*
 * Script with default schema when Unity server is used as OAuth AS.
 * As this is common case, typically should be enabled.
 * 
 * Creates group for oauth clients and creates example client. 
 * Password should be changed though.
 */

import com.google.common.collect.Lists
import pl.edu.icm.unity.engine.server.EngineInitialization
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow
import pl.edu.icm.unity.stdext.attr.EnumAttribute
import pl.edu.icm.unity.stdext.attr.StringAttribute
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken
import pl.edu.icm.unity.stdext.identity.UsernameIdentity
import pl.edu.icm.unity.types.basic.Attribute
import pl.edu.icm.unity.types.basic.EntityParam
import pl.edu.icm.unity.types.basic.EntityState
import pl.edu.icm.unity.types.basic.Group
import pl.edu.icm.unity.types.basic.Identity
import pl.edu.icm.unity.types.basic.IdentityParam
import groovy.transform.Field

@Field final String NAME_ATTR = "name"

if (!isColdStart)
{
	log.debug("Database already initialized with content, skipping...");
	return;
}

try
{
	groupsManagement.addGroup(new Group("/oauth-clients"));
	IdentityParam oauthClient = new IdentityParam(UsernameIdentity.ID, "oauth-client");
	Identity oauthClientA = entityManagement.addEntity(oauthClient,
			EntityState.valid, false);
	PasswordToken pToken2 = new PasswordToken("oauth-pass1");
	
	EntityParam entityP = new EntityParam(oauthClientA.getEntityId());
	entityCredentialManagement.setEntityCredential(entityP, EngineInitialization.DEFAULT_CREDENTIAL, pToken2.toJson());
	log.warn("Default OAuth client user was created with default password.  Please change it! U: oauth-client P: oauth-pass1");
	
	Attribute cnA = StringAttribute.of(NAME_ATTR, "/", "OAuth client");
	attributesManagement.createAttribute(entityP, cnA);
	
	groupsManagement.addMemberFromParent("/oauth-clients", entityP);
	Attribute flowsA = EnumAttribute.of(OAuthSystemAttributesProvider.ALLOWED_FLOWS,
			"/oauth-clients",
			Lists.newArrayList(
			GrantFlow.authorizationCode.toString(), GrantFlow.implicit.toString(),
			GrantFlow.openidHybrid.toString()));
	attributesManagement.createAttribute(entityP, flowsA);
	Attribute returnUrlA = StringAttribute.of(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI,
			"/oauth-clients",
			"https://localhost:2443/unitygw/oauth2ResponseConsumer");
	attributesManagement.createAttribute(entityP, returnUrlA);
} catch (Exception e)
{
	log.warn("Error loading OAuth demo contents. This can happen and by far is not critical." +
			"It means that demonstration contents was not loaded to your database, " +
			"usaully due to conflict with its existing data", e);
}

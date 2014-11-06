/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.ServerInitializer;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

import com.google.common.collect.Lists;

/**
 * Insert demonstrative contents for the OAuth AS functionality.
 * @author K. Benedyczak
 */
@Component
public class OAuthDemoContentsInitializer implements ServerInitializer
{
	private static Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthDemoContentsInitializer.class);
	public static final String NAME = "oauthDemoInitializer";
	private GroupsManagement groupsMan;
	private IdentitiesManagement idsMan;
	private AttributesManagement attrMan;
	
	@Autowired
	public OAuthDemoContentsInitializer(@Qualifier("insecure") GroupsManagement groupsMan, 
			@Qualifier("insecure") IdentitiesManagement idsMan,
			@Qualifier("insecure") AttributesManagement attrMan)
	{
		this.groupsMan = groupsMan;
		this.idsMan = idsMan;
		this.attrMan = attrMan;
	}
	
	@Override
	public void run()
	{
		try
		{
			groupsMan.addGroup(new Group("/oauth-clients"));
			IdentityParam oauthClient = new IdentityParam(UsernameIdentity.ID, "oauth-client");
			Identity oauthClientA = idsMan.addEntity(oauthClient, "Password requirement", 
					EntityState.valid, false);
			PasswordToken pToken2 = new PasswordToken("oauth-pass");
			idsMan.setEntityCredential(new EntityParam(oauthClientA.getEntityId()), "Password credential", 
					pToken2.toJson());
			groupsMan.addMemberFromParent("/oauth-clients", new EntityParam(oauthClientA.getEntityId()));
			EnumAttribute flowsA = new EnumAttribute(OAuthSystemAttributesProvider.ALLOWED_FLOWS, 
					"/oauth-clients", 
					AttributeVisibility.local, 
					Lists.newArrayList(
					GrantFlow.authorizationCode.toString(), GrantFlow.implicit.toString(),
					GrantFlow.openidHybrid.toString()));
			attrMan.setAttribute(new EntityParam(oauthClientA.getEntityId()), flowsA, false);
			StringAttribute returnUrlA = new StringAttribute(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI, 
					"/oauth-clients", AttributeVisibility.local, 
					"https://localhost:2443/unitygw/oauth2ResponseConsumer");
			attrMan.setAttribute(new EntityParam(oauthClientA.getEntityId()), returnUrlA, false);
		} catch (Exception e)
		{
			log.warn("Error loading OAuth demo contents. This can happen and by far is not critical." +
					"It means that demonstration contents was not loaded to your database, " +
					"usaully due to conflict with its existing data", e);
		}
	}

	@Override
	public String getName()
	{
		return NAME;
	}
}

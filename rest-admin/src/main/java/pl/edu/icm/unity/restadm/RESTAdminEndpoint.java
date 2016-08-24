/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.BulkProcessingManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.UserImportManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.confirmation.ConfirmationManager;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.rest.RESTEndpoint;

/**
 * RESTful endpoint providing administration and query API.
 * 
 * @author K. Benedyczak
 */
public class RESTAdminEndpoint extends RESTEndpoint
{
	private EntityManagement identitiesMan;
	private GroupsManagement groupsMan;
	private AttributesManagement attributesMan;
	private IdentityTypesRegistry identityTypesRegistry;
	private ConfirmationManager confirmationManager;
	private EndpointManagement endpointManagement;
	private RegistrationsManagement registrationManagement;
	private BulkProcessingManagement bulkProcessingManagement;
	private UserImportManagement userImportManagement;
	private EntityCredentialManagement entityCredMan;
	private AttributeTypeManagement attributeTypeMan;
	private InvitationManagement invitationMan;
	
	public RESTAdminEndpoint(UnityMessageSource msg, SessionManagement sessionMan,
			NetworkServer server, String servletPath, EntityManagement identitiesMan,
			GroupsManagement groupsMan, AttributesManagement attributesMan, 
			AuthenticationProcessor authnProcessor, IdentityTypesRegistry identityTypesRegistry,
			ConfirmationManager confirmationManager,
			EndpointManagement endpointManagement,
			RegistrationsManagement registrationManagement, 
			BulkProcessingManagement bulkProcessingManagement, 
			UserImportManagement userImportManagement,
			EntityCredentialManagement entityCredMan,
			AttributeTypeManagement attributeTypeMan,
			InvitationManagement invitationMan)
	{
		super(msg, sessionMan, authnProcessor, server, servletPath);
		this.identitiesMan = identitiesMan;
		this.groupsMan = groupsMan;
		this.attributesMan = attributesMan;
		this.identityTypesRegistry = identityTypesRegistry;
		this.confirmationManager = confirmationManager;
		this.endpointManagement = endpointManagement;
		this.registrationManagement = registrationManagement;
		this.bulkProcessingManagement = bulkProcessingManagement;
		this.userImportManagement = userImportManagement;
		this.entityCredMan = entityCredMan;
		this.attributeTypeMan = attributeTypeMan;
		this.invitationMan = invitationMan;
	}

	@Override
	protected Application getApplication()
	{
		return new RESTAdminJAXRSApp();
	}

	@ApplicationPath("/")
	public class RESTAdminJAXRSApp extends Application
	{
		@Override 
		public Set<Object> getSingletons() 
		{
			HashSet<Object> ret = new HashSet<>();
			ret.add(new RESTAdmin(identitiesMan, groupsMan, attributesMan, identityTypesRegistry, 
					confirmationManager, endpointManagement, registrationManagement, 
					bulkProcessingManagement, userImportManagement, 
					entityCredMan, attributeTypeMan, invitationMan));
			installExceptionHandlers(ret);
			return ret;
		}
	}
}

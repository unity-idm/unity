/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import pl.edu.icm.unity.confirmations.ConfirmationManager;
import pl.edu.icm.unity.json.AttributeTypeSerializer;
import pl.edu.icm.unity.rest.RESTEndpoint;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.BulkProcessingManagement;
import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.authn.AuthenticationProcessor;
import pl.edu.icm.unity.server.registries.AttributeSyntaxFactoriesRegistry;
import pl.edu.icm.unity.server.registries.EntityActionsRegistry;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

/**
 * RESTful endpoint providing administration and query API.
 * 
 * @author K. Benedyczak
 */
public class RESTAdminEndpoint extends RESTEndpoint
{
	private IdentitiesManagement identitiesMan;
	private GroupsManagement groupsMan;
	private AttributesManagement attributesMan;
	private IdentityTypesRegistry identityTypesRegistry;
	private AttributeTypeSerializer attributeTypeSerializer;
	private AttributeSyntaxFactoriesRegistry attributeSyntaxFactoriesRegistry;
	private ConfirmationManager confirmationManager;
	private EndpointManagement endpointManagement;
	private RegistrationsManagement registrationManagement;
	private BulkProcessingManagement bulkProcessingManagement;
	private EntityActionsRegistry entityActionsRegistry;
	
	public RESTAdminEndpoint(UnityMessageSource msg, SessionManagement sessionMan,
			NetworkServer server, String servletPath, IdentitiesManagement identitiesMan,
			GroupsManagement groupsMan, AttributesManagement attributesMan, 
			AuthenticationProcessor authnProcessor, IdentityTypesRegistry identityTypesRegistry,
			AttributeTypeSerializer attributeTypeSerializer,
			AttributeSyntaxFactoriesRegistry attributeSyntaxFactoriesRegistry,
			ConfirmationManager confirmationManager,
			EndpointManagement endpointManagement,
			RegistrationsManagement registrationManagement, 
			BulkProcessingManagement bulkProcessingManagement, EntityActionsRegistry entityActionsRegistry)
	{
		super(msg, sessionMan, authnProcessor, server, servletPath);
		this.identitiesMan = identitiesMan;
		this.groupsMan = groupsMan;
		this.attributesMan = attributesMan;
		this.identityTypesRegistry = identityTypesRegistry;
		this.attributeTypeSerializer = attributeTypeSerializer;
		this.attributeSyntaxFactoriesRegistry = attributeSyntaxFactoriesRegistry;
		this.confirmationManager = confirmationManager;
		this.endpointManagement = endpointManagement;
		this.registrationManagement = registrationManagement;
		this.bulkProcessingManagement = bulkProcessingManagement;
		this.entityActionsRegistry = entityActionsRegistry;
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
					attributeTypeSerializer, attributeSyntaxFactoriesRegistry,
					confirmationManager, endpointManagement, registrationManagement,
					bulkProcessingManagement, entityActionsRegistry));
			installExceptionHandlers(ret);
			return ret;
		}
	}
}

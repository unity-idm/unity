/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

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
import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

/**
 * Factory for {@link RESTAdminEndpoint}.
 * @author K. Benedyczak
 */
@Component
public class RESTAdminEndpointFactory implements EndpointFactory
{
	public static final String NAME = "RESTAdmin";
	public static final String V1_PATH = "/v1";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(
			NAME, "A RESTful endpoint exposing Unity management API.", 
			Collections.singleton(JAXRSAuthentication.NAME),
			Collections.singletonMap(V1_PATH, "The REST management base path"));
	
	@Autowired
	private UnityMessageSource msg;
	@Autowired
	private SessionManagement sessionMan;
	@Autowired
	private EntityManagement identitiesMan;
	@Autowired
	private GroupsManagement groupsMan;
	@Autowired
	private AttributesManagement attributesMan;
	@Autowired
	private AuthenticationProcessor authnProcessor;
	@Autowired
	private IdentityTypesRegistry identityTypesRegistry;
	@Autowired
	private ConfirmationManager confirmationManager;
	@Autowired
	private NetworkServer server;
	@Autowired
	private RegistrationsManagement registrationManagement;
	@Autowired
	private BulkProcessingManagement bulkProcessingManagement;
	@Autowired
	private UserImportManagement userImportManagement;
	@Autowired
	private EntityCredentialManagement entityCredMan;
	@Autowired
	private AttributeTypeManagement attributeTypeMan;
	@Autowired
	private InvitationManagement invitationMan;
	
	/**
	 * We depend on app context in order to work around of the dependency cycle. 
	 * We do depend on EndpointsManagement, however it requires this factory to be instantiated first 
	 * and registered. Therefore EndpointsManagement is retrieved only on endpoint creation.
	 */
	@Autowired
	private ApplicationContext appContext;
	
	@Override
	public EndpointTypeDescription getDescription()
	{
		return TYPE;
	}

	@Override
	public EndpointInstance newInstance()
	{
		EndpointManagement endpointManagement = appContext.getBean(EndpointManagement.class);
		return new RESTAdminEndpoint(msg, sessionMan, server, "", identitiesMan, groupsMan, 
				attributesMan, authnProcessor, identityTypesRegistry, confirmationManager, 
				endpointManagement, registrationManagement, bulkProcessingManagement, 
				userImportManagement, entityCredMan, attributeTypeMan, invitationMan);
	}

}

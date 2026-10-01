package io.imunity.upman.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.rest.api.types.registration.RestEnquiryForm;
import io.imunity.rest.api.types.registration.RestRegistrationForm;
import io.imunity.upman.rest.RestProjectFormServiceNoAuthz.RestProjectFormServiceNoAuthzFactory;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;

class RestProjectFormService
{
	private final UpmanRestAuthorizationManager authz;
	private final RestProjectFormServiceNoAuthz service;
	private final String authorizationGroup;

	RestProjectFormService(UpmanRestAuthorizationManager authz, RestProjectFormServiceNoAuthz service,
			String authorizationGroup)
	{
		this.authz = authz;
		this.service = service;
		this.authorizationGroup = authorizationGroup;
	}

	void generateRegistrationForm(String projectId) throws EngineException
	{
		assertAuthorization(projectId);
		service.generateRegistrationForm(projectId);
	}

	void addRegistrationForm(String projectId, RestRegistrationForm form) throws EngineException
	{
		assertAuthorization(projectId);
		service.addRegistrationForm(projectId, form);
	}

	RestRegistrationForm getRegistrationForm(String projectId) throws EngineException
	{
		assertAuthorization(projectId);
		return service.getRegistrationForm(projectId);
	}

	void removeRegistrationForm(String projectId, boolean dropRequests) throws EngineException
	{
		assertAuthorization(projectId);
		service.removeRegistrationForm(projectId, dropRequests);
	}

	void updateRegistrationForm(String projectId, RestRegistrationForm registrationForm, boolean ignoreRequests) throws EngineException
	{
		assertAuthorization(projectId);
		service.updateRegistrationForm(projectId, registrationForm, ignoreRequests);
	}

	void generateSignupEnquiryForm(String projectId) throws EngineException
	{
		assertAuthorization(projectId);
		service.generateSignupEnquiryForm(projectId);
	}

	void addSignupEnquiryForm(String projectId, RestEnquiryForm form) throws EngineException
	{
		assertAuthorization(projectId);
		service.addSignupEnquiryForm(projectId, form);
	}

	RestEnquiryForm getSignupEnquiryForm(String projectId) throws EngineException
	{
		assertAuthorization(projectId);

		return service.getSignupEnquiryForm(projectId);
	}

	void removeSignupEnquiryForm(String projectId, boolean dropRequests) throws EngineException
	{
		assertAuthorization(projectId);
		service.removeSignupEnquiryForm(projectId, dropRequests);

	}

	void updateSignupEnquiryForm(String projectId, RestEnquiryForm restEnquiryForm, boolean ignoreRequests) throws EngineException
	{
		assertAuthorization(projectId);
		service.updateSignupEnquiryForm(projectId, restEnquiryForm, ignoreRequests);
	}

	void generateMembershipUpdateEnquiryForm(String projectId) throws EngineException
	{
		assertAuthorization(projectId);
		service.generateMembershipUpdateEnquiryForm(projectId);
		
	}

	void addMembershipUpdateEnquiryForm(String projectId, RestEnquiryForm form) throws EngineException
	{
		assertAuthorization(projectId);
		service.addMembershipUpdateEnquiryForm(projectId, form);
	}

	RestEnquiryForm getMembershipUpdateEnquiryForm(String projectId) throws EngineException
	{
		assertAuthorization(projectId);
		return service.getMembershipUpdateEnquiryForm(projectId);
	}

	void removeMembershipUpdateEnquiryForm(String projectId, boolean dropRequests) throws EngineException
	{
		assertAuthorization(projectId);
		service.removeMembershipUpdateEnquiryForm(projectId, dropRequests);

	}

	void updateMembershipUpdateEnquiryForm(String projectId, RestEnquiryForm restEnquiryForm, boolean ignoreRequests) throws EngineException
	{
		assertAuthorization(projectId);
		service.updateMembershipUpdateEnquiryForm(projectId, restEnquiryForm, ignoreRequests);
	}

	private void assertAuthorization(String projectId) throws AuthorizationException
	{
		authz.assertProjectAuthorization(authorizationGroup, projectId);
	}


	@Component
	public static class RestProjectFormServiceFactory
	{
		private final UpmanRestAuthorizationManager authz;
		private final RestProjectFormServiceNoAuthzFactory serviceFactory;

		@Autowired
		RestProjectFormServiceFactory(UpmanRestAuthorizationManager authz,
				RestProjectFormServiceNoAuthzFactory serviceFactory)
		{
			this.authz = authz;
			this.serviceFactory = serviceFactory;
		}

		RestProjectFormService newInstance(String rootGroup, String authorizeGroup, List<String> rootGroupAttributes)
		{
			return new RestProjectFormService(authz, serviceFactory.newInstance(rootGroup, rootGroupAttributes),
					authorizeGroup);
		}
	}
}

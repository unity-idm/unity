package io.imunity.upman.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.rest.api.types.registration.RestEnquiryForm;
import io.imunity.rest.api.types.registration.RestRegistrationForm;
import io.imunity.upman.rest.RestProjectFormServiceNoAuthz.RestProjectFormServiceNoAuthzFactory;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;

class RestProjectFormService
{
	private final UpmanRestAuthorizationManager authz;
	private RestProjectFormServiceNoAuthz service;
	private String authorizationGroup;

	RestProjectFormService(UpmanRestAuthorizationManager authz, RestProjectFormServiceNoAuthz service,
			String authorizationGroup)
	{
		this.authz = authz;
		this.service = service;
		this.authorizationGroup = authorizationGroup;
	}

	void generateRegistrationForm(String projectId) throws EngineException
	{
		assertAuthorization();
		service.generateRegistrationForm(projectId);
	}

	void addRegistrationForm(String projectId, RestRegistrationForm form) throws EngineException
	{
		assertAuthorization();
		service.addRegistrationForm(projectId, form);
	}

	RestRegistrationForm getRegistrationForm(String projectId) throws EngineException
	{
		assertAuthorization();
		return service.getRegistrationForm(projectId);
	}

	void removeRegistrationForm(String projectId) throws EngineException
	{
		assertAuthorization();
		service.removeRegistrationForm(projectId);
	}

	void updateRegistrationForm(String projectId, RestRegistrationForm registrationForm) throws EngineException
	{
		assertAuthorization();
		service.updateRegistrationForm(projectId, registrationForm);
	}

	void generateSignupEnquiryForm(String projectId) throws EngineException
	{
		assertAuthorization();
		service.generateSignupEnquiryForm(projectId);
	}

	void addSignupEnquiryForm(String projectId, RestEnquiryForm form) throws EngineException
	{
		assertAuthorization();
		service.addSignupEnquiryForm(projectId, form);
	}

	RestEnquiryForm getSignupEnquiryForm(String projectId) throws EngineException
	{
		assertAuthorization();

		return service.getSignupEnquiryForm(projectId);
	}

	void removeSignupEnquiryForm(String projectId) throws EngineException
	{
		assertAuthorization();
		service.removeSignupEnquiryForm(projectId);

	}

	void updateSignupEnquiryForm(String projectId, RestEnquiryForm restEnquiryForm) throws EngineException
	{
		assertAuthorization();
		service.updateSignupEnquiryForm(projectId, restEnquiryForm);
	}

	void generateMembershipUpdateEnquiryForm(String projectId) throws EngineException
	{
		assertAuthorization();
		service.generateMembershipUpdateEnquiryForm(projectId);
		
	}

	void addMembershipUpdateEnquiryForm(String projectId, RestEnquiryForm form) throws EngineException
	{
		assertAuthorization();
		service.addMembershipUpdateEnquiryForm(projectId, form);
	}

	RestEnquiryForm getMembershipUpdateEnquiryForm(String projectId) throws EngineException
	{
		assertAuthorization();
		return service.getMembershipUpdateEnquiryForm(projectId);
	}

	void removeMembershipUpdateEnquiryForm(String projectId) throws EngineException
	{
		assertAuthorization();
		service.removeMembershipUpdateEnquiryForm(projectId);

	}

	void updateMembershipUpdateEnquiryForm(String projectId, RestEnquiryForm restEnquiryForm) throws EngineException
	{
		assertAuthorization();
		service.updateMembershipUpdateEnquiryForm(projectId, restEnquiryForm);
	}

	private void assertAuthorization() throws AuthorizationException
	{
		authz.assertManagerAuthorization(authorizationGroup);
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

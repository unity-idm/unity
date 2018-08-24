/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.reg;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialParamValue;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;

/**
 * Utility class with handy methods used during automatic processing of
 * invitation action.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
final class AutoProcessInvitationUtil
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER, AutoProcessInvitationUtil.class);

	static RegistrationRequest merge(RegistrationForm formToSubmit, RegistrationRequestState currentRequest,
			InvitationWithCode invitation)
	{
		RegistrationRequest mergedRequest = new RegistrationRequest();
		mergedRequest.setFormId(formToSubmit.getName());
		mergeAttributes(mergedRequest, formToSubmit, currentRequest, invitation);
		return mergedRequest;
	}
	
	private static void mergeAttributes(RegistrationRequest mergedRequest, RegistrationForm formToSubmit,
			RegistrationRequestState currentRequest, InvitationWithCode invitation)
	{
		List<AttributeRegistrationParam> toSubmitAttrs = formToSubmit.getAttributeParams();
		Collection<PrefilledEntry<Attribute>> invitationAttrs = invitation.getAttributes().values();
		List<Attribute> currentAttrs = currentRequest.getRequest().getAttributes();
		
		throw new RuntimeException("Implement me!");
	}
	
	private static <T> T getOrCreate(Map<String, T> consolidatedParams, String type, Supplier<T> ctor)
	{
		T consolidatedParam = consolidatedParams.get(type);
		if (consolidatedParam == null)
		{
			consolidatedParam = ctor.get();
			consolidatedParams.put(type, consolidatedParam);
		}
		return consolidatedParam;
	}
	
	static class ConsolidatedAttributes
	{
		public AttributeRegistrationParam form;
		public PrefilledEntry<Attribute> invitation;
		public Attribute current;
	}

	/**
	 * The logic behind assessing whether the invitation can be auto
	 * processed or not is that, the user in current registration request
	 * already accepted agreements and provided some some data like
	 * identities, credentials, groups, attributes. We are assuming that
	 * identities and credentials must match, provided in current request
	 * means already in the system and processing is possible.
	 * 
	 * Also form can be submitted when there are no agreements, or the form
	 * to submit and the one from original request are the same - this means
	 * user already confirmed the desired agreements.
	 */
	static boolean isAutoProcessingOfInvitationFeasible(RegistrationForm formToSubmit,
			RegistrationRequestState currentRequest, String registrationCode)
	{
		if (!isAgreementsMatch(formToSubmit, currentRequest, registrationCode))
		{
			return false;
		}
		if (!isIdentityTypesMatch(formToSubmit, currentRequest, registrationCode))
		{
			return false;
		}
		if (!isCredentialsMatch(formToSubmit, currentRequest, registrationCode))
		{
			return false;
		}
		return true;
	}

	private static boolean isCredentialsMatch(RegistrationForm formToSubmit, RegistrationRequestState currentRequest,
			String registrationCode)
	{
		Set<String> credNamesToSubmit = formToSubmit.getCredentialParams().stream()
				.map(CredentialRegistrationParam::getCredentialName)
				.collect(Collectors.toSet());
		Set<String> currentCreds = currentRequest.getRequest().getCredentials().stream()
				.map(CredentialParamValue::getCredentialId)
				.collect(Collectors.toSet());
		
		boolean isCredentialsMatch = credNamesToSubmit.equals(currentCreds);
		if (!isCredentialsMatch)
		{
			LOG.warn("Auto processing of invitation {} from the registration request {} is not possible, "
					+ "credentials does not match. Credentials from the registration to submit {}, "
					+ "Credentials from the current request {}", registrationCode, currentRequest.getRequestId(), 
					credNamesToSubmit, currentCreds);
		}
		
		return isCredentialsMatch;
	}

	private static boolean isIdentityTypesMatch(RegistrationForm formToSubmit, RegistrationRequestState currentRequest,
			String registrationCode)
	{
		Set<String> identitiesToSubmit = formToSubmit.getIdentityParams().stream()
				.map(IdentityRegistrationParam::getIdentityType)
				.collect(Collectors.toSet());
		Set<String> currentIdentities = currentRequest.getRequest().getIdentities().stream()
				.map(IdentityParam::getTypeId)
				.collect(Collectors.toSet());
		
		boolean isIdentitiesMatch = identitiesToSubmit.equals(currentIdentities);
		if (!isIdentitiesMatch)
		{
			LOG.warn("Auto processing of invitation {} from the registration request {} is not possible, "
					+ "identities does not match. Identities from the registration to submit {}, identities "
					+ "from the current request {}", registrationCode, currentRequest.getRequestId(), 
					identitiesToSubmit, currentIdentities);
		}
		return isIdentitiesMatch;
	}

	private static boolean isAgreementsMatch(RegistrationForm formToSubmit, RegistrationRequestState currentRequest,
			String registrationCode)
	{
		if (formToSubmit.getAgreements().isEmpty()
				|| formToSubmit.getName().equals(currentRequest.getRequest().getFormId()))
		{
			return true;
		}
		LOG.warn("Auto processing of invitation {} from the registration request {} is not possible, "
				+ "agreements does not match. Accepting the agreements which where not confirmed by user "
				+ "is not allowed.", registrationCode, currentRequest.getRequestId());
		return false;
	}

	private AutoProcessInvitationUtil()
	{
	}
}

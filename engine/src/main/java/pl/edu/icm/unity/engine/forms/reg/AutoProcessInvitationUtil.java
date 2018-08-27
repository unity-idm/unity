/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.reg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.google.common.base.Functions;
import com.google.common.collect.Maps;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.EngineException;
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

	/**
	 * The following merge is valid with the assumption that @param formToSubmit
	 * and @param currentRequest has the same:
	 * <ul>
	 * <li>credentials or no credentials
	 * <li>agreements or no agreements
	 * <li>identities or no identities
	 * </ul>
	 * 
	 * @see #isAutoProcessingOfInvitationFeasible(RegistrationForm,
	 *      RegistrationRequestState, String)
	 */
	static RegistrationRequest merge(RegistrationForm formToSubmit, RegistrationRequestState currentRequest,
			InvitationWithCode invitation) throws EngineException
	{
		RegistrationRequest mergedRequest = new RegistrationRequest();
		mergedRequest.setFormId(formToSubmit.getName());
		mergedRequest.setRegistrationCode(invitation.getRegistrationCode());
		mergeAttributes(mergedRequest, formToSubmit, currentRequest, invitation);
		mergeGroups(mergedRequest, formToSubmit, currentRequest, invitation);
		return mergedRequest;
	}
	
	private static void mergeGroups(RegistrationRequest mergedRequest, RegistrationForm formToSubmit,
			RegistrationRequestState currentRequest, InvitationWithCode invitation)
	{
		if (formToSubmit.getGroupParams() == null || formToSubmit.getGroupParams().isEmpty())
			return;
		
		
	}
	
	private static void mergeAttributes(RegistrationRequest mergedRequest, RegistrationForm formToSubmit,
			RegistrationRequestState currentRequest, InvitationWithCode invitation) throws EngineException
	{
		if (formToSubmit.getAttributeParams() == null || formToSubmit.getAttributeParams().isEmpty())
			return;
		
		List<Attribute> attributeList = new ArrayList<>();
		Map<ConsolidatedAttrKey, ConsolidatedAttributes> consolidatedAttrs = getConsolidatedAttrs(
				formToSubmit, currentRequest, invitation);
		for (AttributeRegistrationParam formAttrParam : formToSubmit.getAttributeParams())
		{
			ConsolidatedAttrKey key = new ConsolidatedAttrKey(formAttrParam);
			ConsolidatedAttributes consolidated = consolidatedAttrs.get(key);
			if (consolidated.current != null)
			{
				LOG.debug("Attribute {} already assigned to entity in current request, no need to do it second time. "
						+ "Autoprocessing of invitation {} from registration {}", key, invitation.getRegistrationCode(),
						currentRequest.getRequestId());
				continue;
			}
			if (!isValueInInvitation(consolidated))
			{
				String error = String.format("It is not possible to auto process invitation %s based on registration %s,"
						+ " unable to determine the value(s) of the \"%s\" attribute within \"%s\" group.", 
						invitation.getRegistrationCode(), currentRequest.getRequestId(), formAttrParam.getAttributeType(),
						formAttrParam.getGroup());
				throw new EngineException(error);
			}
			Attribute newAttr = createAttribute(consolidated.form, consolidated.invitation.getEntry());
			attributeList.add(newAttr);
		}
		
		mergedRequest.setAttributes(attributeList);
	}
	
	private static boolean isValueInInvitation(ConsolidatedAttributes consolidated)
	{
		return consolidated.invitation != null
				&& consolidated.invitation.getEntry() != null
				&& consolidated.invitation.getEntry().getValues() != null
				&& !consolidated.invitation.getEntry().getValues().isEmpty();
	}

	private static Attribute createAttribute(AttributeRegistrationParam registrationForm, Attribute attrFrom)
	{
		return new Attribute(registrationForm.getAttributeType(), attrFrom.getValueSyntax(), 
				registrationForm.getGroup(), attrFrom.getValues(), attrFrom.getRemoteIdp(), 
				attrFrom.getTranslationProfile());
	}

	private static Map<ConsolidatedAttrKey, ConsolidatedAttributes> getConsolidatedAttrs(RegistrationForm formToSubmit, 
			RegistrationRequestState currentRequest, InvitationWithCode invitation)
	{
		Map<ConsolidatedAttrKey, ConsolidatedAttributes> consolidated = Maps.newHashMap();
		
		List<AttributeRegistrationParam> toSubmitAttrs = formToSubmit.getAttributeParams();
		if (toSubmitAttrs != null && !toSubmitAttrs.isEmpty())
		{
			Map<ConsolidatedAttrKey, AttributeRegistrationParam> toSubmitAttrsMap = toSubmitAttrs.stream()
					.collect(Collectors.toMap(
							ConsolidatedAttrKey::new, 
							Functions.identity()));
			toSubmitAttrsMap.forEach((key, value) -> {
				ConsolidatedAttributes consolidatedAttrs = getOrCreate(consolidated, key, ConsolidatedAttributes::new);
				consolidatedAttrs.form = value;
			});
		}
		
		if (invitation.getAttributes() != null && !invitation.getAttributes().isEmpty())
		{
			Collection<PrefilledEntry<Attribute>> invitationAttrs = invitation.getAttributes().values();
			Map<ConsolidatedAttrKey, PrefilledEntry<Attribute>> invitationAttrsMap = invitationAttrs.stream()
					.collect(Collectors.toMap(
							ConsolidatedAttrKey::new, 
							Functions.identity()));
			invitationAttrsMap.forEach((key, value) -> {
				ConsolidatedAttributes consolidatedAttrs = getOrCreate(consolidated, key, ConsolidatedAttributes::new);
				consolidatedAttrs.invitation = value;
			});
		}
		
		List<Attribute> currentAttrs = currentRequest.getRequest().getAttributes();
		if (currentAttrs != null && !currentAttrs.isEmpty())
		{
			Map<ConsolidatedAttrKey, Attribute> currentAttrsMap = currentAttrs.stream()
					.collect(Collectors.toMap(
							ConsolidatedAttrKey::new, 
							Functions.identity()));
			currentAttrsMap.forEach((key, value) -> {
				ConsolidatedAttributes consolidatedAttrs = getOrCreate(consolidated, key, ConsolidatedAttributes::new);
				consolidatedAttrs.current = value;
			});
		}
		return consolidated;
	}

	private static <T> T getOrCreate(Map<ConsolidatedAttrKey, T> consolidatedParams, ConsolidatedAttrKey key, Supplier<T> ctor)
	{
		T consolidatedParam = consolidatedParams.get(key);
		if (consolidatedParam == null)
		{
			consolidatedParam = ctor.get();
			consolidatedParams.put(key, consolidatedParam);
		}
		return consolidatedParam;
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
		if (formToSubmit.getCredentialParams() == null || formToSubmit.getCredentialParams().isEmpty())
			return true;
		
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
		if (formToSubmit.getIdentityParams() == null || formToSubmit.getIdentityParams().isEmpty())
			return true;
		
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
		if (formToSubmit.getAgreements() == null
				|| formToSubmit.getAgreements().isEmpty()
				|| formToSubmit.getName().equals(currentRequest.getRequest().getFormId()))
		{
			return true;
		}
		LOG.warn("Auto processing of invitation {} from the registration request {} is not possible, "
				+ "agreements does not match. Accepting the agreements which where not confirmed by user "
				+ "is not allowed.", registrationCode, currentRequest.getRequestId());
		return false;
	}

	
	static class ConsolidatedAttrKey
	{
		String group;
		String attributeType;
		ConsolidatedAttrKey(String group, String attributeType)
		{
			this.group = group;
			this.attributeType = attributeType;
		}
		ConsolidatedAttrKey(AttributeRegistrationParam registrationParam)
		{
			this(registrationParam.getGroup(), registrationParam.getAttributeType());
		}
		ConsolidatedAttrKey(PrefilledEntry<Attribute> invitationAttr)
		{
			this(invitationAttr.getEntry());
		}
		ConsolidatedAttrKey(Attribute attr)
		{
			this(attr.getGroupPath(), attr.getName());
		}
		@Override
		public String toString()
		{
			return group + "//" + attributeType;
		}
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((attributeType == null) ? 0 : attributeType.hashCode());
			result = prime * result + ((group == null) ? 0 : group.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ConsolidatedAttrKey other = (ConsolidatedAttrKey) obj;
			if (attributeType == null)
			{
				if (other.attributeType != null)
					return false;
			} else if (!attributeType.equals(other.attributeType))
				return false;
			if (group == null)
			{
				if (other.group != null)
					return false;
			} else if (!group.equals(other.group))
				return false;
			return true;
		}
	}
	
	static class ConsolidatedAttributes
	{
		public AttributeRegistrationParam form;
		public PrefilledEntry<Attribute> invitation;
		public Attribute current;
	}
	
	private AutoProcessInvitationUtil()
	{
	}
}

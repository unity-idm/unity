/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.entity.IdentityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.IdentityExistsException;
import pl.edu.icm.unity.base.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.exceptions.IllegalFormContentsException.Category;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.BaseRegistrationInput;
import pl.edu.icm.unity.base.registration.ConfirmationMode;
import pl.edu.icm.unity.base.registration.CredentialParamValue;
import pl.edu.icm.unity.base.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.base.registration.GroupRegistrationParam;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.base.registration.OptionalRegistrationParam;
import pl.edu.icm.unity.base.registration.RegistrationParam;
import pl.edu.icm.unity.base.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.base.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.base.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.base.verifiable.VerifiableElement;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.identity.UnknownIdentityException;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.engine.api.translation.form.GroupParam;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.credential.CredentialRepository;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.generic.InvitationDB;

/**
 * Helper component with methods to validate {@link BaseRegistrationInput}. 
 * @author K. Benedyczak
 */
@Component
public class BaseRequestPreprocessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_FORMS, BaseRequestPreprocessor.class);
	
	private final CredentialRepository credentialRepository;
	private final AttributeTypeDAO dbAttributes;
	private final GroupDAO dbGroups;
	private final AttributesHelper attributesHelper;
	private final AttributeTypeHelper attributeTypesHelper;
	private final EntityResolver idResolver;
	public final IdentityTypesRegistry identityTypesRegistry;
	private final LocalCredentialsRegistry authnRegistry;
	private final InvitationDB invitationDB;

	@Autowired
	public BaseRequestPreprocessor(CredentialRepository credentialRepository, AttributeTypeDAO dbAttributes,
			GroupDAO dbGroups, AttributesHelper attributesHelper, AttributeTypeHelper attributeTypesHelper,
			EntityResolver idResolver, IdentityTypesRegistry identityTypesRegistry,
			LocalCredentialsRegistry authnRegistry, InvitationDB invitationDB)
	{
		this.credentialRepository = credentialRepository;
		this.dbAttributes = dbAttributes;
		this.dbGroups = dbGroups;
		this.attributesHelper = attributesHelper;
		this.attributeTypesHelper = attributeTypesHelper;
		this.idResolver = idResolver;
		this.identityTypesRegistry = identityTypesRegistry;
		this.authnRegistry = authnRegistry;
		this.invitationDB = invitationDB;
	}

	public void validateSubmittedRequest(BaseForm form, BaseRegistrationInput request, 
			boolean doCredentialCheckAndUpdate) throws IllegalFormContentsException
	{
		validateSubmittedRequest(form, request, doCredentialCheckAndUpdate, false);
	}
	
	public void validateSubmittedRequest(BaseForm form, BaseRegistrationInput request, 
			boolean doCredentialCheckAndUpdate, boolean skipCredentialsValidation) throws IllegalFormContentsException
	{
		log.debug("Validating registration request:\n{}", request.toString());
		validateRequestAgreements(form, request);
		validateRequestedAttributes(form, request);
		if (!skipCredentialsValidation)
			validateRequestCredentials(form, request, doCredentialCheckAndUpdate);
		validateRequestedIdentities(form, request);
		validateRequestedGroups(form, request);
		
		if (!form.isCollectComments() && request.getComments() != null)
			throw new IllegalFormContentsException("This registration "
					+ "form doesn't allow for passing comments.");
	}

	private void validateRequestedGroups(BaseForm form, BaseRegistrationInput request) 
			throws IllegalFormContentsException
	{
		if (form.getGroupParams() == null)
			return;
		if (request.getGroupSelections().size() != form.getGroupParams().size())
			throw new IllegalFormContentsException(
					"Wrong amount of group selections, should be: "
							+ form.getGroupParams().size());
		for (int i = 0; i < form.getGroupParams().size(); i++)
		{
			GroupRegistrationParam groupRegistrationParam = form.getGroupParams().get(i);
			GroupSelection groupSelection = request.getGroupSelections().get(i);
			if (groupSelection == null)
				continue;
			validateRequestedGroup(groupRegistrationParam, groupSelection);
		}
	}
	
	private void validateRequestedGroup(GroupRegistrationParam groupRegistrationParam,
			GroupSelection groupSelection) throws IllegalFormContentsException
	{
		if (!groupRegistrationParam.isMultiSelect() && !groupSelection.getSelectedGroups().isEmpty())
		{
			assertGroupsParentChildRelation(groupSelection.getSelectedGroups());
		}
			
		for (String group: groupSelection.getSelectedGroups())
			if (!GroupPatternMatcher.matches(group, groupRegistrationParam.getGroupPath()))
				throw new IllegalFormContentsException(
						"Requested group " + group + " is not matching allowed groups spec " 
								+ groupRegistrationParam.getGroupPath());
	}
	
	
	private void assertGroupsParentChildRelation(List<String> groups) throws IllegalFormContentsException
	{
		List<String> sortedGroups = groups.stream().sorted(
				(g1, g2) -> new Group(g2).getPath().length - new Group(g1).getPath().length)
				.collect(Collectors.toList());

		Iterator<String> it = sortedGroups.iterator();
		Group oldestChild = new Group(it.next());
		while (it.hasNext())
		{
			if (!oldestChild.isChild(new Group(it.next())))
			{
				throw new IllegalFormContentsException(
						"Incorrect selected groups, all selected group should have parent -> child relation");
			}
		}	
	}

	private void validateRequestAgreements(BaseForm form, BaseRegistrationInput request)
			throws IllegalFormContentsException
	{
		if (form.getAgreements() == null)
			return;
		if (form.getAgreements().size() != request.getAgreements().size())
			throw new IllegalFormContentsException("Number of agreements in the"
					+ " request does not match the form agreements.");
		for (int i = 0; i < form.getAgreements().size(); i++)
		{
			if (form.getAgreements().get(i).isManatory()
					&& !request.getAgreements().get(i).isSelected())
				throw new IllegalFormContentsException("Mandatory agreement is not accepted.",
						i, Category.AGREEMENT);
		}
	}
	
	public void validateFinalAttributes(Collection<Attribute> attributes) 
			throws EngineException
	{
		Map<String, AttributeType> atMap = dbAttributes.getAllAsMap();
		for (Attribute attr: attributes)
		{
			AttributeType at = atMap.get(attr.getName());
			if (at == null)
				throw new WrongArgumentException("Attribute of the form "
						+ attr.getName() + " does not exist anymore");
			try
			{
				attributesHelper.validate(attr, at);
			} catch (IllegalAttributeValueException|IllegalAttributeTypeException e)
			{
				throw new IllegalAttributeValueException("Invalid value for the '"  
						+ attr.getName() + "' attribute", e);
			}
		}
	}

	public void validateFinalIdentities(Collection<IdentityParam> identities) 
			throws EngineException
	{
		boolean identitiesFound = false;
		for (IdentityParam idParam: identities)
		{
			if (idParam.getTypeId() == null || idParam.getValue() == null)
				throw new WrongArgumentException("Identity " + idParam + " contains null values");
			identityTypesRegistry.getByName(idParam.getTypeId()).validate(idParam.getValue());
			identitiesFound = true;
			assertIdentityIsNotPresentOnConfirm(idParam);
		}
		if (!identitiesFound)
			throw new WrongArgumentException("At least one identity must be defined in the "
					+ "registration request.");
	}

	public void validateFinalGroups(Collection<GroupParam> groups) 
			throws EngineException
	{
		Map<String, Group> allAsMap = dbGroups.getAllAsMap();
		for (GroupParam group: groups)
		{
			if (group == null)
				throw new WrongArgumentException("Final group memberships contain null values");
			if (!allAsMap.containsKey(group.getGroup()))
				throw new WrongArgumentException("Group to add a user to " + group + " does not exist");
		}
	}
	
	public void validateFinalCredentials(List<CredentialParamValue> credentials) 
			throws EngineException
	{
		for (CredentialParamValue credentialParam: credentials)
			credentialRepository.get(credentialParam.getCredentialId());
	}
	
	private void validateRequestedAttributes(BaseForm form, BaseRegistrationInput request) 
			throws IllegalFormContentsException
	{
		validateParamsBase(form.getAttributeParams(), request.getAttributes(), 
				"attributes", Category.ATTRIBUTE);
		for (int i = 0; i < request.getAttributes().size(); i++)
		{
			Attribute attr = request.getAttributes().get(i);
			if (attr == null)
				continue;
			AttributeRegistrationParam regParam = form.getAttributeParams().get(i);
			if (!regParam.getAttributeType().equals(attr.getName()))
				throw new IllegalFormContentsException("Attribute " + attr.getName()
						+ " in group " + attr.getGroupPath()
						+ " is not allowed for this form",
						i, Category.ATTRIBUTE);
			if (!regParam.isUsingDynamicGroup() && !regParam.getGroup().equals(attr.getGroupPath()))
				throw new IllegalFormContentsException("Attribute " + attr.getName()
						+ " in group " + attr.getGroupPath()
						+ " is not allowed for this form",
						i, Category.ATTRIBUTE);
			forceConfirmationStateOfAttribute(regParam, i, attr);
		}
	}
	
	private void forceConfirmationStateOfAttribute(AttributeRegistrationParam regParam, int i, Attribute attr)
	{
		AttributeValueSyntax<?> syntax = attributeTypesHelper
				.getUnconfiguredSyntaxForAttributeName(attr.getName());
		if (syntax.isUserVerifiable())
		{
			@SuppressWarnings("unchecked")
			AttributeValueSyntax<? extends VerifiableElement> vsyntax = 
				(AttributeValueSyntax<? extends VerifiableElement>) syntax;

			if (regParam.getConfirmationMode() == ConfirmationMode.ON_ACCEPT 
					||regParam.getConfirmationMode() == ConfirmationMode.ON_SUBMIT)
				return;
			
			if (regParam.getConfirmationMode() == ConfirmationMode.CONFIRMED)
				AttributesHelper.setConfirmed(attr, vsyntax);

			if (regParam.getConfirmationMode() == ConfirmationMode.DONT_CONFIRM)
				AttributesHelper.setUnconfirmed(attr, vsyntax);
		}
	}

	private void validateRequestedIdentities(BaseForm form, BaseRegistrationInput request) 
			throws IllegalFormContentsException
	{
		List<IdentityParam> requestedIds = request.getIdentities();
		validateParamsBase(form.getIdentityParams(), requestedIds, "identities", Category.IDENTITY);
		for (int i=0; i<requestedIds.size(); i++)
		{
			IdentityParam idParam = requestedIds.get(i);
			if (idParam == null)
				continue;
			if (idParam.getTypeId() == null || idParam.getValue() == null)
				throw new IllegalFormContentsException("Identity nr " + i + " contains null values",
						i, Category.IDENTITY);
			IdentityRegistrationParam formParam = form.getIdentityParams().get(i);
			if (!formParam.getIdentityType().equals(idParam.getTypeId()))
				throw new IllegalFormContentsException("Identity nr " + i + " must be of " 
						+ form.getIdentityParams().get(i).getIdentityType() + 
						" type, but is " + idParam, i, Category.IDENTITY);
			forceConfirmationStateOfIdentity(formParam, i, idParam);
			
			if (form.isCheckIdentityOnSubmit())
				assertIdentityIsNotPresentOnSubmit(idParam, i);
		}
	}
	
	private boolean isIdentityPresent(IdentityParam idParam)
	{
		try
		{
			idResolver.getFullIdentity(idParam);
			return true;
		} catch (UnknownIdentityException e)
		{
			return false;
		}
	}

	public void assertIdentityIsNotPresentOnConfirm(IdentityParam idParam) throws IdentityExistsException
	{
		if (isIdentityPresent(idParam))
			throw new IdentityExistsException("The user with the given identity is already present.");
	}

	
	public void assertIdentityIsNotPresentOnSubmit(IdentityParam idParam, int position) throws IllegalFormContentsException
	{
		if (isIdentityPresent(idParam))
			throw new IllegalFormContentsException.OccupiedIdentityUsedInRequest(idParam, position);
	}
	
	private void forceConfirmationStateOfIdentity(IdentityRegistrationParam formParam, int i, 
			IdentityParam idParam)
	{
		IdentityTypeDefinition idTypeDef = identityTypesRegistry.getByName(formParam.getIdentityType());
		if (idTypeDef.isEmailVerifiable())
		{
			if (formParam.getConfirmationMode() == ConfirmationMode.ON_ACCEPT 
					||formParam.getConfirmationMode() == ConfirmationMode.ON_SUBMIT)
				return;

			boolean initiallyConfirmed = 
					formParam.getConfirmationMode() == ConfirmationMode.CONFIRMED;
			idParam.setConfirmationInfo(new ConfirmationInfo(initiallyConfirmed));
		}
	}
	
	private void validateRequestCredentials(BaseForm form, BaseRegistrationInput request,
			boolean doCredentialCheckAndUpdate) throws IllegalFormContentsException
	{
		List<CredentialParamValue> requestedCreds = request.getCredentials();
		List<CredentialRegistrationParam> formCreds = form.getCredentialParams();
		if (formCreds == null)
			return;
		if (formCreds.size() != requestedCreds.size())
			throw new IllegalFormContentsException("There should be " + formCreds.size()
					+ " credential parameters");
		for (int i = 0; i < formCreds.size(); i++)
		{
			String credential = formCreds.get(i).getCredentialName();
			try
			{
				CredentialDefinition credDef = credentialRepository.get(credential);
				if (doCredentialCheckAndUpdate)
				{
					LocalCredentialVerificator credVerificator = authnRegistry
							.createLocalCredentialVerificator(credDef);
					String updatedSecrets = credVerificator.prepareCredential(
							requestedCreds.get(i).getSecrets(), "", true);
					requestedCreds.get(i).setSecrets(updatedSecrets);
				}
			} catch (Exception e)
			{
				throw new IllegalFormContentsException("Credential is invalid", 
						i, 
						Category.CREDENTIAL, e);
			}
		}
	}

	private void validateParamsCount(List<? extends RegistrationParam> paramDefinitions,
			List<?> params, String info) throws IllegalFormContentsException
	{
		if (paramDefinitions.size() != params.size())
			throw new IllegalFormContentsException("There should be "
					+ paramDefinitions.size() + " " + info + " parameters");
	}	
	
	private void validateParamsBase(List<? extends OptionalRegistrationParam> paramDefinitions,
			List<?> params, String info, Category category) throws IllegalFormContentsException
	{
		validateParamsCount(paramDefinitions, params, info);
		for (int i = 0; i < paramDefinitions.size(); i++)
			if (!paramDefinitions.get(i).isOptional() && params.get(i) == null)
				throw new IllegalFormContentsException("The parameter nr " + (i + 1)
						+ " of " + info + " is required", i, category);
	}
	
	public InvitationWithCode getInvitation(String codeFromRequest) throws IllegalFormContentsException
	{
		try
		{
			return invitationDB.get(codeFromRequest);
		} catch (Exception e)
		{
			throw new IllegalFormContentsException("The provided registration code is invalid", e);
		}
	}
	
	public void removeInvitation(String codeFromRequest)
	{	
		 invitationDB.delete(codeFromRequest);	
	}
	
	public <T> void processInvitationElements(List<? extends RegistrationParam> paramDef,
			List<T> requested, Map<Integer, PrefilledEntry<T>> fromInvitation, String elementName) 
					throws IllegalFormContentsException
	{
		validateParamsCount(paramDef, requested, elementName);
		for (Map.Entry<Integer, PrefilledEntry<T>> invitationPrefilledEntry : fromInvitation.entrySet())
		{
			if (invitationPrefilledEntry.getKey() >= requested.size())
			{
				log.warn("Invitation has " + elementName + 
						" parameter beyond form limit, skipping it: " + invitationPrefilledEntry.getKey());
				continue;
			}
			
			T invitationEntity = invitationPrefilledEntry.getValue().getEntry();
			if (invitationPrefilledEntry.getValue().getMode() == PrefilledEntryMode.DEFAULT)
			{
				if (requested.get(invitationPrefilledEntry.getKey()) == null)
					requested.set(invitationPrefilledEntry.getKey(), invitationEntity);
			} else
			{
				requested.set(invitationPrefilledEntry.getKey(), invitationEntity);
			}
		}
	}
	
	public Map<Integer, PrefilledEntry<GroupSelection>> filterValueReadOnlyAndHiddenGroupFromInvitation(
			Map<Integer, PrefilledEntry<GroupSelection>> org, List<GroupRegistrationParam> formGroupParams)
	{
		List<Group> all = dbGroups.getAll();

		Map<Integer, PrefilledEntry<GroupSelection>> ret = new HashMap<>();
		for (Map.Entry<Integer, PrefilledEntry<GroupSelection>> fromInvintation : org.entrySet())
		{
			GroupRegistrationParam formGroupParam = formGroupParams.get(fromInvintation.getKey());
			if (fromInvintation.getValue().getMode().equals(PrefilledEntryMode.DEFAULT)
					|| formGroupParam == null)
			{
				ret.put(fromInvintation.getKey(), fromInvintation.getValue());
			}

			List<String> allowedFilteredByMode = GroupPatternMatcher
					.filterByIncludeGroupsMode(GroupPatternMatcher.filterMatching(GroupPatternMatcher.filterMatching(all,
							formGroupParam.getGroupPath()),
							fromInvintation.getValue().getEntry().getSelectedGroups()),
							formGroupParam.getIncludeGroupsMode())
					.stream().map(g -> g.toString()).collect(Collectors.toList());
			log.debug("Filter hidden/readOnly group values from invitation:"
					+ fromInvintation.getValue().getEntry().getSelectedGroups() + " -> "
					+ allowedFilteredByMode);
			ret.put(fromInvintation.getKey(),
					new PrefilledEntry<GroupSelection>(new GroupSelection(allowedFilteredByMode),
							fromInvintation.getValue().getMode()));
		}

		return ret;
	}
}

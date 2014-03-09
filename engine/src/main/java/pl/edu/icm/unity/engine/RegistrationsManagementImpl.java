/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.ac.AttributeClassDB;
import pl.edu.icm.unity.db.generic.cred.CredentialDB;
import pl.edu.icm.unity.db.generic.credreq.CredentialRequirementDB;
import pl.edu.icm.unity.db.generic.reg.RegistrationFormDB;
import pl.edu.icm.unity.db.generic.reg.RegistrationRequestDB;
import pl.edu.icm.unity.db.mapper.GroupsMapper;
import pl.edu.icm.unity.db.resolvers.GroupResolver;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.internal.AttributesHelper;
import pl.edu.icm.unity.engine.internal.EngineHelper;
import pl.edu.icm.unity.engine.notifications.NotificationProducerImpl;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.attributes.AttributeValueChecker;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificator;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.registries.LocalCredentialsRegistry;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeClassAssignment;
import pl.edu.icm.unity.types.registration.AttributeParamValue;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialParamValue;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityParamValue;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.OptionalRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;

/**
 * Implementation of registrations subsystem.
 * 
 * @author K. Benedyczak
 */
@Component
public class RegistrationsManagementImpl implements RegistrationsManagement
{
	public static final String VAR_FORM = "formName";
	public static final String VAR_REQUEST = "requestId";
	public static final String VAR_PUB_COMMENT = "publicComment";
	public static final String VAR_INTERNAL_COMMENT = "internalComment";
	
	private DBSessionManager db;
	private RegistrationFormDB formsDB;
	private RegistrationRequestDB requestDB;
	private CredentialDB credentialDB;
	private CredentialRequirementDB credentialReqDB;
	private AttributeClassDB acDB;
	private DBAttributes dbAttributes;
	private DBIdentities dbIdentities;
	private DBGroups dbGroups;
	
	private GroupResolver groupsResolver;
	private IdentityTypesRegistry identityTypesRegistry;
	private LocalCredentialsRegistry authnRegistry;
	private AuthorizationManager authz;
	private EngineHelper engineHelper;
	private AttributesHelper attributesHelper;
	private NotificationProducerImpl notificationProducer;

	@Autowired
	public RegistrationsManagementImpl(DBSessionManager db, RegistrationFormDB formsDB,
			RegistrationRequestDB requestDB, CredentialDB credentialDB,
			CredentialRequirementDB credentialReqDB, AttributeClassDB acDB,
			DBAttributes dbAttributes, DBIdentities dbIdentities, DBGroups dbGroups,
			GroupResolver groupsResolver, IdentityTypesRegistry identityTypesRegistry,
			LocalCredentialsRegistry authnRegistry, AuthorizationManager authz,
			EngineHelper engineHelper, AttributesHelper attributesHelper,
			NotificationProducerImpl notificationProducer)
	{
		this.db = db;
		this.formsDB = formsDB;
		this.requestDB = requestDB;
		this.credentialDB = credentialDB;
		this.credentialReqDB = credentialReqDB;
		this.acDB = acDB;
		this.dbAttributes = dbAttributes;
		this.dbIdentities = dbIdentities;
		this.dbGroups = dbGroups;
		this.groupsResolver = groupsResolver;
		this.identityTypesRegistry = identityTypesRegistry;
		this.authnRegistry = authnRegistry;
		this.authz = authz;
		this.engineHelper = engineHelper;
		this.attributesHelper = attributesHelper;
		this.notificationProducer = notificationProducer;
	}

	@Override
	public void addForm(RegistrationForm form) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			validateFormContents(form, sql);
			formsDB.insert(form.getName(), form, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void removeForm(String formId, boolean dropRequests) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			List<RegistrationRequestState> requests = requestDB.getAll(sql);
			if (dropRequests)
			{
				for (RegistrationRequestState req: requests)
					if (formId.equals(req.getRequest().getFormId()))
						requestDB.remove(req.getRequestId(), sql);
			} else
			{
				for (RegistrationRequestState req: requests)
					if (formId.equals(req.getRequest().getFormId()))
						throw new SchemaConsistencyException("There are requests bound " +
								"to this form, and it was not chosen to drop them.");
			}
			formsDB.remove(formId, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void updateForm(RegistrationForm updatedForm, boolean ignoreRequests)
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			validateFormContents(updatedForm, sql);
			String formId = updatedForm.getName();
			if (!ignoreRequests)
			{
				List<RegistrationRequestState> requests = requestDB.getAll(sql);
				for (RegistrationRequestState req: requests)
					if (formId.equals(req.getRequest().getFormId()) && 
							req.getStatus() == RegistrationRequestStatus.pending)
						throw new SchemaConsistencyException("There are requests bound to " +
								"this form, and it was not chosen to ignore them.");
			}
			formsDB.update(formId, updatedForm, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public List<RegistrationForm> getForms() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			List<RegistrationForm> ret = formsDB.getAll(sql);
			sql.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public String submitRegistrationRequest(RegistrationRequest request) throws EngineException
	{
		SqlSession sql = db.getSqlSession(true);
		try
		{
			RegistrationForm form = formsDB.get(request.getFormId(), sql);
			validateRequestContents(form, request, true, sql);
			RegistrationRequestState requestFull = new RegistrationRequestState();
			requestFull.setStatus(RegistrationRequestStatus.pending);
			requestFull.setRequest(request);
			requestFull.setRequestId(UUID.randomUUID().toString());
			requestFull.setTimestamp(new Date());
			requestDB.insert(requestFull.getRequestId(), requestFull, sql);
			sql.commit();
			
			RegistrationFormNotifications notificationsCfg = form.getNotificationsConfiguration();
			if (notificationsCfg.getChannel() != null && notificationsCfg.getSubmittedTemplate() != null
					&& notificationsCfg.getAdminsNotificationGroup() != null)
			{
				notificationProducer.sendNotificationToGroup(notificationsCfg.getAdminsNotificationGroup(), 
					notificationsCfg.getChannel(), 
					notificationsCfg.getSubmittedTemplate(),
					getBaseNotificationParams(form.getName(), requestFull.getRequestId()));
			}
			return requestFull.getRequestId();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public List<RegistrationRequestState> getRegistrationRequests() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.read);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			List<RegistrationRequestState> ret = requestDB.getAll(sql);
			sql.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void processRegistrationRequest(String id, RegistrationRequest finalRequest,
			RegistrationRequestAction action, String publicCommentStr,
			String internalCommentStr) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.credentialModify, AuthzCapability.attributeModify,
				AuthzCapability.identityModify, AuthzCapability.groupModify);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			RegistrationRequestState currentRequest = requestDB.get(id, sql);
			if (finalRequest != null)
			{
				finalRequest.setCredentials(currentRequest.getRequest().getCredentials());
				currentRequest.setRequest(finalRequest);
			}
			InvocationContext authnCtx = InvocationContext.getCurrent();
			AuthenticatedEntity client = authnCtx.getAuthenticatedEntity();
			AdminComment publicComment = null;
			AdminComment internalComment = null;
			if (publicCommentStr != null)
			{
				publicComment = new AdminComment(publicCommentStr, client.getEntityId(), true);
				currentRequest.getAdminComments().add(publicComment);
			}
			if (internalCommentStr != null)
			{
				internalComment = new AdminComment(internalCommentStr, client.getEntityId(), false);
				currentRequest.getAdminComments().add(internalComment);
			}
			
			if (currentRequest.getStatus() != RegistrationRequestStatus.pending && 
					(action == RegistrationRequestAction.accept || 
					action == RegistrationRequestAction.reject))
				throw new WrongArgumentException("The request was already processed. " +
						"It is only possible to drop it or to modify its comments.");
			if (currentRequest.getStatus() != RegistrationRequestStatus.pending && 
					action == RegistrationRequestAction.update && finalRequest != null)
				throw new WrongArgumentException("The request was already processed. " +
							"It is only possible to drop it or to modify its comments.");
			RegistrationForm form = formsDB.get(currentRequest.getRequest().getFormId(), sql);
			
			switch (action)
			{
			case drop:
				dropRequest(id, sql);
				break;
			case reject:
				rejectRequest(form, currentRequest, publicComment, internalComment, sql);
				break;
			case update:
				updateRequest(form, currentRequest, publicComment, internalComment, sql);
				break;
			case accept:
				acceptRequest(form, currentRequest, publicComment, internalComment, sql);
				break;
			}
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	private void dropRequest(String id, SqlSession sql) throws EngineException
	{
		requestDB.remove(id, sql);
	}
	
	private void rejectRequest(RegistrationForm form, RegistrationRequestState currentRequest, 
			AdminComment publicComment, AdminComment internalComment, SqlSession sql) throws EngineException
	{
		currentRequest.setStatus(RegistrationRequestStatus.rejected);
		requestDB.update(currentRequest.getRequestId(), currentRequest, sql);
		RegistrationFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		sendProcessingNotification(notificationsCfg.getRejectedTemplate(), 
				currentRequest, currentRequest.getRequestId(), form.getName(),
				true, publicComment, 
				internalComment, notificationsCfg, sql);
	}
	
	private void updateRequest(RegistrationForm form, RegistrationRequestState currentRequest,
			AdminComment publicComment, AdminComment internalComment, SqlSession sql) 
			throws EngineException
	{
		validateRequestContents(form, currentRequest.getRequest(), false, sql);
		requestDB.update(currentRequest.getRequestId(), currentRequest, sql);
		RegistrationFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		sendProcessingNotification(notificationsCfg.getUpdatedTemplate(),
				currentRequest, currentRequest.getRequestId(), form.getName(), false, 
				publicComment, internalComment,	notificationsCfg, sql);
	}
	
	private void acceptRequest(RegistrationForm form, RegistrationRequestState currentRequest, 
			AdminComment publicComment, AdminComment internalComment, SqlSession sql) 
			throws EngineException
	{
		currentRequest.setStatus(RegistrationRequestStatus.accepted);

		validateRequestContents(form, currentRequest.getRequest(), false, sql);
		requestDB.update(currentRequest.getRequestId(), currentRequest, sql);
		
		RegistrationRequest req = currentRequest.getRequest();

		List<Attribute<?>> rootAttributes = new ArrayList<>(req.getAttributes().size() + 
				form.getAttributeAssignments().size());
		Map<String, List<Attribute<?>>> remainingAttributesByGroup = new HashMap<String, List<Attribute<?>>>();
		for (Attribute<?> a: form.getAttributeAssignments())
			addAttr(a, rootAttributes, remainingAttributesByGroup);
		for (AttributeParamValue ap: req.getAttributes())
		{
			if (ap == null)
				continue;
			Attribute<?> a = ap.getAttribute();
			addAttr(a, rootAttributes, remainingAttributesByGroup);
		}

		List<IdentityParamValue> identities = req.getIdentities();
		
		Identity initial = engineHelper.addEntity(identities.get(0), form.getCredentialRequirementAssignment(), 
				form.getInitialEntityState(), false, rootAttributes, sql);

		for (int i=1; i<identities.size(); i++)
		{
			IdentityParam idParam = identities.get(i);
			if (idParam == null)
				continue;
			dbIdentities.insertIdentity(idParam, initial.getEntityId(), sql);
		}

		Set<String> sortedGroups = new TreeSet<>();
		for (String group: form.getGroupAssignments())
			sortedGroups.add(group);
		for (int i=0; i<form.getGroupParams().size(); i++)
		{
			if (req.getGroupSelections().get(i).isSelected())
				sortedGroups.add(form.getGroupParams().get(i).getGroupPath());
		}
			
		EntityParam entity = new EntityParam(initial.getEntityId());
		for (String group: sortedGroups)
		{
			List<Attribute<?>> attributes = remainingAttributesByGroup.get(group);
			if (attributes == null)
				attributes = Collections.emptyList();
			engineHelper.checkGroupAttributeClassesConsistency(attributes, group, sql);
			dbGroups.addMemberFromParent(group, entity, sql);
			engineHelper.addAttributesList(attributes, initial.getEntityId(), sql);
		}
		
		for (AttributeClassAssignment aca: form.getAttributeClassAssignments())
		{
			attributesHelper.setAttributeClasses(initial.getEntityId(), aca.getGroup(), 
					Collections.singleton(aca.getAcName()), sql);
		}
		
		for (CredentialParamValue c: req.getCredentials())
		{
			engineHelper.setPreviouslyPreparedEntityCredentialInternal(
					initial.getEntityId(), c.getSecrets(), c.getCredentialId(), sql);
		}
		
		RegistrationFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		sendProcessingNotification(notificationsCfg.getAcceptedTemplate(),
				currentRequest, currentRequest.getRequestId(), form.getName(), true,
				publicComment, internalComment,	notificationsCfg, sql);
	}
	
	private void addAttr(Attribute<?> a, List<Attribute<?>> rootAttributes, 
			Map<String, List<Attribute<?>>> remainingAttributesByGroup)
	{
		String path = a.getGroupPath();
		if (path.equals("/"))
			rootAttributes.add(a);
		else
		{
			List<Attribute<?>> attrs = remainingAttributesByGroup.get(path);
			if (attrs == null)
			{
				attrs = new ArrayList<>();
				remainingAttributesByGroup.put(path, attrs);
			}
			attrs.add(a);
		}
	}
	
	private void validateRequestContents(RegistrationForm form, RegistrationRequest request, boolean doCredentialCheckAndUpdate,
			SqlSession sql) throws EngineException
	{
		validateRequestAgreements(form, request);
		validateRequestAttributes(form, request, sql);
		validateRequestCode(form, request);
		validateRequestCredentials(form, request, doCredentialCheckAndUpdate, sql);
		validateRequestIdentities(form, request);

		if (!form.isCollectComments() && request.getComments() != null)
			throw new WrongArgumentException("This registration " +
					"form doesn't allow for passing comments.");

		if (request.getGroupSelections().size() != form.getGroupParams().size())
			throw new WrongArgumentException("Wrong amount of group selections, should be: " + 
					form.getGroupParams().size());
	}

	private void validateRequestAgreements(RegistrationForm form, RegistrationRequest request) 
			throws WrongArgumentException
	{
		if (form.getAgreements().size() != request.getAgreements().size())
			throw new WrongArgumentException("Number of agreements in the" +
					" request does not match the form agreements.");
		for (int i=0; i<form.getAgreements().size(); i++)
		{
			if (form.getAgreements().get(i).isManatory() && 
					!request.getAgreements().get(i).isSelected())
				throw new WrongArgumentException("Mandatory agreement is not accepted.");
		}
	}

	private void validateRequestAttributes(RegistrationForm form, RegistrationRequest request, SqlSession sql) 
			throws WrongArgumentException, IllegalAttributeValueException, IllegalAttributeTypeException
	{
		validateParamsBase(form.getAttributeParams(), request.getAttributes(), "attributes");
		Map<String, AttributeType> atMap = dbAttributes.getAttributeTypes(sql);
		for (int i=0; i<request.getAttributes().size(); i++)
		{
			AttributeParamValue attrP = request.getAttributes().get(i);
			if (attrP == null)
				continue;
			Attribute<?> attr = attrP.getAttribute();
			if (attr == null)
				throw new WrongArgumentException("Attribute no " + i + " is null.");
			AttributeRegistrationParam regParam = form.getAttributeParams().get(i);
			if (!regParam.getAttributeType().equals(attr.getName()))
				throw new WrongArgumentException("Attribute " + 
						attr.getName() + " in group " + attr.getGroupPath() + 
						" is not allowed for this form");
			if (!regParam.getGroup().equals(attr.getGroupPath()))
				throw new WrongArgumentException("Attribute " + 
						attr.getName() + " in group " + attr.getGroupPath() + 
						" is not allowed for this form");
			AttributeType at = atMap.get(attr.getName());
			if (at == null)
				throw new WrongArgumentException("Attribute of the form " + attr.getName() + 
						" does not exist anymore");
			AttributeValueChecker.validate(attr, at);
		}
	}

	private void validateRequestIdentities(RegistrationForm form, RegistrationRequest request) 
			throws WrongArgumentException, IllegalIdentityValueException, IllegalTypeException
	{
		List<IdentityParamValue> requestedIds = request.getIdentities();
		validateParamsBase(form.getIdentityParams(), requestedIds, "identities");
		for (int i=0; i<requestedIds.size(); i++)
		{
			IdentityParam idParam = requestedIds.get(i);
			if (idParam == null)
				continue;
			if (idParam.getTypeId() == null || idParam.getValue() == null)
				throw new WrongArgumentException("Identity nr " + i + " contains null values");
			if (!form.getIdentityParams().get(i).getIdentityType().equals(idParam.getTypeId()))
				throw new WrongArgumentException("Identity nr " + i + " must be of " 
						+ idParam.getTypeId() + " type");
			identityTypesRegistry.getByName(idParam.getTypeId()).validate(idParam.getValue());
		}
	}

	private void validateRequestCredentials(RegistrationForm form, RegistrationRequest request, 
			boolean doCredentialCheckAndUpdate, SqlSession sql) 
			throws EngineException
	{
		List<CredentialParamValue> requestedCreds = request.getCredentials();
		List<CredentialRegistrationParam> formCreds = form.getCredentialParams();
		if (formCreds.size() != requestedCreds.size())
			throw new WrongArgumentException("There should be " + formCreds.size() + 
					" credential parameters");
		for (int i=0; i<formCreds.size(); i++)
		{
			String credential = formCreds.get(i).getCredentialName();
			CredentialDefinition credDef = credentialDB.get(credential, sql);
			if (doCredentialCheckAndUpdate)
			{
				LocalCredentialVerificator credVerificator = 
					authnRegistry.createLocalCredentialVerificator(credDef);
				String updatedSecrets = credVerificator.prepareCredential(
						requestedCreds.get(i).getSecrets(), "");
				requestedCreds.get(i).setSecrets(updatedSecrets);
			}
		}
	}

	private void validateRequestCode(RegistrationForm form, RegistrationRequest request) throws WrongArgumentException
	{
		String formCode = form.getRegistrationCode();
		String code = request.getRegistrationCode();
		if (formCode == null && code != null)
			throw new WrongArgumentException("This registration " +
					"form doesn't allow for passing registration code.");
		if (formCode != null && code == null)
			throw new WrongArgumentException("This registration " +
					"form require a registration code.");
		if (formCode != null && code != null && !formCode.equals(code))
			throw new WrongArgumentException("The registration code is invalid.");
	}

	private void validateParamsBase(List<? extends OptionalRegistrationParam> paramDefinitions, List<?> params, 
			String info) throws WrongArgumentException
	{
		if (paramDefinitions.size() != params.size())
			throw new WrongArgumentException("There should be " + paramDefinitions.size() + " " + 
					info + " parameters");
		for (int i=0; i<paramDefinitions.size(); i++)
			if (!paramDefinitions.get(i).isOptional() && params.get(i) == null)
				throw new WrongArgumentException("The parameter nr " + (i+1) + " of " + 
						info + " is required");
	}


	private void validateFormContents(RegistrationForm form, SqlSession sql) throws EngineException
	{
		GroupsMapper gm = sql.getMapper(GroupsMapper.class);

		Map<String, AttributeType> atMap = dbAttributes.getAttributeTypes(sql);
		if (form.getAttributeAssignments() != null)
		{
			Set<String> used = new HashSet<>();
			for (Attribute<?> attr: form.getAttributeAssignments())
			{
				AttributeType at = atMap.get(attr.getName());
				if (at == null)
					throw new WrongArgumentException("Attribute type " + attr.getName() + 
							" does not exist");
				String key = at.getName() + " @ " + attr.getGroupPath();
				if (used.contains(key))
					throw new WrongArgumentException("Assigned attribute " + key + 
							" was specified more then once.");
				used.add(key);
				AttributeValueChecker.validate(attr, at);
				groupsResolver.resolveGroup(attr.getGroupPath(), gm);
			}
		}

		if (form.getAttributeParams() != null)
		{
			Set<String> used = new HashSet<>();
			for (AttributeRegistrationParam attr: form.getAttributeParams())
			{
				if (!atMap.containsKey(attr.getAttributeType()))
					throw new WrongArgumentException("Attribute type " + attr.getAttributeType() + 
							" does not exist");
				String key = attr.getAttributeType() + " @ " + attr.getGroup();
				if (used.contains(key))
					throw new WrongArgumentException("Collected attribute " + key + 
							" was specified more then once.");
				used.add(key);
				groupsResolver.resolveGroup(attr.getGroup(), gm);
			}
		}

		if (form.getAttributeClassAssignments() != null)
		{
			Set<String> acs = new HashSet<>();
			for (AttributeClassAssignment ac: form.getAttributeClassAssignments())
			{
				if (acs.contains(ac.getAcName()))
					throw new WrongArgumentException("Assigned attribute class " + 
							ac.getAcName() + " was specified more then once.");
				acs.add(ac.getAcName());
			}
			acDB.assertExist(acs, sql);
		}

		if (form.getCredentialParams() != null)
		{
			Set<String> creds = new HashSet<>();
			for (CredentialRegistrationParam cred: form.getCredentialParams())
			{
				if (creds.contains(cred.getCredentialName()))
					throw new WrongArgumentException("Collected credential " + 
							cred.getCredentialName() + " was specified more then once.");
				creds.add(cred.getCredentialName());
			}
			credentialDB.assertExist(creds, sql);
		}

		if (form.getCredentialRequirementAssignment() == null)
			throw new WrongArgumentException("Credential requirement must be set for the form");
		if (credentialReqDB.get(form.getCredentialRequirementAssignment(), sql) == null)
			throw new WrongArgumentException("Credential requirement " + 
					form.getCredentialRequirementAssignment() + " does not exist");

		if (form.getGroupAssignments() != null)
		{
			Set<String> used = new HashSet<>();
			for (String group: form.getGroupAssignments())
			{
				groupsResolver.resolveGroup(group, gm);
				if (used.contains(group))
					throw new WrongArgumentException("Assigned group " + group + 
							" was specified more then once.");
				used.add(group);
			}
		}

		if (form.getGroupParams() != null)
		{
			Set<String> used = new HashSet<>();
			for (GroupRegistrationParam group: form.getGroupParams())
			{
				groupsResolver.resolveGroup(group.getGroupPath(), gm);
				if (used.contains(group.getGroupPath()))
					throw new WrongArgumentException("Selectable group " + group.getGroupPath() + 
							" was specified more then once.");
				used.add(group.getGroupPath());
			}
		}

		if (form.getIdentityParams() != null)
		{
			Set<String> usedRemote = new HashSet<>();
			for (IdentityRegistrationParam id: form.getIdentityParams())
			{
				identityTypesRegistry.getByName(id.getIdentityType());
				if (id.getRetrievalSettings() == ParameterRetrievalSettings.automatic || 
						id.getRetrievalSettings() == ParameterRetrievalSettings.automaticHidden)
				{
					if (usedRemote.contains(id.getIdentityType()))
						throw new WrongArgumentException("There can be only one identity " +
								"collected automatically of each type. There are more " +
								"then one of type " + id.getIdentityType());
					usedRemote.add(id.getIdentityType());
				}
			}
		}
		
		if (form.getInitialEntityState() == null)
			throw new WrongArgumentException("Initial entity state must be set in the form.");
		
		if (form.getNotificationsConfiguration() == null)
			throw new WrongArgumentException("NotificationsConfiguration must be set in the form.");
		
		if (form.getAgreements() != null)
		{
			for (AgreementRegistrationParam o: form.getAgreements())
			{
				if (o.getText() == null || o.getText().isEmpty())
					throw new WrongArgumentException("Agreement text must not be empty.");
			}
		}
	}
	
	private Map<String, String> getBaseNotificationParams(String formId, String requestId)
	{
		Map<String, String> ret = new HashMap<>();
		ret.put(VAR_FORM, formId);
		ret.put(VAR_REQUEST, requestId);
		return ret;
	}
	
	/**
	 * Creates and sends notifications to the requester and admins in effect of request processing.
	 * @param sendToRequester if true then the notification is sent to requester if only we have its address.
	 * If false, then notification is sent to requester only if we have its address and 
	 * if a public comment was given.
	 * @throws EngineException 
	 */
	private void sendProcessingNotification(String templateId, RegistrationRequestState currentRequest, 
			String requestId, String formId, boolean sendToRequester,
			AdminComment publicComment, AdminComment internalComment,
			RegistrationFormNotifications notificationsCfg, SqlSession sql) throws EngineException
	{
		if (notificationsCfg.getChannel() == null || templateId == null)
			return;
		Map<String, String> notifyParams = getBaseNotificationParams(formId, requestId);
		notifyParams.put(VAR_PUB_COMMENT, publicComment == null ? "" : publicComment.getContents());
		notifyParams.put(VAR_INTERNAL_COMMENT, "");
		String requesterAddress = getRequesterAddress(currentRequest, notificationsCfg, sql);
		if (requesterAddress != null)
		{
			if (sendToRequester || publicComment != null)
				notificationProducer.sendNotification(requesterAddress, 
						notificationsCfg.getChannel(), 
						templateId,
						notifyParams);
		}
		
		if (notificationsCfg.getAdminsNotificationGroup() != null)
		{
			notifyParams.put(VAR_INTERNAL_COMMENT, internalComment == null ? "" : internalComment.getContents());
			notificationProducer.sendNotificationToGroup(notificationsCfg.getAdminsNotificationGroup(), 
				notificationsCfg.getChannel(), 
				templateId,
				notifyParams);
		}
	}
	
	private String getRequesterAddress(RegistrationRequestState currentRequest, 
			RegistrationFormNotifications notificationsCfg, SqlSession sql) throws EngineException
	{
		List<AttributeParamValue> attrs = currentRequest.getRequest().getAttributes();
		AttributeType addrAttribute = notificationProducer.getChannelAddressAttribute(
				notificationsCfg.getChannel(), sql);
		String requesterAddress = null;
		for (AttributeParamValue ap: attrs)
		{
			Attribute<?> tested = ap.getAttribute();
			if (tested.getName().equals(addrAttribute.getName()) &&
					tested.getGroupPath().equals("/"))
			{
				requesterAddress = (String) tested.getValues().get(0);
				break;
			}
		}
		return requesterAddress;
	}
}

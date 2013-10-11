/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
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
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.attributes.AttributeValueChecker;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificator;
import pl.edu.icm.unity.server.registries.AuthenticatorsRegistry;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.AttributeClassAssignment;
import pl.edu.icm.unity.types.registration.AttributeParamValue;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialParamValue;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationParam;
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
	private DBSessionManager db;
	private RegistrationFormDB formsDB;
	private RegistrationRequestDB requestDB;
	private DBAttributes dbAttributes;
	private GroupResolver groupsResolver;
	private IdentityTypesRegistry identityTypesRegistry;
	private AuthenticatorsRegistry authnRegistry;
	private AuthorizationManager authz;
	private CredentialDB credentialDB;
	private CredentialRequirementDB credentialReqDB;
	private AttributeClassDB acDB;

	@Autowired
	public RegistrationsManagementImpl(DBSessionManager db, RegistrationFormDB formsDB,
			RegistrationRequestDB requestDB, DBAttributes dbAttributes,
			GroupResolver groupsResolver, IdentityTypesRegistry identityTypesRegistry,
			AuthorizationManager authz, CredentialDB credentialDB,
			CredentialRequirementDB credentialReqDB, AttributeClassDB acDB)
	{
		this.db = db;
		this.formsDB = formsDB;
		this.requestDB = requestDB;
		this.dbAttributes = dbAttributes;
		this.groupsResolver = groupsResolver;
		this.identityTypesRegistry = identityTypesRegistry;
		this.authz = authz;
		this.credentialDB = credentialDB;
		this.credentialReqDB = credentialReqDB;
		this.acDB = acDB;
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
					if (formId.equals(req.getRequest().getFormId()))
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
			validateRequestContents(form, request, sql);
			RegistrationRequestState requestFull = new RegistrationRequestState();
			requestFull.setStatus(RegistrationRequestStatus.pending);
			requestFull.setRequest(request);
			requestFull.setRequestId(UUID.randomUUID().toString());
			requestDB.insert(requestFull.getRequestId(), requestFull, sql);
			sql.commit();
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
	public void processReqistrationRequest(String id, RegistrationRequest finalRequest,
			RegistrationRequestAction action, AdminComment publicComment,
			AdminComment privateComment) throws EngineException
	{
		throw new RuntimeException("not implemented"); // TODO Auto-generated method stub
	}


	private void validateRequestContents(RegistrationForm form, RegistrationRequest request, 
			SqlSession sql) throws EngineException
	{
		validateRequestAgreements(form, request);
		validateRequestAttributes(form, request, sql);
		validateRequestCode(form, request);
		validateRequestCredentials(form, request, sql);
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
			AttributeRegistrationParam regParam = form.getAttributeParams().get(i);
			if (!regParam.getAttributeType().equals(attr.getName()))
				throw new WrongArgumentException("Attribute " + 
						attr.getName() + " in group " + attr.getGroupPath() + 
						" is not allowed for this form");
			if (!regParam.getGroup().equals(attr.getGroupPath()))
				throw new WrongArgumentException("Attribute " + 
						attr.getName() + " in group " + attr.getGroupPath() + 
						" is not allowed for this form");
			AttributeValueChecker.validate(attr, atMap.get(attr.getName()));
		}
	}

	private void validateRequestIdentities(RegistrationForm form, RegistrationRequest request) 
			throws WrongArgumentException, IllegalIdentityValueException, IllegalTypeException
	{
		List<IdentityParam> requestedIds = request.getIdentities();
		validateParamsBase(form.getIdentityParams(), requestedIds, "identities");
		for (int i=0; i<requestedIds.size(); i++)
		{
			IdentityParam idParam = requestedIds.get(i);
			if (idParam == null)
				continue;
			if (!form.getIdentityParams().get(i).getIdentityType().equals(idParam.getTypeId()))
				throw new WrongArgumentException("Identity nr " + i + " must be of " 
						+ idParam.getTypeId() + " type");
			identityTypesRegistry.getByName(idParam.getTypeId()).validate(idParam.getValue());
		}
	}

	private void validateRequestCredentials(RegistrationForm form, RegistrationRequest request, SqlSession sql) 
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
			LocalCredentialVerificator credVerificator = 
					authnRegistry.createLocalCredentialVerificator(credDef);
			credVerificator.prepareCredential(requestedCreds.get(i).getSecrets(), "");
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

	private void validateParamsBase(List<? extends RegistrationParam> paramDefinitions, List<?> params, 
			String info) throws WrongArgumentException
	{
		if (paramDefinitions.size() != params.size())
			throw new WrongArgumentException("There should be " + paramDefinitions.size() + " " + 
					info + " parameters");
		for (int i=0; i<paramDefinitions.size(); i++)
			if (!paramDefinitions.get(i).isOptional() && params.get(i) == null)
				throw new WrongArgumentException("The parameter nr " + i + " of " + 
						info + " is required");
	}


	private void validateFormContents(RegistrationForm form, SqlSession sql) throws EngineException
	{
		GroupsMapper gm = sql.getMapper(GroupsMapper.class);

		Map<String, AttributeType> atMap = dbAttributes.getAttributeTypes(sql);
		if (form.getAttributeAssignments() != null)
		{
			for (Attribute<?> attr: form.getAttributeAssignments())
			{
				AttributeType at = atMap.get(attr.getName());
				if (at == null)
					throw new WrongArgumentException("Attribute type " + attr.getName() + 
							" does not exist");
				AttributeValueChecker.validate(attr, at);
				groupsResolver.resolveGroup(attr.getGroupPath(), gm);
			}
		}

		if (form.getAttributeParams() != null)
		{
			for (AttributeRegistrationParam attr: form.getAttributeParams())
			{
				if (!atMap.containsKey(attr.getAttributeType()))
					throw new WrongArgumentException("Attribute type " + attr.getAttributeType() + 
							" does not exist");
				groupsResolver.resolveGroup(attr.getGroup(), gm);
			}
		}

		if (form.getAttributeClassAssignments() != null)
		{
			Set<String> acs = new HashSet<>();
			for (AttributeClassAssignment ac: form.getAttributeClassAssignments())
				acs.add(ac.getAcName());
			acDB.assertExist(acs, sql);
		}

		if (form.getCredentialParams() != null)
		{
			Set<String> creds = new HashSet<>();
			for (CredentialRegistrationParam cred: form.getCredentialParams())
				creds.add(cred.getCredentialName());
			credentialDB.assertExist(creds, sql);
		}

		if (form.getCredentialRequirementAssignment() == null)
			throw new WrongArgumentException("Credential requirement must be set for the form");
		if (credentialReqDB.get(form.getCredentialRequirementAssignment(), sql) == null)
			throw new WrongArgumentException("Credential requirement " + 
					form.getCredentialRequirementAssignment() + " does not exist");

		if (form.getGroupAssignments() != null)
		{
			for (String group: form.getGroupAssignments())
			{
				groupsResolver.resolveGroup(group, gm);
			}
		}

		if (form.getGroupParams() != null)
		{
			for (GroupRegistrationParam group: form.getGroupParams())
			{
				groupsResolver.resolveGroup(group.getGroupPath(), gm);
			}
		}

		if (form.getIdentityParams() != null)
		{
			for (IdentityRegistrationParam id: form.getIdentityParams())
			{
				identityTypesRegistry.getByName(id.getIdentityType());
			}
		}
	}
}

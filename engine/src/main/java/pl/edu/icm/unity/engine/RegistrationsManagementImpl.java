/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.HashMap;
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
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.AttributeClassAssignment;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;

/**
 * Implementation of registrations subsystem.
 * TODO - checking of added request consistency
 *  - DB dependencies of request
 *  - request processing
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
		//TODO
	}
	
	@SuppressWarnings("unchecked")
	private void validateFormContents(RegistrationForm form, SqlSession sql) throws EngineException
	{
		GroupsMapper gm = sql.getMapper(GroupsMapper.class);
		
		List<AttributeType> ats = dbAttributes.getAttributeTypes(sql);
		Map<String, AttributeType> atMap = new HashMap<>(ats.size());
		for (AttributeType at: ats)
			atMap.put(at.getName(), at);
		for (Attribute<?> attr: form.getAttributeAssignments())
		{
			AttributeType at = atMap.get(attr.getName());
			if (at == null)
				throw new WrongArgumentException("Attribute type " + attr.getName() + 
						" does not exist");
			@SuppressWarnings("rawtypes")
			AttributeValueSyntax syntax = at.getValueType();
			for (Object o: attr.getValues())
				syntax.validate(o);
			groupsResolver.resolveGroup(attr.getGroupPath(), gm);
		}

		for (AttributeRegistrationParam attr: form.getAttributeParams())
		{
			if (!atMap.containsKey(attr.getAttributeType()))
				throw new WrongArgumentException("Attribute type " + attr.getAttributeType() + 
						" does not exist");
			groupsResolver.resolveGroup(attr.getGroup(), gm);
		}
		
		Set<String> acs = new HashSet<>();
		for (AttributeClassAssignment ac: form.getAttributeClassAssignments())
			acs.add(ac.getAcName());
		acDB.assertExist(acs, sql);
		
		Set<String> creds = new HashSet<>();
		for (CredentialRegistrationParam cred: form.getCredentialParams())
			creds.add(cred.getCredentialName());
		credentialDB.assertExist(creds, sql);
		
		if (credentialReqDB.get(form.getCredentialRequirementAssignment(), sql) == null)
			throw new WrongArgumentException("Credential requirement " + 
					form.getCredentialRequirementAssignment() + " does not exist");
		
		for (String group: form.getGroupAssignments())
		{
			groupsResolver.resolveGroup(group, gm);
		}

		for (GroupRegistrationParam group: form.getGroupParams())
		{
			groupsResolver.resolveGroup(group.getGroupPath(), gm);
		}
		
		for (IdentityRegistrationParam id: form.getIdentityParams())
		{
			identityTypesRegistry.getByName(id.getIdentityType());
		}
	}
}

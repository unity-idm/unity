/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.reg.RegistrationFormDB;
import pl.edu.icm.unity.db.mapper.GroupsMapper;
import pl.edu.icm.unity.db.resolvers.GroupResolver;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;

/**
 * Implementation of registrations subsystem
 * @author K. Benedyczak
 */
@Component
public class RegistrationsManagementImpl implements RegistrationsManagement
{
	private DBSessionManager db;
	private RegistrationFormDB formsDB;
	private GroupResolver groupsResolver;
	private IdentityTypesRegistry identityTypesRegistry;
	private AuthorizationManager authz;
	
	@Autowired
	public RegistrationsManagementImpl(DBSessionManager db)
	{
		this.db = db;
	}

	@Override
	public void addForm(RegistrationForm form) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			//validateFormContents(form);
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
		throw new RuntimeException("not implemented"); // TODO Auto-generated method stub
	}

	@Override
	public void updateForm(RegistrationForm updatedForm, boolean ignoreRequests)
			throws EngineException
	{
		throw new RuntimeException("not implemented"); // TODO Auto-generated method stub
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
	public void submitRegistrationRequest(RegistrationRequest request) throws EngineException
	{
		throw new RuntimeException("not implemented"); // TODO Auto-generated method stub
	}

	@Override
	public List<RegistrationRequestState> getRegistrationRequests() throws EngineException
	{
		throw new RuntimeException("not implemented"); // TODO Auto-generated method stub
	}

	@Override
	public void processReqistrationRequest(String id, RegistrationRequest finalRequest,
			RegistrationRequestAction action, AdminComment publicComment,
			AdminComment privateComment) throws EngineException
	{
		throw new RuntimeException("not implemented"); // TODO Auto-generated method stub
	}
	
	
	private void validateFormContents(RegistrationForm form, SqlSession sql) throws IllegalGroupValueException, 
			IllegalTypeException, IllegalCredentialException
	{
		GroupsMapper gm = sql.getMapper(GroupsMapper.class);
		
		//TODO
		form.getAttributeAssignments();
		
		form.getAttributeClassAssignments();
		
		form.getAttributeParams();
		
		form.getCredentialParams();
		
		form.getCredentialRequirementAssignment();
		
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

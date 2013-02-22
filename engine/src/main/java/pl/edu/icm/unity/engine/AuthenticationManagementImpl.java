/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.List;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.types.LocalAccessClass;
import pl.edu.icm.unity.types.LocalAuthnMethod;
import pl.edu.icm.unity.types.LocalAuthnMethodConfiguration;
import pl.edu.icm.unity.types.LocalAuthnState;
import pl.edu.icm.unity.types.LocalAuthnVerification;

/**
 * Authentication management implementation.
 * @author K. Benedyczak
 */
@Component
public class AuthenticationManagementImpl implements AuthenticationManagement
{
	@Override
	public List<LocalAuthnMethod> getLAMs()
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	@Override
	public void createLocalAuthnVerification(String id, String description,
			LocalAuthnMethodConfiguration config) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	@Override
	public void updateLocalAuthnVerification(String id, String description,
			LocalAuthnMethodConfiguration config) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	@Override
	public List<LocalAuthnVerification> getLocalAuthnVerifications() throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	@Override
	public void removeLocalAuthnVerification(String id) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	@Override
	public void createLAC(String id, String description, String[] lacmIds)
			throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	@Override
	public void updateLAC(String lacId, String description, String[] lacmIds,
			LocalAuthnState newEntitiesAuthnState) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	@Override
	public List<LocalAccessClass> getLACs() throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	@Override
	public void removeLAC(String lacId) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

}

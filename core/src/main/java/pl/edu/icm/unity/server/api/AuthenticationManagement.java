/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.LocalAuthnMethod;
import pl.edu.icm.unity.types.LocalAuthnVerification;
import pl.edu.icm.unity.types.LocalAccessClass;
import pl.edu.icm.unity.types.LocalAuthnMethodConfiguration;
import pl.edu.icm.unity.types.LocalAuthnState;

/**
 * Internal engine API for general local authentication management.
 * 
 * @author K. Benedyczak
 */
public interface AuthenticationManagement
{
	/**
	 * @return list of available {@link LocalAuthnMethod}s at the server.
	 */
	public List<LocalAuthnMethod> getLAMs();
	
	/**
	 * Creates a new {@link LocalAuthnVerification}.
	 * @param id new for the object
	 * @param description its description
	 * @param config implementation dependent configuration object
	 * @throws EngineException
	 */
	public void createLocalAuthnVerification(String id, String description, 
			LocalAuthnMethodConfiguration config) throws EngineException;
	
	/**
	 * Updates an existing {@link LocalAuthnVerification}
	 * @param id
	 * @param description
	 * @param config
	 * @throws EngineException
	 */
	public void updateLocalAuthnVerification(String id, String description, 
			LocalAuthnMethodConfiguration config) throws EngineException;
	
	/**
	 * Lists {@link LocalAuthnVerification}s
	 * @return
	 * @throws EngineException
	 */
	public List<LocalAuthnVerification> getLocalAuthnVerifications() throws EngineException;
	
	/**
	 * Removes a configured LAM. Possible only when it is not used in any LAC.
	 * @param id
	 * @throws EngineException
	 */
	public void removeLocalAuthnVerification(String id) throws EngineException;
	
	
	/**
	 * Creates a LAC.
	 * @throws EngineException
	 */
	public void createLAC(String id, String description, String[] lacmIds) throws EngineException;

	/**
	 * Updates an existing LAC. 
	 * @param lacId id of lac to be modified
	 * @param lac new configuration of the LAC
	 * @param newEntitiesAuthnState state to which entities should be set after the change. If set to correct
	 * then the change will be performed only if all entities using this LAC which are on correct state
	 * can have their secrets updated to use the new LAC transparently (i.e. if there are no entities in correct state) 
	 * of the LAC or if the LAC is compatible with the previous one.
	 * @throws EngineException
	 */
	public void updateLAC(String lacId, String description, String[] lacmIds, 
			LocalAuthnState newEntitiesAuthnState) throws EngineException;
	
	/**
	 * @return list of all defined LACs
	 * @throws EngineException
	 */
	public List<LocalAccessClass> getLACs() throws EngineException;
	
	/**
	 * Removes a given LAC. Possible only if no entity is using it.
	 * @param lacId
	 * @throws EngineException
	 */
	public void removeLAC(String lacId) throws EngineException;

	
	
}


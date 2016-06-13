/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.confirmation.ConfirmationConfiguration;

/**
 * This interface allows clients to manipulate confirmation configuration.
 * 
 * @author P. Piernik
 */
public interface ConfirmationConfigurationManagement
{
	public final String ATTRIBUTE_CONFIG_TYPE = "attribute";
	public final String IDENTITY_CONFIG_TYPE = "identity";
	
	public void addConfiguration(ConfirmationConfiguration toAdd) throws EngineException;
	
	public void removeConfiguration(String typeToConfirm, String nameToConfirm) throws EngineException;
	
	public void updateConfiguration(ConfirmationConfiguration toUpdate) throws EngineException;
	
	public ConfirmationConfiguration getConfiguration(String typeToConfirm, String nameToConfirm) throws EngineException;
	
	public List<ConfirmationConfiguration> getAllConfigurations() throws EngineException;
}

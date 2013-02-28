/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

import pl.edu.icm.unity.types.DescribedObjectImpl;


/**
 * Configured instance of {@link CredentialType}.
 * 
 * @author K. Benedyczak
 */
public class CredentialDefinition extends DescribedObjectImpl
{
	private String typeId;
	private String jsonConfiguration;

	public CredentialDefinition()
	{
		super();
	}

	public CredentialDefinition(String id, String typeId, String name, String description)
	{
		super(id, name, description);
		this.typeId = typeId;
	}
	
	public String getTypeId()
	{
		return typeId;
	}
	public void setTypeId(String typeId)
	{
		this.typeId = typeId;
	}
	public String getJsonConfiguration()
	{
		return jsonConfiguration;
	}
	public void setJsonConfiguration(String jsonConfiguration)
	{
		this.jsonConfiguration = jsonConfiguration;
	}
}

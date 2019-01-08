/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.userimport;

/**
 * Defines user import to be performed
 * 
 * @author K. Benedyczak
 */
public class UserImportSpec
{
	public final String importerKey;
	public final String identityValue;
	public final String identityType;
	
	public UserImportSpec(String importerKey, String identityValue, String identityType)
	{
		this.importerKey = importerKey;
		this.identityValue = identityValue;
		this.identityType = identityType;
	}
	
	public static UserImportSpec withAllImporters(String identityValue, String identityType)
	{
		return new UserImportSpec(null, identityValue, identityType);
	}
	
	public boolean isUseAllImporters()
	{
		return importerKey == null;
	}
}
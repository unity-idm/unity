/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

/**
 * Overall configuration of local authentication rules. Includes a list of {@link LocalAuthnVerification}s
 * which are atomic, configured authentication methods. For instance it can be used to 
 * define that clients must authenticate with passwords which have several security constraints 
 * (enforced at password assign time) and also with TLS certificate.
 * 
 * LACs are assigned to entities, to define how do they (have to/can) authenticate.
 * @author K. Benedyczak
 */
public class LocalAccessClass
{
	private String id;
	private String description;
	private LocalAuthnVerification[] lacms;


	public LocalAccessClass(String id, String description, LocalAuthnVerification[] lacms)
	{
		this.id = id;
		this.description = description;
		this.lacms = lacms;
	}

	public LocalAuthnVerification[] getLacms()
	{
		return lacms;
	}

	public String getId()
	{
		return id;
	}

	public String getDescription()
	{
		return description;
	}
}

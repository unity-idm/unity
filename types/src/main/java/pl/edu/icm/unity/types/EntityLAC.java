/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

/**
 * Stores information about entity authentication: the {@link LocalAccessClass} and the state.
 * @author K. Benedyczak
 */
public class EntityLAC
{
	private LocalAccessClass lac;
	private LocalAuthnState state;

	public EntityLAC(LocalAccessClass lac, LocalAuthnState state)
	{
		this.lac = lac;
		this.state = state;
	}
	
	public LocalAccessClass getLac()
	{
		return lac;
	}
	
	public LocalAuthnState getState()
	{
		return state;
	}
}

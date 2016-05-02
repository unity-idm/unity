/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz.rdbmsflush;

/**
 * Describes RDBMS mutation.
 * @author K. Benedyczak
 */
public class RDBMSMutationEvent
{
	private String operation;
	private String dao;
	private Object[] args;

	protected RDBMSMutationEvent() {}
	
	public RDBMSMutationEvent(String dao, String operation, Object... args)
	{
		this.operation = operation;
		this.dao = dao;
		this.args = args;
	}

	public String getOperation()
	{
		return operation;
	}

	public String getDao()
	{
		return dao;
	}

	public Object[] getArgs()
	{
		return args;
	}

	@Override
	public String toString()
	{
		return "RDMSMutationEvent [operation=" + operation + ", dao=" + dao + "]";
	}
}

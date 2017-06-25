/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Implementation should handle transactions - used by {@link TransactionalAspect}.
 * Implementing bean name must be NAME_PFX + StorageEngine name.
 * @author K. Benedyczak
 */
public interface TransactionEngine
{
	String NAME_PFX = "TransactionEngine";
	
	Object runInTransaction(ProceedingJoinPoint pjp, int maxRetries, boolean transactional) throws Throwable;
}

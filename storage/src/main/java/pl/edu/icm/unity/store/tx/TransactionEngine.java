/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

import org.aspectj.lang.ProceedingJoinPoint;

import pl.edu.icm.unity.base.internal.Transactional;

/**
 * Implementation should handle transactions - used by {@link TransactionalAspect}.
 * @author K. Benedyczak
 */
public interface TransactionEngine
{
	Object runInTransaction(ProceedingJoinPoint pjp, Transactional transactional) throws Throwable;
}

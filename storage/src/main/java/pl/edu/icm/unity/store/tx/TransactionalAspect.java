/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.internal.StorageEngine;
import pl.edu.icm.unity.base.internal.Transactional;
import pl.edu.icm.unity.base.internal.TransactionalRunner;
import pl.edu.icm.unity.store.hz.tx.HzTransactionEngine;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionEngine;

/**
 * Aspect providing transaction functionality. Real functionality is provided by an {@link TransactionEngine} 
 * implementation.
 * <p>
 * The aspect is installed on all public methods of classes implementing interfaces from the 
 * pl.edu.icm.unity package (and its children). Either the whole class or a method must 
 * be annotated with the {@link Transactional} annotation. The annotation can be used to provide additional settings.
 * If the code to be run in transaction is not fulfilling the above rule then can be wrapped in 
 * {@link TransactionalRunner}. 
 * 
 * @author K. Benedyczak
 */
@Component
@Aspect
public class TransactionalAspect
{
	@Autowired
	private SQLTransactionEngine rdbmsTxEngine;
	@Autowired
	private HzTransactionEngine hzTxEngine;
	
	@Around("execution(public * pl.edu.icm.unity..*.*(..)) && "
			+ "@within(transactional)")
	private Object retryIfNeeded4Class(ProceedingJoinPoint pjp, Transactional transactional) throws Throwable 
	{
		return getEngine(transactional).runInTransaction(pjp, transactional);
	};
	
	@Around("execution(public * pl.edu.icm.unity..*.*(..)) && "
			+ "@annotation(transactional)")
	public Object retryIfNeeded4Method(ProceedingJoinPoint pjp, Transactional transactional) throws Throwable 
	{
		return getEngine(transactional).runInTransaction(pjp, transactional);
	}
	
	private TransactionEngine getEngine(Transactional cfg)
	{
		return cfg.storageEngine() == StorageEngine.hz ? hzTxEngine : rdbmsTxEngine;
	}
}

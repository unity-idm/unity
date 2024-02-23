/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.StorageEngine;
import pl.edu.icm.unity.base.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

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
	private Map<String, TransactionEngine> engines;
	private TransactionEngine defaultEngine;
	
	@Autowired
	public TransactionalAspect(Map<String, TransactionEngine> engines, StorageConfiguration storageCfg)
	{
		this.engines = engines;
		StorageEngine defEngine = storageCfg.getEnumValue(StorageConfiguration.ENGINE, StorageEngine.class);
		defaultEngine = getEngine(defEngine);
	}
	
	@Around("(execution(public * pl.edu.icm.unity..*.*(..)) || execution(public * io.imunity..*.*(..))) && "
			+ "@within(transactional)")
	private Object retryIfNeeded4Class(ProceedingJoinPoint pjp, TransactionalExt transactional) throws Throwable 
	{
		return getEngine(transactional.storageEngine()).runInTransaction(pjp, transactional.maxRetries(), 
				transactional.autoCommit());
	};
	
	@Around("(execution(public * pl.edu.icm.unity..*.*(..)) || execution(public * io.imunity..*.*(..))) && "
			+ "@annotation(transactional)")
	public Object retryIfNeeded4Method(ProceedingJoinPoint pjp, TransactionalExt transactional) throws Throwable 
	{
		return getEngine(transactional.storageEngine()).runInTransaction(pjp, transactional.maxRetries(), 
				transactional.autoCommit());
	}
	
	@Around("(execution(public * pl.edu.icm.unity..*.*(..)) || execution(public * io.imunity..*.*(..))) && "
			+ "@within(transactional)")
	private Object retryIfNeeded4Class(ProceedingJoinPoint pjp, Transactional transactional) throws Throwable 
	{
		return defaultEngine.runInTransaction(pjp, transactional.maxRetries(), 
				transactional.autoCommit());
	};
	
	@Around("(execution(public * pl.edu.icm.unity..*.*(..)) || execution(public * io.imunity..*.*(..))) && "
			+ "@annotation(transactional)")
	public Object retryIfNeeded4Method(ProceedingJoinPoint pjp, Transactional transactional) throws Throwable 
	{
		return defaultEngine.runInTransaction(pjp, transactional.maxRetries(), 
				transactional.autoCommit());
	}
	
	
	private TransactionEngine getEngine(StorageEngine cfg)
	{
		return engines.get(TransactionEngine.NAME_PFX + cfg.name());
	}
}

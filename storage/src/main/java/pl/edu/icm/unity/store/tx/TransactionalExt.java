/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import pl.edu.icm.unity.store.StorageEngine;
import pl.edu.icm.unity.base.tx.Transactional;

/**
 * Extension of the default {@link Transactional} annotation allowing to overwrite the system configured 
 * storage engine. Useful in low level code, especially when one engine relies on operations of another engine.
 * 
 * @author K. Benedyczak
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface TransactionalExt 
{
	public static final int DEF_MAX_RETRIES = 5;
	
	int maxRetries() default DEF_MAX_RETRIES;
	
	boolean autoCommit() default true;

	StorageEngine storageEngine();
}

/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Implementation is executed in a transaction which is automatically retried.
 * @author K. Benedyczak
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Transactional 
{
	public static final int DEF_MAX_RETRIES = 5;
	
	int maxRetries() default DEF_MAX_RETRIES;
	
	boolean autoCommit() default true;
}

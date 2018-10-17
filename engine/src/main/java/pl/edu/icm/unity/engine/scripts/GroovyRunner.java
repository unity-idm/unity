/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.scripts;

import java.io.Reader;

import org.apache.logging.log4j.Logger;

import com.google.common.base.Stopwatch;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Executes a given Groovy script using provided binding (context). 
 *
 * @author golbi
 */
public class GroovyRunner
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER, GroovyRunner.class);
	
	public static void run(String phase, String name, 
			Reader scriptReader, Binding binding)
	{
		GroovyShell shell = new GroovyShell(binding);
		LOG.info("{} event triggers invocation of Groovy script: {}", phase, name);
		Stopwatch timer = Stopwatch.createStarted();
		try
		{
			shell.evaluate(scriptReader);
		} catch (Exception e)
		{
			throw new InternalException("Failed to execute Groovy " 
					+ " script: " + name + ": reason: " + e.getMessage(), e);
		}
		LOG.debug("Groovy script: {} finished in {}", name, timer);
	}
}

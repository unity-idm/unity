/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.control.CompilationFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.base.Stopwatch;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.initializers.ContentInitConf;
import pl.edu.icm.unity.engine.api.initializers.InitializerType;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Executes GROOVY scripts given by user in
 * {@link UnityServerConfiguration#CONTENT_INITIALIZERS} configuration.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
@Component
public class ContentGroovyExecutor
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER, ContentGroovyExecutor.class);
	private GroovyShell shell;
	
	@Autowired
	@Qualifier("insecure")
	private CredentialManagement credMan;
	@Autowired
	private UnityServerConfiguration config;

	@PostConstruct
	public void initialize()
	{
		shell = new GroovyShell(getBinding());
	}

	public void run(ContentInitConf conf)
	{
		if (conf == null || conf.getType() != InitializerType.GROOVY)
			throw new IllegalArgumentException(
					"conf must not be null and must be of " + InitializerType.GROOVY + " type");
		run(conf.getFile());
	}

	private void run(File file)
	{
		LOG.info("Executing {} script: {}", InitializerType.GROOVY, file.toString());
		Stopwatch timer = Stopwatch.createStarted();
		try
		{
			shell.evaluate(file);
		} catch (CompilationFailedException | IOException e)
		{
			throw new InternalException("Failed to initialize content from " + InitializerType.GROOVY 
					+ " script: " + file.toString() + ": reason: " + e.getMessage(), e);
		}
		LOG.info("{} script: {} finished in {}", InitializerType.GROOVY, file.toString(), timer);
	}

	private Binding getBinding()
	{
		Binding binding = new Binding();
		binding.setVariable("credentialManagement", credMan);
		binding.setVariable("config", config);
		binding.setVariable("log", LOG);
		return binding;
	}
}

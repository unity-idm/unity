/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.authproxy;

import java.text.DateFormat;
import java.util.Date;

import org.apache.logging.log4j.Logger;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;


//TODO - shared code, logging, profiles
public class UnityAuthProxyApplication
{
//	static 
//	{
//		System.setProperty(LoggerFactory.LOGGER_FACTORY_PROPERTY, UnityLoggerFactory.class.getName());
//	}
	private static final Logger log = Log.getLogger(Log.U_SERVER, UnityAuthProxyApplication.class);

	private ClassPathXmlApplicationContext container;

//	private String[] activeProfiles;
	
	public UnityAuthProxyApplication(String... activeProfiles)
	{
//		this.activeProfiles = activeProfiles;
	}

	public void run(String[] args)
	{
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG);
		System.out.println(df.format(new Date()) + ": Starting UNITY Web Server");
		log.info("\n**************************\nStarting UNITY Authenticating Terminal Proxy Server\n**************************");
		
		container = new ClassPathXmlApplicationContext(new String[] {"classpath*:META-INF/components.xml"}, false);
		MutablePropertySources propertySources = container.getEnvironment().getPropertySources();
		SimpleCommandLinePropertySource clps = new SimpleCommandLinePropertySource(args);
		propertySources.addFirst(clps);
//		ConfigurableEnvironment env = container.getEnvironment();
//		env.setActiveProfiles(UnityServerConfiguration.PROFILE_PRODUCTION);
//		for (String profile: activeProfiles)
//			env.addActiveProfile(profile);
		container.setAllowBeanDefinitionOverriding(false);
		container.refresh();

		container.registerShutdownHook();
		container.start();
	}

	public static void main(String[] args)
	{
		UnityAuthProxyApplication theServer = new UnityAuthProxyApplication();
		theServer.run(args);
	}
	
	
	
	/**
	 * Responsible for logging the end of startup procedure and the start of the shutdown
	 */
	@Component		
	public static class InnerServerLifecycle extends LifecycleBase
	{
		@Override
		public void start()
		{
			DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG);
			System.out.println(df.format(new Date()) + ": UNITY Server Started");
			log.info("\n**************************\nUNITY Server Started\n**************************");
			super.start();
		}
		
		@Override
		public void stop()
		{
			DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG);
			System.out.println(df.format(new Date()) + ": Stopping UNITY Server");
			log.info("\n**************************\nStopping UNITY Server\n**************************");
			super.stop();
		}

		@Override
		public int getPhase()
		{
			return Integer.MAX_VALUE;
		}
	}

	/**
	 * Responsible for logging the end of shutdown operation
	 */
	@Component
	public static class ShutdownFinished extends LifecycleBase
	{
		@Override
		public void stop()
		{
			DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG);
			System.out.println(df.format(new Date()) + ": Stopped UNITY Server");
			log.info("\n**************************\nStopped UNITY Server\n**************************");
			super.stop();
		}

		@Override
		public int getPhase()
		{
			return Integer.MIN_VALUE;
		}
	}
	
	private static abstract class LifecycleBase implements SmartLifecycle
	{
		protected boolean running;
		
		@Override
		public void start()
		{
			running = true;
		}

		@Override
		public void stop()
		{
			running = false;
		}

		@Override
		public boolean isRunning()
		{
			return running;
		}

		@Override
		public boolean isAutoStartup()
		{
			return true;
		}

		@Override
		public void stop(Runnable callback) {stop(); callback.run();}
	}

}

/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.scripts;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;

import groovy.lang.Binding;
import pl.edu.icm.unity.base.event.Event;
import pl.edu.icm.unity.engine.api.event.EventCategory;

/**
 * Creates a "mock" spring application context and invokes an argument script with it.
 * The mocked managers are merely printing the fact that they were invoked.
 * @author K. Benedyczak
 */
public class ScrpitTestApplication
{
	public static void main(String... args)
	{
		if (args.length != 1)
		{
			System.out.println("This program shall be used with a single argument, "
					+ "path to Groovy script file");
			System.exit(2);
		}
		testDrive(args[0]);
	}
	
	private static void testDrive(String path)
	{
		Reader scriptReader;
		try
		{
			scriptReader = new FileReader(path);
		} catch (FileNotFoundException e)
		{
			System.err.println("Error reading script file: " + e);
			System.exit(3);
			return;
		}
		Event event = new Event(EventCategory.TEST); 
		Binding binding = MockGroovyBindingProvider.getBinding(event);
		System.out.println("Executing " + path + " script in sandbox environment.");
		GroovyRunner.run("sandbox-test", path, scriptReader, binding);
	}
}

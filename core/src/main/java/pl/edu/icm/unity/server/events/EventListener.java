/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.events;

/**
 * Event listeners are {@link Event} consumers. E.g. a listener can send email with notification or 
 * dump the event to an auditing database.
 * @author K. Benedyczak
 */
public interface EventListener
{
	public static final int DEFAULT_MAX_FAILURES = 10+7*24;
	/**
	 * @return only events of the returned category will be fed to the listener
	 */
	public String getCategory();
	
	/**
	 * @return if true is returned then the listener's handle method will be invoked 
	 * without persisting the event into database first. Therefore it is assumed that 
	 * handler is going to perform an operation which nearly always succeeds. Otherwise the event
	 * is stored in DB first and if the handle method fails or throws an exception then the handling will be
	 * repeated after some time.
	 */
	public boolean isLightweight();
	
	/**
	 * This method should perform a fast filtering of uninteresting events.
	 * @param event
	 * @return true if the event should be handled by this listener
	 */
	public boolean isWanted(Event event); 
	
	/**
	 * Called only on events of a proper category, for which isWanted returned true. 
	 * @param event 
	 * @return true if the event was processed successfully. Returning false is relevant only 
	 * for the heavy-weight listeners.
	 */
	public boolean handleEvent(Event event);
	
	/**
	 * @return system unique and constant ID of the listener instance. It is suggested to form it 
	 * from the full class name concatenated with listener name if it can have many instances.
	 */
	public String getId();
	
	/**
	 * @return after how many failures the system should give up sending the event. It is suggested
	 * to return {@link #DEFAULT_MAX_FAILURES}  
	 */
	public int getMaxFailures();
}

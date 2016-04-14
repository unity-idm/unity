/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import org.springframework.stereotype.Component;

/**
 * Responsible for converting DAO mutation operations to events and queuing them. 
 * Transactions are supported: events are queued locally in transaction stack first
 * and are pushed to the global RDBMS operations queue at commit time. 
 * 
 * @author K. Benedyczak
 */
@Component
public class RDBMSEventProducer
{

}

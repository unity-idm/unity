/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 *
 * Created on 15 kwi 2016
 * Author: K. Benedyczak
 */

/**
 * Code supporting back channel with RDBMS updates queued by Hazelcast DAO. The code takes 
 * the queued events and applies them to database.
 * @author K. Benedyczak
 */
package pl.edu.icm.unity.store.rdbmsflush;
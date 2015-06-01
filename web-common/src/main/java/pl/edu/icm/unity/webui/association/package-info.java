/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 *
 * Created on 31 maj 2015
 * Author: K. Benedyczak
 */

/**
 * Code with UI responsible for association of identities. The code is used solely for user-driven
 * (not admin driven) association, which can be performed only in case when a new identity is 
 * associated with an already existing one.
 * The process can be started in two ways: either after login with an unknown remote identity 
 * (as an alternative to filling a registration form) or after login from home UI, where existing user
 * has to login as unknown identity with a remote IdP.
 * 
 * @author K. Benedyczak
 */
package pl.edu.icm.unity.webui.association;
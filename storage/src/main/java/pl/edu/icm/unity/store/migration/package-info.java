/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 *
 * Created on 16 cze 2016
 * Author: K. Benedyczak
 */

/**
 * Functionality purely related to upgrading old JSON dump to current format. Updaters 
 * are incremental, i.e. each is updating from previous version to the fixed version of the updater,
 * and updaters are called in order. 
 * @author K. Benedyczak
 */
package pl.edu.icm.unity.store.migration;
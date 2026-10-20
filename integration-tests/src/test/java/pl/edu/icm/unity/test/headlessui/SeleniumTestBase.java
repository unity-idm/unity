/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.headlessui;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "unityConfig=src/test/resources/selenium/unityServer.conf")
public abstract class SeleniumTestBase extends io.imunity.test.ui.SeleniumTestBase
{
}

/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;


/**
 * Default configuration of annotations required to run Unity integration test with Spring
 * @author K. Benedyczak
 */
@Retention(RetentionPolicy.RUNTIME)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
@ActiveProfiles("test")
@TestPropertySource(properties = { "unityConfig: src/test/resources/unityServer.conf" })
public @interface UnityIntegrationTest {

}

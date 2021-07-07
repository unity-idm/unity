/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Injects {@link ObjectMapper} to Spring container
 * @author K. Benedyczak
 */
@Configuration
public class ObjectMapperSpringProvider
{
	@Bean
	public ObjectMapper defaultObjectMapper()
	{
		return new ObjectMapper().findAndRegisterModules();
	}
}

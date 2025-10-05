/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.DomainValidator.ArrayType;

import pl.edu.icm.unity.engine.api.utils.URLFactory;

/**
 * Generates a list of valid TLDs which are not available for apache commons EmailValidator. 
 * See {@link EmailUtils}. Can be run from time to time and results put there, so that we properly validate all real TLDs.
 */
public class GenerateAdditionalValidTLDsList
{
	private static final String IANA_REGISTRY = "https://data.iana.org/TLD/tlds-alpha-by-domain.txt";
	
	public static void main(String[] args) throws Exception
	{
		List<String> allTlds = readDomains();
		Set<String> known = getKnown();
		List<String> missing = allTlds.stream()
				.map(String::toLowerCase)
				.filter(s -> !known.contains(s))
				.sorted()
				.toList();
		System.out.println(missing.stream().collect(Collectors.joining("\",\n\"", "{\"", "\"};")));
	}

	private static List<String> readDomains() throws IOException
	{
		URL url = URLFactory.of(IANA_REGISTRY);
		InputStream is = url.openStream();
		List<String> ret = IOUtils.readLines(is, StandardCharsets.US_ASCII);
		ret.removeFirst(); //comment line
		return ret;
	}
	
	private static Set<String> getKnown()
	{
		Set<String> allKnown = new HashSet<>();
		Collections.addAll(allKnown, DomainValidator.getTLDEntries(ArrayType.GENERIC_RO));
		Collections.addAll(allKnown, DomainValidator.getTLDEntries(ArrayType.COUNTRY_CODE_RO));
		Collections.addAll(allKnown, DomainValidator.getTLDEntries(ArrayType.INFRASTRUCTURE_RO));
		return allKnown;
	}
}

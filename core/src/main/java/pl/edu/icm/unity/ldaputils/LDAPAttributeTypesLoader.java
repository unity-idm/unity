/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldaputils;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import antlr.RecognitionException;
import antlr.TokenStreamException;

import pl.edu.icm.unity.ldaputils.generated.LdapLexer;
import pl.edu.icm.unity.ldaputils.generated.LdapParser;

/**
 * Loads attribute types from LDAP schema files. Additionally the attribute types can be
 * resolved wrt to schema inheritance.
 * @author K. Benedyczak
 */
public class LDAPAttributeTypesLoader
{
	public static List<LDAPAttributeType> loadSimple(Reader input) throws RecognitionException, TokenStreamException
	{
		LdapLexer lexer = new LdapLexer(input);
		LdapParser parser = new LdapParser(lexer);

		LDAPSchema parsed = parser.startRule();
		return parsed.getAttributeTypes();
	}
	
	public static List<LDAPAttributeType> loadWithInheritance(Reader input, List<LDAPAttributeType> knownTypes) 
			throws RecognitionException, TokenStreamException
	{
		List<LDAPAttributeType> loaded = loadSimple(input);
		
		List<LDAPAttributeType> working = new ArrayList<LDAPAttributeType>(loaded.size() + 
				(knownTypes == null ? 0 : knownTypes.size()));
		working.addAll(loaded);
		if (knownTypes != null)
			working.addAll(knownTypes);
		
		Map<String, LDAPAttributeType> types = new HashMap<String, LDAPAttributeType>();
		for (LDAPAttributeType at: working)
			for (String name: at.getNames())
				types.put(name, at);
		Set<String> resolved = new HashSet<String>(); 
		
		boolean changed = true;
		while (changed)
		{
			changed = false;
			for (LDAPAttributeType at: working)
			{
				String oid = at.getOid();
				if (resolved.contains(oid) || at.getSuperclass() == null)
					continue;
				
				LDAPAttributeType sup = types.get(at.getSuperclass());
				if (sup == null)
					continue;
				
				if (sup.getSuperclass() != null && !resolved.contains(sup.getOid()))
					continue;
				
				setFromParent(at, sup);
				resolved.add(oid);
				changed = true;
			}
		}
		
		return loaded;
	}
	
	private static void setFromParent(LDAPAttributeType at, LDAPAttributeType parent)
	{
		if (at.getEquality() == null)
			at.setEquality(parent.getEquality());
		if (at.getOrdering() == null)
			at.setOrdering(parent.getOrdering());
		if (at.getSubstring() == null)
			at.setSubstring(parent.getSubstring());
		if (at.getSyntax() == null)
			at.setSyntax(parent.getSyntax());
	}
}

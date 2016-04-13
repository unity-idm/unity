/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

import pl.edu.icm.unity.base.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.store.hz.SerializerProvider;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;

/**
 * Serialization of {@link IdentityType} for hazelcast
 * @author K. Benedyczak
 */
@Component
public class IdentityTypeHzSerializer implements SerializerProvider<IdentityType>
{
	@Autowired
	private IdentityTypesRegistry idTypesRegistry;
	
	@Override
	public int getTypeId()
	{
		return 1;
	}

	@Override
	public void destroy()
	{
	}

	@Override
	public void write(ObjectDataOutput out, IdentityType object) throws IOException
	{
		out.writeUTF(object.getIdentityTypeProvider().getId());
		out.writeUTF(object.getDescription());
		out.writeInt(object.getMaxInstances());
		out.writeInt(object.getMinInstances());
		out.writeInt(object.getMinVerifiedInstances());
		out.writeBoolean(object.isSelfModificable());
		writeMap(out, object.getExtractedAttributes());
	}

	private void writeMap(ObjectDataOutput out, Map<String, String> map) throws IOException
	{
		out.writeInt(map.size());
		for (Entry<String, String> entry : map.entrySet())
		{
			out.writeUTF(entry.getKey());
			out.writeUTF(entry.getValue());
		}
	}
	
	private Map<String, String> readMap(ObjectDataInput in) throws IOException
	{
		int size = in.readInt();
		Map<String, String> ret = new HashMap<>(size);
		for (int i=0; i<size; i++)
			ret.put(in.readUTF(), in.readUTF());
		return ret;
	}

	@Override
	public IdentityType read(ObjectDataInput in) throws IOException
	{
		String type = in.readUTF();
		IdentityTypeDefinition idType = idTypesRegistry.getByName(type);
		
		IdentityType ret = new IdentityType(idType);
		ret.setDescription(in.readUTF());
		ret.setMaxInstances(in.readInt());
		ret.setMinInstances(in.readInt());
		ret.setMinVerifiedInstances(in.readInt());
		ret.setSelfModificable(in.readBoolean());
		ret.getExtractedAttributes().putAll(readMap(in));
		return ret;
	}

	@Override
	public Class<IdentityType> getTypeClass()
	{
		return IdentityType.class;
	}
}

/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

import pl.edu.icm.unity.store.hz.SerializerProvider;
import pl.edu.icm.unity.store.impl.attribute.AttributeTypeHzSerializer;
import pl.edu.icm.unity.store.impl.identity.IdentityTypeHzSerializer;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * TODO - currently one big mess
 * @author K. Benedyczak
 */
@Component
public class EventsSerializer implements SerializerProvider<RDBMSEventsBatch>
{
	@Autowired
	private AttributeTypeHzSerializer atS;
	@Autowired
	private IdentityTypeHzSerializer itS;
	
	@Override
	public void write(ObjectDataOutput out, RDBMSEventsBatch batch) throws IOException
	{
		List<RDBMSMutationEvent> events = batch.getEvents();
		out.writeInt(events.size());
		for (RDBMSMutationEvent event: events)
		{
			out.writeUTF(event.getDao());
			out.writeUTF(event.getOperation());
			out.writeInt(event.getArgs().length);
			for (Object arg: event.getArgs())
				writeArg(out, arg);
		}
	}

	private void writeArg(ObjectDataOutput out, Object arg) throws IOException
	{
		if (arg instanceof String)
		{
			out.writeInt(1);
			out.writeUTF((String) arg);
		} else if (arg instanceof Integer)
		{
			out.writeInt(2);
			out.writeInt((Integer) arg);
		} else if (arg instanceof AttributeType)
		{
			out.writeInt(3);
			atS.write(out, (AttributeType) arg);
		} else if (arg instanceof IdentityType)
		{
			out.writeInt(4);
			itS.write(out, (IdentityType) arg);
		}
	}
	
	@Override
	public RDBMSEventsBatch read(ObjectDataInput in) throws IOException
	{
		int size = in.readInt();
		List<RDBMSMutationEvent> events = new ArrayList<>();
		for (int i=0; i<size; i++)
		{
			String dao = in.readUTF();
			String op = in.readUTF();
			int argsLen = in.readInt();
			Object[] args = new Object[argsLen];
			for (int j=0; j<argsLen; j++)
				args[j] = readArg(in);
			RDBMSMutationEvent event = new RDBMSMutationEvent(dao, op, args);
			events.add(event);
		}
		
		return new RDBMSEventsBatch(events);
	}
	
	private Object readArg(ObjectDataInput in) throws IOException
	{
		int type = in.readInt();
		switch (type)
		{
		case 1:
			return in.readUTF();
		case 2:
			return in.readInt();
		case 3:
			return atS.read(in);
		case 4:
			return itS.read(in);
		}
		throw new IllegalStateException("Unknown data type");
	}

	@Override
	public int getTypeId()
	{
		return 1000;
	}

	@Override
	public void destroy()
	{
	}

	@Override
	public Class<RDBMSEventsBatch> getTypeClass()
	{
		return RDBMSEventsBatch.class;
	}
}

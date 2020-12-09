package de.bitc.jackson;

import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;

public class IsEmptyDeserializationWrapper extends BeanDeserializer {

	public IsEmptyDeserializationWrapper(BeanDeserializerBase src) {
		super(src);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -740637402884972568L;



}

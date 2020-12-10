package de.bitc.jackson;

import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;

public class IsEmptyDeserializationWrapper extends BeanDeserializer {

	private static final long serialVersionUID = -740637402884972568L;
	
	public IsEmptyDeserializationWrapper(BeanDeserializerBase src) {
		super(src);
		// TODO Auto-generated constructor stub
	}	

    private Object isEmptyVanillaDeserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        p.nextToken();
        if (p.hasTokenId(JsonTokenId.ID_FIELD_NAME)) {
            final Object bean = _valueInstantiator.createUsingDefault(ctxt);
            // [databind#631]: Assign current value, to be accessible by custom serializers
            p.setCurrentValue(bean);
            String propName = p.getCurrentName();
            do {
                p.nextToken();
                SettableBeanProperty prop = _beanProperties.find(propName);

                if (prop != null) { // normal case
                    logger.debug("property: {}", prop.getName());
                    try {
                        prop.deserializeAndSet(p, ctxt, bean);
                    } catch (Exception e) {
                        wrapAndThrow(e, bean, propName, ctxt);
                    }
                    continue;
                }
                handleUnknownVanilla(p, ctxt, bean, propName);
            } while ((propName = p.nextFieldName()) != null);
            return bean;
        }
        return null;
    }

}

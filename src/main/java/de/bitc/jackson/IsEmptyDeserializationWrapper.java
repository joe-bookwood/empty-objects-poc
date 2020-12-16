package de.bitc.jackson;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;

public class IsEmptyDeserializationWrapper extends BeanDeserializer {

	private static final long serialVersionUID = -740637402884972568L;

	private static final Logger logger = LoggerFactory.getLogger(IsEmptyDeserializationWrapper.class);


	public IsEmptyDeserializationWrapper(BeanDeserializerBase src) {
		super(src);
		_vanillaProcessing = true;
	}

	@Override
	public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

		// _vanillaProcessing = true;
		// common case first
		if (p.isExpectedStartObjectToken()) {
			logger.debug("p.isExpectedStartObjectToken");
			if (_vanillaProcessing) {
				logger.debug("Vanilla Processing!");
				return isEmptyVanillaDeserialize(p, ctxt);
				// return vanillaDeserialize(p, ctxt, p.nextToken());
			}

			// 23-Sep-2015, tatu: This is wrong at some many levels, but for now... it is
			// what it is, including "expected behavior".
			p.nextToken();
			if (_objectIdReader != null) {
				logger.debug("deserializeWithObjectId");
				return deserializeWithObjectId(p, ctxt);
			}
			logger.debug("deserializeFromObject");
			return deserializeFromObject(p, ctxt);
		}
		logger.debug("_deserializeOther");
		return _deserializeOther(p, ctxt, p.getCurrentToken());

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

	/**
	 * General version used when handling needs more advanced features.
	 */
	@Override
	public Object deserializeFromObject(JsonParser p, DeserializationContext ctxt) throws IOException {
		/*
		 * 09-Dec-2014, tatu: As per [databind#622], we need to allow Object Id
		 * references to come in as JSON Objects as well; but for now assume they will
		 * be simple, single-property references, which means that we can recognize them
		 * without having to buffer anything. Once again, if we must, we can do more
		 * complex handling with buffering, but let's only do that if and when that
		 * becomes necessary.
		 */
		if ((_objectIdReader != null) && _objectIdReader.maySerializeAsObject()) {
			if (p.hasTokenId(JsonTokenId.ID_FIELD_NAME)
					&& _objectIdReader.isValidReferencePropertyName(p.getCurrentName(), p)) {
				logger.debug("deserializeFromObjectId {}", p.getCurrentName());
				return deserializeFromObjectId(p, ctxt);
			}
		}
		if (_nonStandardCreation) {
			if (_unwrappedPropertyHandler != null) {
				logger.debug("deserializeWithUnwrapped");
				return deserializeWithUnwrapped(p, ctxt);
			}
			if (_externalTypeIdHandler != null) {
				logger.debug("deserializeWithExternalTypeId");
				return deserializeWithExternalTypeId(p, ctxt);
			}
			Object bean = deserializeFromObjectUsingNonDefault(p, ctxt);
			/*
			 * 27-May-2014, tatu: I don't think view processing would work at this point, so
			 * commenting it out; but leaving in place just in case I forgot something
			 * fundamental...
			 */
			/*
			 * if (_needViewProcesing) { Class<?> view = ctxt.getActiveView(); if (view !=
			 * null) { return deserializeWithView(p, ctxt, bean, view); } }
			 */
			logger.debug("return bean nonStandardCreation {}", bean.getClass().getSimpleName());
			return bean;
		}
		final Object bean = _valueInstantiator.createUsingDefault(ctxt);
		// [databind#631]: Assign current value, to be accessible by custom
		// deserializers
		p.setCurrentValue(bean);
		if (p.canReadObjectId()) {
			Object id = p.getObjectId();
			if (id != null) {
				_handleTypedObjectId(p, ctxt, bean, id);
			}
		}
		if (_injectables != null) {
			injectValues(ctxt, bean);
		}
		if (_needViewProcesing) {
			Class<?> view = ctxt.getActiveView();
			if (view != null) {
				logger.debug("deserializeWithView");
				return deserializeWithView(p, ctxt, bean, view);
			}
		}
		if (p.hasTokenId(JsonTokenId.ID_FIELD_NAME)) {
			String propName = p.getCurrentName();
			do {
				p.nextToken();
				SettableBeanProperty prop = _beanProperties.find(propName);
				if (prop != null) { // normal case
					logger.debug("prop != null name={}", prop.getName());
					try {
						prop.deserializeAndSet(p, ctxt, bean);
					} catch (Exception e) {
						wrapAndThrow(e, bean, propName, ctxt);
					}
					continue;
				}
				logger.debug("handleUnknownVanilla");
				handleUnknownVanilla(p, ctxt, bean, propName);
			} while ((propName = p.nextFieldName()) != null);
		}
		logger.debug("return bean {}", bean.getClass().getSimpleName());
		return bean;
	}
}

package de.bitc.emptyobjectspoc;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;

import de.bitc.jackson.IsEmptyDeserializationWrapper;
import de.bitc.model.Car;
import de.bitc.model.Employee;

@SpringBootApplication
@EnableJpaRepositories(basePackages = { "de.bitc.repository" })
@ComponentScan(basePackages = { "de.bitc.controller" })
@EntityScan(basePackages = { "de.bitc.model" })
public class EmptyObjectsPocApplication {

	private static final Logger logger = LoggerFactory.getLogger(EmptyObjectsPocApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(EmptyObjectsPocApplication.class, args);
	}

	/**
	 * Jackson configuration
	 * 
	 * @return the configuration
	 */
	@Bean
	public Jackson2ObjectMapperFactoryBean jackson2ObjectMapperFactoryBean() {
		List<Module> modules = new ArrayList<Module>();
		final Jackson2ObjectMapperFactoryBean factoryBean = new Jackson2ObjectMapperFactoryBean();
		factoryBean.setFeaturesToEnable(SerializationFeature.INDENT_OUTPUT);
		factoryBean.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
		SimpleModule module = new SimpleModule();
		module.setDeserializerModifier(new BeanDeserializerModifier() {

			@Override
			public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc,
					JsonDeserializer<?> deserializer) {
				final Class<?> beanClass = beanDesc.getBeanClass();
				if (beanClass == Car.class || beanClass == Employee.class) {
					logger.debug("is empty deserializer {}", beanClass.getSimpleName());
					return new IsEmptyDeserializationWrapper((BeanDeserializerBase) deserializer);
				}
				logger.debug("set std deserializer {}", beanClass.getSimpleName());
				return super.modifyDeserializer(config, beanDesc, deserializer);
			}
		});
		modules.add(module);
		factoryBean.setModules(modules);
		return factoryBean;
	}

}

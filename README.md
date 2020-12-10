
# Exclude empty json objects from deserialisation

This POC is used to solve the problem, that Jackson is not able to exclude empty json objects.
This cause empty entries in the database. The problem ist discussed at 
[Stackoverflow](https://stackoverflow.com/questions/65130489/how-ignore-empty-json-objects-like-car-that-cause-empty-pojos-after-deseri)


## The configuration

The important classes to solve the problem:

*   Jackson configuration is located in `de.bitc.emptyobjectspoc.EmptyObjectsPocApplication`
*   The wrapped bean deserializer is located in `de.bitc.jackson.IsEmptyDeserializationWrapper`

## Build and run
To build this project `java 11` and `maven` is needed.
At first enter the main folder `empty-objects-poc`. To build the app, run `mvn -DskipTests package`,
skip the tests, because the test will fail. The test check the correct answer without empty objects from the rest controller. To run the test use `mvn test` or `mvn package`. 

To run the app use `java -jar target/empty-objects-poc-0.0.1-SNAPSHOT.jar` .

To send a test json:

	curl -iX POST -H 'Content-Type: application/json' \
	  -d '{"name" : "ACME", "employee" : { "name " : "worker1", "car" : {}}}' \
	   http://localhost:8080/company

Keep the focus on the empty json object `"car" : {}`, it will be stored in the database as empty object
and the answer looks like:

	{
	  "id" : 1,
	  "version" : 0,
	  "name" : "ACME",
	  "employee" : {
	    "id" : 2,
	    "version" : 0,
	    "car" : {
	      "id" : 3,
	      "version" : 0
	    }
	  }
	}

The empty object `car` is stored in the database. This is unwanted.

### Access the in memory Database

To verify the stored Data you can access the `h2` SQL database via `http://127.0.0.1:8080/h2-console/`
and use `sa` as login and password. Use `jdbc:h2:mem:testdb` as database url and `org.h2.Driver` as
driver class.

## A copy of the stackoverflow question

I have a rest [service][1] that consume json from an Angular UI and also from other rest clients. The data based on a complex structure of entities ~50 that are stored in a database with ~50 tables. The problem are the optional OneToOne relations, because Angular send the optional objects as empty definitions like ``"car": {},``. The spring data repository saves them as empty entries and I got a Json response like ``"car": {"id": 545234, "version": 0}`` back. I found no Jackson annotation to ignore empty objects, only empty or null properties. 

The Employee Entity has the following form:

    @Entity
    public class Employee {
      @Id 
      @GeneratedValue
      private Long id;
      
      @Version
      private Long version;

      private String name;

      @OneToOne(cascade = CascadeType.ALL)
      @JoinColumn(name = "car_id")
      @JsonManagedReference
      private Car car;

      .
      .   Desk, getters and setters
      . 
    }
    
and the other side of the OneToOne Reference 

    @Entity
    public class Car{
      @Id
      @GeneratedValue
      private Long id;

      @Version
      private Long version;

      private String name;

      @OneToOne(fetch = FetchType.LAZY, mappedBy = "employee")
      @JsonBackReference
      private Employee employee;


      .
      .   getters and setters
      . 
    }

For example, I send this to my service as post operation

    {
      "name": "ACME",
          .
          .
          .
      "employee": {
        "name": "worker 1",
        "car": {},
        "desk": {
          floor: 3,
          number: 4,
          phone: 444
        }
          .
          .
          .
      },
      "addresses": [],
      "building": {},
          .
          .
          .
    }

and I got as response the saved data

    {
      "id": 34534,
      "version": 0,
      "name": "ACME",
          .
          .
          .
      "employee": {
        "id": 34535,
        "version":0,
        "name": "worker 1",
        "car": {"id": 34536, "version": 0},
        "desk": {
          "id": 34538,
          "version":0,
          "floor": 3,
          "number": 4,
          "phone": 444
        }
          .
          .
          .
      },
      "addresses": [],
      "building": {"id": 34539, "version": 0},
          .
          .
          .
    }

As seen in the response I got empty table rows with an id, a version, many null values and empty strings, because when I save (persist) the main deserialized company class, the other entity are also saved, because the are annotated as cascading.

I found many examples like [https://stackoverflow.com/questions/53234727/do-not-include-empty-object-to-jackson][2] , with a concrete pojo and a concrete deserializer that are working, but every entity needs his own Deserializer. This causes many work for the current entities and the new ones in the future (only the optional entities). 


I tried the folowing, I write a ``BeanDeserializerModifier`` and try to wrap an own deserializer over the standard beandeserializer:

        SimpleModule module = new SimpleModule();
        module.setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public List<BeanPropertyDefinition> updateProperties(DeserializationConfig config,
                                                                 BeanDescription beanDesc,
                                                                 List<BeanPropertyDefinition> propDefs) {
                logger.debug("update properties, beandesc: {}", beanDesc.getBeanClass().getSimpleName());
                return super.updateProperties(config, beanDesc, propDefs);
            }

            @Override
            public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config,
                                                          BeanDescription beanDesc,
                                                          JsonDeserializer<?> deserializer) {
                
                logger.debug("modify deserializer {}",beanDesc.getBeanClass().getSimpleName());
                // This fails:
               // return new DeserializationWrapper(deserializer, beanDesc);
                return deserializer; // This works, but it is the standard behavior
            }
        });

And here is the wrapper (and the mistake):

    public class DeserializationWrapper extends JsonDeserializer<Object> {
    private static final Logger logger = LoggerFactory.getLogger( DeserializationWrapper.class );

        private final JsonDeserializer<?> deserializer;
        private final BeanDescription beanDesc;

        public DeserializationWrapper(JsonDeserializer<?> deserializer, BeanDescription beanDesc) {
            this.deserializer = deserializer;
            this.beanDesc = beanDesc;
        }

        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            logger.debug("deserialize in wrapper {} ",beanDesc.getBeanClass().getSimpleName());
            final Object deserialized = deserializer.deserialize(p, ctxt);
            ObjectCodec codec = p.getCodec();
            JsonNode node = codec.readTree(p);
            
             // some logig that not work
             // here. The Idea is to detect with the json parser that the node is empty.
             // If it is empty I will return null here and not the deserialized pojo

            return deserialized;
        }

        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt, Object intoValue) throws IOException {
            logger.debug("deserializer - method 2");
            intoValue = deserializer.deserialize(p, ctxt);
            return intoValue;
        }

        @Override
        public boolean isCachable() {
            return deserializer.isCachable();
        }
      .
      .     I try to wrap the calls to the deserializer
      .

The Deserialization Wrapper does not work and crash after the first call with an exception ```com.fasterxml.jackson.databind.exc.MismatchedInputException: No _valueDeserializer assigned
 at [Source: (PushbackInputStream); line: 2, column: 11] (through reference chain: ... Company["name"])``` 

My question: is there a way to extend the behavior of the working standard deserializer in the way,
that the deserializer detect while parsing, that the current jsonNode is empty and return null 
instead the empty class instance? Perhaps my Idea is wrong and there is a completely other solution? 

Solving it on the Angular UI side is no option. We use Jackson 2.9.5.

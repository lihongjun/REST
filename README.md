# Overview

This is a **Spring RESTful Service secured with Spring Security** - it provides a Proof of Concept implementation of a REST service with Spring and Spring Security. 
It also provides a reference implementation in the following connected areas: 
- REST Discoverability and HATEOAS <br/> 
- Basic and Digest Authentication <br/> 
- support for Multiple Representations (on the same URIs) (JSON, XML) <br/> 
- a full REST based query language for advanced filtering of resources <br/> 
- sorting and pagination in REST <br/>
- Statelessness for REST with Spring <br/> 
- full integration testing suites at every layer: unit tests, integration tests for the DAO and Service layers, integration tests against the REST service <br/>


# REST API Documentation
[Link to the **API Documentation**](https://github.com/eugenp/REST/wiki/API-documentation "REST Security - API docs")


# Continuous Integration
![Built on Cloudbees](http://web-static-cloudfront.s3.amazonaws.com/images/badges/BuiltOnDEV.png "Built on Cloudbees")

- **CI server**: https://rest-security.ci.cloudbees.com/


# Technology Stack
The project uses the following technologies: <br/>
- **web/REST**: [Spring](http://www.springsource.org/) 3.1.x <br/>
- **marshalling**: [Jackson](https://github.com/FasterXML/jackson-databind) 2.x (for JSON) and [XStream](http://xstream.codehaus.org/) (for XML) <br/>
- **persistence**: [Spring Data JPA](http://www.springsource.org/spring-data/jpa) and [Hibernate](http://www.hibernate.org/) 4.1.x <br/>
- **persistence providers**: h2, MySQL
- **testing**: [junit](http://www.junit.org/), [hamcrest](http://code.google.com/p/hamcrest/), [mockito](http://code.google.com/p/mockito/), [rest-assured](http://code.google.com/p/rest-assured/) <br/>


# THE PERSISTENCE LAYER (technical notes)
### The DAO layer
- to create a new DAO, only the interface needs to be created; **Spring Data JPA** will generates the DAO implementation automatically
- the DAO interface MUST extend the Spring Data managed interface: _JpaRepository_ (with the correct parametrization)
- the DAO layer is **aware** of the persistence engine it uses; this information MUST be encoded in the name; for **example**: _IPrincipalJpaDAO_ for JPA instead of just _IPrincipalDAO_


### The Service layer
- all Service interfaces MUST extend the _IService_ interface (with the proper parametrization)
- all Service implementations MUST extend the _AbstractService_ abstract class (with the proper parametrization)
- extending _AbstractService_ and _IService_ enables a base of consistent and common functionality across services
- the Service artifacts MUST be annotated with the _@Service_ annotation

- the Service layer is **not aware** of the persistence engine it uses (indirectly); if the persistence engine will change, the DAO artifacts will change, and the service will not


# THE WEB LAYER (technical notes)
### The Controller layer
- the Controller layer MUST only use the Service layer directly (never the DAO layer)
- the Controller layer SHOULD not implement any interface
- the Controller layer MUST extend _AbstractController_ (with the proper parametrization)
- the Controller artifacts MUST be annotated with the _@Controller_ annotation


## Transaction Management and Configuration (technical notes)
- the Service layer is the transaction owner (and is annotated with _@Transactional_)
- the default transaction semantics are: propagation REQUIRED, default isolation, rollback on runtime exceptions
- **NOTE**: the transactional semantics MAY be subject to change


# Eclipse
- see the [Eclipse wiki page](https://github.com/eugenp/REST/wiki/Eclipse:-Setup-and-Configuration) of this project


# Roadmap
- POC for dealing with the lost update problem (using ETAG) - see: http://www.w3.org/1999/04/Editing/
- use the rel="edit" link relation (defined http://www.iana.org/assignments/link-relations/link-relations.xml) to return an edit form for a Resource
- further invesitage form usage (see: http://codebetter.com/glennblock/2011/05/09/hypermedia-and-forms/)
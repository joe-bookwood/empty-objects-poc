package de.bitc.emptyobjectspoc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class EmptyObjectsPocApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;


	@Test
	public void postCompanyTest() throws Exception {
		String json = "{\"name\" : \"ACME\", \"employee\" : { \"name \" : \"worker1\", \"car\" : {}}}";
		String baseUrl = "http://localhost:" + port + "/company";
		URI uri = new URI(baseUrl);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<String> request = new HttpEntity<String>(json, headers);

		String jsonResult = restTemplate.postForObject(uri, request, String.class);
		assertEquals("{name}", jsonResult);
	}
}

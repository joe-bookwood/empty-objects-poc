package de.bitc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import de.bitc.model.Company;
import de.bitc.repository.CompanyRepository;

@RestController
public class CompanyController {

	@Autowired
	private CompanyRepository repository;

	@PostMapping(value = "/company", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Company> saveCompany(@RequestBody Company company) {

		return new ResponseEntity<Company>(repository.save(company), HttpStatus.CREATED);
	}
}

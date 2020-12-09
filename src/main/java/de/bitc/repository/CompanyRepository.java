package de.bitc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.bitc.model.Company;


@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

//	Company findById(long id);
//
//	List<Company> findByName(String name);

}

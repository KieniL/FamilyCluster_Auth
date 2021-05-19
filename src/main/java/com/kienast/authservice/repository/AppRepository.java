package com.kienast.authservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kienast.authservice.model.App;

@Repository
public interface AppRepository extends JpaRepository<App, Long> {

}

package com.kienast.authservice.repository;

import com.kienast.authservice.model.User2App;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface User2AppRepository extends JpaRepository<User2App, Long> {

}

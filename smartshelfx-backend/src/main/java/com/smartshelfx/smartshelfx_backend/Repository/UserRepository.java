

package com.smartshelfx.smartshelfx_backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartshelfx.smartshelfx_backend.model.User;

import java.util.List;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);


    List<User> findAllByRole(String role);

    List<User> findAll();


     List<User> findById(int id);

}
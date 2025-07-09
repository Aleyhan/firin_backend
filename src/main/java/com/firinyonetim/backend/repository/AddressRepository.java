package com.firinyonetim.backend.repository;

import com.firinyonetim.backend.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface AddressRepository extends JpaRepository<Address, Long> {
}

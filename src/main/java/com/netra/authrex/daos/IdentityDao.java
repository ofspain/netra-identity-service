package com.netra.authrex.daos;

import com.netra.commons.models.Identity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class IdentityDao {

    public Optional<Identity> loadIdentityByUsername(String username){
        return Optional.empty();
    }
}

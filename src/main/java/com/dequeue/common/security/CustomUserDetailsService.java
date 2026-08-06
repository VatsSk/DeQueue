package com.dequeue.common.security;

import com.dequeue.staff.entity.Staff;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MongoTemplate mongoTemplate;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Query query = new Query(Criteria.where("email").is(email));
        Staff staff = mongoTemplate.findOne(query, Staff.class);
        if (staff == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }
        return UserPrincipal.create(staff);
    }

    public UserDetails loadUserById(String id) {
        Staff staff = mongoTemplate.findById(id, Staff.class);
        if (staff == null) {
            throw new UsernameNotFoundException("User not found with id: " + id);
        }
        return UserPrincipal.create(staff);
    }
}

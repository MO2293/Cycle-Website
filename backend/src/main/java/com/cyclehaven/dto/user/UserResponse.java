package com.cyclehaven.dto.user;

import com.cyclehaven.entity.Role;
import com.cyclehaven.entity.User;

/**
 * What the API reveals about a user.
 *
 * <p>Note what is absent: {@code passwordHash}. The original app put the whole
 * account — password included — into the HttpSession and read fields from it in
 * JSPs. Returning the entity directly here would serialise the hash into every
 * profile response. A separate response type makes that leak impossible rather
 * than merely unlikely.
 */
public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        String phone,
        String address,
        String city,
        String province,
        String country,
        String cardLast4) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getPhone(),
                user.getAddress(),
                user.getCity(),
                user.getProvince(),
                user.getCountry(),
                user.getCardLast4());
    }
}

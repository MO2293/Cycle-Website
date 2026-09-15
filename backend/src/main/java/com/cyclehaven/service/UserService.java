package com.cyclehaven.service;

import com.cyclehaven.dto.user.AdminUpdateUserRequest;
import com.cyclehaven.dto.user.ChangePasswordRequest;
import com.cyclehaven.dto.user.UpdateProfileRequest;
import com.cyclehaven.dto.user.UserResponse;
import com.cyclehaven.entity.Role;
import com.cyclehaven.entity.User;
import com.cyclehaven.exception.BadRequestException;
import com.cyclehaven.exception.NotFoundException;
import com.cyclehaven.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profile self-service and admin user management.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        user.setName(request.name().trim());
        user.setPhone(request.phone());
        user.setAddress(request.address());
        user.setCity(request.city());
        user.setProvince(request.province());
        user.setCountry(request.country());
        return UserResponse.from(userRepository.save(user));
    }

    /**
     * Changes the caller's own password, after verifying the current one.
     *
     * <p>Unlike the original's profile update — which accepted a password field
     * and silently discarded it — this actually rehashes and stores the new value.
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from the current one");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.info("Password changed for user id={}", userId);
    }

    // ---------- Admin ----------

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(int page, int size) {
        return userRepository
                .findAll(PageRequest.of(
                        Math.max(page, 0),
                        Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.ASC, "name")))
                .map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        return UserResponse.from(findUser(id));
    }

    /**
     * Admin edit of another user, including their role.
     *
     * @param actingAdminId the admin performing the change, used to stop them
     *                      removing their own admin rights and locking everyone
     *                      out of the admin area.
     */
    @Transactional
    public UserResponse adminUpdateUser(Long id, AdminUpdateUserRequest request, Long actingAdminId) {
        User user = findUser(id);

        if (user.getId().equals(actingAdminId)
                && user.getRole() == Role.ADMIN
                && request.role() != Role.ADMIN) {
            // Without this, an admin can demote themselves and — if they are the
            // only admin — leave the store with no way back into the admin area
            // short of editing the database by hand.
            throw new BadRequestException("You cannot remove your own admin role");
        }

        user.setName(request.name().trim());
        user.setPhone(request.phone());
        user.setAddress(request.address());
        user.setCity(request.city());
        user.setProvince(request.province());
        user.setCountry(request.country());

        if (user.getRole() != request.role()) {
            log.info("Admin id={} changed role of user id={} from {} to {}",
                    actingAdminId, id, user.getRole(), request.role());
            user.setRole(request.role());
        }

        return UserResponse.from(userRepository.save(user));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("User", id));
    }
}

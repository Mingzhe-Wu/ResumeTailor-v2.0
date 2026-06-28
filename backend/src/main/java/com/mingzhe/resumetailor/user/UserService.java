package com.mingzhe.resumetailor.user;

import com.mingzhe.resumetailor.auth.UserRequestDTO;
import com.mingzhe.resumetailor.exceptions.BadRequestException;
import com.mingzhe.resumetailor.exceptions.ResourceNotFoundException;
import com.mingzhe.resumetailor.profile.Profile;
import com.mingzhe.resumetailor.profile.ProfileMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Business logic for validating and managing User records.
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ProfileMapper profileMapper;

    private static final String DEFAULT_FULL_NAME = "Anonymous User";

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder, ProfileMapper profileMapper) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.profileMapper = profileMapper;
    }

    public UserResponseDTO register(UserRequestDTO request) {
        return createUserWithProfile(request.getEmail(), request.getPassword(), request.getFullName());
    }

    public UserResponseDTO createUser(CreateUserDTO request) {
        return createUserWithProfile(request.getEmail(), request.getPassword(), request.getFullName());
    }

    private UserResponseDTO createUserWithProfile(String email, String password, String fullName) {
        User existingUser = userMapper.findByEmail(email);
        if (existingUser != null) {
            throw new BadRequestException("User already exists with this email");
        }

        String resolvedFullName = resolveFullName(fullName);

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));

        userMapper.insert(user);
        createProfileIfMissing(user.getId(), resolvedFullName);

        UserResponseDTO response = new UserResponseDTO();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(resolvedFullName);
        return response;
    }

    private void createProfileIfMissing(Long userId, String fullName) {
        if (profileMapper.findByUserId(userId) != null) {
            return;
        }

        Profile profile = new Profile();
        profile.setUserId(userId);
        profile.setFullName(fullName);
        profileMapper.insert(profile);
    }

    private String resolveFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return DEFAULT_FULL_NAME;
        }
        return fullName.trim();
    }

    public User fetchUserById(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        return user;
    }

    public User fetchUserByEmail(String email) {
        User user = userMapper.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }
        return user;
    }

    public User updateUser(Long id, UpdateUserDTO request) {
        User existingUser = userMapper.findById(id);
        if (existingUser == null) {
            throw new ResourceNotFoundException("User not found");
        }

        if (request.getEmail() != null) {
            User existingEmailUser = userMapper.findByEmail(request.getEmail());
            if (existingEmailUser != null && !existingEmailUser.getId().equals(id)) {
                throw new BadRequestException("User already exists with this email");
            }
        }

        User update = new User();
        update.setId(id);
        update.setEmail(request.getEmail());
        if (request.getPassword() != null) {
            update.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userMapper.updateById(update);
        return userMapper.findById(id);
    }

    public void deleteUser(Long id) {
        User existingUser = userMapper.findById(id);
        if (existingUser == null) {
            throw new ResourceNotFoundException("User not found");
        }

        userMapper.deleteById(id);
    }

}

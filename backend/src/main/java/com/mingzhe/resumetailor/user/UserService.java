package com.mingzhe.resumetailor.user;

import com.mingzhe.resumetailor.auth.UserRequestDTO;
import com.mingzhe.resumetailor.exceptions.BadRequestException;
import com.mingzhe.resumetailor.exceptions.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Business logic for validating and managing User records.
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponseDTO register(UserRequestDTO request) {
        User user = createUserEntity(request.getEmail(), request.getPassword(), request.getDisplayName());

        UserResponseDTO response = new UserResponseDTO();
        response.setId(user.getId());
        response.setEmail(user.getEmail());

        return response;
    }

    public User createUser(CreateUserDTO request) {
        return createUserEntity(request.getEmail(), request.getPassword(), request.getDisplayName());
    }

    private User createUserEntity(String email, String password, String displayName) {
        User existingUser = userMapper.findByEmail(email);
        if (existingUser != null) {
            throw new BadRequestException("User already exists with this email");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setDisplayName(displayName);

        userMapper.insert(user);
        return user;
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
        update.setDisplayName(request.getDisplayName());

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

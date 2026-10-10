package com.smartoffice.service;

import com.smartoffice.dto.user.UserCreateRequest;
import com.smartoffice.dto.user.UserResponse;
import com.smartoffice.dto.user.UserUpdateRequest;

public interface UserService {

    UserResponse createUser(UserCreateRequest request);

    UserResponse getUserById(Long id);

    UserResponse getUserByUsername(String username);

    UserResponse updateUser(Long id, UserUpdateRequest request);

    void deleteUser(Long id);
}

package com.gymtracker.application;

import com.gymtracker.domain.model.Gender;
import com.gymtracker.domain.model.User;
import com.gymtracker.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User createUser(String name, String email, String hashedPassword, LocalDate dateOfBirth, Integer heightCm, Gender gender) {
        User user = new User(name, email, hashedPassword, dateOfBirth, heightCm, gender);
        return userRepository.save(user);
    }

    public User updateUser(UUID id, String name, Integer heightCm, Gender gender) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (name != null && !name.isBlank()) {
            user.updateName(name);
        }
        if (heightCm != null) {
            user.updateHeight(heightCm);
        }
        if (gender != null) {
            user.updateGender(gender);
        }

        return userRepository.save(user);
    }

    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found");
        }
        userRepository.deleteById(id);
    }
}
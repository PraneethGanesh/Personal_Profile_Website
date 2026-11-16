package com.portfolio.contact;

import jakarta.validation.constraints.*;

public record ContactRequest(
    @NotBlank @Size(max = 120) @Pattern(regexp = "[^\\r\\n]*") String name,
    @NotBlank @Email @Size(max = 254) @Pattern(regexp = "[^\\r\\n]*") String email,
    @NotBlank @Size(max = 5000) String message
) {
    public ContactRequest {
        name = name == null ? null : name.strip();
        email = email == null ? null : email.strip();
        message = message == null ? null : message.strip();
    }
}

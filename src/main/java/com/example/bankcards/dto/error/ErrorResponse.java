package com.example.bankcards.dto.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse implements Serializable {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

}

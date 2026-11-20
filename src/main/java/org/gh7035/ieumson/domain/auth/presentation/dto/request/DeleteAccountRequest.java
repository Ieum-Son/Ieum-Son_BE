package org.gh7035.ieumson.domain.auth.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record DeleteAccountRequest(

        @NotNull
        String password
        )
{ }

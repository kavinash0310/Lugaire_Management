package com.ecommerce.commerceapi.foundation.error;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String error, String path) {}

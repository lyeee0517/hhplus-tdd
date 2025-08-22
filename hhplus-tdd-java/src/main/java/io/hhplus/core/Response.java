package io.hhplus.core;

// 성공적 응답
public record Response(
        String code,
        String message
) {
}

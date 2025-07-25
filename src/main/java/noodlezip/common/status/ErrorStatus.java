package noodlezip.common.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import noodlezip.common.code.BaseErrorCode;
import noodlezip.common.dto.ErrorReasonDto;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {
    // Global Errors
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "서버 내부 오류가 발생했습니다. 관리자에게 문의하세요."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST, "400", "잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "401", "인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "403", "접근이 금지된 요청입니다."),
    _METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "405", "허용되지 않는 HTTP 메서드입니다."),
    _UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "415", "지원되지 않는 미디어 타입입니다."),
    _NOT_FOUND_HANDLER(HttpStatus.NOT_FOUND, "404", "요청 경로에 대한 핸들러를 찾을 수 없습니다."),

    // Redis Errors
    _FAILED_SAVE_REDIS(HttpStatus.INTERNAL_SERVER_ERROR, "REDIS-001", "Redis 저장에 실패했습니다."),
    _REDIS_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "REDIS-002", "Redis에서 토큰을 찾을 수 없습니다."),

    // JSON Serialization Errors
    _FAILED_SERIALIZING_JSON(HttpStatus.INTERNAL_SERVER_ERROR, "JSON-001", "JSON 직렬화에 실패했습니다."),
    _FAILED_DESERIALIZING_JSON(HttpStatus.INTERNAL_SERVER_ERROR, "JSON-002", "JSON 역직렬화에 실패했습니다."),

    // Swagger Errors
    _FAILED_TRANSLATE_SWAGGER(HttpStatus.INTERNAL_SERVER_ERROR, "SWAGGER-001", "Rest Docs 기반 Swagger 변환에 실패했습니다."),

    // Common Errors
    _DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON-001", "데이터를 찾을 수 없습니다."),
    _INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "COMMON-002", "잘못된 입력 값입니다."),
    _RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON-003", "요청한 리소스를 찾을 수 없습니다."),

    // File Errors
    _FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE-001", "요청하신 파일을 찾을 수 없습니다."),
    _FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE-002", "파일 업로드에 실패했습니다."),
    _FILE_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE-003", "파일 다운로드에 실패했습니다."),
    _FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE-004", "파일 삭제에 실패했습니다."),
    _UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "FILE-005", "지원하지 않는 파일 형식입니다."),
    _FILE_SIZE_LIMIT_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "FILE-006", "파일 크기가 허용된 한도를 초과했습니다."),
    _INVALID_FILE_NAME(HttpStatus.BAD_REQUEST, "FILE-007", "파일 이름이 유효하지 않습니다."),
     _STORAGE_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "FILE-008", "파일 저장 서비스에 문제가 발생했습니다."),
    _FILE_REQUIRED(HttpStatus.BAD_REQUEST, "FILE-009", "대표 이미지는 필수입니다."),

    // Duplicated menuName
    _DUPLICATE_MENU_NAME(HttpStatus.BAD_REQUEST, "MENU_001", "동일한 메뉴명이 이미 존재합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDto getReason() {
        return ErrorReasonDto.builder()
                .isSuccess(false)
                .code(code)
                .message(message)
                .build();
    }

    @Override
    public ErrorReasonDto getReasonHttpStatus() {
        return ErrorReasonDto.builder()
                .isSuccess(false)
                .httpStatus(httpStatus)
                .code(code)
                .message(message)
                .build();
    }
}